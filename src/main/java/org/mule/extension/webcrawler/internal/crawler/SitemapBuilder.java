package org.mule.extension.webcrawler.internal.crawler;

import org.mule.extension.webcrawler.api.PageInsightType;
import org.mule.extension.webcrawler.api.RegexUrlsFilterLogic;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.config.WebCrawlerConfiguration;
import org.mule.extension.webcrawler.internal.connection.WebCrawlerConnection;
import org.mule.extension.webcrawler.internal.helper.page.PageHelper;
import org.mule.extension.webcrawler.internal.util.URLUtils;
import org.mule.extension.webcrawler.internal.util.Utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds an in-memory {@link SiteNode} tree by performing a synchronous breadth-first crawl from a seed URL. The output is fed to
 * {@link SitemapGenerator} to render as XML.
 *
 * <p>
 * Replaces the deleted {@code Crawler}/{@code MuleCrawler} abstract-base + subclass pair. Behavior matches master's
 * {@code MuleCrawler.map()} byte-for-byte:
 * </p>
 * <ul>
 * <li>BFS from the seed URL, depth-bounded by {@code maxDepth}.</li>
 * <li>{@code enforceRobotsTxt} → {@link PageHelper#canCrawl} per URL before fetch.</li>
 * <li>{@code delayMillis} sleep between fetches.</li>
 * <li>Link extraction via {@link PageHelper#getPageInsights} so the regex-filter contract matches both the legacy operations path
 * and the {@code crawl-website-source} Source (all three converge on {@link PageHelper#skipUrl}).</li>
 * <li>{@code restrictToPath} → only follow internal links; {@code !restrictToPath} → also follow external + iframe.</li>
 * <li>Leaf-validity: at {@code depth == maxDepth}, attach the node only if {@link PageHelper#isURLValid} returns true. Master
 * parity — drops broken leaf links from the sitemap.</li>
 * <li>URLs cleaned via {@link URLUtils#cleanURL} for dedup consistency with master.</li>
 * </ul>
 */
public final class SitemapBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SitemapBuilder.class);

    private SitemapBuilder() {}

    /**
     * Run a sitemap-building BFS from {@code rootURL}. Returns {@code null} if the seed URL fails the leaf-validity probe at
     * {@code maxDepth == 0} (master parity for the depth=0 root-only case).
     */
    public static SiteNode build(WebCrawlerConfiguration configuration,
                                 WebCrawlerConnection connection,
                                 String rootURL,
                                 int maxDepth,
                                 boolean restrictToPath,
                                 RegexUrlsFilterLogic regexUrlsFilterLogic,
                                 List<String> regexUrls,
                                 PageLoadOptions pageLoadOptions) {

        String rootURLCleaned = URLUtils.cleanURL(rootURL);

        SiteNode rootNode = new SiteNode(rootURLCleaned, 0, connection.getReferrer());
        Deque<SiteNode> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(rootNode);
        visited.add(rootURLCleaned);

        // Master quirk preserved: when the user asks for depth=0 only (just the seed), validate
        // the seed URL up-front and return null if it's broken. For depth>=1 the seed is fetched
        // unconditionally — its validity is implicit in being able to extract links from it.
        if (maxDepth == 0
                && !PageHelper.isURLValid(configuration, connection, rootNode.getUrl(), rootNode.getReferrer())) {
            return null;
        }

        while (!queue.isEmpty()) {
            SiteNode current = queue.poll();
            try {
                if (configuration.getCrawlerOptions().isEnforceRobotsTxt()
                        && !PageHelper.canCrawl(current.getUrl(), connection.getUserAgent())) {
                    LOGGER.debug("SKIPPING url due to robots.txt: {}", current.getUrl());
                    continue;
                }

                LOGGER.debug("MAPPING url: {}", current.getUrl());

                Utils.addDelay(configuration.getCrawlerOptions().getDelayMillis());

                if (current.getCurrentDepth() == maxDepth) {
                    // Leaf-validity HEAD probe: only attach the leaf node if reachable.
                    if (PageHelper.isURLValid(configuration, connection, current.getUrl(), current.getReferrer())) {
                        SiteNode parent = current.getParent();
                        if (parent != null) {
                            parent.addChild(current);
                        }
                    } else {
                        LOGGER.debug("SKIPPING {} due to invalid URL", current.getUrl());
                    }
                    continue;
                }

                // Below max-depth: fetch the page, extract links, attach + enqueue children.
                Document document = PageHelper.getDocument(configuration, connection, current.getUrl(),
                                                           current.getReferrer(), pageLoadOptions);

                SiteNode parent = current.getParent();
                if (parent != null) {
                    parent.addChild(current);
                }

                for (String childURL : extractLinks(document, restrictToPath, regexUrlsFilterLogic, regexUrls)) {
                    String childCleaned = URLUtils.cleanURL(childURL);
                    if (visited.add(childCleaned)) {
                        SiteNode childNode = new SiteNode(childCleaned,
                                                          current.getCurrentDepth() + 1,
                                                          current.getUrl(),
                                                          current);
                        queue.add(childNode);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error mapping {}: {}", current.getUrl(), e.toString());
                // Master parity: a depth-0 failure surfaces; deeper failures are logged-and-swallowed
                // so the rest of the crawl continues.
                if (current.getCurrentDepth() == 0) {
                    throw new RuntimeException("Sitemap build failed at root: " + e.getMessage(), e);
                }
            }
        }
        return rootNode;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> extractLinks(Document document, boolean restrictToPath,
                                            RegexUrlsFilterLogic regexUrlsFilterLogic, List<String> regexUrls) {
        // Use PageHelper.getPageInsights so the regex filter and link classification are identical
        // to the rest of the connector. Returns a structured map; we read the relevant link sets
        // and merge them into a Set<String> for the BFS to consume.
        Map<String, Object> pageInsights = PageHelper.getPageInsights(
                                                                      document,
                                                                      null,
                                                                      restrictToPath ? PageInsightType.INTERNALLINKS
                                                                              : PageInsightType.ALL,
                                                                      regexUrlsFilterLogic,
                                                                      regexUrls);

        Map<String, Object> linksMap = (Map<String, Object>) pageInsights.get("links");
        Set<String> links = new HashSet<>();
        if (linksMap == null) {
            return links;
        }
        if (restrictToPath) {
            Set<String> internal = (Set<String>) linksMap.get("internal");
            if (internal != null) {
                links.addAll(internal);
            }
        } else {
            // Master quirk preserved: when not restricting to path, follow internal + external + iframe links.
            // Reference links (#anchor) and document links (PDF, etc) are intentionally excluded.
            mergeLinkSet(links, (Set<String>) linksMap.get("internal"));
            mergeLinkSet(links, (Set<String>) linksMap.get("external"));
            mergeLinkSet(links, (Set<String>) linksMap.get("iframe"));
        }
        return links;
    }

    private static void mergeLinkSet(Set<String> dest, Set<String> src) {
        if (src != null) {
            dest.addAll(src);
        }
    }
}
