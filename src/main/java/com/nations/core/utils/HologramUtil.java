package com.nations.core.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import com.nations.core.NationsCore;
import com.nations.core.models.Building;
import com.nations.core.models.BuildingFunction;
import com.nations.core.models.BuildingType;
import com.nations.core.managers.SoldierManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramUtil {
    private static final Map<Location, List<ArmorStand>> buildingHolograms = new HashMap<>();
    private static final NationsCore plugin = NationsCore.getInstance();
    
    public static void createBuildingHologram(Building building) {
        Location loc = building.getBaseLocation().clone().add(0, 3, 0);
        removeBuildingHologram(loc);
        
        List<String> lines = getHologramLines(building);
        List<ArmorStand> stands = new ArrayList<>();
        
        double height = 0.3;
        for (String line : lines) {
            Location lineLoc = loc.clone().add(0, height, 0);
            ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(lineLoc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(line);
            stand.setMarker(true);
            stands.add(stand);
            height += 0.3;
        }
        
        buildingHolograms.put(loc, stands);
    }
    
    private static List<String> getHologramLines(Building building) {
        List<String> lines = new ArrayList<>();
        lines.add("§6" + building.getType().getDisplayName());
        lines.add("§7等级: §f" + building.getLevel());
        
        switch (building.getType()) {
            case FARM -> {
                double production = building.getBonuses().getOrDefault("food_production", 0.0);
                lines.add("§7食物产量: §f" + String.format("%.0f", production) + "/小时");
            }
            case WAREHOUSE -> {
                double storage = building.getBonuses().getOrDefault("storage_size", 0.0);
                lines.add("§7存储容量: §f" + String.format("%.0f", storage));
            }
            case MARKET -> {
                double discount = building.getBonuses().getOrDefault("trade_discount", 0.0) * 100;
                double income = building.getBonuses().getOrDefault("income_bonus", 0.0) * 100;
                lines.add("§7交易折扣: §f" + String.format("%.1f%%", discount));
                lines.add("§7收入加成: §f" + String.format("%.1f%%", income));
            }
            case BARRACKS -> {
                double slots = building.getBonuses().getOrDefault("training_slots", 0.0);
                double bonus = building.getBonuses().getOrDefault("training_bonus", 0.0) * 100;
                double speed = building.getBonuses().getOrDefault("training_speed", 0.0) * 100;
                
                lines.add("§7训练位: §f" + String.format("%.0f", slots));
                lines.add("§7训练加成: §f" + String.format("%.1f%%", bonus));
                lines.add("§7训练速度: §f-" + String.format("%.1f%%", speed));
                
                SoldierManager soldierManager = plugin.getSoldierManager();
                if (soldierManager != null && soldierManager.isLoaded()) {
                    int usedSlots = soldierManager.getTrainingSlots(building);
                    lines.add("§7使用情况: §f" + usedSlots + "/" + (int)slots);
                } else {
                    lines.add("§7使用情况: §f加载中...");
                }
            }
            case TOWN_HALL -> {
                double taxRate = building.getBonuses().getOrDefault("tax_rate", 0.0) * 100;
                double maxMembers = building.getBonuses().getOrDefault("max_members", 0.0);
                lines.add("§7税率: §f+" + String.format("%.1f%%", taxRate));
                lines.add("§7人口上限: §f+" + String.format("%.0f", maxMembers));
            }
        }
        return lines;
    }
    
    public static void removeBuildingHologram(Location loc) {
        List<ArmorStand> stands = buildingHolograms.remove(loc);
        if (stands != null) {
            stands.forEach(ArmorStand::remove);
        }
    }
    
    public static void updateHologram(Building building) {
        Location loc = building.getBaseLocation().clone().add(0, 2, 0);
        List<ArmorStand> stands = buildingHolograms.get(loc);
        
        if (stands == null || stands.isEmpty()) {
            createBuildingHologram(building);
            return;
        }
        
        List<String> lines = getHologramLines(building);
        
        // 如果行数不同，重新创建全息
        if (stands.size() != lines.size()) {
            createBuildingHologram(building);
            return;
        }
        
        // 更新现有行
        for (int i = 0; i < lines.size(); i++) {
            stands.get(i).setCustomName(lines.get(i));
        }
    }
    
    /**
     * 清除所有建筑全息文字
     */
    public static void clearAllHolograms() {
        buildingHolograms.forEach((loc, stands) -> 
            stands.forEach(ArmorStand::remove)
        );
        buildingHolograms.clear();
    }
    
    public static void removeHologram(Building building) {
        Location loc = building.getBaseLocation().clone().add(0, 2, 0);
        removeBuildingHologram(loc);
    }
} 