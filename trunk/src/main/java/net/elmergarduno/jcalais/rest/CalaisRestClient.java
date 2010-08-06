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

package net.elmergarduno.jcalais.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
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

import net.elmergarduno.jcalais.CalaisClient;
import net.elmergarduno.jcalais.CalaisConfig;
import net.elmergarduno.jcalais.CalaisConfig.ProcessingParam;
import net.elmergarduno.jcalais.CalaisConfig.UserParam;
import net.elmergarduno.jcalais.CalaisObject;
import net.elmergarduno.jcalais.CalaisResponse;

public final class CalaisRestClient implements CalaisClient {

  private static final String RESOURCE = "http://api.opencalais.com/enlighten/rest/";
  
  private static final String TYPE = "application/x-www-form-urlencoded";

  private static final int MAX_CONTENT_SIZE = 100000;

  private final Client client;

  private final String apiKey;

  public CalaisRestClient(String apiKey) {
    this.apiKey= apiKey;
    ClientConfig config = new DefaultClientConfig();
    config.getClasses().add(JacksonJsonProvider.class);
    this.client = Client.create(config);
  }

  public CalaisResponse analyze(URL url) throws IOException {
    return analyze(url, new CalaisConfig());
  }
  
  public CalaisResponse analyze(URL url, CalaisConfig config)
    throws IOException {
    config.set(UserParam.EXTERNAL_ID, url.toString());
    config.set(ProcessingParam.CONTENT_TYPE, "TEXT/HTML");
    return analyze(new InputStreamReader(url.openStream()), config);
  }


  public CalaisResponse analyze(Reader reader) throws IOException {
    return analyze(reader, new CalaisConfig());
  }

  public CalaisResponse analyze(Reader reader, CalaisConfig config)
    throws IOException {
    return analyze(CharStreams.toString(reader), config);
  }

  public CalaisResponse analyze(String content) throws IOException {
    return analyze(content, new CalaisConfig());
  }
    
  public CalaisResponse analyze(String content, CalaisConfig config) 
    throws IOException {
    if (Strings.isNullOrEmpty(content) || content.length() > MAX_CONTENT_SIZE) {
      throw new IllegalArgumentException("Invalid content, either empty or "
                                         + "exceeds maximum allowed size");
    }
    WebResource webResource = client.resource(RESOURCE);
    MultivaluedMap formData = new MultivaluedMapImpl();
    formData.add("licenseID", apiKey);
    formData.add("content", content);
    formData.add("paramsXML", config.getParamsXml());
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

    public String toString() {
      return map.toString();
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
      map.put("_uri", me.getKey());
      String group = (String) map.get("_typeGroup");
      result.put(group, new MappedCalaisObject(map));
    }
    return result;
  }

}