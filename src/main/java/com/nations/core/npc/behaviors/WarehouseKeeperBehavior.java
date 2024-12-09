package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import net.citizensnpcs.api.trait.trait.Equipment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WarehouseKeeperBehavior extends AbstractNPCBehavior {
    private BukkitRunnable sortingTask;
    private static final int SORT_INTERVAL = 1200; // 每分钟整理一次
    private final Random random = new Random(); // 添加Random实例
    
    @Override
    public void performWork(NationNPC npc) {
        if (!isValidForWork(npc)) {
            return;
        }
        // 看向最近的玩家
        lookAtNearestPlayer(npc, 10.0);
        
        // 检查工作状态
        if (npc.getState() != WorkState.WORKING) {
            return;
        }

        // 确保在建筑范围内工作
        Location npcLoc = npc.getCitizensNPC().getEntity().getLocation();
        Location buildingLoc = npc.getWorkplace().getBaseLocation();
        double distance = npcLoc.distance(buildingLoc);
        int buildingRadius = npc.getWorkplace().getType().getBaseSize() / 2;

        // 如果没有正在进行的整理任务，启动整理任务
        if (sortingTask == null || sortingTask.isCancelled()) {
            startSortingTask(npc);
        }

        

        // 只有在没有导航任务时才考虑随机移动
        if (!npc.getCitizensNPC().getNavigator().isNavigating()) {
            // 降低随机移动的频率（10%的概率）
            if (random.nextInt(100) < 10) {
                Location randomLoc = getRandomLocationInBuilding(buildingLoc, buildingRadius/2);
                if (randomLoc != null) {
                    npc.getCitizensNPC().getNavigator().setTarget(randomLoc);
                }
            }
        }

        // 消耗体力（每次工作消耗1点体力）
        npc.setEnergy(Math.max(0, npc.getEnergy() - 1));
    }

    private Location getRandomLocationInBuilding(Location center, int radius) {
        if (center == null || center.getWorld() == null) return null;
        
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        Location loc = center.clone();
        loc.setX(x);
        loc.setZ(z);
        
        // 找到安全的地面位置
        loc.setY(center.getWorld().getHighestBlockYAt((int)x, (int)z) + 1);
        
        // 确保位置是安全的
        while (!loc.getBlock().getType().isAir() || 
               !loc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
            loc.add(0, 1, 0);
        }
        
        return loc;
    }

    private void startSortingTask(NationNPC npc) {
        // 取消旧的任务
        if (sortingTask != null) {
            sortingTask.cancel();
            sortingTask = null;
        }

        // 创建新的整理任务
        sortingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValidForWork(npc) || npc.getState() != WorkState.WORKING) {
                    cancel();
                    return;
                }

                Building warehouse = npc.getWorkplace();
                if (warehouse == null || !warehouse.isValidBasic()) {
                    return;
                }

                // 获取技能效果
                double organizationBonus = NationsCore.getInstance().getNPCSkillManager()
                    .getSkillEffectiveness(npc, NPCSkill.ORGANIZATION);
                double storageBonus = NationsCore.getInstance().getNPCSkillManager()
                    .getSkillEffectiveness(npc, NPCSkill.STORAGE_EXPERT);

                // 整理所有箱子
                Location baseLoc = warehouse.getBaseLocation();
                boolean foundChests = false;
                
                // 在建筑范围内索箱子
                int size = warehouse.getType().getBaseSize();
                int halfSize = size / 2;
                
                for (int x = -halfSize; x <= halfSize; x++) {
                    for (int y = 0; y <= 3; y++) {  // 检查4层高度
                        for (int z = -halfSize; z <= halfSize; z++) {
                            Location chestLoc = baseLoc.clone().add(x, y, z);
                            Block block = chestLoc.getBlock();
                            if (block.getState() instanceof Container container) {
                                foundChests = true;
                                sortContainer(container, warehouse, organizationBonus, storageBonus);
                                
                                // 显示整理效果
                                block.getWorld().spawnParticle(
                                    Particle.VILLAGER_HAPPY,
                                    block.getLocation().add(0.5, 0.5, 0.5),
                                    5, 0.2, 0.2, 0.2, 0
                                );
                            }
                        }
                    }
                }

                if (foundChests) {
                    // 消耗体力（根据效率加成减少消耗）
                    int energyCost = (int) Math.ceil(5 / (1 + organizationBonus));
                    npc.setEnergy(Math.max(0, npc.getEnergy() - energyCost));

                    // 获得经验
                    npc.addExperience(1);
                    
                    NationsCore.getInstance().getLogger().info(String.format(
                        "仓库管理员 %s 完成了一次物品整理",
                        npc.getCitizensNPC().getName()
                    ));
                }
            }
        };

        // 启动任务，
        sortingTask.runTaskTimer(NationsCore.getInstance(), 0L,SORT_INTERVAL);
    }

    private void sortContainer(Container container, Building warehouse, double organizationBonus, double storageBonus) {
        try {
            // 获取当前物品
            ItemStack[] contents = container.getInventory().getContents();
            Map<Material, List<ItemStack>> itemsByType = new HashMap<>();
            
            // 按物品类型分组并记录原始数量
            final int originalTotal = 0;
            int runningTotal = 0;
            for (ItemStack item : contents) {
                if (item != null && !item.getType().isAir()) {
                    itemsByType.computeIfAbsent(item.getType(), k -> new ArrayList<>()).add(item.clone());
                    runningTotal += item.getAmount();
                }
            }
            final int finalOriginalTotal = runningTotal;

            // 如果箱子为空，直接返回
            if (itemsByType.isEmpty()) {
                NationsCore.getInstance().getLogger().info("箱子为空，跳过整理");
                return;
            }
            
            // 创建新的排序后的物品列表
            List<ItemStack> sortedItems = new ArrayList<>();
            for (Map.Entry<Material, List<ItemStack>> entry : itemsByType.entrySet()) {
                Material type = entry.getKey();
                List<ItemStack> items = entry.getValue();
                
                // 计算总数量
                int totalAmount = items.stream()
                    .mapToInt(ItemStack::getAmount)
                    .sum();
                
                // 创建最大堆叠的物品
                while (totalAmount > 0) {
                    int stackSize = Math.min(totalAmount, type.getMaxStackSize());
                    ItemStack stack = new ItemStack(type, stackSize);
                    
                    // 复制第一个物品的附魔和元数据
                    if (!items.isEmpty()) {
                        ItemStack original = items.get(0);
                        if (original.hasItemMeta()) {
                            stack.setItemMeta(original.getItemMeta().clone());
                        }
                    }
                    
                    sortedItems.add(stack);
                    totalAmount -= stackSize;
                }
            }
            
            // 按物品类型排序
            sortedItems.sort((a, b) -> {
                // 先按物品类型名称排序
                int typeCompare = a.getType().name().compareTo(b.getType().name());
                if (typeCompare != 0) return typeCompare;
                
                // 同类型按数量降序
                return Integer.compare(b.getAmount(), a.getAmount());
            });

            // 记录排序前的状态
            NationsCore.getInstance().getLogger().info(String.format(
                "整理前状态 - 物品种类: %d, 总数量: %d",
                itemsByType.size(),
                finalOriginalTotal
            ));
            
            // 使用同步任务更新物品栏
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        // 获取方块状态
                        Block block = container.getBlock();
                        if (!(block.getState() instanceof Container)) {
                            NationsCore.getInstance().getLogger().warning("容器已失效，取消整理");
                            return;
                        }

                        // 备份原有物品
                        ItemStack[] oldContents = container.getInventory().getContents().clone();
                        
                        // 创建新的物品数组
                        ItemStack[] newContents = new ItemStack[container.getInventory().getSize()];
                        int slot = 0;
                        int totalPlaced = 0;

                        // 填充新的物品数组
                        for (ItemStack item : sortedItems) {
                            if (slot >= newContents.length) break;
                            newContents[slot++] = item.clone();
                            totalPlaced += item.getAmount();
                        }

                        // 直接设置新的物品数组
                        container.getInventory().setContents(newContents);
                        
                        // 强制更新方块状态
                        block.getState().update(true, false);
                        
                        // 等待一个tick后验证
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                // 验证更新
                                Container verifyContainer = (Container) block.getState();
                                int finalTotal = 0;
                                
                                for (ItemStack item : verifyContainer.getInventory().getContents()) {
                                    if (item != null && !item.getType().isAir()) {
                                        finalTotal += item.getAmount();
                                    }
                                }

                                if (finalTotal != finalOriginalTotal) {
                                    NationsCore.getInstance().getLogger().warning(String.format(
                                        "物品数量不匹配，尝试重新更新。预期: %d, 实际: %d",
                                        finalOriginalTotal,
                                        finalTotal
                                    ));
                                    
                                    // 再次尝试更新
                                    verifyContainer.getInventory().setContents(newContents);
                                    verifyContainer.update(true);
                                    
                                    // 如果还是失败，还原
                                    Container finalCheck = (Container) block.getState();
                                    int finalCheckTotal = 0;
                                    for (ItemStack item : finalCheck.getInventory().getContents()) {
                                        if (item != null && !item.getType().isAir()) {
                                            finalCheckTotal += item.getAmount();
                                        }
                                    }
                                    
                                    if (finalCheckTotal != finalOriginalTotal) {
                                        NationsCore.getInstance().getLogger().severe("更新失败，还原原有物品");
                                        container.getInventory().setContents(oldContents);
                                        block.getState().update(true, false);
                                    }
                                }

                                // 记录最终状态
                                NationsCore.getInstance().getLogger().info(String.format(
                                    "整理完成 - 物品种类: %d, 总数量: %d",
                                    itemsByType.size(),
                                    finalTotal
                                ));
                            }
                        }.runTaskLater(NationsCore.getInstance(), 1L);

                    } catch (Exception e) {
                        NationsCore.getInstance().getLogger().severe("整理箱子时发生错误: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }.runTask(NationsCore.getInstance());
            
        } catch (Exception e) {
            NationsCore.getInstance().getLogger().severe("整理箱子时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enterWorkState(NationNPC npc) {
        NationsCore.getInstance().getLogger().info(String.format(
            "仓库管理员 %s 进入工作状态, 体力: %d, 时间: %d",
            npc.getCitizensNPC().getName(),
            npc.getEnergy(),
            npc.getCitizensNPC().getEntity().getWorld().getTime()
        ));
        npc.setState(WorkState.WORKING);
    }

    private void enterRestState(NationNPC npc) {
        NationsCore.getInstance().getLogger().info(String.format(
            "仓库管理员 %s 进入休息状态, 体力: %d, 时间: %d",
            npc.getCitizensNPC().getName(),
            npc.getEnergy(),
            npc.getCitizensNPC().getEntity().getWorld().getTime()
        ));
        npc.setState(WorkState.RESTING);
    }

    private boolean isValidForWork(NationNPC npc) {
        return npc != null && 
               npc.getCitizensNPC() != null && 
               npc.getCitizensNPC().isSpawned() &&
               npc.getWorkplace() != null;
    }

    @Override
    public void onSpawn(NationNPC npc) {
        setupEquipment(npc);
    }

    @Override
    public void onDespawn(NationNPC npc) {
        if (sortingTask != null) {
            sortingTask.cancel();
            sortingTask = null;
        }
        
        // 移除物流加成
        Building workplace = npc.getWorkplace();
        if (workplace != null) {
            workplace.removeEfficiencyBonus("logistics");
        }
    }

    @Override
    public void setupEquipment(NationNPC npc) {
        Equipment equipment = npc.getCitizensNPC().getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.CHEST));
        equipment.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.LEATHER_CHESTPLATE));
    }
} 