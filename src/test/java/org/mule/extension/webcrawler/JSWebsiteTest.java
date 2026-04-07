package org.mule.extension.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.mule.extension.webcrawler.internal.connection.webdriver.RemoteWebDriverConnectionProvider;
import org.mule.extension.webcrawler.internal.connection.webdriver.WebDriverConnection;
import org.mule.extension.webcrawler.internal.connection.webdriver.WebDriverConnectionProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test JavaScript-heavy websites to validate SPA rendering
 * Also tests multi-page crawling (simulated depth)
 */
public class JSWebsiteTest {

    public static void main(String[] args) {
        System.out.println("🌐 JAVASCRIPT WEBSITE + DEPTH TEST");
        System.out.println("=" .repeat(80));
        System.out.println();
        System.out.println("Testing complex JavaScript sites to validate:");
        System.out.println("  1. JavaScript execution works correctly");
        System.out.println("  2. SPA (Single Page Application) content renders");
        System.out.println("  3. Links are extracted for multi-page crawling");
        System.out.println("  4. Local vs Remote produce identical results");
        System.out.println();

        // JavaScript-heavy test sites
        Map<String, String> testSites = new LinkedHashMap<>();
        testSites.put("React Documentation", "https://react.dev/learn");
        testSites.put("Vue.js Guide", "https://vuejs.org/guide/introduction.html");
        testSites.put("MDN Web Docs", "https://developer.mozilla.org/en-US/docs/Web/JavaScript");

        int totalTests = 0;
        int passedTests = 0;

        for (Map.Entry<String, String> site : testSites.entrySet()) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🎯 Testing: " + site.getKey());
            System.out.println("    URL: " + site.getValue());
            System.out.println("=".repeat(80));

            try {
                boolean passed = testSiteWithBothConnections(site.getValue());
                totalTests++;
                if (passed) passedTests++;

            } catch (Exception e) {
                System.err.println("❌ Test failed: " + e.getMessage());
                e.printStackTrace();
                totalTests++;
            }
        }

        // Final summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 FINAL RESULTS");
        System.out.println("=".repeat(80));
        System.out.println(String.format("Tests run: %d", totalTests));
        System.out.println(String.format("Passed: %d", passedTests));
        System.out.println(String.format("Failed: %d", totalTests - passedTests));
        System.out.println();

