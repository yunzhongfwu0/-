package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.models.NationNPC;
import com.nations.core.models.WorkState;
import com.nations.core.models.NPCSkill;
import com.nations.core.models.NPCSkillData;
import com.nations.core.npc.NPCBehavior;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.npc.NPC;
import java.util.Map;
import java.util.Random;

public class FarmerBehavior implements NPCBehavior {
    
    private static final int BASE_WORK_RADIUS = 5;
    private static final double INTERACTION_DISTANCE = 2.5;
    private static final Random random = new Random();
    private static final Material[] RARE_CROPS = {
        Material.GOLDEN_CARROT,
        Material.GLISTERING_MELON_SLICE,
        Material.GOLDEN_APPLE,
        Material.ENCHANTED_GOLDEN_APPLE
    };
    
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
        Block targetCrop = findMatureCrop(workLoc, BASE_WORK_RADIUS, npc);
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
            Location emptyFarmland = findEmptyFarmland(workLoc, BASE_WORK_RADIUS, npc);
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
        
        // 实现农民NPC的技能效果
        work(npc);
    }

    private boolean isValidForWork(NationNPC npc) {
        return npc != null && 
               npc.getCitizensNPC() != null && 
               npc.getCitizensNPC().getEntity() != null;
    }

    private boolean isInRange(NationNPC npc, Location target) {
        double distance = npc.getCitizensNPC().getEntity().getLocation().distance(target);
        return distance <= INTERACTION_DISTANCE;
    }

    private Block findMatureCrop(Location center, int radius, NationNPC npc) {
        // 获取高效种植技能效果来增加工作范围
        double efficiency = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.EFFICIENT_FARMING);
        int workRadius = radius + (int)(radius * efficiency);

        // 使用增加后的范围寻找成熟作物
        for (int x = -workRadius; x <= workRadius; x++) {
            for (int z = -workRadius; z <= workRadius; z++) {
                Block block = center.clone().add(x, 0, z).getBlock();
                if (block.getBlockData() instanceof Ageable) {
                    Ageable crop = (Ageable) block.getBlockData();
                    if (crop.getAge() == crop.getMaximumAge()) {
                        return block;
                    }
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

    private Location findEmptyFarmland(Location center, int radius, NationNPC npc) {
        // 获取高效种植技能效果来增加工作范围
        double efficiency = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.EFFICIENT_FARMING);
        int workRadius = radius + (int)(radius * efficiency);
        // 使用增加后的范围寻找空地
        for (int x = -workRadius; x <= workRadius; x++) {
            for (int z = -workRadius; z <= workRadius; z++) {
                Location loc = center.clone().add(x, -1, z);
                Block block = loc.getBlock();

                // 检查是否为空地
                if (block.getType() == Material.FARMLAND) {
                    Block above = block.getRelative(0, 1, 0);
                    if (above.getType() == Material.AIR) {
                        return loc;
                    }
                }
            }
        }
        return null;
    }

    private void harvestCrop(Block block, NationNPC npc) {
        if (!(block.getBlockData() instanceof Ageable)) return;
        
        Ageable crop = (Ageable) block.getBlockData();
        if (crop.getAge() < crop.getMaximumAge()) return;

        // 获取技能效果
        double harvestBonus = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.HARVEST_MASTER);
        double rareDropChance = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.CROP_EXPERT);

        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        
        // 基础掉落
        Material cropType = block.getType();
        ItemStack drops = new ItemStack(cropType, 1);
        Map<Integer, ItemStack> overflow = npc.getInventory().addItem(drops);
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                block.getWorld().dropItemNaturally(dropLoc, item);
            }
        }
        
        // 显示收获效果
        block.getWorld().spawnParticle(
            org.bukkit.Particle.VILLAGER_HAPPY,
            dropLoc,
            5, 0.2, 0.2, 0.2, 0,
            null,
            true
        );
        
        // 额外掉落（基于收获大师技能）
        if (random.nextDouble() < harvestBonus) {
            ItemStack bonusDrops = drops.clone();
            overflow = npc.getInventory().addItem(bonusDrops);
            if (!overflow.isEmpty()) {
                for (ItemStack item : overflow.values()) {
                    block.getWorld().dropItemNaturally(dropLoc, item);
                }
            }
            
            // 显示额外收获效果
            block.getWorld().spawnParticle(
                org.bukkit.Particle.VILLAGER_HAPPY,
                dropLoc,
                10, 0.3, 0.3, 0.3, 0,
                null,
                true
            );
        }
        
        // 稀有作物掉落（基于作物专家技能）
        if (random.nextDouble() < rareDropChance) {
            Material rareCrop = RARE_CROPS[random.nextInt(RARE_CROPS.length)];
            ItemStack rareItem = new ItemStack(rareCrop, 1);
            
            overflow = npc.getInventory().addItem(rareItem);
            if (!overflow.isEmpty()) {
                for (ItemStack item : overflow.values()) {
                    block.getWorld().dropItemNaturally(dropLoc, item);
                }
            }
            
            // 显示稀有掉落效果
            block.getWorld().spawnParticle(
                org.bukkit.Particle.TOTEM,
                dropLoc,
                20, 0.3, 0.3, 0.3, 0.5,
                null,
                true
            );
            
            // 发送消息
            String rareCropName = rareCrop.name()
                .replace("_", " ")
                .toLowerCase();
            npc.getCitizensNPC().getEntity().getWorld().getPlayers().forEach(p ->
                p.sendMessage("§e" + npc.getCitizensNPC().getName() + 
                    " §a通过专业技能获得了 §6" + rareCropName + "§a！")
            );
        }

        // 重置作物
        crop.setAge(0);
        block.setBlockData(crop);
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
        Block soil = loc.getBlock();
        if (soil.getType() != Material.FARMLAND) {
            return false;
        }
        Block block = soil.getRelative(0, 1, 0);
        if (block.getType() != Material.AIR) {
            return false;
        }
        
        ItemStack seeds = findSeeds(npc);
        if (seeds == null) return false;
        
        Material cropType = getCropTypeFromSeeds(seeds.getType());
        if (cropType == null) return false;
        
        // 种植作物
        block.setType(cropType);
        seeds.setAmount(seeds.getAmount() - 1);
        
        Location particleLoc = block.getLocation().add(0.5, 0.5, 0.5);
        
        // 高效种植技能效果：有机会直接生长
        double efficiency = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.EFFICIENT_FARMING);
        if (random.nextDouble() < efficiency) {
            Ageable crop = (Ageable) block.getBlockData();
            crop.setAge(crop.getAge() + 1);
            block.setBlockData(crop);
            
            // 显示生长效果
            block.getWorld().spawnParticle(
                org.bukkit.Particle.VILLAGER_HAPPY,
                particleLoc,
                10, 0.2, 0.2, 0.2, 0,
                null,
                true
            );
        }
        
        // 显示种植效果
        block.getWorld().spawnParticle(
            org.bukkit.Particle.VILLAGER_HAPPY,
            particleLoc,
            5, 0.2, 0.2, 0.2, 0,
            null,
            true
        );
        
        return true;
    }

    private Material getCropTypeFromSeeds(Material seedType) {
        switch (seedType) {
            case WHEAT_SEEDS:
                return Material.WHEAT;
            default:
                return null;
        }
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
            Location randomLoc = getRandomLocation(npc.getWorkPosition(), BASE_WORK_RADIUS);
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

    private void work(NationNPC npc) {
        if (npc.getWorkPosition() == null) return;
        
        // 获取技能数据
        NPCSkillData efficientFarming = npc.getSkillData(NPCSkill.EFFICIENT_FARMING);
        NPCSkillData harvestMaster = npc.getSkillData(NPCSkill.HARVEST_MASTER);
        NPCSkillData cropExpert = npc.getSkillData(NPCSkill.CROP_EXPERT);
        
        // 计算技能效果
        double growthBonus = efficientFarming != null && efficientFarming.isUnlocked() ? 
            NPCSkill.EFFICIENT_FARMING.getEffectValue(efficientFarming.getLevel()) : 0;
            
        double harvestBonus = harvestMaster != null && harvestMaster.isUnlocked() ? 
            NPCSkill.HARVEST_MASTER.getEffectValue(harvestMaster.getLevel()) : 0;
            
        double rareDropChance = cropExpert != null && cropExpert.isUnlocked() ? 
            NPCSkill.CROP_EXPERT.getEffectValue(cropExpert.getLevel()) : 0;

        // 计算工作范围 (基础范围 + 技能加成)
        int workRadius = BASE_WORK_RADIUS;
        if (efficientFarming != null && efficientFarming.isUnlocked()) {
            workRadius += Math.floor(efficientFarming.getLevel() * 0.5); // 每2级增加1格范围
        }

        // 计算能量消耗减免 (高等级农民更有效率)
        double energyEfficiency = 1.0;
        if (efficientFarming != null && efficientFarming.isUnlocked()) {
            energyEfficiency -= (efficientFarming.getLevel() * 0.05); // 每级减少5%能量消耗
            energyEfficiency = Math.max(0.5, energyEfficiency); // 最低消耗50%能量
        }
        
        int blockX = npc.getWorkPosition().getBlockX();
        int blockY = npc.getWorkPosition().getBlockY();
        int blockZ = npc.getWorkPosition().getBlockZ();
        
        for (int x = -workRadius; x <= workRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -workRadius; z <= workRadius; z++) {
                    Block block = npc.getWorkPosition().getWorld().getBlockAt(
                        blockX + x, 
                        blockY + y, 
                        blockZ + z
                    );
                    
                    if (block.getBlockData() instanceof Ageable) {
                        Ageable crop = (Ageable) block.getBlockData();
                        
                        // 高效种植: 随机生长
                        if (random.nextDouble() < growthBonus) {
                            if (crop.getAge() < crop.getMaximumAge()) {
                                int growthAmount = 1;
                                // 高级农民可能一次性生长多格
                                if (efficientFarming != null && efficientFarming.getLevel() >= 8) {
                                    growthAmount = random.nextInt(2) + 1;
                                }
                                
                                int newAge = Math.min(crop.getAge() + growthAmount, crop.getMaximumAge());
                                crop.setAge(newAge);
                                block.setBlockData(crop);
                                
                                // 根据生长量给予经验
                                int expGain = 5 * growthAmount;
                                npc.gainExperience(expGain);
                                
                                // 消耗能量
                                int energyCost = (int) Math.ceil(1 * energyEfficiency);
                                npc.setEnergy(Math.max(0, npc.getEnergy() - energyCost));
                            }
                        }
                        
                        // 收获成熟作物
                        if (crop.getAge() == crop.getMaximumAge()) {
                            // 收获大师: 增加产量
                            int baseDrops = 1;
                            int bonusDrops = (int)(baseDrops * harvestBonus);
                            
                            // 作物专家: 稀有作物掉落
                            if (random.nextDouble() < rareDropChance) {
                                // 根据专家等级决定稀有物品
                                int maxRareIndex = cropExpert != null ? 
                                    Math.min(RARE_CROPS.length - 1, 
                                        (int)(cropExpert.getLevel() / 3)) : 0;
                                
                                Material rareCrop = RARE_CROPS[random.nextInt(maxRareIndex + 1)];
                                npc.getInventory().addItem(new ItemStack(rareCrop));
                                
                                // 稀有作物给予更多经验
                                int rareExpGain = 20;
                                if (rareCrop == Material.ENCHANTED_GOLDEN_APPLE) {
                                    rareExpGain = 100; // 特殊奖励
                                }
                                npc.gainExperience(rareExpGain);
                            }
                            
                            // 收获普通作物
                            Material cropType = block.getType();
                            ItemStack drops = new ItemStack(cropType, baseDrops + bonusDrops);
                            npc.getInventory().addItem(drops);
                            
                            // 重置作物生长
                            crop.setAge(0);
                            block.setBlockData(crop);
                            
                            // 收获经验
                            int harvestExp = 10 + (bonusDrops * 2); // 额外产量带来更多经验
                            npc.gainExperience(harvestExp);
                            
                            // 消耗能量 (收获比生长更累)
                            int energyCost = (int) Math.ceil(2 * energyEfficiency);
                            npc.setEnergy(Math.max(0, npc.getEnergy() - energyCost));
                        }
                    }
                }
            }
        }
    }
}