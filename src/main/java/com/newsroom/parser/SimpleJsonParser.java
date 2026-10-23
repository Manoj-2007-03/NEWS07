package com.newsroom.parser;

import com.newsroom.model.Article;
import com.newsroom.model.Category;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, zero-dependency JSON parser optimized for news API payloads.
 * Parses NewsAPI and GNews JSON schemas with defensive sanitization.
 */
public class SimpleJsonParser {

    public List<Article> parseNewsApiArticles(String json, Category category) {
        List<Article> articles = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || !json.contains("\"articles\"")) {
            return articles;
        }

        try {
            // Find articles array start
            int articlesIdx = json.indexOf("\"articles\"");
            int openBracket = json.indexOf('[', articlesIdx);
            if (openBracket == -1) return articles;

            // Extract each JSON object inside the array
            List<String> rawObjects = extractArrayObjects(json, openBracket);

            for (String rawObj : rawObjects) {
                Article article = parseSingleArticle(rawObj, category);
                if (article != null) {
                    articles.add(article);
                }
            }
        } catch (Exception e) {
            System.err.println("Notice: Error parsing NewsAPI JSON: " + e.getMessage());
        }

        return articles;
    }

    private List<String> extractArrayObjects(String json, int startBracketIdx) {
        List<String> list = new ArrayList<>();
        int depth = 0;
        int objStart = -1;
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = startBracketIdx + 1; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) objStart = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart != -1) {
                        list.add(json.substring(objStart, i + 1));
                        objStart = -1;
                    }
                } else if (c == ']' && depth == 0) {
                    break;
                }
            }
        }
        return list;
    }

    private Article parseSingleArticle(String json, Category category) {
        String title = unescape(extractStringField(json, "title"));
        if (title.isEmpty() || "[Removed]".equalsIgnoreCase(title)) {
            return null;
        }

        String description = unescape(extractStringField(json, "description"));
        String content = unescape(extractStringField(json, "content"));
        String author = unescape(extractStringField(json, "author"));
        String url = extractStringField(json, "url");
        String urlToImage = extractStringField(json, "urlToImage");
        if (urlToImage.isEmpty()) {
            urlToImage = extractStringField(json, "image"); // GNews schema
        }

        // Source name
        String source = "News Desk";
        int sourceIdx = json.indexOf("\"source\"");
        if (sourceIdx != -1) {
            int nameIdx = json.indexOf("\"name\"", sourceIdx);
            if (nameIdx != -1 && nameIdx < json.indexOf('}', sourceIdx)) {
                source = unescape(extractStringField(json.substring(nameIdx), "name"));
            }
        }

        String publishedAtStr = extractStringField(json, "publishedAt");
        LocalDateTime publishedAt = LocalDateTime.now();
        if (!publishedAtStr.isEmpty()) {
            try {
                publishedAt = ZonedDateTime.parse(publishedAtStr).toLocalDateTime();
            } catch (Exception ignored) {}
        }

        String summary = !description.isEmpty() ? description : title;
        String fullContent = !content.isEmpty() ? content : summary;

        String id = generateStableId(url.isEmpty() ? title : url);
        int readingTime = Math.max(1, (summary.length() + fullContent.length()) / 500);

        return Article.builder()
                .id(id)
                .title(title)
                .summary(summary)
                .content(fullContent)
                .author(!author.isEmpty() ? author : source + " Correspondent")
                .source(!source.isEmpty() ? source : "Global News Wire")
                .category(category != null ? category : Category.LATEST)
                .publishedAt(publishedAt)
                .readingTimeMinutes(readingTime)
                .imageUrl(urlToImage)
                .originalUrl(url)
                .isFeatured(false)
                .isTrending(false)
                .build();
    }

    private String extractStringField(String json, String fieldName) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .trim();
    }

    private String generateStableId(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return "art-" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return "art-" + Math.abs(input.hashCode());
        }
    }
}
