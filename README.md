### MAC Web Crawler
![Maven Central](https://img.shields.io/maven-central/v/cloud.anypoint/mule-web-crawler-connector)

**MAC Web Crawler** is a MuleSoft custom connector to provide web crawling capabilities to extract data from web pages subsequently based on the structure of the website.


### Installation (using Cloud.Anypoint Dependency)

```xml
<dependency>
   <groupId>cloud.anypoint</groupId>
   <artifactId>mule-web-crawler-connector</artifactId>
   <version>0.1.0</version>
   <classifier>mule-plugin</classifier>
</dependency>
```

### Installation (building locally)

To use this connector, first [build and install](https://mac-project.ai/docs/mulechain-ai/getting-started) the connector into your local maven repository.
Then add the following dependency to your application's `pom.xml`:


```xml
<dependency>
    <groupId>com.mule.mulechain</groupId>
    <artifactId>mac-web-crawler</artifactId>
    <version>0.1.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Installation into private Anypoint Exchange

You can also make this connector available as an asset in your Anyooint Exchange.

This process will require you to build the connector as above, but additionally you will need
to make some changes to the `pom.xml`.  For this reason, we recommend you fork the repository.

Then, follow the MuleSoft [documentation](https://docs.mulesoft.com/exchange/to-publish-assets-maven) to modify and publish the asset.

