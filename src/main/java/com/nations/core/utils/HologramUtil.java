package com.nations.core.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import com.nations.core.models.Building;

public class HologramUtil {
    
    public static void createBuildingHologram(Building building) {
        Location loc = building.getBaseLocation();
        if (loc == null || loc.getWorld() == null) return;
        
        // 在建筑上方创建全息文字
        Location holoLoc = loc.clone().add(0, 3, 0);
        
        // 创建标题
        createHologramLine(holoLoc.clone().add(0, 0.5, 0), 
            "§6" + building.getType().getDisplayName() + " §7Lv." + building.getLevel());
        
        // 创建信息行
        createHologramLine(holoLoc.clone().add(0, 0.25, 0), 
            "§e点击查看详细信息");
        
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
                case "food_production" -> String.format("§7食物产量: §f+%.1f/h", value);
                default -> key + ": " + value;
            };
            createHologramLine(holoLoc.clone(), bonus);
            holoLoc.subtract(0, 0.25, 0);
        });
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
        
        // 移��附近的全息文字
        loc.getWorld().getNearbyEntities(loc, 5, 5, 5).forEach(entity -> {
            if (entity instanceof ArmorStand stand && !stand.isVisible()) {
                stand.remove();
            }
        });
    }
} 