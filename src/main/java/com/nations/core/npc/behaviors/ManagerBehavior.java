package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import com.nations.core.npc.NPCBehavior;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ManagerBehavior implements NPCBehavior {
    private static final Random random = new Random();
    private static final int TAX_COLLECTION_INTERVAL = 720000; // 1小时
    private static final int MEMBER_CHECK_INTERVAL = 120000; // 10分钟
    private static final int MOVE_INTERVAL = 100; // 5秒
    private static final double INTERACTION_DISTANCE = 2.0;
    private static final int WORK_DURATION = 12000; // 60秒工作时间
    private static final int REST_DURATION = 4000; // 20秒休息时间
    
    private int stateTimer = 0;
    private BukkitTask taxCollectionTask;
    private BukkitTask memberCheckTask;
    private BukkitTask moveTask;
    private BukkitTask dailyTask;

    @Override
    public void performWork(NationNPC npc) {
        if (!isValidForWork(npc)) {
            NationsCore.getInstance().getLogger().info("管理员NPC无效，跳过工作");
            return;
        }

        // 检查体力
        if (npc.getEnergy() <= 0) {
            NationsCore.getInstance().getLogger().info("管理员NPC体力不足，进入休息状态");
            enterRestState(npc);
            handleMovement(npc, true);
            return;
        }

        // 如果当前是休息状态且体力未满，继续休息
        if (npc.getState() == WorkState.RESTING && npc.getEnergy() < 100) {
            NationsCore.getInstance().getLogger().info("管理员NPC正在休息，体力: " + npc.getEnergy());
            enterRestState(npc);
            handleMovement(npc, true);
            return;
        }

        // 获取工作地点和国家
        Building workplace = npc.getWorkplace();
        if (workplace == null) {
            NationsCore.getInstance().getLogger().warning("管理员NPC没有工作地点");
            return;
        }
        Nation nation = workplace.getNation();

        // 确保NPC在建筑范围内
        Location currentLoc = npc.getCitizensNPC().getEntity().getLocation();
        Location buildingCenter = workplace.getBaseLocation();
        if (buildingCenter == null || !currentLoc.getWorld().equals(buildingCenter.getWorld())) {
            return;
        }

        double distance = currentLoc.distance(buildingCenter);
        int buildingRadius = workplace.getType().getBaseSize();

        // 如果不在建筑范围内，传送回建筑中心
        if (distance > buildingRadius) {
            Location safeLocation = findSafeLocation(buildingCenter);
            if (safeLocation != null) {
                npc.getCitizensNPC().teleport(safeLocation, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                NationsCore.getInstance().getLogger().info("管理员已传送回建筑范围内");
            }
            return;
        }

        // 执行管理工作
        if (npc.getState() != WorkState.WORKING) {
            NationsCore.getInstance().getLogger().info("管理员NPC进入工作状态");
            enterWorkState(npc);
            startTasks(npc, nation);
        }

        // 在工作状态下随机移动
        if (npc.getState() == WorkState.WORKING) {
            handleMovement(npc, false);
        }
    }

    private void startTasks(NationNPC npc, Nation nation) {
        // 取消旧的任务
        stopTasks();
        
        // 收税任务 (每小时)
        taxCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValidForWork(npc)) {
                    cancel();
                    return;
                }
                NationsCore.getInstance().getLogger().info("管理员NPC开始收税");
                collectTaxes(npc, nation);
            }
        }.runTaskTimer(NationsCore.getInstance(), TAX_COLLECTION_INTERVAL, TAX_COLLECTION_INTERVAL);

        // 检查成员任务 (每10分钟)
        memberCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValidForWork(npc)) {
                    cancel();
                    return;
                }
                NationsCore.getInstance().getLogger().info("管理员NPC开始检查成员");
                checkMembers(npc, nation);
            }
        }.runTaskTimer(NationsCore.getInstance(), MEMBER_CHECK_INTERVAL, MEMBER_CHECK_INTERVAL);

        // 日常事务任务 (每10秒)
        dailyTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValidForWork(npc)) {
                    cancel();
                    return;
                }
                NationsCore.getInstance().getLogger().info("管理员NPC处理日常事务");
                handleDailyTasks(npc, nation);
                
                // 消耗体力
                npc.setEnergy(npc.getEnergy() - 1);
                NationsCore.getInstance().getLogger().info("管理员NPC完成工作，剩余体力: " + npc.getEnergy());
            }
        }.runTaskTimer(NationsCore.getInstance(), 200L, 200L);

        // 移动任务 (每5秒)
        moveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValidForWork(npc)) {
                    cancel();
                    return;
                }
                handleMovement(npc, npc.getState() == WorkState.RESTING);
            }
        }.runTaskTimer(NationsCore.getInstance(), MOVE_INTERVAL, MOVE_INTERVAL);
    }

    private void stopTasks() {
        if (taxCollectionTask != null) {
            taxCollectionTask.cancel();
            taxCollectionTask = null;
        }
        if (memberCheckTask != null) {
            memberCheckTask.cancel();
            memberCheckTask = null;
        }
        if (dailyTask != null) {
            dailyTask.cancel();
            dailyTask = null;
        }
        if (moveTask != null) {
            moveTask.cancel();
            moveTask = null;
        }
    }

    private Location findSafeLocation(Location target) {
        if (target == null || target.getWorld() == null) return null;
        
        Location safe = target.clone();
        
        // 找到地面
        while (safe.getBlock().getType().isAir() && safe.getY() > 0) {
            safe.subtract(0, 1, 0);
        }
        
        // 确保脚下是实心方块
        if (!safe.getBlock().getType().isSolid()) {
            return null;
        }
        
        // 确保头部有空间
        safe.add(0, 1, 0);
        if (!safe.getBlock().getType().isAir()) {
            return null;
        }
        
        return safe;
    }

    private Location getRandomLocationInBuilding(Location center, int radius) {
        if (center == null || center.getWorld() == null) return null;
        
        // 生成随机角度和距离
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        
        // 计算随机位置的X和Z坐标
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        // 创建新位置，保持Y坐标不变
        return new Location(center.getWorld(), x, center.getY(), z);
    }

    private void handleMovement(NationNPC npc, boolean isResting) {
        if (!npc.getCitizensNPC().isSpawned()) return;
        
        stateTimer++;
        
        // 状态切换检查
        if (isResting && stateTimer >= REST_DURATION) {
            stateTimer = 0;
            npc.setState(WorkState.WORKING);
            return;
        } else if (!isResting && stateTimer >= WORK_DURATION) {
            stateTimer = 0;
            npc.setState(WorkState.RESTING);
            return;
        }

        // 检查位置
        Building workplace = npc.getWorkplace();
        if (workplace == null) return;
        
        Location currentLoc = npc.getCitizensNPC().getEntity().getLocation();
        Location buildingCenter = workplace.getBaseLocation();
        
        if (buildingCenter == null || !currentLoc.getWorld().equals(buildingCenter.getWorld())) {
            return;
        }
        
        double distance = currentLoc.distance(buildingCenter);
        int buildingRadius = workplace.getType().getBaseSize();
        
        // 如果不在建筑范围内，直接传送回建筑中心
        if (distance > buildingRadius) {
            Location safeLocation = findSafeLocation(buildingCenter);
            if (safeLocation != null) {
                npc.getCitizensNPC().teleport(safeLocation, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                NationsCore.getInstance().getLogger().info("管理员已传送回建筑范围内");
            }
            return;
        }
        
        // 在建筑范围内，有30%概率随机走动
        if (Math.random() < 0.3) {
            Location randomLoc = findSafeLocation(getRandomLocationInBuilding(buildingCenter, buildingRadius / 2));
            if (randomLoc != null) {
                // 直接传送到随机位置，不使用导航
                npc.getCitizensNPC().teleport(randomLoc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }
    }

    private void collectTaxes(NationNPC npc, Nation nation) {
        // 计算税收效率（基于管理员等级和技能）
        double efficiency = calculateEfficiency(npc);
        
        // 基础税收（每个在线成员10金币/小时）
        double baseTax = 10.0;
        
        // 获取在线成员
        List<Player> onlineMembers = nation.getOnlineMembers();
        
        // 计算总税收
        double totalTax = onlineMembers.size() * baseTax * efficiency;
        
        // 建筑加成
        double buildingBonus = nation.getBuildingBonus("tax_rate");
        totalTax *= (1 + buildingBonus);
        NationsCore.getInstance().getLogger().info("税收收入: " + totalTax);
        // 收取税收
        if (totalTax > 0) {
            nation.deposit(totalTax);
            NationsCore.getInstance().getNationManager().recordTransaction(
                nation, null, Transaction.TransactionType.DEPOSIT, totalTax, 
                "税收收入 (效率: " + String.format("%.1f%%", efficiency * 100) + ")"
            );
            
            // 给予经验
            npc.gainExperience((int)(totalTax / 10));

            // 向所有在线成员发送税收提示
            String taxMessage = String.format(
                "§a[国家税收] §f管理员§e%s§f收取了税收:\n" +
                "§7- 基础税率: §e%.1f§7金币/人\n" +
                "§7- 在线人数: §e%d§7人\n" +
                "§7- 管理效率: §e%.1f%%\n" +
                "§7- 建筑加成: §e+%.1f%%\n" +
                "§7- 总计收入: §e%.1f§7金币",
                npc.getCitizensNPC().getName(),
                baseTax,
                onlineMembers.size(),
                efficiency * 100,
                buildingBonus * 100,
                totalTax
            );
            
            for (Player member : onlineMembers) {
                member.sendMessage(taxMessage);
            }
        }
    }

    private void checkMembers(NationNPC npc, Nation nation) {
        // 检查成员活跃度
        List<Player> onlineMembers = nation.getOnlineMembers();
        
        // 更新在线时间统计
        for (Player member : onlineMembers) {
            nation.updateMemberActivity(member.getUniqueId());
        }
        
        // 获取经验
        npc.gainExperience(onlineMembers.size());
    }

    private void handleDailyTasks(NationNPC npc, Nation nation) {
        // 1. 更新建筑效率
        for (Building building : nation.getBuildings()) {
            double bonus = (npc.getLevel() * 0.01); // 每级增加1%效率
            
            // 添加危机处理技能效果
            NPCSkillData crisisSkill = npc.getSkillData(NPCSkill.CRISIS_HANDLING);
            if (crisisSkill != null && crisisSkill.isUnlocked()) {
                bonus += crisisSkill.getEffectiveness();
            }
            
            building.addEfficiencyBonus("manager", bonus);
        }
        
        // 2. 管理NPC心情和工资
        if (random.nextDouble() < 0.1) { // 10%概率触发
            // 获取资源管理技能效果
            double costReduction = 0.0;
            NPCSkillData resourceSkill = npc.getSkillData(NPCSkill.RESOURCE_MANAGEMENT);
            if (resourceSkill != null && resourceSkill.isUnlocked()) {
                costReduction = resourceSkill.getEffectiveness();
            }
            
            for (NationNPC worker : NationsCore.getInstance().getNPCManager().getBuildingWorkers(npc.getWorkplace())) {
                if (worker != npc) {
                    // 心情提升
                    int happinessBonus = 5 + (npc.getLevel() / 2); // 基础5点+等级加成
                    worker.setHappiness(Math.min(100, worker.getHappiness() + happinessBonus));
                    
                    // 应用工资减免
                    if (costReduction > 0) {
                        double originalSalary = worker.getCurrentSalary();
                        double reducedSalary = originalSalary * (1 - costReduction);
                        // 这里我们不直接修改工资，而是添加一个临时的减免效果
                        worker.addSalaryModifier("resource_management", -costReduction);
                    }
                }
            }
            npc.gainExperience(1);
        }
    }

    private double calculateEfficiency(NationNPC npc) {
        // 基础效率100%
        double efficiency = 1.0;
        
        // 等级加成（每级2%）
        efficiency += (npc.getLevel() * 0.02);
        
        // 技能加成
        NPCSkillData leadershipSkill = npc.getSkillData(NPCSkill.LEADERSHIP);
        if (leadershipSkill != null && leadershipSkill.isUnlocked()) {
            efficiency += leadershipSkill.getEffectiveness();
        }
        
        // 心情影响（低于50时开始降低效率）
        if (npc.getHappiness() < 50) {
            efficiency *= (0.5 + (npc.getHappiness() / 100.0));
        }
        
        // 体影响（低于30时始降低效率）
        if (npc.getEnergy() < 30) {
            efficiency *= (0.3 + (npc.getEnergy() / 100.0));
        }
        
        return efficiency;
    }

    private boolean isValidForWork(NationNPC npc) {
        return npc != null && 
               npc.getCitizensNPC() != null && 
               npc.getCitizensNPC().getEntity() != null;
    }

    private void enterWorkState(NationNPC npc) {
        npc.setState(WorkState.WORKING);
    }

    private void enterRestState(NationNPC npc) {
        npc.setState(WorkState.RESTING);
        // 休息时缓慢恢复体力
        if (npc.getEnergy() < 100) {
            npc.setEnergy(Math.min(100, npc.getEnergy() + 5));
        }
    }

    @Override
    public void onSpawn(NationNPC npc) {
        // 给予初始装备
        setupEquipment(npc);
    }

    @Override
    public void onDespawn(NationNPC npc) {
        stopTasks();
        // 清理效果
        Building workplace = npc.getWorkplace();
        if (workplace != null) {
            workplace.removeEfficiencyBonus("manager");
        }
    }

    @Override
    public void setupEquipment(NationNPC npc) {
        // 设置管理员的装备
        Equipment equipment = npc.getCitizensNPC().getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.BOOK));
        equipment.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.GOLDEN_HELMET));
    }
} 