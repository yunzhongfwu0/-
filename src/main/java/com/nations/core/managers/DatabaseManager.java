package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    
    private final NationsCore plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;
    private final String serverId;
    
    public DatabaseManager(NationsCore plugin) {
        this.plugin = plugin;
        ConfigurationSection dbConfig = plugin.getConfigManager().getDatabase();
        this.tablePrefix = dbConfig.getString("table-prefix", "");
        this.serverId = "server_" + plugin.getServer().getPort();
    }
    
    public boolean connect() {
        try {
            plugin.getLogger().info("正在连接到数据库...");
            setupDataSource();
            testConnection();
            createTables();
            plugin.getLogger().info("数据库连接成功！服务器ID: " + serverId);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("数据库连接失败！错误信息：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void setupDataSource() {
        ConfigurationSection dbConfig = plugin.getConfigManager().getDatabase();
        
        HikariConfig config = new HikariConfig();
        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true",
                dbConfig.getString("host"),
                dbConfig.getString("port"),
                dbConfig.getString("database"));
                
        plugin.getLogger().info("正在连接到数据库: " + jdbcUrl);
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbConfig.getString("username"));
        config.setPassword(dbConfig.getString("password"));
        
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        
        dataSource = new HikariDataSource(config);
    }
    
    private void testConnection() throws SQLException {
        try (Connection conn = getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("无法建立有效的数据库连接！");
            }
            plugin.getLogger().info("数据库连接测试成功！");
        }
    }
    
    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // 创建服务器表
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS " + tablePrefix + "servers (" +
                "id VARCHAR(64) PRIMARY KEY," +  // 服务器ID
                "port INT NOT NULL," +           // 服务器端口
                "last_online TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );

            // 创建国家表，添加server_id字段
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS " + tablePrefix + "nations (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "name VARCHAR(32) UNIQUE NOT NULL," +
                "owner_uuid VARCHAR(36) UNIQUE NOT NULL," +
                "server_id VARCHAR(64) NOT NULL," +  // 关联服务器ID
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "level INT DEFAULT 1," +
                "balance DECIMAL(20,2) DEFAULT 0," +
                "spawn_world VARCHAR(64)," +
                "spawn_x DOUBLE," +
                "spawn_y DOUBLE," +
                "spawn_z DOUBLE," +
                "spawn_yaw FLOAT," +
                "spawn_pitch FLOAT," +
                "description TEXT," +
                "FOREIGN KEY (server_id) REFERENCES " + tablePrefix + "servers(id)" +
                ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );

            // 注册/更新当前服务器信息
            PreparedStatement serverStmt = conn.prepareStatement(
                "INSERT INTO " + tablePrefix + "servers (id, port) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE port = ?, last_online = CURRENT_TIMESTAMP"
            );
            serverStmt.setString(1, serverId);
            serverStmt.setInt(2, plugin.getServer().getPort());
            serverStmt.setInt(3, plugin.getServer().getPort());
            serverStmt.executeUpdate();

            plugin.getLogger().info("成功创建/确认数据表并注册服务器信息: " + serverId);
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            plugin.getLogger().info("正在关闭数据库连接池...");
            dataSource.close();
            plugin.getLogger().info("数据库连接池已关闭。服务器ID: " + serverId);
        }
    }
    
    public String getTablePrefix() {
        return tablePrefix;
    }
    
    public String getServerId() {
        return serverId;
    }
} 