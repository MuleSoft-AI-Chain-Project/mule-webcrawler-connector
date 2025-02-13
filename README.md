# <img src="icon/icon.svg" width="6%" alt="banner"> MuleSoft  WebCrawler Connector

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mulesoft-ai-chain-project/mule4-webcrawler-connector)](https://central.sonatype.com/artifact/io.github.mulesoft-ai-chain-project/mule4-webcrawler-connector/overview)

## <img src="https://raw.githubusercontent.com/MuleSoft-AI-Chain-Project/.github/main/profile/assets/mulechain-project-logo.png" width="6%" alt="banner">   [MuleSoft AI Chain (MAC) Project](https://mac-project.ai/docs/)

### <img src="icon/icon.svg" width="6%" alt="banner"> MAC Web Crawler

**MuleSoft WebCrawler** provides web crawling capabilities to extract data from web pages subsequently based on the structure of the website.

### Requirements

- The maximum supported version for Java SDK is JDK 17. You can use JDK 17 only for running your application.
- Compilation with Java SDK must be done with JDK 11.

### Installation (using maven central dependency)

```xml
<dependency>
   <groupId>io.github.mulesoft-ai-chain-project</groupId>
   <artifactId>mule4-webcrawler-connector</artifactId>
   <version>{version}</version>
   <classifier>mule-plugin</classifier>
</dependency>
```

### Installation (building locally)

To use this connector, first [build and install](https://mac-project.ai/docs/mac-webcrawler/getting-started) the connector into your local maven repository.
Then add the following dependency to your application's `pom.xml`:


```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule4-webcrawler-connector</artifactId>
    <version>{version}</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Installation into private Anypoint Exchange

You can also make this connector available as an asset in your Anypoint Exchange.

This process will require you to build the connector as above, but additionally you will need
to make some changes to the `pom.xml`.  For this reason, we recommend you fork the repository.

Then, follow the MuleSoft [documentation](https://docs.mulesoft.com/exchange/to-publish-assets-maven) to modify and publish the asset.

### Deploying to CloudHub (Cloudhub 2.0 NOT supported yet.)

In order for dynamic content retrieval to work in CloudHub based deployments, you will need
to set the `cloudhub.deployment` property to `true`.  

This can be done either via an application property in Runtime Manager, or in your CloudHub deployment
configuration in your `pom.xml`.

This property will allow the installation of Chrome at runtime into your CloudHub worker VM, along with necessary
dependencies.
