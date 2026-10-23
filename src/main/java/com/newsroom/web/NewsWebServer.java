package com.newsroom.web;

import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.HistoryEntry;
import com.newsroom.provider.CompositeNewsProvider;
import com.newsroom.repository.SqliteArticleRepository;
import com.newsroom.service.DatabaseBookmarkServiceImpl;
import com.newsroom.service.DatabaseHistoryServiceImpl;
import com.newsroom.service.DatabaseNewsServiceImpl;
import com.newsroom.service.DatabaseSearchServiceImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Embedded Zero-Dependency HTTP Web Server for NEWSROOM.
 * Enables live cloud deployment to Render, Docker, and any standard browser.
 */
public class NewsWebServer {
    private final int port;
    private HttpServer server;

    private final DatabaseNewsServiceImpl newsService;
    private final DatabaseBookmarkServiceImpl bookmarkService;
    private final DatabaseHistoryServiceImpl historyService;
    private final DatabaseSearchServiceImpl searchService;

    public NewsWebServer(int port) {
        this.port = port;
        this.newsService = new DatabaseNewsServiceImpl();
        this.bookmarkService = new DatabaseBookmarkServiceImpl();
        this.historyService = new DatabaseHistoryServiceImpl();
        this.searchService = new DatabaseSearchServiceImpl(new SqliteArticleRepository(), new CompositeNewsProvider());
    }

    public static void startServer(int preferredPort) {
        int port = preferredPort;
        boolean explicitPort = System.getenv("PORT") != null && !System.getenv("PORT").trim().isEmpty();

        while (true) {
            try {
                NewsWebServer webServer = new NewsWebServer(port);
                webServer.start();
                return;
            } catch (java.net.BindException be) {
                if (explicitPort) {
                    System.err.println("Fatal: Explicitly configured PORT " + port + " is already in use: " + be.getMessage());
                    return;
                }
                System.out.println("Notice: Port " + port + " is in use, trying port " + (port + 1) + "...");
                port++;
                if (port > preferredPort + 20) {
                    System.err.println("Fatal: Could not find an available port.");
                    return;
                }
            } catch (Exception e) {
                System.err.println("Fatal: Failed to start NEWSROOM Web Server: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }
    }

    public void start() throws IOException {
        // Bind to 0.0.0.0 for container / cloud hosting platforms (Render, Heroku, etc.)
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // API Endpoints
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/categories", this::handleCategories);
        server.createContext("/api/articles", this::handleArticles);
        server.createContext("/api/search", this::handleSearch);
        server.createContext("/api/refresh", this::handleRefresh);
        server.createContext("/api/bookmarks", this::handleBookmarks);
        server.createContext("/api/history", this::handleHistory);

        // Static Assets / Root Web UI
        server.createContext("/", this::handleStatic);

        server.start();

        System.out.println("=================================================");
        System.out.println("  NEWSROOM Cloud & Web Service Started Successfully!");
        System.out.println("  Listening on: http://0.0.0.0:" + port);
        System.out.println("  Local URL:    http://localhost:" + port);
        System.out.println("=================================================");
    }

    // --- Handlers ---

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 200, "{\"status\":\"UP\",\"name\":\"NEWSROOM Editorial\",\"version\":\"2.0.0\"}");
    }

    private void handleCategories(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 200, JsonUtil.categoriesToJson());
    }

    private void handleArticles(HttpExchange exchange) throws IOException {
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String type = queryParams.getOrDefault("type", "");
        String categoryParam = queryParams.getOrDefault("category", "HOME");

        List<Article> result;
        if ("featured".equalsIgnoreCase(type)) {
            result = newsService.getFeaturedArticles();
        } else if ("trending".equalsIgnoreCase(type)) {
            result = newsService.getTrendingArticles();
        } else {
            Category category = Category.fromDisplayName(categoryParam);
            result = newsService.getArticles(category);
        }

        sendJsonResponse(exchange, 200, JsonUtil.articlesToJson(result));
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String query = queryParams.getOrDefault("q", "");
        List<Article> results = searchService.search(query);
        sendJsonResponse(exchange, 200, JsonUtil.articlesToJson(results));
    }

