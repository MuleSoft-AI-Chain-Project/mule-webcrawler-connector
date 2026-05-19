# MuleSoft Webcrawler Connector Operations Demo

Small Mule application demonstrating the **MuleSoft WebCrawler Connector**.

The demo exposes the per-page operations behind a small REST surface (APIKit-routed) and shows the new
streaming `crawl-website-source` Source in a separate flow.

## What's exposed

### REST endpoints (`/api/...`)

| Endpoint              | Connector op              | Notes                                                  |
|-----------------------|---------------------------|--------------------------------------------------------|
| `POST /api/page`      | `page-content`            | Fetch one page; `outputFormat=HTML\|TEXT\|MARKDOWN`    |
| `GET /api/insights`   | `page-insights`           | Page-link breakdown / element counts                   |
| `GET /api/sitemap`    | `get-sitemap`             | Synchronous sitemap XML for a seed URL + depth         |
| `GET /api/metatag`    | `page-meta-tags`          | Meta-tag scrape                                        |
| `GET /api/image`      | `page-download-image`     | Download images linked from a page                     |
| `GET /api/document`   | `page-download-document`  | Download docs (PDF/DOCX/...) linked from a page        |
| `GET /api/search`     | `search-google`           | Serper.dev-backed Google search; needs `Serper-Api-Key` header |

Each endpoint accepts a `connection=http\|webdriver` query param to switch between the HTTP fetch and the
Remote WebDriver fetch path. Defaults to `http`.

### Streaming Source flow (`crawlerSourceFlow`)

Drives the new `crawl-website-source` Source. Seeds from `webcrawler.crawl.seedUrl` (set in
`config.properties`), fetches each page, emits one Mule event per fetched page. The flow logs each event by
default — adapt it to pipe events to S3, Data Cloud, Kafka, or any other sink in real apps.

## Migration note (vs. previous demo versions)

This demo was rewritten for connector `1.0.0`+:

- Old `crawl-website-full-scan` and `crawl-website-streaming` operations are **removed** — replaced by the
  `crawl-website-source` Source. The corresponding `/api/crawl-full-scan` and `/api/crawl-streaming`
  endpoints are dropped from the RAML.
- XML namespace prefix changed from `ms-webcrawler` → `webcrawler`.
- Connection element aliases changed: `<webcrawler:http-connection .../>` and
  `<webcrawler:remote-webdriver-connection .../>` (previously `<ms-webcrawler:http-connection>` /
  `<ms-webcrawler:web-driver-connection>` for the now-removed embedded Chrome path).
- `<webcrawler:config>` now requires `url` (seed) and `queueName` parameters.

## Configuration

Edit `src/main/resources/config.properties`:

```properties
http.port=8081
webcrawler.userAgent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 ...
webcrawler.referrer=https://www.google.com
webcrawler.remoteBrowser.url=https://remote-browser.example.com
webcrawler.crawl.seedUrl=https://example.com
webcrawler.crawl.maxDepth=1
```

`webcrawler.remoteBrowser.url` only needs to be reachable if you intend to use the `connection=webdriver`
branch of the API endpoints, or if you switch the `crawlerSourceFlow` config to use the
`<webcrawler:remote-webdriver-connection>` element instead of `<webcrawler:http-connection>`.

## Run

```bash
mvn clean package
mvn mule:run    # or deploy the resulting mule-application JAR
```

API console: `http://localhost:8081/console/`
