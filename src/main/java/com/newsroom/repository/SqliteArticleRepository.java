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
import java.util.Optional;

/**
 * SQLite JDBC implementation of ArticleRepository.
 * 
 * Demonstrates:
 * - Data Access Object (DAO) / Repository Pattern.
 * - Parameterized PreparedStatements preventing SQL injection.
 * - Batch operations & transactional integrity.
 * - Row mapping encapsulation.
 */
public class SqliteArticleRepository implements ArticleRepository {
    private final DatabaseManager dbManager;

    public SqliteArticleRepository() {
        this(DatabaseManager.getInstance());
    }

    public SqliteArticleRepository(DatabaseManager dbManager) {
        this.dbManager = Objects.requireNonNull(dbManager, "DatabaseManager must not be null");
    }

    private static final String SELECT_BASE = """
        SELECT a.id, a.title, a.summary, a.content, a.author, a.source,
               a.category, a.published_at, a.reading_time, a.image_url, a.original_url,
               a.is_featured, a.is_trending,
               (SELECT COUNT(*) FROM bookmarks b WHERE b.article_id = a.id) AS is_bookmarked
        FROM articles a
    """;

    @Override
    public void save(Article article) {
        if (article == null) return;
        saveAll(Collections.singletonList(article));
    }

    @Override
    public void saveAll(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;

        String sql = """
            INSERT INTO articles (
                id, title, summary, content, author, source, category,
                published_at, reading_time, image_url, original_url, is_featured, is_trending
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                summary = excluded.summary,
                content = excluded.content,
                author = excluded.author,
                source = excluded.source,
                category = excluded.category,
                published_at = excluded.published_at,
                reading_time = excluded.reading_time,
                image_url = excluded.image_url,
                original_url = excluded.original_url,
                is_featured = CASE WHEN excluded.is_featured = 1 THEN 1 ELSE articles.is_featured END,
                is_trending = CASE WHEN excluded.is_trending = 1 THEN 1 ELSE articles.is_trending END;
        """;

        try {
            Connection conn = dbManager.getConnection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Article a : articles) {
                    ps.setString(1, a.getId());
                    ps.setString(2, a.getTitle());
                    ps.setString(3, a.getSummary());
                    ps.setString(4, a.getContent());
                    ps.setString(5, a.getAuthor());
                    ps.setString(6, a.getSource());
                    ps.setString(7, a.getCategory().name());
                    ps.setTimestamp(8, Timestamp.valueOf(a.getPublishedAt()));
                    ps.setInt(9, a.getReadingTimeMinutes());
                    ps.setString(10, a.getImageUrl());
                    ps.setString(11, a.getOriginalUrl());
                    ps.setInt(12, a.isFeatured() ? 1 : 0);
                    ps.setInt(13, a.isTrending() ? 1 : 0);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            System.err.println("Error saving articles to SQLite: " + e.getMessage());
        }
    }

