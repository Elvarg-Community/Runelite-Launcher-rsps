package net.runelite.launcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.runelite.launcher.Launcher.stage;

public class JavaInstaller {

    private static final File SAVE_LOC = new File(System.getenv("APPDATA") + "/" + LauncherProperties.getApplicationName() + "/");

    private static final File LOCAL_HASH_FILE = new File(SAVE_LOC,"java_hash.txt");

    public static void init() {
        // Define the platform and architecture
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String version = "";
        String checksum = "";
        String downloadLink = "";

        // Select the appropriate version details based on the OS and architecture
        if (os.contains("win")) {
            if (arch.contains("64")) {
                version = "WIN64_VERSION";
                checksum = "WIN64_CHKSUM";
                downloadLink = "WIN64_LINK";
            } else {
                version = "WIN32_VERSION";
                checksum = "WIN32_CHKSUM";
                downloadLink = "WIN32_LINK";
            }
        } else if (os.contains("mac")) {
            if (arch.contains("64")) {
                version = "MAC_AMD64_VERSION";
                checksum = "MAC_AMD64_CHKSUM";
                downloadLink = "MAC_AMD64_LINK";
            } else {
                version = "MAC_AARCH64_VERSION";
                checksum = "MAC_AARCH64_CHKSUM";
                downloadLink = "MAC_AARCH64_LINK";
            }
        } else if (os.contains("linux")) {
            if (arch.contains("64")) {
                version = "LINUX_AMD64_VERSION";
                checksum = "LINUX_AMD64_CHKSUM";
                downloadLink = "LINUX_AMD64_LINK";
            } else {
                version = "LINUX_AARCH64_VERSION";
                checksum = "LINUX_AARCH64_CHKSUM";
                downloadLink = "LINUX_AARCH64_LINK";
            }
        } else {
            FatalErrorDialog.showWindow("Unsupported OS");
            return;
        }

        // Download, install Java and process files
        try {
            Launcher.forcedJava = new File(SAVE_LOC,"jdk-" + getValue(version) + "-jre").getAbsolutePath();

            SAVE_LOC.mkdirs();

            System.out.println(Launcher.forcedJava);

            String javaChecksum = getValue(checksum);
            String javaDownloadLink = getValue(downloadLink);

            // Check if Java is already downloaded and hash matches
            File javaFile = new File(SAVE_LOC,"java.zip");
            String localHash = readLocalHash();
            SplashScreen.stage(0.2,"", "Verifying Java");
            if (!javaFile.exists() || !javaChecksum.equals(localHash) || !verifyChecksum(javaFile, javaChecksum)) {
                // Download the file if it doesn't exist or hash doesn't match
                javaFile = downloadFile(javaDownloadLink, javaChecksum);
                // Unzip and install Java

                Path installPath = SAVE_LOC.toPath();
                Files.createDirectories(installPath);
                unzip(javaFile, installPath.toFile());

                // Process all files in the app data directory
                processFiles(installPath);
                storeLocalHash(javaChecksum);
                stage(0,"", "Finished Downloading Java");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readLocalHash() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(LOCAL_HASH_FILE));
            String localHash = reader.readLine();
            reader.close();
            return localHash;
        } catch (IOException e) {
            // If the file doesn't exist or any other IO error occurs, return null
            return null;
        }
    }

    private static void storeLocalHash(String hash) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(LOCAL_HASH_FILE));
            writer.write(hash);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getValue(String key) throws IOException {
        // Read the value from the provided file in resources
        InputStream inputStream = JavaInstaller.class.getResourceAsStream("/java_versions.properties");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(key)) {
                return line.split("=")[1].trim();
            }
        }

        return null;
    }

    private static File downloadFile(String fileURL, String expectedChecksum) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            int contentLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            File tempFile = new File(SAVE_LOC,"java.zip");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                int progress = (int) ((totalBytesRead * 100) / contentLength);
                stage((double) progress / 100, progress + "%", "Downloading Java");
            }
            outputStream.close();
            inputStream.close();

            // Verify checksum
            if (!verifyChecksum(tempFile, expectedChecksum)) {
                stage(0,"Checksum verification failed.", "Deleting the downloaded file.");
                tempFile.delete();
                FatalErrorDialog.showWindow("Checksum verification failed.");
                throw new IOException("Checksum verification failed.");
            }

            // Rename the file
            File javaFile = new File(SAVE_LOC,"java.zip");
            tempFile.renameTo(javaFile);
            return javaFile;
        } else {
            FatalErrorDialog.showWindow("Failed to download file. HTTP error code: " + responseCode);
            throw new IOException("Failed to download file. HTTP error code: " + responseCode);
        }
    }

    private static boolean verifyChecksum(File file, String expectedChecksum) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            String actualChecksum = bytesToHex(digest).toLowerCase();
            return actualChecksum.equals(expectedChecksum.toLowerCase());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    private static void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            long totalSize = zipFile.length();
            long extractedSize = 0;

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File entryDestination = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    File parent = entryDestination.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(entryDestination)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            extractedSize += bytesRead;
                            // Calculate progress
                            int progress = (int) Math.min(100, ((extractedSize * 100L) / totalSize));
                            SplashScreen.stage((double) progress / 100, progress + "%", "Unzipping Java");
                        }
                    }
                }
            }
        }
    }

    private static void processFiles(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }
}