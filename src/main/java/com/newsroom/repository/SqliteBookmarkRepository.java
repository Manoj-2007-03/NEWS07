package com.newsroom.repository;

import com.newsroom.database.DatabaseManager;
import com.newsroom.model.Article;
import com.newsroom.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * SQLite JDBC implementation of BookmarkRepository.
 * Manages persistent bookmarks with relational foreign key integrity.
 */
public class SqliteBookmarkRepository implements BookmarkRepository {
    private final DatabaseManager dbManager;

    public SqliteBookmarkRepository() {
        this(DatabaseManager.getInstance());
    }

    public SqliteBookmarkRepository(DatabaseManager dbManager) {
        this.dbManager = Objects.requireNonNull(dbManager, "DatabaseManager must not be null");
    }

    @Override
    public void addBookmark(String articleId) {
        addBookmark(articleId, null);
    }

    @Override
    public void addBookmark(String articleId, String notes) {
        if (articleId == null || articleId.isEmpty()) return;
        String sql = "INSERT INTO bookmarks (article_id, bookmarked_at, notes) VALUES (?, CURRENT_TIMESTAMP, ?) " +
                     "ON CONFLICT(article_id) DO UPDATE SET bookmarked_at = CURRENT_TIMESTAMP, notes = excluded.notes;";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, articleId);
            ps.setString(2, notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding bookmark in SQLite: " + e.getMessage());
        }
    }

    @Override
    public void removeBookmark(String articleId) {
        if (articleId == null || articleId.isEmpty()) return;
        String sql = "DELETE FROM bookmarks WHERE article_id = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, articleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing bookmark in SQLite: " + e.getMessage());
        }
    }

    @Override
    public boolean isBookmarked(String articleId) {
        if (articleId == null || articleId.isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM bookmarks WHERE article_id = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking bookmark status: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<Article> getAllBookmarkedArticles() {
        String sql = """
            SELECT a.id, a.title, a.summary, a.content, a.author, a.source,
                   a.category, a.published_at, a.reading_time, a.image_url, a.original_url,
                   a.is_featured, a.is_trending, 1 AS is_bookmarked
            FROM articles a
            INNER JOIN bookmarks b ON a.id = b.article_id
            ORDER BY b.bookmarked_at DESC
        """;

        List<Article> results = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("published_at");
                LocalDateTime publishedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                Category cat;
                try {
                    cat = Category.valueOf(rs.getString("category"));
                } catch (Exception e) {
                    cat = Category.fromDisplayName(rs.getString("category"));
                }

                results.add(Article.builder()
                        .id(rs.getString("id"))
                        .title(rs.getString("title"))
                        .summary(rs.getString("summary"))
                        .content(rs.getString("content"))
                        .author(rs.getString("author"))
                        .source(rs.getString("source"))
                        .category(cat)
                        .publishedAt(publishedAt)
                        .readingTimeMinutes(rs.getInt("reading_time"))
                        .imageUrl(rs.getString("image_url"))
                        .originalUrl(rs.getString("original_url"))
                        .isFeatured(rs.getInt("is_featured") == 1)
                        .isTrending(rs.getInt("is_trending") == 1)
                        .isBookmarked(true)
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Error loading bookmarked articles: " + e.getMessage());
        }
        return Collections.unmodifiableList(results);
    }

    @Override
    public int getBookmarkCount() {
        String sql = "SELECT COUNT(*) FROM bookmarks";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting bookmarks: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void clearAll() {
        try (Statement stmt = dbManager.getConnection().createStatement()) {
            stmt.executeUpdate("DELETE FROM bookmarks");
        } catch (SQLException e) {
            System.err.println("Error clearing bookmarks: " + e.getMessage());
        }
    }
}