    @Override
    public Optional<Article> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = SELECT_BASE + " WHERE a.id = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToArticle(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying article by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Article> findByOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) return Optional.empty();
        String sql = SELECT_BASE + " WHERE a.original_url = ? LIMIT 1";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, originalUrl);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToArticle(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying article by originalUrl: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Article> findAll() {
        String sql = SELECT_BASE + " ORDER BY a.published_at DESC";
        return executeQueryList(sql, null);
    }

    @Override
    public List<Article> findByCategory(Category category, int limit) {
        if (category == null || category == Category.HOME || category == Category.LATEST) {
            return findLatestHeadlines(limit);
        }
        String sql = SELECT_BASE + " WHERE a.category = ? ORDER BY a.published_at DESC LIMIT ?";
        return executeQueryList(sql, ps -> {
            ps.setString(1, category.name());
            ps.setInt(2, Math.max(1, limit));
        });
    }

    @Override
    public Optional<Article> findFeatured() {
        String sql = SELECT_BASE + " WHERE a.is_featured = 1 ORDER BY a.published_at DESC LIMIT 1";
        List<Article> list = executeQueryList(sql, null);
        if (!list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        // Fallback to most recent
        List<Article> latest = findLatestHeadlines(1);
        return latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
    }

    @Override
    public List<Article> findTopStories(int limit) {
        String sql = SELECT_BASE + " WHERE a.is_featured = 0 ORDER BY a.published_at DESC LIMIT ?";
        return executeQueryList(sql, ps -> ps.setInt(1, Math.max(1, limit)));
    }

    @Override
    public List<Article> findLatestHeadlines(int limit) {
        String sql = SELECT_BASE + " ORDER BY a.published_at DESC LIMIT ?";
        return executeQueryList(sql, ps -> ps.setInt(1, Math.max(1, limit)));
    }

    @Override
    public List<Article> findTrending(int limit) {
        String sql = SELECT_BASE + " WHERE a.is_trending = 1 ORDER BY a.published_at DESC LIMIT ?";
        return executeQueryList(sql, ps -> ps.setInt(1, Math.max(1, limit)));
    }

    @Override
    public List<Article> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String q = "%" + query.trim().toLowerCase() + "%";
        String sql = SELECT_BASE + """
            WHERE LOWER(a.title) LIKE ?
               OR LOWER(a.summary) LIKE ?
               OR LOWER(a.content) LIKE ?
               OR LOWER(a.author) LIKE ?
               OR LOWER(a.source) LIKE ?
               OR LOWER(a.category) LIKE ?
            ORDER BY a.published_at DESC
        """;

        return executeQueryList(sql, ps -> {
            for (int i = 1; i <= 6; i++) {
                ps.setString(i, q);
            }
        });
    }

    @Override
    public Optional<Article> findNext(Article current) {
        if (current == null) return Optional.empty();
        String sql = SELECT_BASE + " WHERE a.published_at < ? ORDER BY a.published_at DESC LIMIT 1";

        List<Article> list = executeQueryList(sql, ps -> ps.setTimestamp(1, Timestamp.valueOf(current.getPublishedAt())));
        if (!list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        // Wrap around to top
        List<Article> latest = findLatestHeadlines(1);
        return latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
    }

    @Override
    public Optional<Article> findPrevious(Article current) {
        if (current == null) return Optional.empty();
        String sql = SELECT_BASE + " WHERE a.published_at > ? ORDER BY a.published_at ASC LIMIT 1";

        List<Article> list = executeQueryList(sql, ps -> ps.setTimestamp(1, Timestamp.valueOf(current.getPublishedAt())));
        if (!list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        // Wrap around to oldest
        String oldestSql = SELECT_BASE + " ORDER BY a.published_at ASC LIMIT 1";
        List<Article> oldest = executeQueryList(oldestSql, null);
        return oldest.isEmpty() ? Optional.empty() : Optional.of(oldest.get(0));
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM articles";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting articles: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void clearAll() {
        try (Statement stmt = dbManager.getConnection().createStatement()) {
            stmt.executeUpdate("DELETE FROM articles");
        } catch (SQLException e) {
            System.err.println("Error clearing articles: " + e.getMessage());
        }
    }

    private List<Article> executeQueryList(String sql, PreparedStatementSetter setter) {
        List<Article> results = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            if (setter != null) {
                setter.setValues(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToArticle(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error executing article query: " + e.getMessage());
        }
        return results;
    }

    private Article mapResultSetToArticle(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("published_at");
        LocalDateTime publishedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        Category cat;
        try {
            cat = Category.valueOf(rs.getString("category"));
        } catch (Exception e) {
            cat = Category.fromDisplayName(rs.getString("category"));
        }

        return Article.builder()
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
                .isBookmarked(rs.getInt("is_bookmarked") > 0)
                .build();
    }

    @FunctionalInterface
    private interface PreparedStatementSetter {
        void setValues(PreparedStatement ps) throws SQLException;
    }
}
