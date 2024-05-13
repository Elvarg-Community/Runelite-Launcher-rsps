package net.runelite.launcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.common.net.HttpHeaders.USER_AGENT;

public class HttpRequestManager {
    private static final String USER_AGENT = "Mozilla/5.0"; // Example User-Agent

    byte[] sendGet(String url) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {

            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                return readFully(inputStream);
            } else {
                System.err.println("HTTP request failed with status code: " + responseCode);
                // Print response body if available
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    System.err.println("Response body:");
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                throw new IOException("HTTP request failed with status code: " + responseCode);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }
}