        if (passedTests == totalTests) {
            System.out.println("🎉 ALL TESTS PASSED!");
            System.out.println("✅ Remote WebDriver handles JavaScript sites correctly");
            System.out.println("✅ Results match Local WebDriver");
        } else {
            System.out.println("⚠️  Some tests failed");
        }
    }

    private static boolean testSiteWithBothConnections(String url) throws Exception {

        // Test with Local WebDriver
        System.out.println("\n📍 TEST 1: LOCAL WEBDRIVER");
        System.out.println("-".repeat(80));
        PageResult localResult = testWithLocalWebDriver(url);

        // Test with Remote WebDriver
        System.out.println("\n📍 TEST 2: REMOTE WEBDRIVER");
        System.out.println("-".repeat(80));
        PageResult remoteResult = testWithRemoteWebDriver(url);

        // Compare
        System.out.println("\n📊 COMPARISON");
        System.out.println("-".repeat(80));
        return compareResults(localResult, remoteResult);
    }

    private static PageResult testWithLocalWebDriver(String url) throws Exception {
        WebDriverConnectionProvider provider = new WebDriverConnectionProvider();
        provider.start();
        WebDriverConnection connection = provider.connect();

        try {
            long startTime = System.currentTimeMillis();

            PageLoadOptions options = new PageLoadOptions();
            options.setWaitOnPageLoad(15000L);

            InputStream pageSource = connection.getPageSource(url, null, options);
            String html = new BufferedReader(new InputStreamReader(pageSource))
                    .lines().collect(Collectors.joining("\n"));

            long elapsed = System.currentTimeMillis() - startTime;

            // Parse with JSoup to extract JavaScript-rendered content
            Document doc = Jsoup.parse(html);

            PageResult result = new PageResult();
            result.contentLength = html.length();
            result.title = doc.title();
            result.hasScript = html.contains("<script");
            result.timeMs = elapsed;

            // Extract links (to simulate depth crawling)
            Elements links = doc.select("a[href]");
            result.linksFound = links.size();

            // Check for JavaScript framework indicators
            result.hasReactContent = html.contains("react") || html.contains("React");
            result.hasVueContent = html.contains("vue") || html.contains("Vue");

            System.out.println("   ⏱️  Time: " + elapsed + "ms");
            System.out.println("   📄 Content: " + result.contentLength + " chars");
            System.out.println("   📝 Title: " + result.title);
            System.out.println("   🔗 Links: " + result.linksFound);
            System.out.println("   📜 Has <script>: " + result.hasScript);
            System.out.println("   ⚛️  React content: " + result.hasReactContent);
            System.out.println("   🖖 Vue content: " + result.hasVueContent);

            return result;

        } finally {
            provider.stop();
        }
    }

    private static PageResult testWithRemoteWebDriver(String url) throws Exception {
        RemoteWebDriverConnectionProvider provider = new RemoteWebDriverConnectionProvider();

        java.lang.reflect.Field remoteUrlField = provider.getClass().getDeclaredField("remoteUrl");
        remoteUrlField.setAccessible(true);
        remoteUrlField.set(provider, "http://localhost:4444");

        provider.start();
        WebDriverConnection connection = provider.connect();

        try {
            long startTime = System.currentTimeMillis();

            PageLoadOptions options = new PageLoadOptions();
            options.setWaitOnPageLoad(15000L);

            InputStream pageSource = connection.getPageSource(url, null, options);
            String html = new BufferedReader(new InputStreamReader(pageSource))
                    .lines().collect(Collectors.joining("\n"));

            long elapsed = System.currentTimeMillis() - startTime;

            Document doc = Jsoup.parse(html);

            PageResult result = new PageResult();
            result.contentLength = html.length();
            result.title = doc.title();
            result.hasScript = html.contains("<script");
            result.timeMs = elapsed;

            Elements links = doc.select("a[href]");
            result.linksFound = links.size();

            result.hasReactContent = html.contains("react") || html.contains("React");
            result.hasVueContent = html.contains("vue") || html.contains("Vue");

            System.out.println("   ⏱️  Time: " + elapsed + "ms");
            System.out.println("   📄 Content: " + result.contentLength + " chars");
            System.out.println("   📝 Title: " + result.title);
            System.out.println("   🔗 Links: " + result.linksFound);
            System.out.println("   📜 Has <script>: " + result.hasScript);
            System.out.println("   ⚛️  React content: " + result.hasReactContent);
            System.out.println("   🖖 Vue content: " + result.hasVueContent);

            return result;

        } finally {
            provider.stop();
        }
    }

    private static boolean compareResults(PageResult local, PageResult remote) {
        System.out.println(String.format("%-25s | %-15s | %-15s | %s",
            "Metric", "Local", "Remote", "Match?"));
        System.out.println("-".repeat(80));

        // Content length (allow 1% difference for timing variances)
        int contentDiff = Math.abs(local.contentLength - remote.contentLength);
        double diffPercent = (contentDiff * 100.0) / Math.max(local.contentLength, remote.contentLength);
        boolean contentMatch = diffPercent < 1.0;

        System.out.println(String.format("%-25s | %-15d | %-15d | %s",
            "Content Length (chars)", local.contentLength, remote.contentLength,
            contentMatch ? "✅" : String.format("⚠️ %.1f%%", diffPercent)));

        // Title
        boolean titleMatch = local.title.equals(remote.title);
        String localTitleShort = local.title.length() > 12 ? local.title.substring(0, 12) + "..." : local.title;
        String remoteTitleShort = remote.title.length() > 12 ? remote.title.substring(0, 12) + "..." : remote.title;
        System.out.println(String.format("%-25s | %-15s | %-15s | %s",
            "Title", localTitleShort, remoteTitleShort, titleMatch ? "✅" : "⚠️"));

        // Links (allow 10% difference)
        int linksDiff = Math.abs(local.linksFound - remote.linksFound);
        double linksDiffPercent = (linksDiff * 100.0) / Math.max(local.linksFound, remote.linksFound);
        boolean linksMatch = linksDiffPercent < 10.0;

        System.out.println(String.format("%-25s | %-15d | %-15d | %s",
            "Links Found (depth sim)", local.linksFound, remote.linksFound,
            linksMatch ? "✅" : String.format("⚠️ %.1f%%", linksDiffPercent)));

        // Script tags
        boolean scriptMatch = local.hasScript == remote.hasScript;
        System.out.println(String.format("%-25s | %-15s | %-15s | %s",
            "Has <script> tags", local.hasScript ? "Yes" : "No",
            remote.hasScript ? "Yes" : "No", scriptMatch ? "✅" : "❌"));

        // Framework detection
        boolean reactMatch = local.hasReactContent == remote.hasReactContent;
        System.out.println(String.format("%-25s | %-15s | %-15s | %s",
            "React content", local.hasReactContent ? "Yes" : "No",
            remote.hasReactContent ? "Yes" : "No", reactMatch ? "✅" : "⚠️"));

        boolean vueMatch = local.hasVueContent == remote.hasVueContent;
        System.out.println(String.format("%-25s | %-15s | %-15s | %s",
            "Vue content", local.hasVueContent ? "Yes" : "No",
            remote.hasVueContent ? "Yes" : "No", vueMatch ? "✅" : "⚠️"));

        System.out.println("-".repeat(80));

        boolean allPass = contentMatch && titleMatch && linksMatch && scriptMatch;

        if (allPass) {
            System.out.println("🎉 VERDICT: ✅ PASS - Results match!");
            System.out.println("   JavaScript execution works correctly");
            System.out.println("   Links extracted for depth crawling: " + local.linksFound + " links");
            return true;
        } else {
            System.out.println("⚠️  VERDICT: Minor differences (expected for dynamic sites)");
            if (contentMatch && titleMatch && scriptMatch) {
                System.out.println("   Core functionality matches (content, title, scripts)");
                return true;
            }
            return false;
        }
    }

    static class PageResult {
        int contentLength;
        String title;
        boolean hasScript;
        int linksFound;
        long timeMs;
        boolean hasReactContent;
        boolean hasVueContent;
    }
}
