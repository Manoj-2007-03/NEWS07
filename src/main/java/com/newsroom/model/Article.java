package com.newsroom.model;

import com.newsroom.interfaces.Bookmarkable;
import com.newsroom.interfaces.Searchable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Core domain entity representing an editorial news article.
 * 
 * Demonstrates:
 * - Encapsulation: private properties with controlled public accessors.
 * - Abstraction & Polymorphism: implements Searchable and Bookmarkable interfaces.
 * - Builder Pattern: facilitates flexible, readable construction of complex article data.
 * - Phase 2 Extensions: canonical original article URL, full content transparency.
 */
public class Article implements Searchable, Bookmarkable {
    private final String id;
    private final String title;
    private final String summary;
    private final String content;
    private final String author;
    private final String source;
    private final Category category;
    private final LocalDateTime publishedAt;
    private final int readingTimeMinutes;
    private final String imageUrl;
    private final String originalUrl;
    private final boolean isFeatured;
    private final boolean isTrending;
    private boolean isBookmarked;

    private Article(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Article id must not be null");
        this.title = Objects.requireNonNull(builder.title, "Article title must not be null");
        this.summary = builder.summary != null ? builder.summary : "";
        this.content = builder.content != null ? builder.content : "";
        this.author = builder.author != null ? builder.author : "Staff Reporter";
        this.source = builder.source != null ? builder.source : "NEWSROOM Editorial";
        this.category = builder.category != null ? builder.category : Category.LATEST;
        this.publishedAt = builder.publishedAt != null ? builder.publishedAt : LocalDateTime.now();
        this.readingTimeMinutes = Math.max(1, builder.readingTimeMinutes);
        this.imageUrl = builder.imageUrl != null ? builder.imageUrl : "";
        this.originalUrl = builder.originalUrl != null ? builder.originalUrl : "";
        this.isFeatured = builder.isFeatured;
        this.isTrending = builder.isTrending;
        this.isBookmarked = builder.isBookmarked;
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Getters (Encapsulation) ---

    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public String getSource() {
        return source;
    }

    public Category getCategory() {
        return category;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public int getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public boolean hasOriginalUrl() {
        return originalUrl != null && !originalUrl.trim().isEmpty() && originalUrl.startsWith("http");
    }

    public boolean hasFullContent() {
        return content != null && content.trim().length() > (summary != null ? summary.trim().length() + 80 : 150);
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public boolean isTrending() {
        return isTrending;
    }

    @Override
    public boolean isBookmarked() {
        return isBookmarked;
    }

    @Override
    public void setBookmarked(boolean bookmarked) {
        this.isBookmarked = bookmarked;
    }

    // --- Searchable implementation (Polymorphism) ---

    @Override
    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        String q = query.trim().toLowerCase();
        return title.toLowerCase().contains(q)
                || summary.toLowerCase().contains(q)
                || author.toLowerCase().contains(q)
                || source.toLowerCase().contains(q)
                || category.getDisplayName().toLowerCase().contains(q)
                || content.toLowerCase().contains(q);
    }

    @Override
    public String getSearchableContent() {
        return title + " " + summary + " " + author + " " + source + " " + category.getDisplayName();
    }

    // --- Standard Object overrides ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Article other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Article{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", author='" + author + '\'' +
                ", readingTimeMinutes=" + readingTimeMinutes +
                ", originalUrl='" + originalUrl + '\'' +
                '}';
    }

    /**
     * Builder for constructing Article instances.
     */
    public static class Builder {
        private String id;
        private String title;
        private String summary;
        private String content;
        private String author;
        private String source;
        private Category category;
        private LocalDateTime publishedAt;
        private int readingTimeMinutes = 4;
        private String imageUrl = "";
        private String originalUrl = "";
        private boolean isFeatured = false;
        private boolean isTrending = false;
        private boolean isBookmarked = false;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder readingTimeMinutes(int readingTimeMinutes) {
            this.readingTimeMinutes = readingTimeMinutes;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public Builder isFeatured(boolean isFeatured) {
            this.isFeatured = isFeatured;
            return this;
        }

        public Builder isTrending(boolean isTrending) {
            this.isTrending = isTrending;
            return this;
        }

        public Builder isBookmarked(boolean isBookmarked) {
            this.isBookmarked = isBookmarked;
            return this;
        }

        public Article build() {
            return new Article(this);
        }
    }
}
