# <img src="icon/icon.svg" width="6%" alt="banner"> MuleSoft  WebCrawler Connector
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-7-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mulesoft-ai-chain-project/mule4-webcrawler-connector)](https://central.sonatype.com/artifact/io.github.mulesoft-ai-chain-project/mule4-webcrawler-connector/overview)

# <img src="https://raw.githubusercontent.com/MuleSoft-AI-Chain-Project/.github/main/profile/assets/mulechain-project-logo.png" width="6%" alt="banner">   [MuleSoft AI Chain (MAC) Project](https://mac-project.ai/docs/)

# <img src="icon/icon.svg" width="6%" alt="banner"> MAC Web Crawler

**MuleSoft WebCrawler** provides web crawling capabilities to extract data from web pages based on the structure of the website. From `1.0.0` it ships a queue-driven, ObjectStore-backed Source (`crawl-website-source`) that emits one Mule event per fetched page, plus per-page operations for metadata / insight / sitemap / image / document / search use cases.

## Requirements

- **JDK 11 or 17** for both compilation and runtime.
- **Mule runtime 4.6.0** or newer.
- For the Remote WebDriver fetch path, a reachable W3C WebDriver endpoint (Selenium Grid 4, standalone ChromeDriver, BrowserStack, or a CloudHub-hosted remote-browser app). The connector itself does not bundle a browser — embedded Chrome support was removed in `1.0.0`.

## What's in the connector

- **`crawl-website-source` (Source)** — listens on a Mule VM queue, fetches each URL, emits the page content, enqueues discovered links up to the configured depth. Backed by an ObjectStore for visited-URL deduplication.
- **`get-sitemap` (Operation)** — synchronous batch crawl that returns an XML sitemap document.
- **`page-content`, `page-meta-tags`, `page-insights`, `page-download-image`, `page-download-document` (Operations)** — single-URL utilities. Take a URL per call; do not consult the queue.
- **`search-google` (Operation)** — Google search via the Serper.dev API. Requires a Serper API key from the caller.

The connector exposes two connection providers:

| Connection | DSL element | When to use |
|---|---|---|
| HTTP | `<ms-webcrawler:http-connection .../>` | Server-rendered pages (most documentation sites, static HTML, sitemaps). Fastest, FIPS-compatible, no external infra. |
| Remote WebDriver | `<ms-webcrawler:remote-webdriver-connection remoteUrl="..."/>` | JS-heavy / SPA / shadow-DOM sites. Drives a remote Chromium-family browser via the W3C WebDriver protocol. |

## Configuration and Deployment

### Maven dependency

```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule4-webcrawler-connector</artifactId>
    <version>{version}</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Minimal example

```xml
<ms-webcrawler:config name="crawlConfig"
                   url="https://docs.example.com"
                   queueName="my-crawl-queue">
    <ms-webcrawler:http-connection userAgent="MyApp/1.0"/>
</ms-webcrawler:config>

<flow name="crawlFlow">
    <ms-webcrawler:crawl-website-source config-ref="crawlConfig"
                                     outputFormat="HTML"
                                     maxDepth="2"
                                     restrictToPath="true"/>
    <!-- one event per fetched page lands here; pipe to S3, Data Cloud, etc. -->
    <logger level="INFO" message='#["fetched: " ++ attributes.url]'/>
</flow>
```

The config seeds its `url` onto `queueName` at startup; the Source drains the queue, fetches each page, extracts links, enqueues internal links up to `maxDepth`. `restrictToPath=true` keeps the crawl under the seed URL's path.

### Switching to Remote WebDriver

```xml
<ms-webcrawler:config name="crawlConfig"
                   url="https://docs.example.com"
                   queueName="my-crawl-queue">
    <ms-webcrawler:remote-webdriver-connection
        remoteUrl="https://my-remote-browser.example.com"
        userAgent="MyApp/1.0"/>
</ms-webcrawler:config>
```

The Remote WebDriver path supports `waitOnPageLoad`, `waitForXPath`, `extractShadowDom`, and `shadowHostXPath` page-load options. It also accepts a `blockedUrls` list (Chrome URL-pattern globs sent to the worker via the `sfRemote:blockedUrls` capability) — used by the CloudHub remote-browser worker to drop ad / tracker network requests at the wire layer.

## Debugging

Logging is via SLF4J. Tune via `log4j2.xml`:

```xml
<!-- Connector-internal events (queue activity, per-page fetch outcomes, session recovery) -->
<AsyncLogger name="org.mule.extension.webcrawler" level="DEBUG"/>

<!-- Full HTTP wire traffic for the HTTP fetch path. The Mule HttpService dumps request lines,
     headers, and bodies at DEBUG. Selenium W3C protocol traffic on the Remote WebDriver path is
     not surfaced through this logger; use the connector-internal logger above. -->
<AsyncLogger name="org.mule.service.http.impl.service.HttpMessageLogger" level="DEBUG"/>
```

## Migration from `0.x`

`1.0.0` reshaped the connector around streaming + remote browser:

- Embedded Chrome path **removed**. `<ms-webcrawler:webdriver-connection .../>` (in-process Chrome) replaced by `<ms-webcrawler:remote-webdriver-connection remoteUrl="..."/>`.
- `crawl-website-full-scan` and `crawl-website-streaming` operations **removed**. Migrate to `crawl-website-source` and pipe events into your downstream sink.
- `<ms-webcrawler:config>` requires `url` only when using `crawl-website-source` (operations-only configs can omit it). `queueName` is optional and defaults to `webcrawler-{configName}-queue` when not set.
- XML namespace prefix is `ms-webcrawler` (unchanged from master).
- `cloudhub.deployment` property no longer needed.

The `0.x` line will receive **security-only fixes for 6 months** after `1.0.0` ships. New features land on `1.x` only. Plan your migration.

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

The connector deploys cleanly to CloudHub 2.0 with no special properties.

For the **Remote WebDriver** fetch path, point `<ms-webcrawler:remote-webdriver-connection remoteUrl="..."/>` at any reachable W3C WebDriver endpoint — a Selenium Grid 4 deployment, a hosted vendor (BrowserStack / Sauce Labs), or a CloudHub-hosted remote-browser app provisioned for your workspace.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="25%"><a href="https://github.com/tbolis-at-mulesoft"><img src="https://avatars.githubusercontent.com/u/95849087?v=4?s=200" width="200px;" alt="Tommaso Bolis"/><br /><sub><b>Tommaso Bolis</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=tbolis-at-mulesoft" title="Code">💻</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=tbolis-at-mulesoft" title="Tests">⚠️</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/pulls?q=is%3Apr+reviewed-by%3Atbolis-at-mulesoft" title="Reviewed Pull Requests">👀</a> <a href="#platform-tbolis-at-mulesoft" title="Packaging/porting to new platform">📦</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=tbolis-at-mulesoft" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="25%"><a href="https://github.com/shumonsharif"><img src="https://avatars.githubusercontent.com/u/13334073?v=4?s=200" width="200px;" alt="Shumon Sharif"/><br /><sub><b>Shumon Sharif</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=shumonsharif" title="Code">💻</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=shumonsharif" title="Tests">⚠️</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/pulls?q=is%3Apr+reviewed-by%3Ashumonsharif" title="Reviewed Pull Requests">👀</a> <a href="#platform-shumonsharif" title="Packaging/porting to new platform">📦</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=shumonsharif" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="25%"><a href="https://www.linkedin.com/in/amir-khan-ak/"><img src="https://avatars.githubusercontent.com/u/86777111?v=4?s=200" width="200px;" alt="Amir Khan"/><br /><sub><b>Amir Khan</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=amirkhan-ak-sf" title="Code">💻</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=amirkhan-ak-sf" title="Tests">⚠️</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/pulls?q=is%3Apr+reviewed-by%3Aamirkhan-ak-sf" title="Reviewed Pull Requests">👀</a> <a href="#platform-amirkhan-ak-sf" title="Packaging/porting to new platform">📦</a> <a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=amirkhan-ak-sf" title="Documentation">📖</a></td>
      <td align="center" valign="top" width="25%"><a href="https://github.com/codedbyyogesh"><img src="https://avatars.githubusercontent.com/u/87764828?v=4?s=200" width="200px;" alt="Yogesh Mudaliar"/><br /><sub><b>Yogesh Mudaliar</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=codedbyyogesh" title="Code">💻</a></td>
    </tr>
    <tr>
      <td align="center" valign="top" width="25%"><a href="http://hoegg.software/"><img src="https://avatars.githubusercontent.com/u/44559?v=4?s=200" width="200px;" alt="Ryan Hoegg"/><br /><sub><b>Ryan Hoegg</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=rhoegg" title="Code">💻</a> <a href="#platform-rhoegg" title="Packaging/porting to new platform">📦</a></td>
      <td align="center" valign="top" width="25%"><a href="https://github.com/mboss37"><img src="https://avatars.githubusercontent.com/u/29606687?v=4?s=200" width="200px;" alt="Mihael Bosnjak"/><br /><sub><b>Mihael Bosnjak</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=mboss37" title="Code">💻</a></td>
      <td align="center" valign="top" width="25%"><a href="https://github.com/saicharan0610"><img src="https://avatars.githubusercontent.com/u/53638266?v=4?s=200" width="200px;" alt="saicharan0610"/><br /><sub><b>saicharan0610</b></sub></a><br /><a href="https://github.com/MuleSoft-AI-Chain-Project/mule-webcrawler-connector/commits?author=saicharan0610" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
