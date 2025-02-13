package org.mule.extension.webcrawler.internal.helper.webdriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CloudHubRpmExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudHubRpmExtractor.class);

    public static void extractRpmsFromJar() throws IOException {
        LOGGER.info("Extracting RPMs from " + CloudHubChromeConfigurer.CHROME_DEPENDENCY_RESOURCE_PATH);
        Files.createDirectories(Paths.get(CloudHubChromeConfigurer.CHROME_DEPENDENCY_RPM_PATH));
        Files.createDirectories(Paths.get(CloudHubChromeConfigurer.CHROME_DEPENDENCY_LIBS_PATH));
        try (InputStream zipStream = CloudHubRpmExtractor.class.getResourceAsStream(CloudHubChromeConfigurer.CHROME_DEPENDENCY_RESOURCE_PATH)) {
            try (ZipInputStream zipInputStream = new ZipInputStream(zipStream)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    LOGGER.debug("Extracting RPM: {}", entry.getName());
                    Path rpmPath = Paths.get(CloudHubChromeConfigurer.CHROME_DEPENDENCY_RPM_PATH, entry.getName());
                    try (FileOutputStream fos = new FileOutputStream(rpmPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    extractSoFilesUsingLinuxTools(rpmPath);
                    Files.deleteIfExists(rpmPath); // Clean up the extracted RPM
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error extracting RPMs from " + CloudHubChromeConfigurer.CHROME_DEPENDENCY_RESOURCE_PATH, e);
        }
    }

    private static void extractSoFilesUsingLinuxTools(Path rpmPath) {
        try {
            LOGGER.debug("Running rpm2cpio and cpio to extract .so files from: {}", rpmPath);
            String command = String.format("rpm2cpio %s | cpio --extract --no-preserve-owner --make-directories --unconditional --verbose", rpmPath.toAbsolutePath());
            ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
            processBuilder.directory(new File(CloudHubChromeConfigurer.CHROME_DEPENDENCY_RPM_PATH)); // Extract to temp directory first
            processBuilder.redirectErrorStream(true);
            Process proc = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.debug("Extracted: {}", line);
                }
            }

            int exitCode = proc.waitFor();
            if (exitCode != 0) {
                LOGGER.debug("Extraction failed with exit code: {}", exitCode);
            } else {
                LOGGER.debug("Extraction completed successfully.");
                moveSoFilesToLibsDir();
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error processing RPM: {}", rpmPath, e);
        }
    }

    private static void moveSoFilesToLibsDir() throws IOException {
        Files.walk(Paths.get(CloudHubChromeConfigurer.CHROME_DEPENDENCY_RPM_PATH))
                .filter(path -> path.toString().endsWith(".so") || path.toString().contains(".so."))
                .forEach(path -> {
                    try {
                        Path targetPath = Paths.get(CloudHubChromeConfigurer.CHROME_DEPENDENCY_LIBS_PATH, path.getFileName().toString());
                        Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.debug("Moved: " + targetPath);
                    } catch (IOException e) {
                        LOGGER.error("Error moving file: " + path, e.getMessage());
                    }
                });
    }
}

