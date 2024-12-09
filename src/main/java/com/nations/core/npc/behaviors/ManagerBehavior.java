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

public class ManagerBehavior extends AbstractNPCBehavior {
    private static final Random random = new Random();
    private static final int TAX_COLLECTION_INTERVAL = 20*60*60; // 1小时
    private static final int MEMBER_CHECK_INTERVAL = 20*60*10; // 10分钟
    
    private BukkitTask taxCollectionTask;
    private BukkitTask memberCheckTask;
    private BukkitTask dailyTask;

    @Override
    public void performWork(NationNPC npc) {
        if (!isValidForWork(npc)) {
            return;
        }
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

        // 如果不在建筑范围内，返回建筑中心
        if (distance > buildingRadius) {
            return;
        }

        // 执行管理工作
        Nation nation = npc.getWorkplace().getNation();
        if (nation != null) {
            // 应用管理效率加成
            double efficiencyBonus = 0.1 * npc.getLevel(); // 每级增加10%效率
            npc.getWorkplace().addEfficiencyBonus("management", efficiencyBonus);
        }
        startTasks(npc, nation);
        
    }

    private void startTasks(NationNPC npc, Nation nation) {
        

        if (isTasksRunning()) {
            return; 
        }
        
        // 收税任务 (每小时)
        taxCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValidForWork(npc)) {
                    cancel();
                    return;
                }
                // 获取当前时间
                long time = npc.getCitizensNPC().getEntity().getWorld().getTime();
                boolean isWorkTime = time >= 0 && time < 12000; // 白天是工作时间
                if (isWorkTime) {
                    NationsCore.getInstance().getLogger().info("管理员NPC开始收税");
                    collectTaxes(npc, nation);
                    // 消耗体力
                    npc.setEnergy(Math.max(0, npc.getEnergy() - 30));
                }
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
                long time = npc.getCitizensNPC().getEntity().getWorld().getTime();
                boolean isWorkTime = time >= 0 && time < 12000; // 白天是工作时间
                if (isWorkTime) {
                    NationsCore.getInstance().getLogger().info("管理员NPC开始检查成员");
                    checkMembers(npc, nation);
                    // 消耗体力
                    npc.setEnergy(Math.max(0, npc.getEnergy() - 10));
                }
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
                long time = npc.getCitizensNPC().getEntity().getWorld().getTime();
                boolean isWorkTime = time >= 0 && time < 12000; // 白天是工作时间
                if (isWorkTime) {
                    NationsCore.getInstance().getLogger().info("管理员NPC处理日常事务");
                    handleDailyTasks(npc, nation);
                // 消耗体力
                npc.setEnergy(npc.getEnergy() - 5);
                NationsCore.getInstance().getLogger().info("管理员NPC完成工作，剩余体力: " + npc.getEnergy());
                }
            }
        }.runTaskTimer(NationsCore.getInstance(), 200L, 200L);
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
    }
    private boolean isTasksRunning() {
        if (taxCollectionTask != null || memberCheckTask != null || dailyTask != null ) {
            return true;
        }
        return false;
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
        // 取消旧的任务
        stopTasks();
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