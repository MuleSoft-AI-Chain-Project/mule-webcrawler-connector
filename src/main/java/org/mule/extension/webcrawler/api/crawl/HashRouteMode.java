package org.mule.extension.webcrawler.api.crawl;

/**
 * Controls how the crawler interprets URL fragments (the {@code #...} portion of a URL) when extracting links.
 *
 * <p>
 * Single-page applications (SPAs) sometimes use the fragment to identify a route. Server-rendered sites typically use fragments
 * only as scroll positions. This enum lets the user opt into the right behaviour for the target site.
 * </p>
 *
 * <ul>
 * <li>{@link #IGNORE} — strip fragments from extracted URLs. {@code foo.html#section-a} and {@code foo.html#section-b}
 * deduplicate to the same URL. This is the right default for SSR sites where fragments are scroll positions.</li>
 * <li>{@link #PATH} — rewrite hash routes to path routes. {@code #/users} becomes {@code /users}, then crawled. Useful for SPAs
 * where the hash route also exists as a real path.</li>
 * <li>{@link #PRESERVE} — keep fragments verbatim. {@code foo.html#section-a} and {@code foo.html#section-b} are treated as
 * distinct URLs. Useful for pure SPAs where fragments identify pages.</li>
 * </ul>
 */
public enum HashRouteMode {
  IGNORE, PATH, PRESERVE
}
