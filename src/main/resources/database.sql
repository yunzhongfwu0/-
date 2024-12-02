-- 创建数据库
CREATE DATABASE IF NOT EXISTS nations DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'nations'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON nations.* TO 'nations'@'localhost';
FLUSH PRIVILEGES;

-- 使用数据库
USE nations;

-- 创建服务器表
CREATE TABLE IF NOT EXISTS nc_servers (
    id VARCHAR(64) PRIMARY KEY,
    port INT NOT NULL,
    last_online TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建国家表
CREATE TABLE IF NOT EXISTS nc_nations (
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
    FOREIGN KEY (server_id) REFERENCES nc_servers(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建成员表
CREATE TABLE IF NOT EXISTS nc_nation_members (
    nation_id BIGINT,
    player_uuid VARCHAR(36),
    rank VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (nation_id, player_uuid),
    FOREIGN KEY (nation_id) REFERENCES nc_nations(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建领土表
CREATE TABLE IF NOT EXISTS nc_territories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nation_id BIGINT NOT NULL,
    world_name VARCHAR(64) NOT NULL,
    center_x INT NOT NULL,
    center_z INT NOT NULL,
    radius INT NOT NULL,
    FOREIGN KEY (nation_id) REFERENCES nc_nations(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建交易记录表
CREATE TABLE IF NOT EXISTS nc_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nation_id BIGINT NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    description TEXT,
    timestamp BIGINT NOT NULL,
    FOREIGN KEY (nation_id) REFERENCES nc_nations(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建建筑表
CREATE TABLE IF NOT EXISTS nc_buildings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nation_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    level INT NOT NULL DEFAULT 1,
    world VARCHAR(64) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    created_time BIGINT NOT NULL,
    FOREIGN KEY (nation_id) REFERENCES nc_nations(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建建筑升级记录表
CREATE TABLE IF NOT EXISTS nc_building_upgrades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id BIGINT NOT NULL,
    from_level INT NOT NULL,
    to_level INT NOT NULL,
    upgrade_time BIGINT NOT NULL,
    FOREIGN KEY (building_id) REFERENCES nc_buildings(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建建筑资源消耗记录表
CREATE TABLE IF NOT EXISTS nc_building_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id BIGINT NOT NULL,
    material VARCHAR(32) NOT NULL,
    amount INT NOT NULL,
    action VARCHAR(16) NOT NULL, -- BUILD/UPGRADE/DEMOLISH
    action_time BIGINT NOT NULL,
    FOREIGN KEY (building_id) REFERENCES nc_buildings(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建NPC背包表
CREATE TABLE IF NOT EXISTS nc_npc_inventories (
    npc_id BIGINT NOT NULL,
    slot INT NOT NULL,
    item_type VARCHAR(64) NOT NULL,
    amount INT NOT NULL,
    PRIMARY KEY (npc_id, slot),
    FOREIGN KEY (npc_id) REFERENCES nc_npcs(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; 