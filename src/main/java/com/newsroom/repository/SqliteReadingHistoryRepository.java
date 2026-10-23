package com.newsroom.repository;

import com.newsroom.database.DatabaseManager;
import com.newsroom.model.Article;
import com.newsroom.model.Category;
import com.newsroom.model.HistoryEntry;

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
 * SQLite JDBC implementation of ReadingHistoryRepository.
 * Automatically deduplicates and promotes accessed articles to the top of the history stack.
 */
public class SqliteReadingHistoryRepository implements ReadingHistoryRepository {
    private final DatabaseManager dbManager;

    public SqliteReadingHistoryRepository() {
        this(DatabaseManager.getInstance());
    }

    public SqliteReadingHistoryRepository(DatabaseManager dbManager) {
        this.dbManager = Objects.requireNonNull(dbManager, "DatabaseManager must not be null");
    }

    @Override
    public void recordAccess(String articleId) {
        if (articleId == null || articleId.isEmpty()) return;

        String sql = """
            INSERT INTO reading_history (article_id, last_accessed_at, access_count)
            VALUES (?, ?, 1)
            ON CONFLICT(article_id) DO UPDATE SET
                last_accessed_at = excluded.last_accessed_at,
                access_count = reading_history.access_count + 1;
        """;

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, articleId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error recording reading history in SQLite: " + e.getMessage());
        }
    }

    @Override
    public List<HistoryEntry> getHistory() {
        String sql = """
            SELECT a.id, a.title, a.summary, a.content, a.author, a.source,
                   a.category, a.published_at, a.reading_time, a.image_url, a.original_url,
                   a.is_featured, a.is_trending,
                   (SELECT COUNT(*) FROM bookmarks b WHERE b.article_id = a.id) AS is_bookmarked,
                   h.last_accessed_at, h.access_count
            FROM reading_history h
            INNER JOIN articles a ON h.article_id = a.id
            ORDER BY h.last_accessed_at DESC, h.rowid DESC
        """;

        List<HistoryEntry> results = new ArrayList<>();
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

                Article article = Article.builder()
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

                HistoryEntry entry = new HistoryEntry(article);
                results.add(entry);
            }
        } catch (SQLException e) {
            System.err.println("Error loading reading history: " + e.getMessage());
        }
        return Collections.unmodifiableList(results);
    }

    @Override
    public void clearHistory() {
        String sql = "DELETE FROM reading_history";
        try (Statement stmt = dbManager.getConnection().createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error clearing reading history: " + e.getMessage());
        }
    }

    @Override
    public int getHistoryCount() {
        String sql = "SELECT COUNT(*) FROM reading_history";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting reading history: " + e.getMessage());
        }
        return 0;
    }
}
