package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.npc.behaviors.AbstractNPCBehavior;
import com.nations.core.models.NationNPC;
import com.nations.core.models.WorkState;
import com.nations.core.models.Building;
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

public class FarmerBehavior extends AbstractNPCBehavior {
    // 基础工作范围
    private static final int BASE_WORK_RADIUS = 3;
    // 交互距离
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

        // 获取当前时间
        long time = npc.getCitizensNPC().getEntity().getWorld().getTime();
        boolean isWorkTime = time >= 0 && time < 12000; // 白天是工作时间

        // 检查体力和状态
        if (npc.getEnergy() <= 0) {
            if (npc.getState() != WorkState.RESTING) {
                enterRestState(npc);
            }
            return;
        }

        // 如果在休息状态，检查是否可以恢复工作
        if (npc.getState() == WorkState.RESTING) {
            if (npc.getEnergy() >= 100 && isWorkTime) {
                enterWorkState(npc);
            } else {
                return;
            }
        }

        // 如果不是工作时间，进入休息状态
        if (!isWorkTime && npc.getState() != WorkState.RESTING) {
            enterRestState(npc);
            return;
        }

        // 每次工作有概率实际执行（降低工作频率）
        if (random.nextDouble() >= 0.6) {
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
            npc.setEnergy(npc.getEnergy() - 2); // 增加体力消耗
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
        if (!hasWork) {
            enterRestState(npc);
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

    private int getWorkRadius(NationNPC npc) {
        Building workplace = npc.getWorkplace();
        if (workplace == null) return BASE_WORK_RADIUS;
        
        // 使用建筑尺寸的一半作为基础工作半�����，向上取整以确保完全覆盖
        int baseRadius = (int) Math.ceil(workplace.getSize() / 2.0);
        
        // 获取高效种植技能效果来增加工作范围
        double efficiency = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.EFFICIENT_FARMING);
        
        // 技能加成增加工作范围
        int finalRadius = baseRadius + (int)(baseRadius * efficiency);
        
        return finalRadius;
    }

    private Block findMatureCrop(Location center, int radius, NationNPC npc) {
        int workRadius = getWorkRadius(npc);
        Building workplace = npc.getWorkplace();

        // 使用建筑实际大小的范围寻找成熟作物
        for (int x = -workRadius; x <= workRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -workRadius; z <= workRadius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getBlockData() instanceof Ageable) {
                        Ageable crop = (Ageable) block.getBlockData();
                        if (crop.getAge() == crop.getMaximumAge()) {
                            return block;
                        }
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
        int workRadius = getWorkRadius(npc);
        
        // 使用建筑实际大小的范围寻找空地
        for (int x = -workRadius; x <= workRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -workRadius; z <= workRadius; z++) {
                    Location loc = center.clone().add(x, y, z);
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
        }
        return null;
    }

    private void harvestCrop(Block block, NationNPC npc) {
        if (!(block.getBlockData() instanceof Ageable)) return;
        
        Ageable crop = (Ageable) block.getBlockData();
        if (crop.getAge() < crop.getMaximumAge()) return;

        // 获���技能效果
        double harvestBonus = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.HARVEST_MASTER);
        double rareDropChance = NationsCore.getInstance().getNPCSkillManager()
            .getSkillEffectiveness(npc, NPCSkill.CROP_EXPERT);

        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        
        // 基础掉落
        Material cropType = block.getType();
        ItemStack drops = new ItemStack(getCropDropType(cropType), 1);
        Map<Integer, ItemStack> overflow = npc.getInventory().addItem(drops);
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                block.getWorld().dropItemNaturally(dropLoc, item);
            }
        }
        
