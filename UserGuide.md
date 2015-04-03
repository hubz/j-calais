# j-calais -- User Guide

## Usage ##

### Setup the client ###

```
    CalaisClient client = new CalaisRestClient("OpenCalais API key");
```

### Analyze texts ###

You can analyze a text fragment from a `String`:

```
    CalaisResponse response = client.analyze("Text to analyze");
```

From a `URL`:

```
    CalaisResponse response = client.analyze(new URL("http://example.com/sample.html"));
```

Or from any `Readable`

```
    CalaisResponse response = client.analyze(new FileReader("sample.txt"));
```

### Configure options ###

You can configure OpenCalais [input parameters](http://www.opencalais.com/documentation/calais-web-service-api/forming-api-calls/input-parameters) using `CalaisConfig` class

```
    CalaisConfig config = new CalaisConfig();
    config.set(CalaisConfig.UserParam.EXTERNAL_ID, "User generated ID");
    config.set(CalaisConfig.ProcessingParam.CALCULATE_RELEVANCE_SCORE, "true");
```

And call any of the `analyze` methods

```
    CalaisResponse response = client.analyze("Text to analyze", config);
```

### Work with the results ###

Display recognized entities:

```
    for (CalaisObject entity : response.getEntities()) {
      System.out.println(entity.getField("_type") + ":" 
                         + entity.getField("name"));
    }
```


Display topics:

```
    for (CalaisObject topic : response.getTopics()) {
      System.out.println(topic.getField("categoryName"));
    }
```


Display Social Tags:

```
    for (CalaisObject tags : response.getSocialTags()){
      System.out.println(tags.getField("_typeGroup") + ":" 
                         + tags.getField("name"));
    }
```

### Connection options ###
Set connection options:


```
    CalaisConfig config = new CalaisConfig();
    config.set(CalaisConfig.ConnParam.CONNECT_TIMEOUT, 1000);
    config.set(CalaisConfig.ConnParam.READ_TIMEOUT, 1000);
```


## Running ##

To integrate j-calais in your source code use the following Maven artifact:

```
    <dependency>
      <groupId>mx.bigdata.jcalais</groupId>
      <artifactId>j-calais</artifactId>
      <version>1.0</version>
    </dependency>
```

Or you can add each of the dependencies independently to the CLASSPATH.

  * Run `mvn dependency:copy-dependencies`, to copy all the dependencies to the the `target/dependency/` directory, then add them to your CLASSPATH.