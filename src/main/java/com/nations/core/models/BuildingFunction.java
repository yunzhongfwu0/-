package com.nations.core.models;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.nations.core.NationsCore;

import java.util.List;
import java.util.stream.Collectors;

public class BuildingFunction {
    private final Building building;
    private final NationsCore plugin;
    
    public BuildingFunction(Building building) {
        this.building = building;
        this.plugin = NationsCore.getInstance();
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
    
    private void runFarmTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!building.isValidBasic()) {
                    cancel();
                    return;
                }
                
                // 获取工作中的农民
                List<NationNPC> workers = plugin.getNPCManager().getBuildingWorkers(building)
                    .stream()
                    .filter(npc -> npc.getState() == WorkState.WORKING)
                    .collect(Collectors.toList());
                
                // 计算总效率
                double totalEfficiency = workers.stream()
                    .mapToDouble(NationNPC::getEfficiency)
                    .sum();
                
                // 基础产量 * 效率加成
                int production = (int)(10 * building.getLevel() * (1 + totalEfficiency));
                
                // 生成食物
                ItemStack food = new ItemStack(Material.BREAD, production);
                Location loc = building.getBaseLocation();
                
                if (loc != null && loc.getWorld() != null) {
                    // 让收割工运送食物
                    workers.stream()
                        .filter(npc -> npc.getType() == NPCType.FARMER)
                        .findFirst()
                        .ifPresentOrElse(
                            harvester -> {
                                // 让NPC移动到食物位置
                                harvester.setState(WorkState.TRAVELING);
                                harvester.getCitizensNPC().getNavigator().setTarget(loc);
                                
                                // 等NPC到达后再处理食物
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Building warehouse = findNearestWarehouse();
                                        if (warehouse != null) {
                                            // 运送到仓库
                                            harvester.getCitizensNPC().getNavigator()
                                                .setTarget(warehouse.getBaseLocation());
                                            addToWarehouse(warehouse, food);
                                        } else {
                                            loc.getWorld().dropItemNaturally(loc, food);
                                        }
                                        harvester.setState(WorkState.WORKING);
                                        harvester.gainExperience(production);
                                    }
                                }.runTaskLater(plugin, 60L);
                            },
                            () -> loc.getWorld().dropItemNaturally(loc, food)
                        );
                }
                
                // 农民获得经验
                workers.forEach(npc -> npc.gainExperience(production / workers.size()));
            }
        }.runTaskTimer(plugin, 0L, 72000L);
    }
    
    private void runWarehouseTask() {
        // 自动整理存储的物品
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!building.isValidBasic()) {
                    cancel();
                    return;
                }
                
                // 获取仓库容器
                Location loc = building.getBaseLocation();
                if (loc != null) {
                    Block block = loc.getBlock();
                    if (block.getState() instanceof Container container) {
                        sortContainer(container);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1200L); // 每分钟
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
    
    private void sortContainer(Container container) {
        // 实现物品分类逻辑
    }
}