package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.models.NationRank;
import com.nations.core.models.Territory;
import com.nations.core.models.Transaction;
import com.nations.core.utils.MessageUtil;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.sql.Types;
import com.nations.core.models.Transaction.TransactionType;

public class NationManager {
    
    private final NationsCore plugin;
    private final Map<String, Nation> nationsByName = new HashMap<>();
    private final Map<UUID, Nation> nationsByOwner = new HashMap<>();
    private final Map<Long, Set<UUID>> joinRequests = new HashMap<>();
    private final Map<UUID, Nation> playerNations = new HashMap<>();
    
    public NationManager(NationsCore plugin) {
        this.plugin = plugin;
        loadNations();
    }
    
    private void loadNations() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT n.*, s.port as server_port " +
                "FROM " + plugin.getDatabaseManager().getTablePrefix() + "nations n " +
                "LEFT JOIN " + plugin.getDatabaseManager().getTablePrefix() + "servers s ON n.server_id = s.id"
            );
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String worldName = rs.getString("spawn_world");
                Location spawnPoint = null;
                
                if (worldName != null) {
                    spawnPoint = plugin.getWorldManager().createLocation(
                        worldName,
                        rs.getDouble("spawn_x"),
                        rs.getDouble("spawn_y"),
                        rs.getDouble("spawn_z"),
                        rs.getFloat("spawn_yaw"),
                        rs.getFloat("spawn_pitch")
                    );
                }
                
                Nation nation = new Nation(
                    rs.getLong("id"),
                    rs.getString("name"),
                    UUID.fromString(rs.getString("owner_uuid")),
                    rs.getInt("level"),
                    rs.getDouble("balance"),
                    rs.getString("server_id"),
                    rs.getInt("server_port"),
                    rs.getString("server_id").equals(plugin.getDatabaseManager().getServerId())
                );
                
                if (worldName != null) {
                    nation.setSpawnWorldName(worldName);
                    nation.setSpawnCoordinates(
                        rs.getDouble("spawn_x"),
                        rs.getDouble("spawn_y"),
                        rs.getDouble("spawn_z"),
                        rs.getFloat("spawn_yaw"),
                        rs.getFloat("spawn_pitch")
                    );
                    if (spawnPoint != null) {
                        nation.setSpawnPoint(spawnPoint);
                    }
                }
                
                // 加载领地数据
                Territory territory = loadTerritory(nation);
                nation.setTerritory(territory);
                nationsByName.put(nation.getName().toLowerCase(), nation);
                nationsByOwner.put(nation.getOwnerUUID(), nation);
                
                // 为国主建立映射关系
                playerNations.put(nation.getOwnerUUID(), nation);
                
                plugin.getLogger().info(String.format(
                    "已加载国家: %s (ID: %d) %s %s",
                    nation.getName(),
                    nation.getId(),
                    nation.isLocalServer() ? "[本服]" : "[子服:" + nation.getServerId() + "]",
                    nation.isSpawnPointValid() ? "[传送点已加载]" : 
                        (worldName != null ? "[等待世界加载: " + worldName + "]" : "[无传送点]")
                ));
            }
            
            // 加载成员数据时建立映射
            PreparedStatement memberStmt = conn.prepareStatement(
                "SELECT nation_id, player_uuid, rank FROM " + 
                plugin.getDatabaseManager().getTablePrefix() + "nation_members"
            );
            ResultSet memberRs = memberStmt.executeQuery();
            
            while (memberRs.next()) {
                long nationId = memberRs.getLong("nation_id");
                UUID playerUuid = UUID.fromString(memberRs.getString("player_uuid"));
                String rank = memberRs.getString("rank");
                
                Nation nation = nationsByName.values().stream()
                    .filter(n -> n.getId() == nationId)
                    .findFirst()
                    .orElse(null);
                    
                if (nation != null) {
                    nation.addMember(playerUuid, rank);
                    playerNations.put(playerUuid, nation);
                }
            }
            
            plugin.getLogger().info("共加载了 " + nationsByName.size() + " 个国家");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("加载国家数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 如果需要单独载领土数据
    private Territory loadTerritory(Nation nation) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "territories WHERE nation_id = ?"
            );
            stmt.setLong(1, nation.getId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Territory territory = new Territory(
                    rs.getLong("id"),
                    nation.getId(),
                    rs.getString("world_name"),
                    rs.getInt("center_x"),
                    rs.getInt("center_z"),
                    rs.getInt("radius")
                );
                nation.setTerritory(territory); // 直接设置给 nation
                return territory;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("加载领土数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean deleteNation(Nation nation) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. 清除加入申请
                clearJoinRequests(nation);
                
                // 2. 删除所有成员记录
                PreparedStatement memberStmt = conn.prepareStatement(
                    "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nation_members WHERE nation_id = ?"
                );
                memberStmt.setLong(1, nation.getId());
                memberStmt.executeUpdate();

                // 3. 删除领土记录
                PreparedStatement territoryStmt = conn.prepareStatement(
                    "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                    "territories WHERE nation_id = ?"
                );
                territoryStmt.setLong(1, nation.getId());
                territoryStmt.executeUpdate();

                // 4. 除国家记录
                PreparedStatement nationStmt = conn.prepareStatement(
                    "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations WHERE id = ?"
                );
                nationStmt.setLong(1, nation.getId());

                if (nationStmt.executeUpdate() > 0) {
                    // 清除领土标记
                    if (nation.getTerritory() != null) {
                        nation.getTerritory().clearBorder(plugin.getServer().getWorld(nation.getTerritory().getWorldName()));
                    }
                    
                    // 从缓存中移除
                    nationsByName.remove(nation.getName().toLowerCase());
                    nationsByOwner.remove(nation.getOwnerUUID());
                    
                    // 清理所有成员的映射关系
                    nation.getMembers().keySet().forEach(playerNations::remove);
                    // 清理国主的映射关系
                    playerNations.remove(nation.getOwnerUUID());
                    
                    conn.commit();
                    return true;
                }

                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("删除国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean renameNation(Nation nation, String newName, double cost) {
        // 检查新名称是否已存在
        if (nationsByName.containsKey(newName.toLowerCase())) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 开事务
            conn.setAutoCommit(false);
            try {
                // 更新名称
                PreparedStatement renameStmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + "nations SET name = ? WHERE id = ?"
                );
                renameStmt.setString(1, newName);
                renameStmt.setLong(2, nation.getId());
                renameStmt.executeUpdate();
                
                // 扣除费用
                PreparedStatement balanceStmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations SET balance = balance - ? WHERE id = ?"
                );
                balanceStmt.setDouble(1, cost);
                balanceStmt.setLong(2, nation.getId());
                balanceStmt.executeUpdate();
                
                // 提交事务
                conn.commit();
                
                // 更新缓存
                nationsByName.remove(nation.getName().toLowerCase());
                nation.setBalance(nation.getBalance() - cost);
                nation.setName(newName);
                nationsByName.put(newName.toLowerCase(), nation);
                
                return true;
            } catch (SQLException e) {
                // 发生错误时回滚事务
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("重命名国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean renameNation(Nation nation, String newName) {
        double renameCost = plugin.getConfig().getDouble("nations.rename-cost", 5000.0);
        if (nation.getBalance() < renameCost) {
            return false;
        }
        return renameNation(nation, newName, renameCost);
    }
    
    public boolean setSpawnPoint(Nation nation, Location location) {
        // 检查是否在国家领土内
        if (!nation.isInTerritory(location)) {
            return false;
        }

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "nations SET spawn_world = ?, spawn_x = ?, spawn_y = ?, spawn_z = ?, " +
                "spawn_yaw = ?, spawn_pitch = ? WHERE id = ?"
            );
            
            stmt.setString(1, location.getWorld().getName());
            stmt.setDouble(2, location.getX());
            stmt.setDouble(3, location.getY());
            stmt.setDouble(4, location.getZ());
            stmt.setFloat(5, location.getYaw());
            stmt.setFloat(6, location.getPitch());
            stmt.setLong(7, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                nation.setSpawnPoint(location);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("设置传送点失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public Optional<Nation> getNationByName(String name) {
        return Optional.ofNullable(nationsByName.get(name.toLowerCase()));
    }
    
    public Optional<Nation> getNationByOwner(UUID ownerUUID) {
        return Optional.ofNullable(nationsByOwner.get(ownerUUID));
    }
    
    public Optional<Nation> getNationByPlayer(Player player) {
        // 先检查是否是国家所有者
        Optional<Nation> ownerNation = nationsByOwner.values().stream()
            .filter(nation -> nation.getOwnerUUID().equals(player.getUniqueId()))
            .findFirst();
        if (ownerNation.isPresent()) {
            return ownerNation;
        }

        // 再检查否是国家成员
        return nationsByName.values().stream()
            .filter(nation -> nation.isMember(player.getUniqueId()))
            .findFirst();
    }
    
    public void teleportToNation(Player player, Nation nation) {
        Location spawnPoint = nation.getSpawnPoint();
        
        if (!nation.isSpawnPointValid()) {
            if (nation.fixSpawnPoint()) {
                spawnPoint = nation.getSpawnPoint();
            } else {
                player.sendMessage(MessageUtil.error("无法传送：传送点所在世界未加载"));
                return;
            }
        }
        
        player.teleport(spawnPoint);
        player.sendMessage(MessageUtil.success("已传送到国家传送点"));
    }

    /**
     * 检查位置是否安全
     */
    private boolean isSafeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        
        // 检查脚下的方块
        Location ground = loc.clone().subtract(0, 1, 0);
        if (!ground.getBlock().getType().isSolid()) {
            return false;
        }
        
        // 检查玩家位置和头顶的方块
        return loc.getBlock().getType().isAir() && 
               loc.clone().add(0, 1, 0).getBlock().getType().isAir();
    }
    
    public boolean setBalance(Nation nation, double amount) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + "nations SET balance = ? WHERE id = ?"
            );
            stmt.setDouble(1, amount);
            stmt.setLong(2, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                nation.setBalance(amount);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("设置余额失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean addBalance(Nation nation, double amount) {
        return setBalance(nation, nation.getBalance() + amount);
    }
    
    public boolean takeBalance(Nation nation, double amount) {
        if (nation.getBalance() < amount) {
            return false;
        }
        return setBalance(nation, nation.getBalance() - amount);
    }
    
    public boolean transferOwnership(Nation nation, Player newOwner) {
        // 检查新主人是否已有国家
        if (getNationByPlayer(newOwner).isPresent()) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + "nations SET owner_uuid = ? WHERE id = ?"
            );
            stmt.setString(1, newOwner.getUniqueId().toString());
            stmt.setLong(2, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                nationsByOwner.remove(nation.getOwnerUUID());
                nation.setOwnerUUID(newOwner.getUniqueId());
                nationsByOwner.put(newOwner.getUniqueId(), nation);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("转让国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean transferMoney(Nation from, Nation to, double amount) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 除发送方的金额
                PreparedStatement deductStmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations SET balance = balance - ? WHERE id = ? AND balance >= ?"
                );
                deductStmt.setDouble(1, amount);
                deductStmt.setLong(2, from.getId());
                deductStmt.setDouble(3, amount);
                
                // 增加接收方的金额
                PreparedStatement addStmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations SET balance = balance + ? WHERE id = ?"
                );
                addStmt.setDouble(1, amount);
                addStmt.setLong(2, to.getId());
                
                if (deductStmt.executeUpdate() > 0 && addStmt.executeUpdate() > 0) {
                    conn.commit();
                    from.setBalance(from.getBalance() - amount);
                    to.setBalance(to.getBalance() + amount);
                    return true;
                }
                
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("转账失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // 添加获取所有国家的方法
    public Collection<Nation> getAllNations() {
        return nationsByName.values();
    }
    
    // 获取本服的国家
    public Collection<Nation> getLocalNations() {
        return nationsByName.values().stream()
            .filter(Nation::isLocalServer)
            .collect(Collectors.toList());
    }
    
    // 获取其他服的国家
    public Collection<Nation> getRemoteNations() {
        return nationsByName.values().stream()
            .filter(nation -> !nation.isLocalServer())
            .collect(Collectors.toList());
    }
    
    public List<Nation> getPlayerNations(UUID playerUuid) {
        List<Nation> playerNations = new ArrayList<>();
        for (Nation nation : nationsByName.values()) {
            if (nation.isMember(playerUuid)) {
                playerNations.add(nation);
            }
        }
        return playerNations;
    }
    
    public boolean addMember(Nation nation, UUID playerUuid, String rank) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                "nation_members (nation_id, player_uuid, rank) VALUES (?, ?, ?)"
            );
            stmt.setLong(1, nation.getId());
            stmt.setString(2, playerUuid.toString());
            stmt.setString(3, rank);
            
            if (stmt.executeUpdate() > 0) {
                nation.addMember(playerUuid, rank);
                // 建立新的映射关系
                playerNations.put(playerUuid, nation);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("添加成员失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean removeMember(Nation nation, UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "nation_members WHERE nation_id = ? AND player_uuid = ?"
            );
            stmt.setLong(1, nation.getId());
            stmt.setString(2, playerUuid.toString());
            
            if (stmt.executeUpdate() > 0) {
                nation.removeMember(playerUuid);
                // 从映射中移除玩家
                playerNations.remove(playerUuid);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("移除成员失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    private void loadNationMembers(Nation nation) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "nation_members WHERE nation_id = ?"
            );
            stmt.setLong(1, nation.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                String rank = rs.getString("rank");
                nation.addMember(playerUuid, rank);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("加载国家成员失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean promoteMember(Nation nation, UUID playerUuid, NationRank newRank) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nation_members SET rank = ? WHERE nation_id = ? AND player_uuid = ?"
                );
                stmt.setString(1, newRank.name());
                stmt.setLong(2, nation.getId());
                stmt.setString(3, playerUuid.toString());

                if (stmt.executeUpdate() > 0) {
                    nation.promoteMember(playerUuid, newRank);
                    
                    // 通知玩家
                    Player player = plugin.getServer().getPlayer(playerUuid);
                    if (player != null) {
                        player.sendMessage(MessageUtil.success("你的职位已被提升为: " + newRank.getDisplayName()));
                    }
                    
                    conn.commit();
                    return true;
                }

                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning(MessageUtil.error("更新成员职位失败: " + e.getMessage()));
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean createNationWithTerritory(Player owner, String nationName, Location center) {
        String lowerName = nationName.toLowerCase();
        if (nationsByName.containsKey(lowerName)) {
            owner.sendMessage(MessageUtil.error("已存在同名的国家！"));
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT 1 FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "nations WHERE LOWER(name) = LOWER(?)"
            );
            checkStmt.setString(1, nationName);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                owner.sendMessage(MessageUtil.error("已存在同名的国家！"));
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("检查国家名称时发生错误: " + e.getMessage());
            return false;
        }
        
        if (nationsByOwner.containsKey(owner.getUniqueId())) {
            owner.sendMessage(MessageUtil.error("你已经拥有一个国家了！"));
            return false;
        }
        
        // 检查创建费用
        ConfigurationSection costConfig = plugin.getConfig().getConfigurationSection("nations.creation");
        double money = costConfig.getDouble("money", 0);
        
        // 检查金钱
        if (money > 0 && !plugin.getVaultEconomy().has(owner, money)) {
            owner.sendMessage("§c你没有足够的金币！需要: " + money);
            return false;
        }
        
        // 检查物品
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        if (items != null) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                if (!hasEnoughItems(owner, material, amount)) {
                    owner.sendMessage("§c你没有足够的 " + itemName + "！需要: " + amount);
                    return false;
                }
            }
        }
        
        // 创建新的领土对象
        Territory newTerritory = Territory.createNew(
            center.getWorld().getName(),
            center.getBlockX(),
            center.getBlockZ(),
            15 // 初始半径15（30*30）
        );
        
        // 检查领土重叠
        if (isOverlapping(newTerritory)) {
            owner.sendMessage(MessageUtil.error("该区域与其他国家的领土重叠！"));
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 扣除金钱
                if (money > 0) {
                    if (!plugin.getVaultEconomy().withdrawPlayer(owner, money).transactionSuccess()) {
                        conn.rollback();
                        return false;
                    }
                }
                
                // 扣除物品
                if (items != null) {
                    for (String itemName : items.getKeys(false)) {
                        Material material = Material.valueOf(itemName);
                        int amount = items.getInt(itemName);
                        removeItems(owner, material, amount);
                    }
                }
                
                // 1. 创建国家记录
                PreparedStatement nationStmt = conn.prepareStatement(
                    "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations (name, owner_uuid, server_id, level, balance) VALUES (?, ?, ?, 1, 0.0)",
                    PreparedStatement.RETURN_GENERATED_KEYS
                );
                
                nationStmt.setString(1, nationName);
                nationStmt.setString(2, owner.getUniqueId().toString());
                nationStmt.setString(3, plugin.getDatabaseManager().getServerId());
                nationStmt.executeUpdate();
                
                ResultSet rs = nationStmt.getGeneratedKeys();
                if (!rs.next()) {
                    conn.rollback();
                    return false;
                }
                
                long nationId = rs.getLong(1);
                
                // 2. 创建领土信息
                PreparedStatement territoryStmt = conn.prepareStatement(
                    "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                    "territories (nation_id, world_name, center_x, center_z, radius) VALUES (?, ?, ?, ?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS
                );
                
                territoryStmt.setLong(1, nationId);
                territoryStmt.setString(2, newTerritory.getWorldName());
                territoryStmt.setInt(3, newTerritory.getCenterX());
                territoryStmt.setInt(4, newTerritory.getCenterZ());
                territoryStmt.setInt(5, newTerritory.getRadius());
                territoryStmt.executeUpdate();
                
                ResultSet territoryRs = territoryStmt.getGeneratedKeys();
                if (!territoryRs.next()) {
                    conn.rollback();
                    return false;
                }
                
                Territory territory = new Territory(
                    territoryRs.getLong(1),
                    nationId,
                    newTerritory.getWorldName(),
                    newTerritory.getCenterX(),
                    newTerritory.getCenterZ(),
                    newTerritory.getRadius()
                );
                
                // 3. 创建Nation对象并缓存
                Nation nation = new Nation(
                    nationId,
                    nationName,
                    owner.getUniqueId(),
                    1,
                    0.0,
                    plugin.getDatabaseManager().getServerId(),
                    plugin.getServer().getPort(),
                    true
                );
                nation.setTerritory(territory);
                
                nationsByName.put(nationName.toLowerCase(), nation);
                nationsByOwner.put(owner.getUniqueId(), nation);
                playerNations.put(owner.getUniqueId(), nation);
                
                // 4. 标记领土边界
                territory.markBorder(center.getWorld());
                
                // 清除玩家的所有申请
                clearAllPlayerRequests(owner.getUniqueId());
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("创建国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean isOverlapping(Territory newTerritory) {
        return nationsByName.values().stream()
            .map(Nation::getTerritory)
            .anyMatch(t -> t != null && t.overlaps(newTerritory));
    }
    
    public boolean expandTerritory(Nation nation, int newRadius) {
        if (newRadius > nation.getMaxRadius()) {
            return false;
        }
        
        Territory currentTerritory = nation.getTerritory();
        Territory expandedTerritory = new Territory(
            currentTerritory.getId(),
            currentTerritory.getNationId(),
            currentTerritory.getWorldName(),
            currentTerritory.getCenterX(),
            currentTerritory.getCenterZ(),
            newRadius
        );
        
        if (isOverlapping(expandedTerritory)) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "territories SET radius = ? WHERE id = ?"
            );
            
            stmt.setInt(1, newRadius);
            stmt.setLong(2, currentTerritory.getId());
            
            if (stmt.executeUpdate() > 0) {
                nation.setTerritory(expandedTerritory);
                expandedTerritory.markBorder(
                    plugin.getServer().getWorld(expandedTerritory.getWorldName())
                );
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("扩展领土失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean deposit(Nation nation, Player player, double amount) {
        if (!plugin.getVaultEconomy().has(player, amount)) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 扣除玩家金钱
                if (!plugin.getVaultEconomy().withdrawPlayer(player, amount).transactionSuccess()) {
                    conn.rollback();
                    return false;
                }
                
                // 更新国库余额
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations SET balance = balance + ? WHERE id = ?"
                );
                updateStmt.setDouble(1, amount);
                updateStmt.setLong(2, nation.getId());
                
                if (updateStmt.executeUpdate() > 0) {
                    // 记录交易
                    PreparedStatement transStmt = conn.prepareStatement(
                        "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                        "transactions (nation_id, player_uuid, type, amount, description, timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
                    );
                    
                    transStmt.setLong(1, nation.getId());
                    transStmt.setString(2, player.getUniqueId().toString());
                    transStmt.setString(3, Transaction.TransactionType.DEPOSIT.name());
                    transStmt.setDouble(4, amount);
                    transStmt.setString(5, "玩家 " + player.getName() + " 向国库存入金钱");
                    transStmt.setLong(6, System.currentTimeMillis());
                    transStmt.executeUpdate();
                    
                    nation.setBalance(nation.getBalance() + amount);
                    conn.commit();
                    return true;
                }
                
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("存入金钱失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean withdraw(Nation nation, Player player, double amount) {
        if (!nation.hasPermission(player.getUniqueId(), "nation.withdraw")) {
            return false;
        }
        
        if (nation.getBalance() < amount) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 更新国库余额
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations SET balance = balance - ? WHERE id = ? AND balance >= ?"
                );
                updateStmt.setDouble(1, amount);
                updateStmt.setLong(2, nation.getId());
                updateStmt.setDouble(3, amount);
                
                if (updateStmt.executeUpdate() > 0) {
                    // 给予玩家金钱
                    if (!plugin.getVaultEconomy().depositPlayer(player, amount).transactionSuccess()) {
                        conn.rollback();
                        return false;
                    }
                    
                    // 记录交易
                    PreparedStatement transStmt = conn.prepareStatement(
                        "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                        "transactions (nation_id, player_uuid, type, amount, description, timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
                    );
                    
                    transStmt.setLong(1, nation.getId());
                    transStmt.setString(2, player.getUniqueId().toString());
                    transStmt.setString(3, Transaction.TransactionType.WITHDRAW.name());
                    transStmt.setDouble(4, amount);
                    transStmt.setString(5, "玩家 " + player.getName() + " 从国库取钱");
                    transStmt.setLong(6, System.currentTimeMillis());
                    transStmt.executeUpdate();
                    
                    nation.setBalance(nation.getBalance() - amount);
                    conn.commit();
                    return true;
                }
                
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("取出金钱失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public List<Transaction> getTransactions(Nation nation, int page, int pageSize) {
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "transactions WHERE nation_id = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?"
            );
            
            stmt.setLong(1, nation.getId());
            stmt.setInt(2, pageSize);
            stmt.setInt(3, (page - 1) * pageSize);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID playerUuid = null;
                String playerUuidStr = rs.getString("player_uuid");
                if (playerUuidStr != null) {
                    playerUuid = UUID.fromString(playerUuidStr);
                }
                
                transactions.add(new Transaction(
                    rs.getLong("id"),
                    nation.getId(),
                    playerUuid,
                    TransactionType.valueOf(rs.getString("type")),
                    rs.getDouble("amount"),
                    rs.getString("description"),
                    rs.getLong("timestamp")
                ));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("获取交易记录失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }

    public boolean checkCreationCost(Player player) {
        ConfigurationSection costConfig = plugin.getConfig().getConfigurationSection("nations.creation");
        double money = costConfig.getDouble("money", 0);
        
        // 检查金钱
        if (money > 0 && !plugin.getVaultEconomy().has(player, money)) {
            return false;
        }
        
        // 检查物品
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        if (items != null) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                if (!hasEnoughItems(player, material, amount)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public boolean deductCreationCost(Player player) {
        ConfigurationSection costConfig = plugin.getConfig().getConfigurationSection("nations.creation");
        double money = costConfig.getDouble("money", 0);
        
        // 扣除金钱
        if (money > 0) {
            if (!plugin.getVaultEconomy().withdrawPlayer(player, money).transactionSuccess()) {
                return false;
            }
        }
        
        // 扣除物品
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        if (items != null) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                removeItems(player, material, amount);
            }
        }
        
        return true;
    }

    public boolean canUpgradeNation(Nation nation, Player player) {
        int nextLevel = nation.getLevel() + 1;
        ConfigurationSection levelConfig = plugin.getConfig().getConfigurationSection("nations.levels." + nextLevel);
        
        if (levelConfig == null) {
            return false; // 已达到最高等级
        }
        
        ConfigurationSection costConfig = levelConfig.getConfigurationSection("upgrade-cost");
        double money = costConfig.getDouble("money", 0);
        
        // 检查国库金钱
        if (money > 0 && nation.getBalance() < money) {
            return false;
        }
        
        // 检查物品
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        if (items != null) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                if (!hasEnoughItems(player, material, amount)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    public boolean upgradeNation(Nation nation, Player player) {
        int nextLevel = nation.getLevel() + 1;
        ConfigurationSection levelConfig = plugin.getConfig().getConfigurationSection("nations.levels." + nextLevel);
        
        if (levelConfig == null) {
            return false;
        }
        
        ConfigurationSection costConfig = levelConfig.getConfigurationSection("upgrade-cost");
        double money = costConfig.getDouble("money", 0);
        
        // 检查国库金钱
        if (money > 0 && nation.getBalance() < money) {
            player.sendMessage("§c国库余额不足！需要: " + money);
            return false;
        }
        
        // 检查物品
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        if (items != null) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                if (!hasEnoughItems(player, material, amount)) {
                    player.sendMessage("§c你没有足够的 " + itemName + "！需要: " + amount);
                    return false;
                }
            }
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 扣除国库金钱
                if (money > 0) {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                        "nations SET balance = balance - ?, level = ? WHERE id = ? AND balance >= ?"
                    );
                    stmt.setDouble(1, money);
                    stmt.setInt(2, nextLevel);
                    stmt.setLong(3, nation.getId());
                    stmt.setDouble(4, money);
                    
                    if (stmt.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                
                // 扣除物品
                if (items != null) {
                    for (String itemName : items.getKeys(false)) {
                        Material material = Material.valueOf(itemName);
                        int amount = items.getInt(itemName);
                        removeItems(player, material, amount);
                    }
                }
                
                nation.setLevel(nextLevel);
                nation.setBalance(nation.getBalance() - money);
                
                // 记录交易
                logTransaction(conn, nation, player.getUniqueId(), 
                    Transaction.TransactionType.WITHDRAW, money, 
                    "升级国家到 " + nextLevel + " 级");
                
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("升级国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasEnoughItems(Player player, Material material, int amount) {
        return player.getInventory().all(material).values().stream()
            .mapToInt(ItemStack::getAmount)
            .sum() >= amount;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.updateInventory();
    }

    private void logTransaction(Connection conn, Nation nation, UUID playerUuid, 
                              Transaction.TransactionType type, double amount, String description) 
            throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
            "transactions (nation_id, player_uuid, type, amount, description, timestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?)"
        );
        
        stmt.setLong(1, nation.getId());
        stmt.setString(2, playerUuid.toString());
        stmt.setString(3, type.name());
        stmt.setDouble(4, amount);
        stmt.setString(5, description);
        stmt.setLong(6, System.currentTimeMillis());
        
        stmt.executeUpdate();
    }

    public boolean transferNation(Nation nation, Player newOwner) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 检查新所有者是否已有国家
                if (nationsByOwner.containsKey(newOwner.getUniqueId())) {
                    return false;
                }
                
                // 更新数据库中的所有者
                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "nations SET owner_uuid = ? WHERE id = ?"
                );
                stmt.setString(1, newOwner.getUniqueId().toString());
                stmt.setLong(2, nation.getId());
                
                if (stmt.executeUpdate() > 0) {
                    // 更新缓存
                    UUID oldOwner = nation.getOwnerUUID();
                    nationsByOwner.remove(oldOwner);
                    nationsByOwner.put(newOwner.getUniqueId(), nation);
                    
                    // 更新Nation对象
                    nation.setOwnerUUID(newOwner.getUniqueId());
                    
                    // 如果旧所有者在线，通知他
                    Player oldOwnerPlayer = plugin.getServer().getPlayer(oldOwner);
                    if (oldOwnerPlayer != null) {
                        oldOwnerPlayer.sendMessage("§e你的国家已被转让给 " + newOwner.getName());
                    }
                    
                    // 通知新所有者
                    newOwner.sendMessage("§a你已成为国家 " + nation.getName() + " 的新领袖！");
                    
                    // 广播消息
                    plugin.getServer().broadcast(
                        Component.text("§e" + newOwner.getName() + " 成为了国家 " + nation.getName() + " 的新领袖！")
                    );
                    
                    conn.commit();
                    return true;
                }
                
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("转让国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean setTerritorySize(Nation nation, int radius) {
        if (nation.getTerritory() == null) return false;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "territories SET radius = ? WHERE nation_id = ?"
            );
            stmt.setInt(1, radius);
            stmt.setLong(2, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                nation.getTerritory().setRadius(radius);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("设置领土范围失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean setTerritoryCenter(Nation nation, Location location) {
        if (nation.getTerritory() == null) return false;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "territories SET world_name = ?, center_x = ?, center_z = ? WHERE nation_id = ?"
            );
            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockZ());
            stmt.setLong(4, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                nation.getTerritory().setWorldName(location.getWorld().getName());
                nation.getTerritory().setCenterX(location.getBlockX());
                nation.getTerritory().setCenterZ(location.getBlockZ());
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("设置领土中心失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean setMemberRank(Nation nation, UUID playerUuid, NationRank rank) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "nation_members SET rank = ? WHERE nation_id = ? AND player_uuid = ?"
            );
            stmt.setString(1, rank.name());
            stmt.setLong(2, nation.getId());
            stmt.setString(3, playerUuid.toString());
            
            if (stmt.executeUpdate() > 0) {
                nation.getMembers().get(playerUuid).setRank(rank);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("设置成员职位失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public int getOnlineNationsCount() {
        return (int) getAllNations().stream()
            .filter(nation -> Bukkit.getPlayer(nation.getOwnerUUID()) != null)
            .count();
    }

    public int getTotalTerritoryArea() {
        return getAllNations().stream()
            .filter(nation -> nation.getTerritory() != null)
            .mapToInt(nation -> {
                int radius = nation.getTerritory().getRadius();
                return radius * radius * 4;
            })
            .sum();
    }

    public double getTotalBalance() {
        return getAllNations().stream()
            .mapToDouble(Nation::getBalance)
            .sum();
    }

    public double getTodayTransactions() {
        long todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT SUM(amount) FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "transactions WHERE timestamp >= ?"
            );
            stmt.setLong(1, todayStart);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取今日交易额失败: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalTransactions() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTablePrefix() + "transactions"
            );
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取总交易次数失败: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalPlayers() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(DISTINCT player_uuid) FROM " + 
                plugin.getDatabaseManager().getTablePrefix() + "nation_members"
            );
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("获取总玩家数失败: " + e.getMessage());
        }
        return 0;
    }

    public int getPlayersWithoutNation() {
        int totalPlayers = Bukkit.getOfflinePlayers().length;
        return totalPlayers - getTotalPlayers();
    }

    /**
     * 添加加入申请
     */
    public boolean addJoinRequest(Nation nation, UUID playerUuid) {
        Set<UUID> requests = joinRequests.computeIfAbsent(nation.getId(), k -> new HashSet<>());
        return requests.add(playerUuid);
    }

    /**
     * 移除加入申请
     */
    public boolean removeJoinRequest(Nation nation, UUID playerUuid) {
        Set<UUID> requests = joinRequests.get(nation.getId());
        return requests != null && requests.remove(playerUuid);
    }

    /**
     * 检查是否有加入申请
     */
    public boolean hasJoinRequest(Nation nation, UUID playerUuid) {
        Set<UUID> requests = joinRequests.get(nation.getId());
        return requests != null && requests.contains(playerUuid);
    }

    /**
     * 获取所有加入申请
     */
    public List<UUID> getJoinRequests(Nation nation) {
        Set<UUID> requests = joinRequests.get(nation.getId());
        return requests != null ? new ArrayList<>(requests) : new ArrayList<>();
    }

    /**
     * 清除国家的所有加入申请
     */
    public void clearJoinRequests(Nation nation) {
        joinRequests.remove(nation.getId());
    }

    /**
     * 清除玩家的所有加入申请
     */
    public void clearAllPlayerRequests(UUID playerUuid) {
        // 遍历所有国家的申请列表，移除该玩家的申请
        for (Set<UUID> requests : joinRequests.values()) {
            requests.remove(playerUuid);
        }
    }

    /**
     * 通过玩家UUID获取国家
     */
    public Optional<Nation> getNationByUUID(UUID playerUuid) {
        return Optional.ofNullable(playerNations.get(playerUuid));
    }

    public Nation getNationById(long id) {
        return nationsByName.values().stream()
            .filter(nation -> nation.getId() == id)
            .findFirst()
            .orElse(null);
    }

    public void recordTransaction(Nation nation, UUID playerUuid, Transaction.TransactionType type, double amount, String description) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                "transactions (nation_id, player_uuid, type, amount, description, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?)"
            );
            
            stmt.setLong(1, nation.getId());
            if (playerUuid != null) {
                stmt.setString(2, playerUuid.toString());
            } else {
                stmt.setNull(2, Types.VARCHAR);
            }
            stmt.setString(3, type.name());
            stmt.setDouble(4, amount);
            stmt.setString(5, description);
            stmt.setLong(6, System.currentTimeMillis());
            
            stmt.executeUpdate();
            
            // 更新国家余额
            updateNationBalance(nation);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("记录交易失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateNationBalance(Nation nation) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "nations SET balance = ? WHERE id = ?"
            );
            
            stmt.setDouble(1, nation.getBalance());
            stmt.setLong(2, nation.getId());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("更新国家余额失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 