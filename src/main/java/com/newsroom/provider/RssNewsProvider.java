package com.newsroom.provider;

import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.network.HttpClientManager;
import com.newsroom.parser.RssParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-time News Provider backed by premier global editorial RSS syndication feeds.
 * Requires ZERO API keys, allowing the application to work out of the box with live news.
 * Demonstrates Strategy Pattern and Fault-Tolerant Feed Aggregation.
 */
public class RssNewsProvider implements NewsProvider {
    private final HttpClientManager httpManager;
    private final RssParser rssParser;
    private final Map<Category, List<FeedEndpoint>> feedRegistry = new HashMap<>();

    public record FeedEndpoint(String url, String sourceName) {}

    public RssNewsProvider() {
        this(HttpClientManager.getInstance(), new RssParser());
    }

    public RssNewsProvider(HttpClientManager httpManager, RssParser rssParser) {
        this.httpManager = httpManager;
        this.rssParser = rssParser;
        registerEndpoints();
    }

    private void registerEndpoints() {
        // HOME & LATEST
        register(Category.HOME, new FeedEndpoint("https://feeds.bbci.co.uk/news/rss.xml", "BBC News"));
        register(Category.HOME, new FeedEndpoint("https://feeds.npr.org/1001/rss.xml", "NPR News"));

        register(Category.LATEST, new FeedEndpoint("https://feeds.bbci.co.uk/news/world/rss.xml", "BBC World"));

        // WORLD
        register(Category.WORLD, new FeedEndpoint("https://feeds.bbci.co.uk/news/world/rss.xml", "BBC World News"));
        register(Category.WORLD, new FeedEndpoint("https://feeds.npr.org/1004/rss.xml", "NPR World"));

        // INDIA
        register(Category.INDIA, new FeedEndpoint("https://www.thehindu.com/news/national/feeder/default.rss", "The Hindu"));
        register(Category.INDIA, new FeedEndpoint("https://timesofindia.indiatimes.com/rssfeedstopstories.cms", "Times of India"));

        // TECHNOLOGY
        register(Category.TECHNOLOGY, new FeedEndpoint("https://feeds.arstechnica.com/arstechnica/index", "Ars Technica"));
        register(Category.TECHNOLOGY, new FeedEndpoint("https://techcrunch.com/feed/", "TechCrunch"));
        register(Category.TECHNOLOGY, new FeedEndpoint("https://www.wired.com/feed/rss", "Wired"));

        // BUSINESS
        register(Category.BUSINESS, new FeedEndpoint("https://search.cnbc.com/rs/search/view.html?partnerId=2000&keywords=business&sort=date", "CNBC Markets"));
        register(Category.BUSINESS, new FeedEndpoint("https://feeds.a.dj.com/rss/RSSMarketsMain.xml", "Wall Street Journal"));

        // SCIENCE
        register(Category.SCIENCE, new FeedEndpoint("https://www.sciencedaily.com/rss/top/science.xml", "ScienceDaily"));
        register(Category.SCIENCE, new FeedEndpoint("https://phys.org/rss-feed/", "Phys.org"));

        // SPORTS
        register(Category.SPORTS, new FeedEndpoint("https://feeds.bbci.co.uk/sport/rss.xml", "BBC Sport"));
        register(Category.SPORTS, new FeedEndpoint("https://www.espn.com/espn/rss/news", "ESPN"));

        // ENTERTAINMENT
        register(Category.ENTERTAINMENT, new FeedEndpoint("https://variety.com/feed/", "Variety"));
        register(Category.ENTERTAINMENT, new FeedEndpoint("https://www.hollywoodreporter.com/feed/", "Hollywood Reporter"));
    }

    private void register(Category category, FeedEndpoint endpoint) {
        feedRegistry.computeIfAbsent(category, k -> new ArrayList<>()).add(endpoint);
    }

    @Override
    public String getProviderId() {
        return "RSS";
    }

    @Override
    public String getDisplayName() {
        return "Editorial RSS Network (Live Real-Time Feeds)";
    }

    @Override
    public boolean isAvailable() {
        return true; // Always operational when network is connected
    }

    @Override
    public List<Article> fetchHeadlines(Category category) throws Exception {
        Category target = category != null ? category : Category.HOME;
        List<FeedEndpoint> endpoints = feedRegistry.getOrDefault(target, feedRegistry.get(Category.HOME));
        List<Article> aggregated = new ArrayList<>();
        Exception lastError = null;

        for (FeedEndpoint ep : endpoints) {
            try {
                String xml = httpManager.fetchString(ep.url());
                List<Article> parsed = rssParser.parse(xml, target, ep.sourceName());
                if (!parsed.isEmpty()) {
                    aggregated.addAll(parsed);
                }
            } catch (Exception e) {
                lastError = e;
                System.err.println("Notice: Feed endpoint temporarily unavailable (" + ep.sourceName() + "): " + e.getMessage());
            }
        }

        if (aggregated.isEmpty() && lastError != null) {
            throw lastError;
        }

        return aggregated;
    }

    @Override
    public List<Article> search(String query) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Live Google News RSS Search Endpoint for real-time keyword queries
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String searchUrl = "https://news.google.com/rss/search?q=" + encoded + "&hl=en-US&gl=US&ceid=US:en";

        try {
            String xml = httpManager.fetchString(searchUrl);
            return rssParser.parse(xml, Category.LATEST, "Google News Wire");
        } catch (Exception e) {
            System.err.println("Notice: Live RSS search failed, falling back to local database matches: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
