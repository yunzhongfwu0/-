package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SoldierManager {
    private final NationsCore plugin;
    private final Map<Long, Soldier> soldiers = new HashMap<>();
    private final Map<Long, Set<Soldier>> soldiersByBarracks = new HashMap<>();
    private final Map<UUID, Set<Soldier>> soldiersByPlayer = new HashMap<>();
    private final Map<Long, Set<Soldier>> trainingSlots = new HashMap<>();
    private final Map<Long, Long> trainingEndTimes = new HashMap<>();
    private boolean isLoaded = false;
    
    public SoldierManager(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    public void load() {
        if (!isLoaded) {
            loadSoldiers();
            isLoaded = true;
        }
    }
    
    public boolean recruitSoldier(Player player, Building barracks, SoldierType type, String name) {
        if (barracks.getType() != BuildingType.BARRACKS) {
            return false;
        }
        
        // 检查兵营等级限制
        int maxSoldiers = barracks.getLevel() * 5;
        if (getSoldiersByBarracks(barracks).size() >= maxSoldiers) {
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                "soldiers (uuid, type, barracks_id, name, level, experience) VALUES (?, ?, ?, ?, 1, 0)",
                PreparedStatement.RETURN_GENERATED_KEYS
            );
            
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, type.name());
            stmt.setLong(3, barracks.getId());
            stmt.setString(4, name);
            
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            
            if (rs.next()) {
                long id = rs.getLong(1);
                Soldier soldier = new Soldier(id, player.getUniqueId(), type, barracks, name);
                addSoldierToCache(soldier);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("招募士兵失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    public void saveSoldier(Soldier soldier) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "soldiers SET level = ?, experience = ? WHERE id = ?"
            );
            stmt.setInt(1, soldier.getLevel());
            stmt.setInt(2, soldier.getExperience());
            stmt.setLong(3, soldier.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存士兵数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Soldier getSoldierById(long id) {
        if (!isLoaded) {
            load();
        }
        return soldiers.get(id);
    }
    
    public Set<Soldier> getSoldiersByPlayer(UUID playerUuid) {
        if (!isLoaded) {
            load();
        }
        return soldiersByPlayer.getOrDefault(playerUuid, new HashSet<>());
    }
    
    public Set<Soldier> getSoldiersByBarracks(Building barracks) {
        if (!isLoaded) {
            load();
        }
        return soldiersByBarracks.getOrDefault(barracks.getId(), new HashSet<>());
    }
    
    public boolean dismissSoldier(Soldier soldier) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "soldiers WHERE id = ?"
            );
            stmt.setLong(1, soldier.getId());
            
            if (stmt.executeUpdate() > 0) {
                // 如果士兵正在训练，清除训练位置
                if (isTraining(soldier)) {
                    // 获取训练的兵营ID
                    for (Map.Entry<Long, Set<Soldier>> entry : trainingSlots.entrySet()) {
                        if (entry.getValue().contains(soldier)) {
                            entry.getValue().remove(soldier);
                            break;
                        }
                    }
                    trainingEndTimes.remove(soldier.getId());
                }
                
                // 从缓存中移除
                removeSoldierFromCache(soldier);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("解雇士兵失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    private void loadSoldiers() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "soldiers"
            );
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong("id");
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                SoldierType type = SoldierType.valueOf(rs.getString("type"));
                long barracksId = rs.getLong("barracks_id");
                String name = rs.getString("name");
                
                Building barracks = plugin.getBuildingManager().getBuildingById(barracksId);
                if (barracks != null) {
                    Soldier soldier = new Soldier(id, uuid, type, barracks, name);
                    soldier.gainExperience(rs.getInt("experience"));
                    addSoldierToCache(soldier);
                    
                    // 加载训练状态
                    long trainingEndTime = rs.getLong("training_end_time");
                    if (!rs.wasNull() && trainingEndTime > System.currentTimeMillis()) {
                        long trainingBarracksId = rs.getLong("training_barracks_id");
                        Building trainingBarracks = plugin.getBuildingManager().getBuildingById(trainingBarracksId);
                        if (trainingBarracks != null) {
                            trainingEndTimes.put(id, trainingEndTime);
                            trainingSlots.computeIfAbsent(trainingBarracksId, k -> new HashSet<>())
                                .add(soldier);
                                
                            // 重新启动训练任务
                            long remainingTime = trainingEndTime - System.currentTimeMillis();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    trainingSlots.get(trainingBarracks.getId()).remove(soldier);
                                    trainingEndTimes.remove(soldier.getId());
                                    
                                    // 清除数据库中的训练状态
                                    try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                                        PreparedStatement stmt = conn.prepareStatement(
                                            "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                                            "soldiers SET training_end_time = NULL, training_barracks_id = NULL WHERE id = ?"
                                        );
                                        stmt.setLong(1, soldier.getId());
                                        stmt.executeUpdate();
                                    } catch (SQLException e) {
                                        plugin.getLogger().severe("清除训练状态失败: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                    
                                    // 计算经验值（考虑训练加成）
                                    double bonus = 1 + trainingBarracks.getBonuses().getOrDefault("training_bonus", 0.0);
                                    int experience = (int)(50 * soldier.getLevel() * bonus);
                                    soldier.gainExperience(experience);
                                    saveSoldier(soldier);
                                    
                                    Player owner = plugin.getServer().getPlayer(soldier.getUuid());
                                    if (owner != null) {
                                        owner.sendMessage("§a士兵 " + soldier.getName() + " 完成训练！获得 " + experience + " 经验");
                                    }
                                }
                            }.runTaskLater(plugin, remainingTime / 50); // 转换为tick
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载士兵数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void addSoldierToCache(Soldier soldier) {
        soldiers.put(soldier.getId(), soldier);
        soldiersByBarracks.computeIfAbsent(soldier.getBarracks().getId(), k -> new HashSet<>())
            .add(soldier);
        soldiersByPlayer.computeIfAbsent(soldier.getUuid(), k -> new HashSet<>())
            .add(soldier);
    }
    
    private void removeSoldierFromCache(Soldier soldier) {
        soldiers.remove(soldier.getId());
        soldiersByBarracks.getOrDefault(soldier.getBarracks().getId(), new HashSet<>())
            .remove(soldier);
        soldiersByPlayer.getOrDefault(soldier.getUuid(), new HashSet<>())
            .remove(soldier);
    }
    
    public int getTrainingSlots(Building barracks) {
        return trainingSlots.getOrDefault(barracks.getId(), new HashSet<>()).size();
    }
    
    public boolean startTraining(Soldier soldier, Building barracks) {
        int usedSlots = getTrainingSlots(barracks);
        double maxSlotsDouble = barracks.getBonuses().getOrDefault("training_slots", 2.0);
        int maxSlots = (int)Math.floor(maxSlotsDouble);
        
        if (usedSlots >= maxSlots) {
            return false;
        }
        
        // 添加到训练位
        trainingSlots.computeIfAbsent(barracks.getId(), k -> new HashSet<>())
            .add(soldier);
        
        // 计算训练时间（考虑速度减少）
        int baseTime = 15 + (soldier.getLevel() - 1) * 5;
        int actualTime = (int)(baseTime * (1 - barracks.getBonuses().getOrDefault("training_speed", 0.0)));
        long endTime = System.currentTimeMillis() + (actualTime * 60 * 1000);
        trainingEndTimes.put(soldier.getId(), endTime);
        
        // 保存训练状态到数据库
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "soldiers SET training_end_time = ?, training_barracks_id = ? WHERE id = ?"
            );
            stmt.setLong(1, endTime);
            stmt.setLong(2, barracks.getId());
            stmt.setLong(3, soldier.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存训练状态失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 启动训练任务
        new BukkitRunnable() {
            @Override
            public void run() {
                trainingSlots.get(barracks.getId()).remove(soldier);
                trainingEndTimes.remove(soldier.getId());
                
                // 清除数据库中的训练状态
                try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                        "soldiers SET training_end_time = NULL, training_barracks_id = NULL WHERE id = ?"
                    );
                    stmt.setLong(1, soldier.getId());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("清除训练状态失败: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // 计算经验值（考虑训练加成）
                double bonus = 1 + barracks.getBonuses().getOrDefault("training_bonus", 0.0);
                int experience = (int)(50 * soldier.getLevel() * bonus);
                soldier.gainExperience(experience);
                saveSoldier(soldier);
                
                Player owner = plugin.getServer().getPlayer(soldier.getUuid());
                if (owner != null) {
                    owner.sendMessage("§a士兵 " + soldier.getName() + " 完成训练！得 " + experience + " 经验");
                }
            }
        }.runTaskLater(plugin, actualTime * 1200L);
        
        return true;
    }
    
    public boolean isTraining(Soldier soldier) {
        return trainingEndTimes.containsKey(soldier.getId());
    }
    
    public long getTrainingTimeLeft(Soldier soldier) {
        Long endTime = trainingEndTimes.get(soldier.getId());
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    public boolean isLoaded() {
        return isLoaded;
    }
    
    public void clearTrainingByBarracks(Building barracks) {
        // 获取该兵营正在训练的士兵
        Set<Soldier> trainingSoldiers = trainingSlots.getOrDefault(barracks.getId(), new HashSet<>());
        
        // 清除训练状态
        for (Soldier soldier : trainingSoldiers) {
            // 从训练位移除
            trainingSlots.get(barracks.getId()).remove(soldier);
            trainingEndTimes.remove(soldier.getId());
            
            // 清除数据库中的训练状态
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                    "soldiers SET training_end_time = NULL, training_barracks_id = NULL WHERE id = ?"
                );
                stmt.setLong(1, soldier.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("清除训练状态失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 通知玩家
            Player owner = plugin.getServer().getPlayer(soldier.getUuid());
            if (owner != null) {
                owner.sendMessage("§c由于兵营被拆除，士兵 " + soldier.getName() + " 的训练已被取消！");
            }
        }
        
        // 清除该兵营的训练位记录
        trainingSlots.remove(barracks.getId());
    }
    
    public void clearNationSoldiers(Nation nation) {
        // 清除该国家所有建筑中的士兵
        nation.getBuildings().stream()
            .filter(building -> building.getType() == BuildingType.BARRACKS)
            .forEach(barracks -> {
                Set<Soldier> barracksSoldiers = soldiersByBarracks.remove(barracks.getId());
                if (barracksSoldiers != null) {
                    barracksSoldiers.forEach(soldier -> {
                        soldiers.remove(soldier.getId());
                        soldiersByPlayer.getOrDefault(soldier.getUuid(), new HashSet<>()).remove(soldier);
                    });
                }
            });
    }
} 