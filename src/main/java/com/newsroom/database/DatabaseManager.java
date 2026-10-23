package com.newsroom.database;

import com.newsroom.config.AppConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite Database Connection and Schema Lifecycle Manager.
 * 
 * Demonstrates:
 * - Encapsulation of JDBC connection pooling and driver registration.
 * - Versioned schema migration strategy.
 * - Resource safety with try-with-resources.
 * - WAL mode and concurrency optimizations for desktop responsiveness.
 */
public class DatabaseManager {
    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static volatile DatabaseManager instance;

    private final String dbUrl;
    private Connection connection;

    public DatabaseManager() {
        this(AppConfig.getInstance().getDbPath());
    }

    public DatabaseManager(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + (dbPath != null ? dbPath : "newsroom.db");
        initDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    private synchronized void initDatabase() {
        try {
            // Explicitly load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Open or create connection
            Connection conn = getConnection();

            // Enable WAL mode for high concurrency and foreign keys
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
            }

            // Run schema initialization / migration
            applyMigrations(conn);

        } catch (ClassNotFoundException e) {
            System.err.println("Fatal: SQLite JDBC driver not found on classpath: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
        }
        return connection;
    }

    private void applyMigrations(Connection conn) throws SQLException {
        // 1. Create schema_version table if missing
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    description TEXT
                );
            """);
        }

        int currentVersion = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (rs.next()) {
                currentVersion = rs.getInt(1);
            }
        }

        if (currentVersion < 1) {
            migrateToVersion1(conn);
        }
    }

    private void migrateToVersion1(Connection conn) throws SQLException {
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (Statement stmt = conn.createStatement()) {
            // Table: articles
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS articles (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    summary TEXT,
                    content TEXT,
                    author TEXT,
                    source TEXT,
                    category TEXT NOT NULL,
                    published_at TIMESTAMP NOT NULL,
                    reading_time INTEGER NOT NULL,
                    image_url TEXT,
                    original_url TEXT,
                    is_featured INTEGER DEFAULT 0,
                    is_trending INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Table: bookmarks
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bookmarks (
                    article_id TEXT PRIMARY KEY,
                    bookmarked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    notes TEXT,
                    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
                );
            """);

            // Table: reading_history
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reading_history (
                    article_id TEXT PRIMARY KEY,
                    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    access_count INTEGER DEFAULT 1,
                    FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
                );
            """);

            // Table: reader_settings
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reader_settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT NOT NULL
                );
            """);

            // Indexes for accelerated editorial queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_articles_cat_pub ON articles(category, published_at DESC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_articles_pub ON articles(published_at DESC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_articles_orig_url ON articles(original_url);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bookmarks_date ON bookmarks(bookmarked_at DESC);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_history_date ON reading_history(last_accessed_at DESC);");

            // Record schema migration
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO schema_version (version, description) VALUES (?, ?)")) {
                ps.setInt(1, 1);
                ps.setString(2, "Initial Phase 2 schema: articles, bookmarks, history, settings");
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("SQLite database initialized successfully at schema version 1.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }

    public long getDatabaseFileSizeBytes() {
        File file = new File(AppConfig.getInstance().getDbPath());
        return file.exists() ? file.length() : 0;
    }
}
