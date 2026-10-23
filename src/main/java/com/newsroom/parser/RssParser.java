package com.newsroom.parser;

import com.newsroom.model.Article;
import com.newsroom.model.Category;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust XML Parser for standard RSS 2.0 and Atom syndication feeds.
 * Demonstrates XML Security (XXE prevention), Content Sanitization, and Multi-Format Date Parsing.
 */
public class RssParser {
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)
    };

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);

    public List<Article> parse(String xmlContent, Category category, String sourceFallback) {
        List<Article> articles = new ArrayList<>();
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            return articles;
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Secure XML parser configuration against XXE injection
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmlContent)));
            doc.getDocumentElement().normalize();

            // Check if RSS 2.0 (<item>) or Atom (<entry>)
            NodeList itemNodes = doc.getElementsByTagName("item");
            boolean isAtom = false;
            if (itemNodes.getLength() == 0) {
                itemNodes = doc.getElementsByTagName("entry");
                isAtom = true;
            }

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node node = itemNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    Article article = parseArticleElement(element, isAtom, category, sourceFallback);
                    if (article != null) {
                        articles.add(article);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Notice: Failed to parse RSS XML: " + e.getMessage());
        }

        return articles;
    }

    private Article parseArticleElement(Element element, boolean isAtom, Category category, String sourceFallback) {
        String title = cleanText(getElementText(element, isAtom ? "title" : "title"));
        if (title.isEmpty()) return null;

        String link = getLink(element, isAtom);
        String rawDescription = getElementText(element, isAtom ? "summary" : "description");
        String contentEncoded = getElementText(element, "content:encoded");
        if (contentEncoded.isEmpty() && isAtom) {
            contentEncoded = getElementText(element, "content");
        }

        String summary = cleanHtml(rawDescription);
        String fullContent = !contentEncoded.isEmpty() ? cleanHtml(contentEncoded) : summary;

        String author = getAuthor(element, isAtom, sourceFallback);
        LocalDateTime publishedAt = parsePublishedDate(element, isAtom);
        String imageUrl = extractImageUrl(element, rawDescription, contentEncoded);

        // Generate stable ID based on link hash or title hash
        String id = generateStableId(link.isEmpty() ? title : link);

        int readingTime = calculateReadingTime(summary + " " + fullContent);

        return Article.builder()
                .id(id)
                .title(title)
                .summary(summary)
                .content(fullContent)
                .author(author)
                .source(sourceFallback != null ? sourceFallback : "Editorial Wire")
                .category(category != null ? category : Category.LATEST)
                .publishedAt(publishedAt)
                .readingTimeMinutes(readingTime)
                .imageUrl(imageUrl)
                .originalUrl(link)
                .isFeatured(false)
                .isTrending(false)
                .build();
    }

    private String getElementText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            Node n = list.item(0);
            return n.getTextContent() != null ? n.getTextContent().trim() : "";
        }
        return "";
    }

    private String getLink(Element element, boolean isAtom) {
        if (isAtom) {
            NodeList list = element.getElementsByTagName("link");
            for (int i = 0; i < list.getLength(); i++) {
                Element linkEl = (Element) list.item(i);
                String rel = linkEl.getAttribute("rel");
                if (rel == null || rel.isEmpty() || "alternate".equalsIgnoreCase(rel)) {
                    String href = linkEl.getAttribute("href");
                    if (href != null && !href.isEmpty()) return href.trim();
                }
            }
        }
        return getElementText(element, "link");
    }

    private String getAuthor(Element element, boolean isAtom, String fallback) {
        String author = getElementText(element, "dc:creator");
        if (author.isEmpty()) author = getElementText(element, "author");
        if (author.isEmpty() && isAtom) {
            NodeList authorNodes = element.getElementsByTagName("author");
            if (authorNodes.getLength() > 0) {
                Element authorEl = (Element) authorNodes.item(0);
                author = getElementText(authorEl, "name");
            }
        }
        return !author.isEmpty() ? cleanText(author) : (fallback != null ? fallback + " Correspondent" : "Staff Reporter");
    }

    private LocalDateTime parsePublishedDate(Element element, boolean isAtom) {
        String dateStr = getElementText(element, isAtom ? "published" : "pubDate");
        if (dateStr.isEmpty()) dateStr = getElementText(element, "dc:date");
        if (dateStr.isEmpty() && isAtom) dateStr = getElementText(element, "updated");

        if (!dateStr.isEmpty()) {
            for (DateTimeFormatter fmt : DATE_FORMATTERS) {
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(dateStr, fmt);
                    return zdt.toLocalDateTime();
                } catch (Exception ignored) {}
            }
        }
        return LocalDateTime.now();
    }

    private String extractImageUrl(Element element, String rawDescription, String rawContent) {
        // 1. media:content
        NodeList mediaList = element.getElementsByTagName("media:content");
        if (mediaList.getLength() > 0) {
            Element mediaEl = (Element) mediaList.item(0);
            String url = mediaEl.getAttribute("url");
            if (url != null && !url.isEmpty()) return url.trim();
        }

        // 2. enclosure
        NodeList enclosureList = element.getElementsByTagName("enclosure");
        if (enclosureList.getLength() > 0) {
            Element encEl = (Element) enclosureList.item(0);
            String type = encEl.getAttribute("type");
            if (type == null || type.startsWith("image/")) {
                String url = encEl.getAttribute("url");
                if (url != null && !url.isEmpty()) return url.trim();
            }
        }

        // 3. media:thumbnail
        NodeList thumbList = element.getElementsByTagName("media:thumbnail");
        if (thumbList.getLength() > 0) {
            Element thumbEl = (Element) thumbList.item(0);
            String url = thumbEl.getAttribute("url");
            if (url != null && !url.isEmpty()) return url.trim();
        }

        // 4. regex from html img tag
        Matcher m = IMG_SRC_PATTERN.matcher(rawDescription);
        if (m.find()) {
            return m.group(1).trim();
        }
        if (rawContent != null) {
            Matcher m2 = IMG_SRC_PATTERN.matcher(rawContent);
            if (m2.find()) {
                return m2.group(1).trim();
            }
        }

        return "";
    }

    private String cleanHtml(String raw) {
        if (raw == null) return "";
        String text = HTML_TAG_PATTERN.matcher(raw).replaceAll(" ");
        return cleanText(text);
    }

    private String cleanText(String s) {
        if (s == null) return "";
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#8216;", "‘")
                .replace("&#8217;", "’")
                .replace("&#8220;", "“")
                .replace("&#8221;", "”")
                .replace("&#8212;", "—")
                .replace("&#8211;", "–")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int calculateReadingTime(String text) {
        if (text == null || text.isEmpty()) return 3;
        int words = text.split("\\s+").length;
        return Math.max(1, Math.min(12, words / 180));
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