    private void handleRefresh(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod()) && !"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
            return;
        }

        Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
        String catParam = queryParams.getOrDefault("category", "HOME");
        Category category = Category.fromDisplayName(catParam);

        try {
            newsService.refreshNews(category).get(); // Await completion
            sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"Refreshed headlines for " + category.getDisplayName() + "\"}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 200, "{\"status\":\"notice\",\"message\":\"Served from local archive (" + JsonUtil.escape(e.getMessage()) + ")\"}");
        }
    }

    private void handleBookmarks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            sendCorsOptions(exchange);
            return;
        }

        if ("GET".equals(method)) {
            List<Article> bookmarked = bookmarkService.getBookmarkedArticles();
            sendJsonResponse(exchange, 200, JsonUtil.articlesToJson(bookmarked));
            return;
        }

        if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            String articleId = JsonUtil.extractField(body, "articleId");
            if (articleId != null && !articleId.isEmpty()) {
                Optional<Article> articleOpt = newsService.getArticleById(articleId);
                articleOpt.ifPresent(bookmarkService::addBookmark);
                sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"bookmarked\":true}");
            } else {
                sendJsonResponse(exchange, 400, "{\"error\":\"Missing articleId\"}");
            }
            return;
        }

        if ("DELETE".equals(method)) {
            String[] parts = path.split("/");
            String articleId = parts.length > 3 ? parts[3] : null;
            if (articleId == null) {
                String body = readRequestBody(exchange);
                articleId = JsonUtil.extractField(body, "articleId");
            }

            if (articleId != null) {
                bookmarkService.removeBookmark(articleId);
                sendJsonResponse(exchange, 200, "{\"status\":\"success\",\"bookmarked\":false}");
            } else {
                sendJsonResponse(exchange, 400, "{\"error\":\"Missing articleId\"}");
            }
            return;
        }

        sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
    }

    private void handleHistory(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("OPTIONS".equals(method)) {
            sendCorsOptions(exchange);
            return;
        }

        if ("GET".equals(method)) {
            List<HistoryEntry> history = historyService.getHistory();
            sendJsonResponse(exchange, 200, JsonUtil.historyToJson(history));
            return;
        }

        if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            String articleId = JsonUtil.extractField(body, "articleId");
            if (articleId != null) {
                newsService.getArticleById(articleId).ifPresent(historyService::recordAccess);
                sendJsonResponse(exchange, 200, "{\"status\":\"success\"}");
            } else {
                sendJsonResponse(exchange, 400, "{\"error\":\"Missing articleId\"}");
            }
            return;
        }

        if ("DELETE".equals(method)) {
            historyService.clearHistory();
            sendJsonResponse(exchange, 200, "{\"status\":\"cleared\"}");
            return;
        }

        sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/api/")) {
            sendResponse(exchange, 404, "API Endpoint Not Found", "text/plain");
            return;
        }

        // Load index.html
        byte[] htmlBytes = loadResource("/web/index.html");
        if (htmlBytes == null || htmlBytes.length == 0) {
            File localHtml = new File("src/main/resources/web/index.html");
            if (localHtml.exists()) {
                try (FileInputStream fis = new FileInputStream(localHtml)) {
                    htmlBytes = fis.readAllBytes();
                }
            }
        }

        if (htmlBytes != null && htmlBytes.length > 0) {
            sendResponse(exchange, 200, htmlBytes, "text/html; charset=UTF-8");
        } else {
            String fallback = "<html><body><h1>NEWSROOM Cloud Web Service</h1><p>Running successfully.</p></body></html>";
            sendResponse(exchange, 200, fallback.getBytes(StandardCharsets.UTF_8), "text/html; charset=UTF-8");
        }
    }

    // --- Utility Methods ---

    private byte[] loadResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in != null) {
                return in.readAllBytes();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        sendResponse(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private void sendResponse(HttpExchange exchange, int status, byte[] bytes, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void sendCorsOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                } catch (Exception e) {
                    map.put(kv[0], kv[1]);
                }
            } else if (kv.length == 1) {
                map.put(kv[0], "");
            }
        }
        return map;
    }
}
