package org.mule.extension.webcrawler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.extension.webcrawler.internal.connection.webdriver.RemoteWebDriverConnectionProvider;
import org.mule.extension.webcrawler.internal.connection.webdriver.WebDriverConnection;
import org.mule.extension.webcrawler.internal.config.PageLoadOptions;
import org.openqa.selenium.WebDriver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * POC Test for Remote Browser functionality.
 *
 * Prerequisites:
 * - Selenium Grid running on localhost:4444 (Docker: seleniarm/standalone-chromium)
 *
 * Run: mvn test -Dtest=RemoteBrowserPOCTest
 */
public class RemoteBrowserPOCTest {

    private RemoteWebDriverConnectionProvider provider;
    private WebDriverConnection connection;

    @Before
    public void setUp() throws Exception {
        System.out.println("=== Remote Browser POC Test ===");
        System.out.println("Setting up Remote WebDriver connection to http://localhost:4444");

        provider = new RemoteWebDriverConnectionProvider();
        // Use reflection to set private field remoteUrl
        java.lang.reflect.Field remoteUrlField = provider.getClass().getDeclaredField("remoteUrl");
        remoteUrlField.setAccessible(true);
        remoteUrlField.set(provider, "http://localhost:4444");

        provider.start();
        connection = provider.connect();

        System.out.println("✅ Connected to remote browser successfully");
    }

    @Test
    public void testPageContentExtraction() throws Exception {
        System.out.println("\n--- Test 1: Page Content Extraction ---");
        String testUrl = "https://example.com";
        System.out.println("Fetching: " + testUrl);

        PageLoadOptions options = new PageLoadOptions();
        options.setWaitOnPageLoad(10000L);

        InputStream pageSource = connection.getPageSource(testUrl, null, options);
        assertNotNull("Page source should not be null", pageSource);

        String content = new BufferedReader(new InputStreamReader(pageSource))
                .lines().collect(Collectors.joining("\n"));

        System.out.println("Content length: " + content.length() + " characters");
        assertTrue("Content should not be empty", content.length() > 0);
        assertTrue("Should contain 'Example Domain'", content.contains("Example Domain"));

        System.out.println("✅ Page content extracted successfully");
        System.out.println("Sample content: " + content.substring(0, Math.min(200, content.length())) + "...");
    }

    @Test
    public void testURLStatusCode() throws Exception {
        System.out.println("\n--- Test 2: URL Status Code Check ---");
        String testUrl = "https://example.com";
        System.out.println("Checking status for: " + testUrl);

        Integer statusCode = connection.getUrlStatusCode(testUrl, null);
        assertNotNull("Status code should not be null", statusCode);

        System.out.println("Status code: " + statusCode);
        assertEquals("Status code should be 200", Integer.valueOf(200), statusCode);

        System.out.println("✅ Status code check successful");
    }

    @Test
    public void testDifferentWebsite() throws Exception {
        System.out.println("\n--- Test 3: Different Website ---");
        String testUrl = "https://www.wikipedia.org";
        System.out.println("Fetching: " + testUrl);

        PageLoadOptions options = new PageLoadOptions();
        options.setWaitOnPageLoad(15000L);

        InputStream pageSource = connection.getPageSource(testUrl, null, options);
        String content = new BufferedReader(new InputStreamReader(pageSource))
                .lines().collect(Collectors.joining("\n"));

        System.out.println("Content length: " + content.length() + " characters");
        assertTrue("Content should contain 'Wikipedia'", content.contains("Wikipedia"));

        System.out.println("✅ Successfully fetched Wikipedia content");
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("\n--- Cleaning up ---");
        if (provider != null) {
            provider.stop();
            System.out.println("✅ Remote browser connection closed");
        }
        System.out.println("=== POC Test Complete ===\n");
    }

    /**
     * Manual test - run this main method directly to verify setup
     */
    public static void main(String[] args) {
        System.out.println("🚀 Running Remote Browser POC Test...\n");

        RemoteBrowserPOCTest test = new RemoteBrowserPOCTest();
        try {
            test.setUp();
            test.testPageContentExtraction();
            test.testURLStatusCode();
            test.testDifferentWebsite();
            test.tearDown();

            System.out.println("\n🎉 All tests passed!");
            System.out.println("\n✅ POC VALIDATION:");
            System.out.println("   - Remote browser connection: WORKING");
            System.out.println("   - Page content extraction: WORKING");
            System.out.println("   - Status code check: WORKING");
            System.out.println("   - No Chrome download required: CONFIRMED");

        } catch (Exception e) {
            System.err.println("\n❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
