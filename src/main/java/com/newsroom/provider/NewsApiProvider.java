package com.newsroom.provider;

import com.newsroom.config.AppConfig;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.network.HttpClientManager;
import com.newsroom.parser.SimpleJsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * NewsAPI.org REST Provider.
 * Integrates commercial news API with defensive authentication checks and JSON response parsing.
 * Demonstrates Strategy Pattern and API Integration Best Practices.
 */
public class NewsApiProvider implements NewsProvider {
    private final AppConfig config;
    private final HttpClientManager httpManager;
    private final SimpleJsonParser jsonParser;

    public NewsApiProvider() {
        this(AppConfig.getInstance(), HttpClientManager.getInstance(), new SimpleJsonParser());
    }

    public NewsApiProvider(AppConfig config, HttpClientManager httpManager, SimpleJsonParser jsonParser) {
        this.config = config;
        this.httpManager = httpManager;
        this.jsonParser = jsonParser;
    }

    @Override
    public String getProviderId() {
        return "NEWS_API";
    }

    @Override
    public String getDisplayName() {
        return "NewsAPI.org (Global REST Service)";
    }

    @Override
    public boolean isAvailable() {
        return config.hasValidApiKey();
    }

    @Override
    public List<Article> fetchHeadlines(Category category) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("NewsAPI key is not configured. Configure it in newsroom.properties or Settings.");
        }

        String baseUrl = config.getApiBaseUrl();
        String apiKey = config.getApiKey();
        String country = config.getCountry();

        String url;
        if (category == Category.INDIA) {
            url = baseUrl + "/top-headlines?country=in&pageSize=30&apiKey=" + apiKey;
        } else {
            String apiCat = mapCategoryToApiCategory(category);
            url = baseUrl + "/top-headlines?country=" + country + "&category=" + apiCat + "&pageSize=30&apiKey=" + apiKey;
        }

        String json = httpManager.fetchString(url);
        return jsonParser.parseNewsApiArticles(json, category);
    }

    @Override
    public List<Article> search(String query) throws Exception {
        if (!isAvailable() || query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String baseUrl = config.getApiBaseUrl();
        String apiKey = config.getApiKey();
        String encodedQ = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);

        String url = baseUrl + "/everything?q=" + encodedQ + "&sortBy=relevancy&pageSize=30&apiKey=" + apiKey;
        String json = httpManager.fetchString(url);
        return jsonParser.parseNewsApiArticles(json, Category.LATEST);
    }

    private String mapCategoryToApiCategory(Category cat) {
        if (cat == null) return "general";
        return switch (cat) {
            case TECHNOLOGY -> "technology";
            case BUSINESS -> "business";
            case SCIENCE -> "science";
            case SPORTS -> "sports";
            case ENTERTAINMENT -> "entertainment";
            default -> "general";
        };
    }
}
