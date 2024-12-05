package com.nations.core.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.nations.core.NationsCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BuildingFunction {
    private final NationsCore plugin;
    private final Building building;
    
    public BuildingFunction(Building building) {
        this.plugin = NationsCore.getInstance();
        this.building = building;
    }
    
    /**
     * 执行建筑的定时任务
     */
    public void runTasks() {
        switch (building.getType()) {
            case FARM -> runFarmTask();
            case WAREHOUSE -> runWarehouseTask();
            case MARKET -> runMarketTask();
            case BARRACKS -> runBarracksTask();
            case TOWN_HALL -> runTownHallTask();
        }
    }
    
    public int calculateFarmProduction(Building building) {
        // 基础产量
        int baseProduction = 10; // 基础每小时产10个
        
        // 获取工人效率
        List<NationNPC> workers = plugin.getNPCManager().getBuildingWorkers(building);
        double efficiency = workers.stream()
            .mapToDouble(NationNPC::getEfficiency)
            .average()
            .orElse(0.0);
        
        // 效率加成改为最多增加50%产量
        return (int) Math.ceil(baseProduction * building.getLevel() * (1 + efficiency * 0.5));
    }
    
    private void runFarmTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (building == null || building.getType() != BuildingType.FARM || !building.isValidBasic()) {
                    cancel();
                    return;
                }
                
                // 获取农民
                List<NationNPC> workers = plugin.getNPCManager().getBuildingWorkers(building);
                if (workers.isEmpty()) {
                    return;
                }
                
                int production = calculateFarmProduction(building);
                
                // 创建食物物品
                ItemStack food = new ItemStack(Material.BREAD, production);
                
                // 寻找最近的仓库
                Building warehouse = findNearestWarehouse();
                if (warehouse != null) {
                    // 尝试存入仓库
                    if (tryAddToWarehouse(warehouse, food)) {
                        plugin.getLogger().info(String.format(
                            "农场生产的食物已存入仓库 %d: 数量=%d",
                            warehouse.getId(), production
                        ));
                        
                        // 农民获得经验
                        workers.forEach(npc -> npc.gainExperience(production / workers.size()));
                    } else {
                        // 仓库已满,掉落在地上
                        Location loc = building.getBaseLocation();
                        loc.getWorld().dropItemNaturally(loc, food);
                        plugin.getLogger().warning(String.format(
                            "仓库 %d 已满,农场食物已掉落: 数量=%d",
                            warehouse.getId(), production
                        ));
                    }
                } else {
                    // 找不到仓库,掉落在地上
                    Location loc = building.getBaseLocation();
                    loc.getWorld().dropItemNaturally(loc, food);
                    plugin.getLogger().info(String.format(
                        "农场食物已掉落: 数量=%d (未找到仓库)",
                        production
                    ));
                }
            }
        }.runTaskTimer(plugin, 0L, 72000L); // 1小时 = 20ticks/s * 60s * 60min = 72000 ticks
    }
    
    private void runWarehouseTask() {
        // 仓库的自动整理功能已移至仓库管理员NPC
    }
    
    private void runMarketTask() {
        // 市场相关任务
    }
    
    private void runBarracksTask() {
        // 军营相关任务
    }
    
    private void runTownHallTask() {
        // 市政厅相关任务
    }
    
    private Building findNearestWarehouse() {
        Location loc = building.getBaseLocation();
        Building nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Building b : building.getNation().getBuildings()) {
            if (b.getType() == BuildingType.WAREHOUSE) {
                double dist = b.getBaseLocation().distance(loc);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = b;
                }
            }
        }
        return nearest;
    }
    
    private void addToWarehouse(Building warehouse, ItemStack item) {
        Location loc = warehouse.getBaseLocation();
        Block block = loc.getBlock();
        if (block.getState() instanceof Container container) {
            container.getInventory().addItem(item);
        }
    }
    /**
     * 尝试将物品添加到仓库
     * @return 是否成功添加
     */
    private boolean tryAddToWarehouse(Building warehouse, ItemStack item) {
        if (warehouse == null || warehouse.getType() != BuildingType.WAREHOUSE || !warehouse.isValidBasic()) {
            return false;
        }
        
        Location baseLoc = warehouse.getBaseLocation();
        if (baseLoc == null || baseLoc.getWorld() == null) {
            return false;
        }
        
        // 先尝试放入主箱子
        Location mainChestLoc = baseLoc.clone().add(0, 1, 0);
        Block mainBlock = mainChestLoc.getBlock();
        if (mainBlock.getState() instanceof Container mainContainer) {
            HashMap<Integer, ItemStack> leftover = mainContainer.getInventory().addItem(item);
            if (leftover.isEmpty()) {
                return true;
            }
            // 如果主箱子放不下，尝试其他箱子
            item = leftover.get(0);
        }
        
        // 检查其他箱子
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // 跳过主箱子
                
                Location chestLoc = baseLoc.clone().add(x, 1, z);
                Block block = chestLoc.getBlock();
                if (block.getState() instanceof Container container) {
                    HashMap<Integer, ItemStack> leftover = container.getInventory().addItem(item);
                    if (leftover.isEmpty()) {
                        return true;
                    }
                    // 更新剩余物品
                    item = leftover.get(0);
                }
            }
        }
        
        return false;
    }
}