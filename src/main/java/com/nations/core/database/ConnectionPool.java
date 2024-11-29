package com.nations.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

public class ConnectionPool {
    private final HikariDataSource dataSource;
    
    public ConnectionPool(ConfigurationSection config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s",
            config.getString("host"),
            config.getInt("port"),
            config.getString("database")));
        hikariConfig.setUsername(config.getString("username"));
        hikariConfig.setPassword(config.getString("password"));
        
        // 连接池优化配置
        hikariConfig.setMaximumPoolSize(config.getInt("pool.maximum-pool-size", 5));
        hikariConfig.setMinimumIdle(config.getInt("pool.minimum-idle", 2));
        hikariConfig.setIdleTimeout(config.getLong("pool.idle-timeout", 300000));
        hikariConfig.setConnectionTimeout(config.getLong("pool.connection-timeout", 5000));
        hikariConfig.setMaxLifetime(config.getLong("pool.max-lifetime", 1800000));
        
        // 性能优化
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        dataSource = new HikariDataSource(hikariConfig);
    }
    
    public HikariDataSource getDataSource() {
        return dataSource;
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
} 