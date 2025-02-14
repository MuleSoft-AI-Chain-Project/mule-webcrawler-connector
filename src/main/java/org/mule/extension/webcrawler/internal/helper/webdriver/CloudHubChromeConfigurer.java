package org.mule.extension.webcrawler.internal.helper.webdriver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudHubChromeConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudHubChromeConfigurer.class);

    // Ensure that Chrome is only configured once for the lifetime of the JVM
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static final String LATEST_MILESTONE_VERSION_URL =
            "https://googlechromelabs.github.io/chrome-for-testing/latest-versions-per-milestone.json";
    public static final String CHROME_CDP_VERSION = "133";
    public static final String CHROME_DEPENDENCY_RESOURCE_PATH = "/cloudhub-chrome-dependencies.zip";
    public static final String CHROME_DEPENDENCY_LIBS_PATH = "/tmp/chrome-deps-linux64";
    public static final String CHROME_PATH = "/tmp/chrome-headless-shell-linux64";
    public static final String CHROME_WRAPPER_SCRIPT_CONTENT = "#!/bin/bash\n" +
            "export LD_LIBRARY_PATH=" + CHROME_DEPENDENCY_LIBS_PATH + ":$LD_LIBRARY_PATH\n" +
            "exec " + CHROME_PATH + "/chrome \"$@\"\n";
    public static final String CHROME_WEBDRIVER_PATH = "/tmp/chromedriver-linux64";
    public static String CHROME_WEBDRIVER_WRAPPER_SCRIPT_CONTENT = "#!/bin/bash\n" +
            "export LD_LIBRARY_PATH=" + CHROME_DEPENDENCY_LIBS_PATH + ":$LD_LIBRARY_PATH\n" +
            "exec " + CHROME_WEBDRIVER_PATH + "/chromedriver \"$@\"\n";
    public static final String CHROME_LIB_WRAPPER_SCRIPT = "/tmp/chrome-linux64/chrome-lib-wrapper";
    public static final String CHROME_WEBDRIVER_WRAPPER_SCRIPT = "/tmp/chrome-linux64/chrome-webdriver-wrapper";

    public static boolean isCloudHubDeployment() {
        // Check if the system property cloudhub.deployment is set to true
        return Boolean.getBoolean("cloudhub.deployment");
    }

    public static void setup() {
        if (initialized.compareAndSet(false, true)) {
            LOGGER.debug("Executing Chrome setup");
            try {
                JSONObject jsonResponse = new JSONObject(getChromeMilestoneVersionJsonAsString());
                String chromeVersion = jsonResponse.getJSONObject("milestones").getJSONObject(CHROME_CDP_VERSION).getString("version");
                String chromeDownloadUrl =
                        String.format("https://storage.googleapis.com/chrome-for-testing-public/%s/linux64/chrome-headless-shell-linux64.zip", chromeVersion);
                String chromeDriverDownloadUrl =
                        String.format("https://storage.googleapis.com/chrome-for-testing-public/%s/linux64/chromedriver-linux64.zip", chromeVersion);

                LOGGER.info("Downloading Chrome Headless Shell from: {}", chromeDownloadUrl);
                FileUtils.copyURLToFile(new URL(chromeDownloadUrl), new File("/tmp/chrome-headless-shell-linux64.zip"));

                LOGGER.info("Downloading ChromeDriver from: {}", chromeDriverDownloadUrl);
                FileUtils.copyURLToFile(new URL(chromeDriverDownloadUrl), new File("/tmp/chromedriver-linux64.zip"));

                unzip("/tmp/chrome-headless-shell-linux64.zip", "/tmp");
                unzip("/tmp/chromedriver-linux64.zip", "/tmp");

                createLibWrapperScript(CHROME_LIB_WRAPPER_SCRIPT, CHROME_WRAPPER_SCRIPT_CONTENT);
                createLibWrapperScript(CHROME_WEBDRIVER_WRAPPER_SCRIPT, CHROME_WEBDRIVER_WRAPPER_SCRIPT_CONTENT);

                extractSoLibs();

                System.setProperty("webdriver.chrome.driver", CHROME_WEBDRIVER_WRAPPER_SCRIPT);

                logVersion(CHROME_LIB_WRAPPER_SCRIPT, CHROME_PATH + "/chrome-headless-shell");
                logVersion(CHROME_WEBDRIVER_WRAPPER_SCRIPT, CHROME_WEBDRIVER_PATH + "/chromedriver");

            } catch (Exception e) {
                LOGGER.error("Error in Chrome setup", e);
            }
        }
    }

    private static String getChromeMilestoneVersionJsonAsString() {
        try {
            URL url = new URL(LATEST_MILESTONE_VERSION_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            return response.toString();
        } catch (Exception e) {
            LOGGER.error("Error fetching latest Chrome milestone version JSON", e.getMessage());
        }
        return null;
    }

    private static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();

            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                extractEntry(zipFile, entry, destDir);
            }
        }
    }

    private static void extractSoLibs() throws IOException {
        // Extract JAR resource to a temp file
        File tempZip = File.createTempFile("chrome_dependencies", ".zip", new File("/tmp"));

        try (InputStream zipStream = CloudHubChromeConfigurer.class.getResourceAsStream(CHROME_DEPENDENCY_RESOURCE_PATH);
             OutputStream outStream = new FileOutputStream(tempZip)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = zipStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        }

        try (ZipFile zipFile = new ZipFile(tempZip)) {
            ZipArchiveEntry entry;
            for (Enumeration<ZipArchiveEntry> entries = zipFile.getEntries(); entries.hasMoreElements(); ) {
                entry = entries.nextElement();
                extractEntry(zipFile, entry, new File(CHROME_DEPENDENCY_LIBS_PATH));
            }
        }

        tempZip.delete();
    }

    private static void extractEntry(ZipFile zipFile, ZipArchiveEntry entry, File destDir) throws IOException {
        File outputFile = new File(destDir, entry.getName());

        if (entry.isDirectory()) {
            outputFile.mkdirs();
        } else {
            outputFile.getParentFile().mkdirs();

            // Handle symlinks
            if (entry.isUnixSymlink()) {
                String targetPath = zipFile.getUnixSymlink(entry); // Get symlink target
                Path target = Paths.get(destDir.getAbsolutePath(), targetPath);
                Files.createSymbolicLink(outputFile.toPath(), target);
            } else {
                Files.copy(zipFile.getInputStream(entry), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.setPosixFilePermissions(outputFile.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
            }

            // Set executable permissions if applicable
            if ((entry.getUnixMode() & 0b001000000) != 0) { // Check Unix executable bit
                outputFile.setExecutable(true);
            }
        }
    }

    public static void logFilesWithProperties(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            LOGGER.info("Invalid directory path: {}", directoryPath);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            LOGGER.info("No files found in the directory: {}", directoryPath);
            return;
        }

        for (File file : files) {
            logFileProperties(file);
        }
    }

    public static void logFileProperties(File file) {
        try {
            Path filePath = file.toPath();
            boolean isExecutable = Files.isExecutable(filePath);
            boolean isReadable = Files.isReadable(filePath);
            boolean isWritable = Files.isWritable(filePath);
            boolean isSymbolicLink = Files.isSymbolicLink(filePath);
            long fileSize = Files.size(filePath);

            LOGGER.info("{} | Executable: {} | Readable: {} | Writable: {} | SymLink: {} | Size: {} bytes",
                    file.getAbsolutePath(), isExecutable, isReadable, isWritable, isSymbolicLink, fileSize);

        } catch (Exception e) {
            LOGGER.info("Error reading properties for file: {}", file.getAbsolutePath());
        }
    }

    public static void testCommand(String[] command) throws IOException, InterruptedException {
        LOGGER.info("Executing command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge stdout and stderr
        Process process = processBuilder.start();

        // Capture the output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            LOGGER.info("command completed successfully.");
        } else {
            LOGGER.error("command failed with exit code: {}", exitCode);
        }
    }

    public static void logVersion(String wrapperScript, String binary) throws IOException, InterruptedException {
        try {
            Process process = new ProcessBuilder(wrapperScript, "--version").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String version = reader.readLine();
            if (version != null) {
                LOGGER.info("{} version is: {}", wrapperScript, version);
            } else {
                LOGGER.info("Dumping diagnostic logs for {}", binary);
                logLddOutput(binary);
                logFilesWithProperties(Paths.get(binary).getParent().toString());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get version", e.getMessage());
        }
    }

    public static void createLibWrapperScript(String path, String content) {
        try {
            File scriptFile = new File(path);
            // Ensure the parent directory exists
            scriptFile.getParentFile().mkdirs();

            // Write the script content
            try (FileWriter writer = new FileWriter(scriptFile)) {
                writer.write(content);
            }

            // Make the script executable
            scriptFile.setExecutable(true);
            LOGGER.info("Wrapper script created at: {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to create wrapper script", e.getMessage());
        }
    }

    public static void logLddOutput(String lddCommand) throws IOException, InterruptedException {
        String[] command = {"/bin/bash", "-c",
                "export LD_LIBRARY_PATH=" + CHROME_DEPENDENCY_LIBS_PATH + " && ldd " + lddCommand};

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge stdout and stderr
        Process process = processBuilder.start();

        LOGGER.info("ldd diagnostic logs");

        // Capture the output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            LOGGER.info("ldd completed successfully.");
        } else {
            LOGGER.error("ldd failed with exit code: {}", exitCode);
        }
    }
}

