# <img src="icon/icon.svg" width="6%" alt="banner"> MuleSoft  WebCrawler Connector
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-8-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mulesoft-ai-chain-project/mule4-webcrawler-connector)](https://central.sonatype.com/artifact/io.github.mulesoft-ai-chain-project/mule4-webcrawler-connector/overview)

# <img src="https://raw.githubusercontent.com/MuleSoft-AI-Chain-Project/.github/main/profile/assets/mulechain-project-logo.png" width="6%" alt="banner">   [MuleSoft AI Chain (MAC) Project](https://mac-project.ai/docs/)

# <img src="icon/icon.svg" width="6%" alt="banner"> MAC Web Crawler

**MuleSoft WebCrawler** provides web crawling capabilities to extract data from web pages subsequently based on the structure of the website.

## Requirements

- The **maximum** supported version for Java SDK is **JDK 17**. 
- You can use JDK 17 only for running your application.
- Compilation with Java SDK must be done with JDK 11.

## Configuration and Deployment

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

### Deploying to CloudHub

In order for dynamic content retrieval to work in CloudHub based deployments, you will need
to set the `cloudhub.deployment` property to `true`.  

This can be done either via an application property in Runtime Manager, or in your CloudHub deployment
configuration in your `pom.xml`.

This property will allow the installation of Chrome at runtime into your CloudHub 1.0 worker VM, 
or your CloudHub 2.0 container, along with the necessary dependencies.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/tbolis-at-mulesoft"><img src="https://avatars.githubusercontent.com/u/95849087?v=4?s=150" width="150px;" alt="Tommaso Bolis"/><br /><sub><b>Tommaso Bolis</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=tbolis-at-mulesoft" title="Code">💻</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=tbolis-at-mulesoft" title="Tests">⚠️</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/pulls?q=is%3Apr+reviewed-by%3Atbolis-at-mulesoft" title="Reviewed Pull Requests">👀</a> <a href="#platform-tbolis-at-mulesoft" title="Packaging/porting to new platform">📦</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=tbolis-at-mulesoft" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/shumonsharif"><img src="https://avatars.githubusercontent.com/u/13334073?v=4?s=150" width="150px;" alt="Shumon Sharif"/><br /><sub><b>Shumon Sharif</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=shumonsharif" title="Code">💻</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=shumonsharif" title="Tests">⚠️</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/pulls?q=is%3Apr+reviewed-by%3Ashumonsharif" title="Reviewed Pull Requests">👀</a> <a href="#platform-shumonsharif" title="Packaging/porting to new platform">📦</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=shumonsharif" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://www.linkedin.com/in/amir-khan-ak/"><img src="https://avatars.githubusercontent.com/u/86777111?v=4?s=150" width="150px;" alt="Amir Khan"/><br /><sub><b>Amir Khan</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=amirkhan-ak-sf" title="Code">💻</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=amirkhan-ak-sf" title="Tests">⚠️</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/pulls?q=is%3Apr+reviewed-by%3Aamirkhan-ak-sf" title="Reviewed Pull Requests">👀</a> <a href="#platform-amirkhan-ak-sf" title="Packaging/porting to new platform">📦</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=amirkhan-ak-sf" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/codedbyyogesh"><img src="https://avatars.githubusercontent.com/u/87764828?v=4?s=150" width="150px;" alt="Yogesh Mudaliar"/><br /><sub><b>Yogesh Mudaliar</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=codedbyyogesh" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="http://hoegg.software/"><img src="https://avatars.githubusercontent.com/u/44559?v=4?s=150" width="150px;" alt="Ryan Hoegg"/><br /><sub><b>Ryan Hoegg</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=rhoegg" title="Code">💻</a> <a href="#platform-rhoegg" title="Packaging/porting to new platform">📦</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/yogeshmudaliar"><img src="https://avatars.githubusercontent.com/u/33849871?v=4?s=150" width="150px;" alt="yogeshmudaliar"/><br /><sub><b>yogeshmudaliar</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=yogeshmudaliar" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/mboss37"><img src="https://avatars.githubusercontent.com/u/29606687?v=4?s=150" width="150px;" alt="Mihael Bosnjak"/><br /><sub><b>Mihael Bosnjak</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=mboss37" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/saicharan0610"><img src="https://avatars.githubusercontent.com/u/53638266?v=4?s=150" width="150px;" alt="saicharan0610"/><br /><sub><b>saicharan0610</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=saicharan0610" title="Code">💻</a></td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <td align="center" size="13px" colspan="7">
        <img src="https://raw.githubusercontent.com/all-contributors/all-contributors-cli/1b8533af435da9854653492b1327a23a4dbd0a10/assets/logo-small.svg">
          <a href="https://all-contributors.js.org/docs/en/bot/usage">Add your contributions</a>
        </img>
      </td>
    </tr>
  </tfoot>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
