package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.npc.NPCBehavior;
import com.nations.core.models.NationNPC;
import com.nations.core.models.WorkState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.npc.NPC;
import java.util.Map;

public class FarmerBehavior implements NPCBehavior {
    
    private static final int WORK_RADIUS = 5;
    private static final double INTERACTION_DISTANCE = 2.5;
    
    @Override
    public void performWork(NationNPC npc) {
        // 基础检查
        if (!isValidForWork(npc)) return;
        
        Location workLoc = npc.getWorkPosition();
        if (workLoc == null) {
            NationsCore.getInstance().getLogger().info("工作位置为空");
            return;
        }

        // 检查体力
        if (npc.getEnergy() <= 0) {
            enterRestState(npc);
            return;
        }

        // 如果当前是休息状态且体力未满，继续休息
        if (npc.getState() == WorkState.RESTING && npc.getEnergy() < 100) {
            enterRestState(npc);
            return;
        }

        // 检查是否有工作可做
        boolean hasWork = false;
        
        // 1. 检查是否有成熟作物
        Block targetCrop = findMatureCrop(workLoc, WORK_RADIUS);
        if (targetCrop != null) {
            hasWork = true;
            if (npc.getState() != WorkState.WORKING) {
                enterWorkState(npc);
            }
            
            // 如果不在范围内,先移动过去
            if (!isInRange(npc, targetCrop.getLocation())) {
                npc.getCitizensNPC().getNavigator().setTarget(targetCrop.getLocation());
                return;
            }
            
            // 在范围内,直接收获
            harvestCrop(targetCrop, npc);
            npc.gainExperience(1);
            npc.setEnergy(npc.getEnergy() - 1);
            return;
        }

        // 2. 检查是否有空地可种植
        if (!hasWork) {  // 只有在没找到成熟作物时才检查空地
            Location emptyFarmland = findEmptyFarmland(workLoc, WORK_RADIUS);
            if (emptyFarmland != null && findSeeds(npc) != null) {
                hasWork = true;
                if (npc.getState() != WorkState.WORKING) {
                    enterWorkState(npc);
                }

                // 如果不在范围内,先移动过去
                if (!isInRange(npc, emptyFarmland)) {
                    npc.getCitizensNPC().getNavigator().setTarget(emptyFarmland);
                    return;
                }
                
                // 在范围内,直接种植
                if (plantCrop(emptyFarmland, npc)) {
                    npc.gainExperience(1);
                    npc.setEnergy(npc.getEnergy() - 1);
                }
                return;
            }
        }

        // 如果没有工作可做，进入休息状态
        if (!hasWork) {
            if (npc.getState() != WorkState.RESTING) {
                NationsCore.getInstance().getLogger().info(
                    "农民没有找到工作，进入休息状态"
                );
                enterRestState(npc);
            }
        }
    }

    private boolean isValidForWork(NationNPC npc) {
        return npc != null && 
               npc.getCitizensNPC() != null && 
               npc.getCitizensNPC().getEntity() != null;
    }

    private boolean isInRange(NationNPC npc, Location target) {
        double distance = npc.getCitizensNPC().getEntity().getLocation().distance(target);
        // 记录距离判断
        NationsCore.getInstance().getLogger().info(String.format(
            "检查距离: 当前=%.2f, 需要=%.2f",
            distance, INTERACTION_DISTANCE
        ));
        return distance <= INTERACTION_DISTANCE;
    }

