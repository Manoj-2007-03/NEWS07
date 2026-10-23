package com.newsroom.network;

import com.newsroom.config.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thread-safe HTTP Client Manager.
 * Uses standard java.net.http.HttpClient with connection pooling, timeouts, and bounded retries.
 * Demonstrates Encapsulation and Networking Reliability Best Practices.
 */
public class HttpClientManager {
    private static final String USER_AGENT = "NEWSROOM-Editorial-Desktop/2.0 (Java 21; Windows)";
    private static volatile HttpClientManager instance;

    private final HttpClient httpClient;

    private HttpClientManager() {
        int timeoutSec = AppConfig.getInstance().getNetworkTimeoutSeconds();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSec))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public static HttpClientManager getInstance() {
        if (instance == null) {
            synchronized (HttpClientManager.class) {
                if (instance == null) {
                    instance = new HttpClientManager();
                }
            }
        }
        return instance;
    }

    /**
     * Executes an HTTP GET request with retries and returns the response body as a string.
     */
    public String fetchString(String url) throws IOException, InterruptedException {
        int maxRetries = 2;
        int delayMs = 500;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(AppConfig.getInstance().getNetworkTimeoutSeconds()))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/rss+xml, application/xml, text/xml, application/json, */*")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    return response.body();
                } else if (status == 429 || status >= 500) {
                    // Rate limit or server error - retry with backoff
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                } else {
                    throw new IOException("HTTP request failed with status code: " + status + " for URL: " + url);
                }
            } catch (IOException | InterruptedException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                }
            }
        }

        throw new IOException("Failed to fetch URL after " + maxRetries + " attempts: " + url, lastException);
    }

    /**
     * Fetches raw binary content (e.g. for images).
     */
    public byte[] fetchBytes(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/*, */*")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        throw new IOException("Image fetch returned status " + response.statusCode());
    }
}
