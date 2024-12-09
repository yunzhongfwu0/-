package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.Building;
import com.nations.core.models.BuildingFunction;
import com.nations.core.models.BuildingType;
import com.nations.core.models.Nation;
import com.nations.core.utils.ItemNameUtil;
import com.nations.core.utils.MessageUtil;
import com.nations.core.utils.HologramUtil;
import com.nations.core.utils.BuildingBorderUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class BuildingManager {
    private final NationsCore plugin;
    private final Map<Long, Building> buildings = new HashMap<>();
    private final Map<Long, Set<Building>> buildingsByNation = new HashMap<>();
    private final Map<Long, BukkitTask> updateTasks = new HashMap<>();
    
    public BuildingManager(NationsCore plugin) {
        this.plugin = plugin;
        loadBuildings();
    }
    
    private void loadBuildings() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 取消所有更新任务
            updateTasks.values().forEach(BukkitTask::cancel);
            updateTasks.clear();
            
            // 清除所有缓存
            buildings.clear();
            buildingsByNation.clear();
            
            // 清除所有全息文字
            HologramUtil.clearAllHolograms();
            
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "buildings"
            );
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Building building = loadBuilding(rs);
                if (building != null) {
                    buildings.put(building.getId(), building);
                    buildingsByNation.computeIfAbsent(building.getNationId(), k -> new HashSet<>())
                        .add(building);
                        
                    // 创建全息显示并启动更新任务
                    HologramUtil.createBuildingHologram(building);
                    startBuildingUpdateTask(building);
                }
            }
            
            plugin.getLogger().info("已加载 " + buildings.size() + " 个建筑");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("加载建筑数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startBuildingUpdateTask(Building building) {
        BukkitTask oldTask = updateTasks.remove(building.getId());
        if (oldTask != null) {
            oldTask.cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!building.isValidBasic()) {
                    cancel();
                    updateTasks.remove(building.getId());
                    HologramUtil.removeBuildingHologram(building.getBaseLocation().clone().add(0, 2, 0));
                    return;
                }
                
                building.updateBonuses();
                
                HologramUtil.updateHologram(building);
            }
        }.runTaskTimer(plugin, 0L, 20L * 5);
        
        updateTasks.put(building.getId(), task);
    }
    
    private boolean checkRequirements(Nation nation, BuildingType type, Player player) {
        // 检查国家等级
        if (nation.getLevel() < type.getMinNationLevel()) {
            player.sendMessage(MessageUtil.error("建造失败！国家等级不足："));
            player.sendMessage(MessageUtil.error("- 需要等级: " + type.getMinNationLevel()));
            player.sendMessage(MessageUtil.error("- 当前等级: " + nation.getLevel()));
            return false;
        }
        
        // 检查前置建筑
        if (type.getRequiredBuilding() != null) {
            Building required = nation.getBuilding(type.getRequiredBuilding());
            if (required == null || required.getLevel() < type.getRequiredBuildingLevel()) {
                player.sendMessage(MessageUtil.error("建造失败！缺少前置建筑："));
                player.sendMessage(MessageUtil.error("- 需建筑: " + type.getRequiredBuilding().getDisplayName() + 
                    " Lv." + type.getRequiredBuildingLevel()));
                if (required != null) {
                    player.sendMessage(MessageUtil.error("- 当前等级: Lv." + required.getLevel()));
                }
                return false;
            }
        }
        
        return true;
    }
    
    private boolean checkResources(Nation nation, BuildingType type, Player player) {
        Map<Material, Integer> costs = type.getBuildCosts();
        boolean hasAll = true;
        StringBuilder message = new StringBuilder("§c建造失败！资源不足：\n");
        
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            Material material = cost.getKey();
            int required = cost.getValue();
            int has = countPlayerItems(player, material);
            
            if (has < required) {
                hasAll = false;
                message.append(MessageUtil.formatResourceRequirement(material, required, has)).append("\n");
            }
        }
        
        if (!hasAll) {
            player.sendMessage(message.toString());
            return false;
        }
        return true;
    }
    
    private void deductResources(Nation nation, BuildingType type) {
        Player owner = plugin.getServer().getPlayer(nation.getOwnerUUID());
        if (owner == null) return;
        
        for (Map.Entry<Material, Integer> cost : type.getBuildCosts().entrySet()) {
            removeItems(owner, cost.getKey(), cost.getValue());
        }
    }
    
    private void placeFoundation(Building building) {
        Location loc = building.getBaseLocation();
        
        // 放置建筑结构
        building.getType().placeStructure(loc);
        
        // 添加粒子效果
        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks++ >= 40) {
                    cancel();
                    return;
                }
                
                angle += Math.PI / 8;
                double radius = building.getSize() / 2.0;
                
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                    double x = Math.cos(i + angle) * radius;
                    double z = Math.sin(i + angle) * radius;
                    Location particleLoc = loc.clone().add(x, 0.5, z);
                    loc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private int calculateSize(BuildingType type, int level) {
        return type.getBaseSize() + (level - 1);
    }
    
    private int countPlayerItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    private void removeItems(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.getInventory().setContents(contents);
    }
    
    private boolean checkBuildingLocation(Nation nation, BuildingType type, Location location) {
        // 检查是否在国家领土内
        if (nation.getTerritory() == null || !nation.getTerritory().contains(location)) {
            return false;
        }
        
        // 计算新建筑的占地范围
        int size = type.getBaseSize();
        int halfSize = size / 2;
        Location baseLocation = location.clone();
        
        // 检查是否与其他建筑重叠
        for (Building existingBuilding : nation.getBuildings()) {
            Location existingLoc = existingBuilding.getBaseLocation();
            int existingSize = existingBuilding.getSize();
            int existingHalfSize = existingSize / 2;
            
            // 检查两个建筑的边界是否重叠
            if (Math.abs(baseLocation.getBlockX() - existingLoc.getBlockX()) <= (halfSize + existingHalfSize + 2) && 
                Math.abs(baseLocation.getBlockZ() - existingLoc.getBlockZ()) <= (halfSize + existingHalfSize + 2)) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean createBuilding(Nation nation, BuildingType type, Location location) {
        Player owner = plugin.getServer().getPlayer(nation.getOwnerUUID());
        if (owner == null) return false;
        
        // 检查前置条件
        if (!checkRequirements(nation, type, owner)) {
            return false;
        }
        
        // 检查资源
        if (!checkResources(nation, type, owner)) {
            return false;
        }
        
        // 检查建筑位置
        if (!checkBuildingLocation(nation, type, location)) {
            owner.sendMessage(MessageUtil.error("建造失败！位置不合适："));
            if (nation.getTerritory() == null) {
                owner.sendMessage(MessageUtil.error("- 国家还未设置领土"));
            } else if (!nation.getTerritory().contains(location)) {
                owner.sendMessage(MessageUtil.error("- 必须在国家领土范围内建造"));
            } else {
                owner.sendMessage(MessageUtil.error("- 与其他建筑太近，需要保持至少2格间距"));
            }
            return false;
        }
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 扣除资源
                deductResources(nation, type);
                
                // 插入建筑记录
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                    "buildings (nation_id, type, level, world, x, y, z, created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                
                stmt.setLong(1, nation.getId());
                stmt.setString(2, type.name());
                stmt.setInt(3, 1);
                stmt.setString(4, location.getWorld().getName());
                stmt.setDouble(5, location.getX());
                stmt.setDouble(6, location.getY());
                stmt.setDouble(7, location.getZ());
                stmt.setLong(8, System.currentTimeMillis() / 1000); // Unix时间戳(秒)
                
                stmt.executeUpdate();
                
                // 获取生成的ID
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    long buildingId = rs.getLong(1);
                    
                    // 创建建筑实例
                    Building building = new Building(
                        buildingId,
                        nation.getId(),
                        type,
                        1,
                        location,
                        calculateSize(type, 1)
                    );
                    
                    // 添加到缓存
                    buildings.put(buildingId, building);
                    buildingsByNation.computeIfAbsent(nation.getId(), k -> new HashSet<>())
                        .add(building);
                    nation.addBuilding(building);
                    
                    // 放置建筑结构
                    placeFoundation(building);
                    
                    // 创建全息显示
                    HologramUtil.createBuildingHologram(building);
                    
                    // 启动更新任务
                    startBuildingUpdateTask(building);
                    
                    // 初始化建筑功能
                    new BuildingFunction(building).runTasks();
                    
                    // 显示建筑边界
                    BuildingBorderUtil.showBuildingBorder(building);
                    
                    // 发送成功消息
                    owner.sendMessage(MessageUtil.success("建造成功！"));
                    
                    conn.commit();
                    return true;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("创建建筑失败: " + e.getMessage());
            e.printStackTrace();
            owner.sendMessage(MessageUtil.error("建造失败！请联系管理员检查后台错误。"));
        }
        
        return false;
    }
    
    public void reloadBuildings() {
        updateTasks.values().forEach(BukkitTask::cancel);
        updateTasks.clear();
        
        loadBuildings();
    }
    
    public boolean demolishBuilding(Nation nation, Building building) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 1. 删除建筑前的清理工作
            switch (building.getType()) {
                case BARRACKS -> {
                    // 清除该兵营的训练状态
                    plugin.getSoldierManager().clearTrainingByBarracks(building);
                }
                case MARKET -> {
                    // 取消所有交易
                    plugin.getTradeManager().cancelTradesByBuilding(building);
                }
                case WAREHOUSE -> {
                    // 清空仓库物品
                    plugin.getStorageManager().clearStorage(building);
                }
                case FARM -> {
                    // 停止农场生产
                    plugin.getBuildingManager().getBuildingFunction(building).stopProduction();
                }
            }

            // 2. 删除建筑数据
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "buildings WHERE id = ? AND nation_id = ?"
            );
            stmt.setLong(1, building.getId());
            stmt.setLong(2, nation.getId());
            
            if (stmt.executeUpdate() > 0) {
                // 3. 删除全息文字
                HologramUtil.removeHologram(building);
                
                // 4. 解雇所有工人
                plugin.getNPCManager().dismissAllWorkers(building);
                
                // 5. 从缓存中移除
                buildings.remove(building.getId());
                buildingsByNation.get(nation.getId()).remove(building);
                
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("删除建筑失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public void saveBuilding(Building building) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "buildings SET level = ? WHERE id = ?"
            );
            stmt.setInt(1, building.getLevel());
            stmt.setLong(2, building.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("保存建筑失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Building getBuildingById(long id) {
        return buildings.get(id);
    }
    
    private Building loadBuilding(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long nationId = rs.getLong("nation_id");
        BuildingType type = BuildingType.valueOf(rs.getString("type"));
        int level = rs.getInt("level");
        
        // 先获取国家
        Nation nation = plugin.getNationManager().getNationById(nationId);
        if (nation == null) {
            plugin.getLogger().warning("找不到ID为 " + nationId + " 的国家，跳过加载筑 " + id);
            return null;
        }
        
        // 获取位置
        String worldName = rs.getString("world");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("找不到世界 " + worldName + "，跳过加载建筑 " + id);
            return null;
        }
        
        Location location = new Location(
            world,
            rs.getDouble("x"),
            rs.getDouble("y"),
            rs.getDouble("z")
        );
        
        // 创建建筑实例
        Building building = new Building(
            id,
            nationId,
            type,
            level,
            location,
            calculateSize(type, level)
        );
        
        // 添加到国家
        nation.addBuilding(building);
        
        plugin.getLogger().info(String.format(
            "已加载建筑: %s (ID: %d, 国家: %s)",
            type.getDisplayName(),
            id,
            nation.getName()
        ));
        
        return building;
    }
    
    public BuildingFunction getBuildingFunction(Building building) {
        return new BuildingFunction(building);
    }
    
    public Building getNearestBuilding(Location location, BuildingType type, int maxDistance) {
        Building nearest = null;
        double minDistance = maxDistance;
        
        for (Building building : buildings.values()) {
            if (building.getType() == type && 
                building.getBaseLocation().getWorld().equals(location.getWorld())) {
                double distance = building.getBaseLocation().distance(location);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = building;
                }
            }
        }
        
        return nearest;
    }
    
    public void onDisable() {
        // 取消所有更新任务
        updateTasks.values().forEach(BukkitTask::cancel);
        updateTasks.clear();
        
        // 清除所有全息文字
        HologramUtil.clearAllHolograms();
    }
    
    public void cancelUpdateTask(Building building) {
        BukkitTask task = updateTasks.remove(building.getId());
        if (task != null) {
            task.cancel();
        }
    }
    
    public void clearNationBuildings(Nation nation) {
        // 清除该国家的所有建筑缓存
        Set<Building> nationBuildings = buildingsByNation.remove(nation.getId());
        if (nationBuildings != null) {
            nationBuildings.forEach(building -> buildings.remove(building.getId()));
        }
    }
    
    // 其他方法...
}