        // 种子掉落
        Material seedType = getSeedType(cropType);
        if (seedType != null) {
            // 基础掉落概率为30%，每级增加2%，最高90%
            double seedDropChance = Math.min(0.3 + (npc.getLevel() * 0.02), 0.9);
            
            // 收获大师技能增加10%-30%的掉落概率
            if (harvestBonus > 0) {
                seedDropChance += harvestBonus * 0.2; // 0.1-0.3的额外概率
            }
            
            if (random.nextDouble() < seedDropChance) {
                // 基础掉落1个种子，每5级增加1个上限
                int maxSeeds = 1 + (npc.getLevel() / 5);
                int seedAmount = 1 + random.nextInt(maxSeeds);
                
                // 收获大师技能有几率额外给予种子
                if (harvestBonus > 0 && random.nextDouble() < harvestBonus) {
                    seedAmount += 1;
                }
                
                ItemStack seeds = new ItemStack(seedType, seedAmount);
                overflow = npc.getInventory().addItem(seeds);
                if (!overflow.isEmpty()) {
                    for (ItemStack item : overflow.values()) {
                        block.getWorld().dropItemNaturally(dropLoc, item);
                    }
                }
                
                // 显示种子掉落效果
                block.getWorld().spawnParticle(
                    org.bukkit.Particle.VILLAGER_HAPPY,
                    dropLoc,
                    3, 0.2, 0.2, 0.2, 0,
                    null,
                    true
                );
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
        
        // 额外作物掉落（基于收获大师技能）
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

    private Material getSeedType(Material cropType) {
        return switch (cropType) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case POTATOES -> Material.POTATO;
            case CARROTS -> Material.CARROT;
            case PUMPKIN_STEM -> Material.PUMPKIN_SEEDS;
            case MELON_STEM -> Material.MELON_SEEDS;
            default -> null;
        };
    }

    private Material getCropDropType(Material cropType) {
        return switch (cropType) {
            case WHEAT -> Material.WHEAT;
            case BEETROOTS -> Material.BEETROOT;
            case POTATOES -> Material.POTATO;
            case CARROTS -> Material.CARROT;
            case PUMPKIN_STEM -> Material.PUMPKIN;
            case MELON_STEM -> Material.MELON;
            default -> cropType;
        };
    }

    private ItemStack findSeeds(NationNPC npc) {
        // 按优先级检查各种种子
        Material[] seedTypes = {
            Material.WHEAT_SEEDS,
            Material.BEETROOT_SEEDS,
            Material.POTATO,
            Material.CARROT,
            Material.PUMPKIN_SEEDS,
            Material.MELON_SEEDS
        };
        
        for (Material seedType : seedTypes) {
            int seedSlot = npc.getInventory().first(seedType);
            if (seedSlot != -1) {
                return npc.getInventory().getItem(seedSlot);
            }
        }
        
        // 记录日志：没有找到种子
        NationsCore.getInstance().getLogger().info(
            "农民没种子可用"
        );
        return null;
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
        return switch (seedType) {
            case WHEAT_SEEDS -> Material.WHEAT;
            case BEETROOT_SEEDS -> Material.BEETROOTS;
            case POTATO -> Material.POTATOES;
            case CARROT -> Material.CARROTS;
            case PUMPKIN_SEEDS -> Material.PUMPKIN_STEM;
            case MELON_SEEDS -> Material.MELON_STEM;
            default -> null;
        };
    }

    @Override
    public void onSpawn(NationNPC npc) {
        setupEquipment(npc);
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
        
        Location workPos = npc.getWorkPosition();
        int workRadius = getWorkRadius(npc);
        

        // 获取技能数据
        NPCSkillData efficientFarming = npc.getSkillData(NPCSkill.EFFICIENT_FARMING);
        NPCSkillData harvestMaster = npc.getSkillData(NPCSkill.HARVEST_MASTER);
        NPCSkillData cropExpert = npc.getSkillData(NPCSkill.CROP_EXPERT);
        
        // 计算技能效果（降低效果到原来的40%）
        double growthBonus = efficientFarming != null && efficientFarming.isUnlocked() ? 
            NPCSkill.EFFICIENT_FARMING.getEffectValue(efficientFarming.getLevel()) * 0.4 : 0;
            
        double harvestBonus = harvestMaster != null && harvestMaster.isUnlocked() ? 
            NPCSkill.HARVEST_MASTER.getEffectValue(harvestMaster.getLevel()) * 0.4 : 0;
            
        double rareDropChance = cropExpert != null && cropExpert.isUnlocked() ? 
            NPCSkill.CROP_EXPERT.getEffectValue(cropExpert.getLevel()) * 0.4 : 0;

        // 计算能量消耗减免 (高等级农民更有效率)
        double energyEfficiency = 1.0;
        if (efficientFarming != null && efficientFarming.isUnlocked()) {
            energyEfficiency -= (efficientFarming.getLevel() * 0.03); // 降低能量减免
            energyEfficiency = Math.max(0.7, energyEfficiency); // 最低消耗70%能量
        }
        
        // 遍历完整的3D工作空间
        int checkedBlocks = 0;
        int foundCrops = 0;
        
        for (int x = -workRadius; x <= workRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -workRadius; z <= workRadius; z++) {
                    Block block = workPos.clone().add(x, y, z).getBlock();
                    checkedBlocks++;
                    
                    if (block.getBlockData() instanceof Ageable) {
                        foundCrops++;
                        Ageable crop = (Ageable) block.getBlockData();
                        
                        // 高效种植: 随机生长（降低概率）
                        if (random.nextDouble() < growthBonus * 0.4) {
                            if (crop.getAge() < crop.getMaximumAge()) {
                                int growthAmount = 1;
                                // 高级农民可能一次性生长多格（降低等级要求和概率）
                                if (efficientFarming != null && efficientFarming.getLevel() >= 12 
                                    && random.nextDouble() < 0.3) {
                                    growthAmount = random.nextInt(2) + 1;
                                }
                                
                                int newAge = Math.min(crop.getAge() + growthAmount, crop.getMaximumAge());
                                crop.setAge(newAge);
                                block.setBlockData(crop);
                                
                                // 根据生长量给予经验（降低经验）
                                int expGain = 3 * growthAmount;
                                npc.gainExperience(expGain);
                                
                                // 消耗能量
                                int energyCost = (int) Math.ceil(1 * energyEfficiency);
                                npc.setEnergy(Math.max(0, npc.getEnergy() - energyCost));
                            }
                        }
                        
                        // 收获成熟作物
                        if (crop.getAge() == crop.getMaximumAge() && random.nextDouble() < 0.4) {
                            // 收获大师: 增加产量（降低加成）
                            int baseDrops = 1;
                            int bonusDrops = (int)(baseDrops * (harvestBonus * 0.5));
                            
                            // 作物专家: 稀有作物掉落（降低概率）
                            if (random.nextDouble() < rareDropChance * 0.3) {
                                // 根据专家等级决定稀有物品
                                int maxRareIndex = cropExpert != null ? 
                                    Math.min(RARE_CROPS.length - 1, 
                                        (int)(cropExpert.getLevel() / 4)) : 0;
                                
                                Material rareCrop = RARE_CROPS[random.nextInt(maxRareIndex + 1)];
                                npc.getInventory().addItem(new ItemStack(rareCrop));
                                
                                // 稀有作物给予更多经验（降低经验）
                                int rareExpGain = 15;
                                if (rareCrop == Material.ENCHANTED_GOLDEN_APPLE) {
                                    rareExpGain = 50;
                                }
                                npc.gainExperience(rareExpGain);
                            }
                            
                            // 收获普通作物
                            Material cropType = block.getType();
                            ItemStack drops = new ItemStack(cropType, baseDrops + bonusDrops);
                            npc.getInventory().addItem(drops);
                            
                            // 重置作物长
                            crop.setAge(0);
                            block.setBlockData(crop);
                            
                            // 收获经验（降低经验）
                            int harvestExp = 5 + bonusDrops;
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