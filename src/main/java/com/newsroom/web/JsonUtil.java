package com.newsroom.web;

import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.HistoryEntry;

import java.util.List;

/**
 * High-performance, zero-dependency JSON utility for the NEWSROOM Web Server.
 */
public final class JsonUtil {

    private JsonUtil() {}

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String articleToJson(Article a) {
        if (a == null) return "null";
        return "{"
                + "\"id\":\"" + escape(a.getId()) + "\","
                + "\"title\":\"" + escape(a.getTitle()) + "\","
                + "\"summary\":\"" + escape(a.getSummary()) + "\","
                + "\"content\":\"" + escape(a.getContent()) + "\","
                + "\"author\":\"" + escape(a.getAuthor()) + "\","
                + "\"source\":\"" + escape(a.getSource()) + "\","
                + "\"category\":\"" + escape(a.getCategory() != null ? a.getCategory().name() : "LATEST") + "\","
                + "\"categoryDisplayName\":\"" + escape(a.getCategory() != null ? a.getCategory().getDisplayName() : "Latest") + "\","
                + "\"publishedAt\":\"" + (a.getPublishedAt() != null ? a.getPublishedAt().toString() : "") + "\","
                + "\"readingTimeMinutes\":" + a.getReadingTimeMinutes() + ","
                + "\"imageUrl\":\"" + escape(a.getImageUrl()) + "\","
                + "\"originalUrl\":\"" + escape(a.getOriginalUrl()) + "\","
                + "\"isFeatured\":" + a.isFeatured() + ","
                + "\"isTrending\":" + a.isTrending() + ","
                + "\"isBookmarked\":" + a.isBookmarked()
                + "}";
    }

    public static String articlesToJson(List<Article> articles) {
        if (articles == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < articles.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(articleToJson(articles.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String categoriesToJson() {
        StringBuilder sb = new StringBuilder("[");
        Category[] values = Category.values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            Category cat = values[i];
            String hexColor = "#334155";
            if (cat.getThemeColor() != null) {
                hexColor = String.format("#%02x%02x%02x", 
                        cat.getThemeColor().getRed(), 
                        cat.getThemeColor().getGreen(), 
                        cat.getThemeColor().getBlue());
            }
            sb.append("{")
              .append("\"name\":\"").append(escape(cat.name())).append("\",")
              .append("\"displayName\":\"").append(escape(cat.getDisplayName())).append("\",")
              .append("\"description\":\"").append(escape(cat.getDescription())).append("\",")
              .append("\"color\":\"").append(hexColor).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String historyToJson(List<HistoryEntry> history) {
        if (history == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append(",");
            HistoryEntry entry = history.get(i);
            sb.append("{")
              .append("\"article\":").append(articleToJson(entry.getArticle())).append(",")
              .append("\"readAt\":\"").append(entry.getAccessTime() != null ? entry.getAccessTime().toString() : "").append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Extracts a simple string field from a JSON object: {"fieldName": "value"}
     */
    public static String extractField(String json, String fieldName) {
        if (json == null || fieldName == null) return null;
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
