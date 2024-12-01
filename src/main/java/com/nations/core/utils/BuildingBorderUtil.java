package com.nations.core.utils;

import com.nations.core.NationsCore;
import com.nations.core.models.Building;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class BuildingBorderUtil {
    
    // 存储每个建筑的边界任务ID
    private static final Map<Long, Integer> borderTasks = new HashMap<>();
    
    /**
     * 检查建筑边界是否正在显示
     */
    public static boolean isBorderVisible(Building building) {
        return borderTasks.containsKey(building.getId());
    }
    
    /**
     * 显示建筑边界
     */
    public static void showBuildingBorder(Building building) {
        Location loc = building.getBaseLocation();
        if (loc == null || loc.getWorld() == null) return;
        
        // 如果已有边界显示，先移除
        removeBuildingBorder(building);
        
        int size = building.getSize();
        int halfSize = size / 2;
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!building.isValidBasic()) {
                    cancel();
                    borderTasks.remove(building.getId());
                    return;
                }
                
                // 显示四个角落的柱子
                for (double y = 0; y <= 3; y += 0.5) {
                    showCornerParticle(loc.clone().add(-halfSize, y, -halfSize));
                    showCornerParticle(loc.clone().add(-halfSize, y, halfSize));
                    showCornerParticle(loc.clone().add(halfSize, y, -halfSize));
                    showCornerParticle(loc.clone().add(halfSize, y, halfSize));
                }
                
                // 显示边界线
                for (double i = -halfSize; i <= halfSize; i += 0.5) {
                    showBorderParticle(loc.clone().add(i, 0.5, -halfSize));
                    showBorderParticle(loc.clone().add(i, 0.5, halfSize));
                    showBorderParticle(loc.clone().add(-halfSize, 0.5, i));
                    showBorderParticle(loc.clone().add(halfSize, 0.5, i));
                }
            }
        };
        
        // 保存任务ID
        int taskId = task.runTaskTimer(NationsCore.getInstance(), 0L, 20L).getTaskId();
        borderTasks.put(building.getId(), taskId);
    }
    
    /**
     * 移除建筑边界显示
     */
    public static void removeBuildingBorder(Building building) {
        Integer taskId = borderTasks.remove(building.getId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
    
    private static void showCornerParticle(Location loc) {
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
    }
    
    private static void showBorderParticle(Location loc) {
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 1, 0, 0, 0, 0);
    }
    
    public static void showPlacementBorder(Location center, int size) {
        int halfSize = size / 2;
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks++ >= 100) { // 显示5秒
                    cancel();
                    return;
                }
                
                // 显示四个角落
                for (double y = 0; y <= 3; y += 0.5) {
                    showCornerParticle(center.clone().add(-halfSize, y, -halfSize));
                    showCornerParticle(center.clone().add(-halfSize, y, halfSize));
                    showCornerParticle(center.clone().add(halfSize, y, -halfSize));
                    showCornerParticle(center.clone().add(halfSize, y, halfSize));
                }
                
                // 显示边界线
                for (double i = -halfSize; i <= halfSize; i += 0.5) {
                    showBorderParticle(center.clone().add(i, 0.5, -halfSize));
                    showBorderParticle(center.clone().add(i, 0.5, halfSize));
                    showBorderParticle(center.clone().add(-halfSize, 0.5, i));
                    showBorderParticle(center.clone().add(halfSize, 0.5, i));
                }
            }
        }.runTaskTimer(NationsCore.getInstance(), 0L, 4L); // 每0.2秒更新一次
    }
} 