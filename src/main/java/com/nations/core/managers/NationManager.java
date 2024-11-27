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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "nations"
            );
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Nation nation;
                // 检查是否有传送点数据
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
                        rs.getFloat("spawn_pitch")
                    );
                } else {
                    nation = new Nation(
                        rs.getLong("id"),
                        rs.getString("name"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getInt("level"),
                        rs.getDouble("balance")
                    );
                }
                
                nationsByName.put(nation.getName().toLowerCase(), nation);
                nationsByOwner.put(nation.getOwnerUUID(), nation);
                plugin.getLogger().info("已加载国家: " + nation.getName());
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
                "nations (name, owner_uuid, level, balance) VALUES (?, ?, 1, 0.0)",
                PreparedStatement.RETURN_GENERATED_KEYS
            );
            
            stmt.setString(1, nationName);
            stmt.setString(2, owner.getUniqueId().toString());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                Nation nation = new Nation(
                    rs.getLong(1),
                    nationName,
                    owner.getUniqueId(),
                    1,
                    0.0
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
    
    public boolean renameNation(Nation nation, String newName) {
        // 检查新名称是否已存在
        if (nationsByName.containsKey(newName.toLowerCase())) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + "nations SET name = ? WHERE id = ?"
            );
            stmt.setString(1, newName);
            stmt.setLong(2, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                nationsByName.remove(nation.getName().toLowerCase());
                nation.setName(newName);
                nationsByName.put(newName.toLowerCase(), nation);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("重命名国家失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
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
} 