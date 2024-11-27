package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    
    private final NationsCore plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;
    
    public DatabaseManager(NationsCore plugin) {
        this.plugin = plugin;
        ConfigurationSection dbConfig = plugin.getConfigManager().getDatabase();
        this.tablePrefix = dbConfig.getString("table-prefix", "");
    }
    
    public boolean connect() {
        try {
            plugin.getLogger().info("正在连接到数据库...");
            createDatabase();
            setupDataSource();
            createTables();
            plugin.getLogger().info("数据库连接成功！");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库连接失败！错误信息：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void createDatabase() throws SQLException {
        ConfigurationSection dbConfig = plugin.getConfigManager().getDatabase();
        String dbName = dbConfig.getString("database");
        
        // 首先创建一个不指定数据库的连接
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%s?useSSL=false&allowPublicKeyRetrieval=true",
                dbConfig.getString("host"),
                dbConfig.getString("port")));
        config.setUsername(dbConfig.getString("username"));
        config.setPassword(dbConfig.getString("password"));
        
        try (HikariDataSource tempDataSource = new HikariDataSource(config);
             Connection conn = tempDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 创建数据库（如果不存在）
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            plugin.getLogger().info("成功创建/确认数据库: " + dbName);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("创建数据库失败！请检查MySQL用户权限。");
            throw e;
        }
    }
    
    private void setupDataSource() {
        ConfigurationSection dbConfig = plugin.getConfigManager().getDatabase();
        
        HikariConfig config = new HikariConfig();
        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true",
                dbConfig.getString("host"),
                dbConfig.getString("port"),
                dbConfig.getString("database"));
                
        plugin.getLogger().info("正在连接到数据库: " + jdbcUrl);
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbConfig.getString("username"));
        config.setPassword(dbConfig.getString("password"));
        
        // 设置连接池配置
        config.setMaximumPoolSize(dbConfig.getInt("pool.maximum-pool-size", 10));
        config.setMinimumIdle(dbConfig.getInt("pool.minimum-idle", 5));
        config.setMaxLifetime(dbConfig.getLong("pool.maximum-lifetime", 1800000));
        config.setConnectionTimeout(dbConfig.getLong("pool.connection-timeout", 5000));
        config.setIdleTimeout(dbConfig.getLong("pool.idle-timeout", 600000));
        
        // 设置其他重要属性
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        dataSource = new HikariDataSource(config);
    }
    
    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS " + tablePrefix + "nations (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(32) UNIQUE NOT NULL," +
                "owner_uuid VARCHAR(36) UNIQUE NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "level INT DEFAULT 1," +
                "balance DECIMAL(20,2) DEFAULT 0," +
                "spawn_world VARCHAR(64)," +
                "spawn_x DOUBLE," +
                "spawn_y DOUBLE," +
                "spawn_z DOUBLE," +
                "spawn_yaw FLOAT," +
                "spawn_pitch FLOAT," +
                "description TEXT" +
                ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );
            plugin.getLogger().info("成功创建/确认数据表: " + tablePrefix + "nations");
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("数据库连接已关闭。");
        }
    }
    
    public String getTablePrefix() {
        return tablePrefix;
    }
} 