package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.models.Territory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    
    public void createTables() {
        try (Connection conn = getConnection()) {
            // 创建服务器表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %sservers (
                    id VARCHAR(64) PRIMARY KEY,
                    port INT NOT NULL,
                    last_online TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix));

            // 创建国家表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %snations (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(32) UNIQUE NOT NULL,
                    owner_uuid VARCHAR(36) NOT NULL,
                    server_id VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    level INT DEFAULT 1,
                    balance DECIMAL(20,2) DEFAULT 0,
                    spawn_world VARCHAR(64),
                    spawn_x DOUBLE,
                    spawn_y DOUBLE,
                    spawn_z DOUBLE,
                    spawn_yaw FLOAT,
                    spawn_pitch FLOAT,
                    description TEXT,
                    FOREIGN KEY (server_id) REFERENCES %sservers(id)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));

            // 创建成员表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %snation_members (
                    nation_id BIGINT,
                    player_uuid VARCHAR(36),
                    rank VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
                    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (nation_id, player_uuid),
                    FOREIGN KEY (nation_id) REFERENCES %snations(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));

            // 创建领土表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %sterritories (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    nation_id BIGINT NOT NULL,
                    world_name VARCHAR(64) NOT NULL,
                    center_x INT NOT NULL,
                    center_z INT NOT NULL,
                    radius INT NOT NULL,
                    FOREIGN KEY (nation_id) REFERENCES %snations(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));

            // 创建交易记录表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %stransactions (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    nation_id BIGINT NOT NULL,
                    player_uuid VARCHAR(36),
                    type VARCHAR(32) NOT NULL,
                    amount DECIMAL(20,2) NOT NULL,
                    description TEXT,
                    timestamp BIGINT NOT NULL,
                    FOREIGN KEY (nation_id) REFERENCES %snations(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));

            // 创建建筑表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %sbuildings (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    nation_id BIGINT NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    level INT NOT NULL DEFAULT 1,
                    world VARCHAR(64) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    created_time BIGINT NOT NULL,
                    FOREIGN KEY (nation_id) REFERENCES %snations(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));

            // 创建建筑升级记录表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %sbuilding_upgrades (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    building_id BIGINT NOT NULL,
                    from_level INT NOT NULL,
                    to_level INT NOT NULL,
                    upgrade_time BIGINT NOT NULL,
                    FOREIGN KEY (building_id) REFERENCES %sbuildings(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));

            // 创建建筑资��消耗记录表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %sbuilding_resources (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    building_id BIGINT NOT NULL,
                    material VARCHAR(32) NOT NULL,
                    amount INT NOT NULL,
                    action VARCHAR(16) NOT NULL,
                    action_time BIGINT NOT NULL,
                    FOREIGN KEY (building_id) REFERENCES %sbuildings(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));

            // 创建 NPC 表
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %snpcs (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        building_id BIGINT NOT NULL,
                        type VARCHAR(32) NOT NULL,
                        citizens_id INT NOT NULL,
                        level INT NOT NULL DEFAULT 1,
                        experience INT NOT NULL DEFAULT 0,
                        happiness INT NOT NULL DEFAULT 100,
                        energy INT NOT NULL DEFAULT 100,
                        work_position_x DOUBLE,
                        work_position_y DOUBLE,
                        work_position_z DOUBLE,
                        work_position_world VARCHAR(64),
                        rest_position_x DOUBLE,
                        rest_position_y DOUBLE,
                        rest_position_z DOUBLE,
                        rest_position_world VARCHAR(64),
                        state VARCHAR(32) NOT NULL DEFAULT 'WORKING',
                        FOREIGN KEY (building_id) REFERENCES %sbuildings(id) ON DELETE CASCADE
                    )
                """.formatted(tablePrefix, tablePrefix));
            }

            // 创建 NPC 工作记录表
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %snpc_work_records (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        npc_id BIGINT NOT NULL,
                        work_type VARCHAR(32) NOT NULL,
                        start_time BIGINT NOT NULL,
                        end_time BIGINT,
                        efficiency DOUBLE NOT NULL,
                        FOREIGN KEY (npc_id) REFERENCES %snpcs(id) ON DELETE CASCADE
                    )
                """.formatted(tablePrefix, tablePrefix));
            }

            // 创建 NPC 工资记录表
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %snpc_salary_records (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        npc_id BIGINT NOT NULL,
                        amount INT NOT NULL,
                        pay_time BIGINT NOT NULL,
                        FOREIGN KEY (npc_id) REFERENCES %snpcs(id) ON DELETE CASCADE
                    )
                """.formatted(tablePrefix, tablePrefix));
            }

            // 创建NPC背包表
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS %snpc_inventories (
                    npc_id BIGINT NOT NULL,
                    slot INT NOT NULL,
                    item_type VARCHAR(64) NOT NULL,
                    amount INT NOT NULL,
                    PRIMARY KEY (npc_id, slot),
                    FOREIGN KEY (npc_id) REFERENCES %snpcs(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """.formatted(tablePrefix, tablePrefix));
            }

            // 创建士兵表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %ssoldiers (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    uuid VARCHAR(36) NOT NULL,
                    type VARCHAR(32) NOT NULL,
                    barracks_id BIGINT NOT NULL,
                    name VARCHAR(64) NOT NULL,
                    level INT NOT NULL DEFAULT 1,
                    experience INT NOT NULL DEFAULT 0,
                    training_end_time BIGINT DEFAULT NULL,
                    training_barracks_id BIGINT DEFAULT NULL,
                    FOREIGN KEY (barracks_id) REFERENCES %sbuildings(id) ON DELETE CASCADE,
                    FOREIGN KEY (training_barracks_id) REFERENCES %sbuildings(id) ON DELETE SET NULL
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix, tablePrefix));
            
            // 士兵统计数据表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %ssoldier_stats (
                    soldier_id BIGINT PRIMARY KEY,
                    kills INT NOT NULL DEFAULT 0,
                    deaths INT NOT NULL DEFAULT 0,
                    battles_won INT NOT NULL DEFAULT 0,
                    battles_lost INT NOT NULL DEFAULT 0,
                    damage_dealt DOUBLE NOT NULL DEFAULT 0,
                    damage_taken DOUBLE NOT NULL DEFAULT 0,
                    FOREIGN KEY (soldier_id) REFERENCES %ssoldiers(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));
            
            // 士兵训练记录表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %ssoldier_training (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    soldier_id BIGINT NOT NULL,
                    start_time BIGINT NOT NULL,
                    end_time BIGINT NOT NULL,
                    training_type VARCHAR(32) NOT NULL,
                    exp_gained INT NOT NULL DEFAULT 0,
                    FOREIGN KEY (soldier_id) REFERENCES %ssoldiers(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix));
            
            // 战斗记录表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %sbattles (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    attacker_nation_id BIGINT NOT NULL,
                    defender_nation_id BIGINT NOT NULL,
                    battle_time BIGINT NOT NULL,
                    winner_nation_id BIGINT NOT NULL,
                    attack_power DOUBLE NOT NULL,
                    defense_power DOUBLE NOT NULL,
                    FOREIGN KEY (attacker_nation_id) REFERENCES %snations(id) ON DELETE CASCADE,
                    FOREIGN KEY (defender_nation_id) REFERENCES %snations(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix, tablePrefix));
            
            // 战斗参与记录表
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS %sbattle_participants (
                    battle_id BIGINT NOT NULL,
                    soldier_id BIGINT NOT NULL,
                    is_attacker BOOLEAN NOT NULL,
                    survived BOOLEAN NOT NULL,
                    damage_dealt DOUBLE NOT NULL DEFAULT 0,
                    damage_taken DOUBLE NOT NULL DEFAULT 0,
                    PRIMARY KEY (battle_id, soldier_id),
                    FOREIGN KEY (battle_id) REFERENCES %sbattles(id) ON DELETE CASCADE,
                    FOREIGN KEY (soldier_id) REFERENCES %ssoldiers(id) ON DELETE CASCADE
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
            """.formatted(tablePrefix, tablePrefix, tablePrefix));
            
        } catch (SQLException e) {
            plugin.getLogger().severe("创建数据表失败: " + e.getMessage());
            e.printStackTrace();
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
    
    public void loadNationData(Nation nation) {
        try (Connection conn = getConnection()) {
            plugin.getLogger().info("正在加载国家 " + nation.getName() + " 的数据...");
            
            // 加载传送点数据
            PreparedStatement spawnStmt = conn.prepareStatement(
                "SELECT spawn_world, spawn_x, spawn_y, spawn_z, spawn_yaw, spawn_pitch " +
                "FROM " + tablePrefix + "nations WHERE id = ?"
            );
            spawnStmt.setLong(1, nation.getId());
            ResultSet spawnRs = spawnStmt.executeQuery();
            
            if (spawnRs.next()) {
                String worldName = spawnRs.getString("spawn_world");
                if (worldName != null) {
                    // 先保存世界名称和坐标信息
                    double x = spawnRs.getDouble("spawn_x");
                    double y = spawnRs.getDouble("spawn_y");
                    double z = spawnRs.getDouble("spawn_z");
                    float yaw = spawnRs.getFloat("spawn_yaw");
                    float pitch = spawnRs.getFloat("spawn_pitch");
                    
                    nation.setSpawnWorldName(worldName);
                    nation.setSpawnCoordinates(x, y, z, yaw, pitch);
                    
                    // 尝试获取世界并创建Location对象
                    World world = plugin.getServer().getWorld(worldName);
                    if (world != null) {
                        Location spawnPoint = new Location(world, x, y, z, yaw, pitch);
                        nation.setSpawnPoint(spawnPoint);
                        plugin.getLogger().info("成功加载国家 " + nation.getName() + " 的传送点: " + 
                            String.format("%.1f, %.1f, %.1f in %s", x, y, z, worldName));
                    } else {
                        plugin.getLogger().warning("国家 " + nation.getName() + " 的传送点世界 " + 
                            worldName + " 未加载，将在世界加载后自动设置 (坐标: " + 
                            String.format("%.1f, %.1f, %.1f)", x, y, z));
                    }
                } else {
                    plugin.getLogger().info("国家 " + nation.getName() + " 未设置传送点");
                }
            }
            
            // 加载领地数据
            PreparedStatement territoryStmt = conn.prepareStatement(
                "SELECT * FROM " + tablePrefix + "territories WHERE nation_id = ?"
            );
            territoryStmt.setLong(1, nation.getId());
            ResultSet territoryRs = territoryStmt.executeQuery();
            
            if (territoryRs.next()) {
                String worldName = territoryRs.getString("world_name");
                World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    Territory territory = new Territory(
                        territoryRs.getLong("id"),
                        nation.getId(),
                        worldName,
                        territoryRs.getInt("center_x"),
                        territoryRs.getInt("center_z"),
                        territoryRs.getInt("radius")
                    );
                    nation.setTerritory(territory);
                    plugin.getLogger().info("成功加载国家 " + nation.getName() + " 的领地数据: " + 
                        String.format("中心(%d, %d), 半径%d, 世界%s", 
                            territory.getCenterX(), territory.getCenterZ(), 
                            territory.getRadius(), territory.getWorldName()));
                } else {
                    plugin.getLogger().warning("国家 " + nation.getName() + " 的领地世界 " + 
                        worldName + " 未加载，领地功能将暂时不可用");
                }
            } else {
                plugin.getLogger().info("国家 " + nation.getName() + " 未设置领地");
            }
            
            plugin.getLogger().info("国家 " + nation.getName() + " 的数据加载完成");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("加载国家 " + nation.getName() + " 的数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveNationData(Nation nation) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 保存传送点数据
                PreparedStatement spawnStmt = conn.prepareStatement(
                    "UPDATE " + tablePrefix + "nations SET " +
                    "spawn_world = ?, spawn_x = ?, spawn_y = ?, spawn_z = ?, spawn_yaw = ?, spawn_pitch = ? " +
                    "WHERE id = ?"
                );
                
                if (nation.getSpawnWorldName() != null) {
                    spawnStmt.setString(1, nation.getSpawnWorldName());
                    spawnStmt.setDouble(2, nation.getSpawnX());
                    spawnStmt.setDouble(3, nation.getSpawnY());
                    spawnStmt.setDouble(4, nation.getSpawnZ());
                    spawnStmt.setFloat(5, nation.getSpawnYaw());
                    spawnStmt.setFloat(6, nation.getSpawnPitch());
                } else {
                    spawnStmt.setNull(1, java.sql.Types.VARCHAR);
                    spawnStmt.setNull(2, java.sql.Types.DOUBLE);
                    spawnStmt.setNull(3, java.sql.Types.DOUBLE);
                    spawnStmt.setNull(4, java.sql.Types.DOUBLE);
                    spawnStmt.setNull(5, java.sql.Types.FLOAT);
                    spawnStmt.setNull(6, java.sql.Types.FLOAT);
                }
                spawnStmt.setLong(7, nation.getId());
                spawnStmt.executeUpdate();
                
                // 保存领地数据
                if (nation.getTerritory() != null) {
                    PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO " + tablePrefix + "territories " +
                        "(nation_id, world_name, center_x, center_z, radius) VALUES (?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE world_name=?, center_x=?, center_z=?, radius=?"
                    );
                    
                    stmt.setLong(1, nation.getId());
                    stmt.setString(2, nation.getTerritory().getWorldName());
                    stmt.setInt(3, nation.getTerritory().getCenterX());
                    stmt.setInt(4, nation.getTerritory().getCenterZ());
                    stmt.setInt(5, nation.getTerritory().getRadius());
                    
                    stmt.setString(6, nation.getTerritory().getWorldName());
                    stmt.setInt(7, nation.getTerritory().getCenterX());
                    stmt.setInt(8, nation.getTerritory().getCenterZ());
                    stmt.setInt(9, nation.getTerritory().getRadius());
                    
                    stmt.executeUpdate();
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存国家 " + nation.getName() + " 的数据时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 