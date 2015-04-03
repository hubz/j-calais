### j-calais a Java RESTful interface to the [OpenCalais](http://www.opencalais.com/) API ###

**`**` 02/27/2012 -- Artifacts for version 1.0 have been deployed to Maven Central repository `**`**

**`**` 05/16/2011 -- Package names have been updated on version 0.2.1 please use mx.bigdata.jcalais `**`**

**Usage:**

Setup the client, and analyze a text fragment:

```
    CalaisClient client = new CalaisRestClient("OpenCalais API key");
    CalaisResponse response = client.analyze("Prosecutors at the trial of former Liberian President Charles Taylor " 
           + " hope the testimony of supermodel Naomi Campbell " 
           + " will link Taylor to the trade in illegal conflict diamonds, "
           + " which they say he used to fund a bloody civil war in Sierra Leone.");
```

Display recognized entities:

```
    for (CalaisObject entity : response.getEntities()) {
      System.out.println(entity.getField("_type") + ":" 
                         + entity.getField("name"));
    }
```

```
Entities:
Person:Charles Taylor
Position:President
Person:Naomi Campbell
Country:Sierra Leone
NaturalFeature:Sierra Leone
Country:Liberia
```

Display topics:

```
    for (CalaisObject topic : response.getTopics()) {
      System.out.println(topic.getField("categoryName"));
    }
```

```
Topics:
Politics
War_Conflict
Law_Crime
```


Display Social Tags:

```
    for (CalaisObject tags : response.getSocialTags()){
      System.out.println(tags.getField("_typeGroup") + ":" 
                         + tags.getField("name"));
    }
```


**Maven:**

```
    <dependency>
      <groupId>mx.bigdata.jcalais</groupId>
      <artifactId>j-calais</artifactId>
      <version>1.0</version>
    </dependency>
```

**Dependencies:**

  * Jackson JAX-RS (http://wiki.fasterxml.com/JacksonHome)
  * Guava Libraries (http://code.google.com/p/guava-libraries/)

**Acknowledgements:**

  * To zack.hr and Isuru Haththotuwa for the Social Tags code
