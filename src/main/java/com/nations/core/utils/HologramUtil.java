package com.nations.core.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import com.nations.core.NationsCore;
import com.nations.core.models.Building;
import com.nations.core.models.BuildingFunction;
import com.nations.core.models.BuildingType;

import java.util.HashMap;
import java.util.Map;

public class HologramUtil {
    private static final Map<Long, Integer> updateTasks = new HashMap<>();
    
    public static void createBuildingHologram(Building building) {
        Location loc = building.getBaseLocation();
        if (loc == null || loc.getWorld() == null) return;
        
        // 先移除旧的全息文字
        removeBuildingHologram(loc);
        
        // 在建筑上方创建全息文字
        Location holoLoc = loc.clone().add(0, 3, 0);
        
        // 创建标题
        createHologramLine(holoLoc.clone().add(0, 0.5, 0), 
            "§6" + building.getType().getDisplayName() + " §7Lv." + building.getLevel());
        
        // 如果是农场，显示当前产量
        if (building.getType() == BuildingType.FARM) {
            BuildingFunction function = NationsCore.getInstance().getBuildingManager().getBuildingFunction(building);
            if (function != null) {
                int production = function.calculateFarmProduction(building);
                createHologramLine(holoLoc.clone().add(0, 0.25, 0), 
                    String.format("§7食物产量: §f%d/小时", production));
                holoLoc.subtract(0, 0.25, 0);
            }
        }
        
        // 创建加成效果
        building.getBonuses().forEach((key, value) -> {
            String bonus = switch (key) {
                case "tax_rate" -> String.format("§7税收加成: §f+%.1f%%", value * 100);
                case "max_members" -> String.format("§7成员上限: §f+%.0f", value);
                case "strength" -> String.format("§7战斗力: §f+%.1f", value);
                case "defense" -> String.format("§7防御力: §f+%.1f", value);
                case "trade_discount" -> String.format("§7交易折扣: §f%.1f%%", value * 100);
                case "income_bonus" -> String.format("§7收入加成: §f+%.1f%%", value * 100);
                case "storage_size" -> String.format("§7存储空间: §f+%.0f", value);
                default -> key + ": " + value;
            };
            createHologramLine(holoLoc.clone(), bonus);
            holoLoc.subtract(0, 0.25, 0);
        });
        
        // 启动定时更新任务
        startUpdateTask(building);
    }
    
    private static void startUpdateTask(Building building) {
        // 如果已有更新任务，先取消
        Integer oldTaskId = updateTasks.remove(building.getId());
        if (oldTaskId != null) {
            NationsCore.getInstance().getServer().getScheduler().cancelTask(oldTaskId);
        }
        
        // 创建新的更新任务
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!building.isValidBasic()) {
                    cancel();
                    updateTasks.remove(building.getId());
                    return;
                }
                
                // 更新全息显示
                createBuildingHologram(building);
            }
        };
        
        // 启动任务并保存任务ID
        int taskId = task.runTaskTimer(NationsCore.getInstance(), 1200L, 1200L).getTaskId(); // 每分钟更新一次
        updateTasks.put(building.getId(), taskId);
    }
    
    private static ArmorStand createHologramLine(Location loc, String text) {
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        return stand;
    }
    
    public static void removeBuildingHologram(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        
        // 移除附近的全息文字
        loc.getWorld().getNearbyEntities(loc, 5, 5, 5).forEach(entity -> {
            if (entity instanceof ArmorStand stand && !stand.isVisible()) {
                stand.remove();
            }
        });
    }
} 