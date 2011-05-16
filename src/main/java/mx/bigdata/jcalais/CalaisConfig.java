/*
 *  Copyright 2010 Elmer Garduno
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Inspired by python-calais (http://code.google.com/p/python-calais/)
 */

package net.elmergarduno.jcalais;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

/**
 * No thread-safe, use one instance per request.
 */
public final class CalaisConfig {
  
  private static final String SUBMITTER = "j-calais client v 0.2";
  
  private static final String PARAMS_HEADER = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">";

  private static final String PARAMS_FOOTER = "</c:params>";

  public enum UserParam {
    /**
     * User-generated ID for the submission
     */
    EXTERNAL_ID("externalID"), 
      /**
       * Indicates whether the extracted metadata can be distributed
       * Valid values are: true, false
       */
      ALLOW_DISTRIBUTION("allowDistribution"), 
      /**
       * Indicates whether future searches can be performed on the extracted 
       *  metadata
       * Valid values are: true, false
       */
      ALLOW_SEARCH("allowSearch"), 
      /**
       * Identifier for the content submitter
       */
      SUBMITTER("submitter");

    private final String key;
    
    private UserParam(String key) {
      this.key = key;
    }
    
    private String getKey() {
      return key;
    }
  }

  public enum ProcessingParam {
    /**
     * Format of the input content (see details below)
     * Valid values are: "TEXT/XML", "TEXT/HTML", "TEXT/HTMLRAW", "TEXT/RAW"
     */
    CONTENT_TYPE("contentType"),
      /**
       * Base URL to be put in rel-tag microformats
       */ 
      RELTAG_BASE_URL("reltagBaseURL"),
      /**
       * Indicates whether output will include Generic Relation extractions 
       * (RDF) and/or SocialTags
       * Valid values are: "GenericRelations" - to enable Generic Relations;
       * "SocialTags"  - to enable Social Tags 
       * (could combine comma-separated options: "GenericRelations,SocialTags"
       */
      ENABLE_METADATA_TYPE("enableMetadataType"), 
      /**
       * Indicates whether the extracted metadata will include relevance score
       * for each unique entity
       */
      CALCULATE_RELEVANCE_SCORE("calculateRelevanceScore"), 
      /**
       * Indicates whether entire XML/RDF document is saved in the 
       * Calais Linked Data repository
       * Valid values are: true, false
       */
      DOC_RDFACCESSIBLE("docRDFaccessible");

    private final String key;
    
    private ProcessingParam(String key) {
      this.key = key;
    }
    
    private String getKey() {
      return key;
    }
  }

  private static final ImmutableMap<String, String> PROCESSING_DEFAULTS = 
       new ImmutableMap.Builder<String, String>()
    .put("contentType", "TEXT/RAW")
    .put("outputFormat", "application/json")
    .put("enableMetadataType","GenericRelations,SocialTags")
    .put("calculateRelevanceScore","true")
    .build();
    
  private static final ImmutableMap<String, String> USER_DEFAULTS = 
       new ImmutableMap.Builder<String, String>()
    .put("externalID", UUID.randomUUID().toString())
    .put("submitter", SUBMITTER)
    .build();

  private final Map<String, String> processingDirectives = 
    Maps.newHashMap(PROCESSING_DEFAULTS);
  
  private final Map<String, String> userDirectives = 
    Maps.newHashMap(USER_DEFAULTS);
  
  private final Map<String, String> externalMetadata = Maps.newHashMap();


  public void set(UserParam key, String value) {
    if (Strings.isNullOrEmpty(value)) {
      userDirectives.remove(value);
    } else {
      userDirectives.put(key.getKey(), value);
    }
  }

  public void set(ProcessingParam key, String value) {
    if (Strings.isNullOrEmpty(value)) {
      processingDirectives.remove(value);
    } else {
      processingDirectives.put(key.getKey(), value);
    }
  }

  public String getParamsXml() {
    StringBuilder sb = new StringBuilder(PARAMS_HEADER);
    addDirectives("processingDirectives", processingDirectives, sb);
    addDirectives("userDirectives", userDirectives, sb);
    addDirectives("externalMetadata", externalMetadata, sb);
    sb.append(PARAMS_FOOTER);
    return sb.toString();
  }

  private void addDirectives(String name, Map<String, String> map, 
                             StringBuilder sb) {
    sb.append("<c:");
    sb.append(name);
    Formatter formatter = new Formatter(sb);
    for (Map.Entry<String, String> me : map.entrySet()) {
      formatter.format(" c:%s=\"%s\"", me.getKey(), me.getValue());
    }
    sb.append("/>");
  }

}