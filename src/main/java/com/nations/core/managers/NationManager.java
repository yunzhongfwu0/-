package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class NationManager {
    
    private final NationsCore plugin;
    private final Map<String, Nation> nationsByName = new HashMap<>();
    private final Map<UUID, Nation> nationsByOwner = new HashMap<>();
    
    public NationManager(NationsCore plugin) {
        this.plugin = plugin;
        loadNations();
    }
    
    private void loadNations() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT n.*, s.port as server_port FROM " + 
                plugin.getDatabaseManager().getTablePrefix() + "nations n " +
                "LEFT JOIN " + plugin.getDatabaseManager().getTablePrefix() + 
                "servers s ON n.server_id = s.id"
            );
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String serverId = rs.getString("server_id");
                boolean isLocalServer = serverId.equals(plugin.getDatabaseManager().getServerId());
                int serverPort = rs.getInt("server_port");
                
                Nation nation;
                if (rs.getString("spawn_world") != null) {
                    nation = new Nation(
                        rs.getLong("id"),
                        rs.getString("name"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getInt("level"),
                        rs.getDouble("balance"),
                        rs.getString("spawn_world"),
                        rs.getDouble("spawn_x"),
                        rs.getDouble("spawn_y"),
                        rs.getDouble("spawn_z"),
                        rs.getFloat("spawn_yaw"),
                        rs.getFloat("spawn_pitch"),
                        serverId,
                        serverPort,
                        isLocalServer
                    );
                } else {
                    nation = new Nation(
                        rs.getLong("id"),
                        rs.getString("name"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getInt("level"),
                        rs.getDouble("balance"),
                        serverId,
                        serverPort,
                        isLocalServer
                    );
                }
                
                nationsByName.put(nation.getName().toLowerCase(), nation);
                nationsByOwner.put(nation.getOwnerUUID(), nation);
                plugin.getLogger().info("已加载国家: " + nation.getName() + 
                    (isLocalServer ? " (本服)" : " (子服:" + serverId + ")"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载国家数据时发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean createNation(Player owner, String nationName) {
        // 检查玩家是否已经拥有国家
        if (nationsByOwner.containsKey(owner.getUniqueId())) {
            return false;
        }
        
        // 检查名称是否已存在
        if (nationsByName.containsKey(nationName.toLowerCase())) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                "nations (name, owner_uuid, server_id, level, balance) VALUES (?, ?, ?, 1, 0.0)",
                PreparedStatement.RETURN_GENERATED_KEYS
            );
            
            stmt.setString(1, nationName);
            stmt.setString(2, owner.getUniqueId().toString());
            stmt.setString(3, plugin.getDatabaseManager().getServerId());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                Nation nation = new Nation(
                    rs.getLong(1),
                    nationName,
                    owner.getUniqueId(),
                    1,
                    0.0,
                    plugin.getDatabaseManager().getServerId(),
                    plugin.getServer().getPort(),
                    true
                );
                nationsByName.put(nationName.toLowerCase(), nation);
                nationsByOwner.put(owner.getUniqueId(), nation);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("创建国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean deleteNation(Nation nation) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + "nations WHERE id = ?"
            );
            stmt.setLong(1, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                nationsByName.remove(nation.getName().toLowerCase());
                nationsByOwner.remove(nation.getOwnerUUID());
                return true;
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
            // 开始事务
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
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "nations SET spawn_world = ?, spawn_x = ?, spawn_y = ?, spawn_z = ?, spawn_yaw = ?, spawn_pitch = ? " +
                "WHERE id = ?"
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
        return getNationByOwner(player.getUniqueId());
    }
    
    public void teleportToNation(Player player, Nation nation) {
        if (nation.getSpawnPoint() == null) {
            player.sendMessage("§c该国家还未设置传送点！");
            return;
        }
        
        player.teleport(nation.getSpawnPoint());
        player.sendMessage("§a已传送至国家 " + nation.getName() + " 的传送点！");
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
                // ���除发送方的金额
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
} 