    private Block findMatureCrop(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = center.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.WHEAT && isMatureCrop(block)) {
                    return block;
                }
            }
        }
        return null;
    }

    private boolean isMatureCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return false;
    }

    private Location findEmptyFarmland(Location center, int radius) {
        // 记录开始查找
        NationsCore.getInstance().getLogger().info(String.format(
            "开始查找空耕地 - 中心点: x=%d, y=%d, z=%d, 半径: %d",
            center.getBlockX(), center.getBlockY(), center.getBlockZ(), radius
        ));

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // 从中心点向下查找耕地
                Location loc = center.clone().add(x, 0, z);
                for (int y = 0; y >= -3; y--) {  // 向下搜索3格
                    loc.setY(center.getY() + y);
                    Block block = loc.getBlock();
                    
                    if (block.getType() == Material.FARMLAND) {
                        Block above = block.getRelative(0, 1, 0);

                        if (above.getType() == Material.AIR) {
                            NationsCore.getInstance().getLogger().info(
                                "找到可用的空耕地！"
                            );
                            return block.getLocation();  // 返回耕地的位置
                        }
                    }
                }
            }
        }

        NationsCore.getInstance().getLogger().info("未找到可用的空耕地");
        return null;
    }

    private void harvestCrop(Block block, NationNPC npc) {
        if (block.getBlockData() instanceof Ageable) {
            // 收获小麦
            ItemStack wheat = new ItemStack(Material.WHEAT);
            Map<Integer, ItemStack> leftover = npc.getInventory().addItem(wheat);
            
            // 如果背包满了,掉落在地上
            if (!leftover.isEmpty()) {
                block.getWorld().dropItemNaturally(block.getLocation(), wheat);
            }
            
            // 有50%几率获得种子
            if (Math.random() < 0.5) {
                ItemStack seeds = new ItemStack(Material.WHEAT_SEEDS);
                leftover = npc.getInventory().addItem(seeds);
                if (!leftover.isEmpty()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), seeds);
                }
            }
            
            // 清除作物
            block.setType(Material.AIR);
        }
    }

    private ItemStack findSeeds(NationNPC npc) {
        // 检查是否有种子
        int seedSlot = npc.getInventory().first(Material.WHEAT_SEEDS);
        if (seedSlot == -1) {
            // 记录日志：没有找到种子
            NationsCore.getInstance().getLogger().info(
                "农民没有种子可用"
            );
            return null;
        }
        return npc.getInventory().getItem(seedSlot);
    }

    private boolean plantCrop(Location loc, NationNPC npc) {
        // 记录日志：开始尝试种植
        NationsCore.getInstance().getLogger().info(String.format(
            "农民正在尝试在 x=%d, y=%d, z=%d 种植",
            loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()
        ));

        Block block = loc.getBlock();
        // 检查方块类型
        if (block.getType() != Material.FARMLAND) {
            NationsCore.getInstance().getLogger().info("种植失败：不是耕地");
            return false;
        }

        Block above = block.getRelative(0, 1, 0);
        // 检查上方方块
        if (above.getType() != Material.AIR) {
            NationsCore.getInstance().getLogger().info("种植失败：上方不是空气");
            return false;
        }

        // 检查是否有种子
        ItemStack seeds = findSeeds(npc);
        if (seeds == null) {
            NationsCore.getInstance().getLogger().info("种植失败：没有种子");
            return false;
        }

        // 执行种植
        above.setType(Material.WHEAT);
        
        // 移除种子
        int seedSlot = npc.getInventory().first(Material.WHEAT_SEEDS);
        if (seedSlot != -1) {
            ItemStack seedStack = npc.getInventory().getItem(seedSlot);
            if (seedStack.getAmount() > 1) {
                seedStack.setAmount(seedStack.getAmount() - 1);
            } else {
                npc.getInventory().setItem(seedSlot, null);
            }
            
            // 记录成功种植
            NationsCore.getInstance().getLogger().info(String.format(
                "农民成功在 x=%d, y=%d, z=%d 种植了小麦",
                above.getX(), above.getY(), above.getZ()
            ));
            return true;
        }

        NationsCore.getInstance().getLogger().info("种植失败：移除种子时出错");
        return false;
    }

    @Override
    public void onSpawn(NationNPC npc) {
        // 增加初始种子数量并记录日志
        npc.getInventory().addItem(new ItemStack(Material.WHEAT_SEEDS, 64));
        NationsCore.getInstance().getLogger().info(
            "给予农民 64 个小麦种子"
        );
    }

    @Override
    public void onDespawn(NationNPC npc) {
        npc.getInventory().clear();
    }

    @Override
    public void setupEquipment(NationNPC npc) {
        Equipment equipment = npc.getCitizensNPC().getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.IRON_HOE));
        equipment.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.LEATHER_HELMET));
    }

    private void enterWorkState(NationNPC npc) {
        if (npc.getState() != WorkState.WORKING) {
            npc.setState(WorkState.WORKING);
            NationsCore.getInstance().getLogger().info(
                "农民找到工作，进入工作状态"
            );
        }
    }

    private void enterRestState(NationNPC npc) {
        if (npc.getState() != WorkState.RESTING) {
            npc.setState(WorkState.RESTING);
            // 随机移动
            Location randomLoc = getRandomLocation(npc.getWorkPosition(), WORK_RADIUS);
            if (randomLoc != null) {
                npc.getCitizensNPC().getNavigator().setTarget(randomLoc);
            }
        }
        
        // 恢复体力
        if (npc.getEnergy() < 100) {
            npc.setEnergy(Math.min(100, npc.getEnergy() + 5));
            NationsCore.getInstance().getLogger().info(
                String.format("农民正在休息，体力恢复到 %d%%", npc.getEnergy())
            );
        } else if (npc.getState() == WorkState.RESTING) {
            // 如果体力已满且处于休息状态，准备恢复工作
            NationsCore.getInstance().getLogger().info(
                String.format("NPC %s 休息完毕 (体力: %d%%)，准备寻找工作",
                    npc.getCitizensNPC().getName(),
                    npc.getEnergy()
                )
            );
        }
    }

    private Location getRandomLocation(Location center, int radius) {
        if (center == null) return null;
        
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        Location loc = center.clone();
        loc.setX(x);
        loc.setZ(z);
        loc.setY(center.getWorld().getHighestBlockYAt((int)x, (int)z));
        
        return loc;
    }
} 