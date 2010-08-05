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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

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

public final class CalaisRestClient implements CalaisClient {
  
  private static final String SUBMITTER = "j-calais client v 0.1";

  private static final String RESOURCE = "http://api.opencalais.com/enlighten/rest/";
  
  private static final String TYPE = "application/x-www-form-urlencoded";

  private static final int MAX_CONTENT_SIZE = 100000;
  
  private static final String PARAMS_HEADER = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">";

  private static final String PARAMS_FOOTER = "</c:params>";

  private static final ImmutableMap<String, String> PROCESSING_DEFAULTS = 
       new ImmutableMap.Builder<String, String>()
    .put("contentType", "TEXT/RAW")
    .put("outputFormat", "application/json")
  //.put("reltagBaseURL", null)
    .put("calculateRelevanceScore", "true")
  //.put("enableMetadataType", null)
    .put("docRDFaccessible", "true")
    .build();
   
  private final Map<String, String> userDirectives = Maps.newHashMap();

  {
    userDirectives.put("allowDistribution", "false"); 
    userDirectives.put("allowSearch", "false"); 
    userDirectives.put("externalID", UUID.randomUUID().toString());
    userDirectives.put("submitter", SUBMITTER);
  }
  
  private final Map<String, String> externalMetadata = Maps.newHashMap();

  private final Client client;

  private final String apiKey;

  public CalaisRestClient(String apiKey) {
    this.apiKey= apiKey;
    ClientConfig config = new DefaultClientConfig();
    config.getClasses().add(JacksonJsonProvider.class);
    this.client = Client.create(config);
  }

  public CalaisResponse analyze(Reader reader) throws IOException {
    return analyze(CharStreams.toString(reader));
  }

  private static final class Prueba {
  
  }

  public CalaisResponse analyze(String content) throws IOException {
    if (Strings.isNullOrEmpty(content) || content.length() > MAX_CONTENT_SIZE) {
      throw new IllegalArgumentException("Invalid content, either empty or "
                                         + "exceeds maximum allowed size");
    }
    WebResource webResource = client.resource(RESOURCE);
    MultivaluedMap formData = new MultivaluedMapImpl();
    formData.add("licenseID", apiKey);
    formData.add("content", content);
    formData.add("paramsXML", getParamsXml());
    Map<String, Object> map = webResource.type(TYPE)
      .accept(MediaType.APPLICATION_JSON_TYPE)
      .post(Map.class, formData);
    return processResponse(map);
  }
  
  private CalaisResponse processResponse(Map<String, Object> map) {
    Map<String, Object> doc = (Map<String, Object>) map.remove("doc");
    final CalaisObject info = extractObject(doc, "info");
    final CalaisObject meta = extractObject(doc, "meta");
    Multimap<String, CalaisObject> hierarchy = createHierarchy(map);
    final Iterable<CalaisObject> topics = Iterables
      .unmodifiableIterable(hierarchy.get("topics"));
    final Iterable<CalaisObject> entities = Iterables
      .unmodifiableIterable(hierarchy.get("entities"));
    final Iterable<CalaisObject> relations = Iterables
      .unmodifiableIterable(hierarchy.get("relations"));
    return new CalaisResponse() {
      public CalaisObject getInfo() { return info; }
      
      public CalaisObject getMeta() { return meta; }
      
      public Iterable<CalaisObject> getTopics() { return topics; }
           
      public Iterable<CalaisObject> getEntities() { return entities; }
      
      public Iterable<CalaisObject> getRelations() { return relations; }
    };
  }

  private CalaisObject extractObject(Map<String, Object> map, String key) {
    return new MappedCalaisObject((Map<String, Object>) map.remove(key));
  }
  
  private final static class MappedCalaisObject 
    implements CalaisObject {
    
    private final Map<String, Object> map;

    private MappedCalaisObject(Map<String, Object> map) {
      this.map = ImmutableMap.copyOf(map);
    }

    public String getField(String field) {
      Object o = map.get(field);
      return (o == null) ? null : o.toString();
    }
    
    public Iterable getList(String field) {
      Object o = map.get(field);
      return (o instanceof Iterable) 
        ? Iterables.unmodifiableIterable((Iterable) o) : null;
    }
    
  }

  private void resolveReferences(Map<String, Object> root) {
    for (Object o : root.values()) {
      Map<String, Object> map = (Map<String, Object>) o;
      for (Map.Entry<String, Object> me : map.entrySet()) {
        String key = me.getKey();
        Object o2 = me.getValue();
        if (o2 instanceof String) {
          String value = (String) o2;
          if (value.startsWith("http://") && root.containsKey(value)) {
            map.put(key, root.get(value));
          }
        }
      }
    }
  }

  private Multimap<String, CalaisObject> createHierarchy(
                                                Map<String, Object> root) {
    Multimap<String, CalaisObject> result = ArrayListMultimap.create();
    for (Map.Entry<String, Object> me : root.entrySet()) {
      Map<String, Object> map = (Map<String, Object>) me.getValue();
      String group = (String) map.get("_typeGroup");
      result.put(group, new MappedCalaisObject(map));
    }
    return result;
  }

  private String getParamsXml() {
    Map<String, String> processingDirectives = 
      Maps.newHashMap(PROCESSING_DEFAULTS);
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