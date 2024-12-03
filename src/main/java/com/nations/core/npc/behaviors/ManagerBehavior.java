package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import com.nations.core.npc.NPCBehavior;

import net.citizensnpcs.api.trait.trait.Equipment;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class ManagerBehavior implements NPCBehavior {
    private static final Random random = new Random();
    private static final int TAX_COLLECTION_INTERVAL = 72000; // 1小时
    private static final int MEMBER_CHECK_INTERVAL = 12000; // 10分钟
    
    private int taxCollectionTimer = 0;
    private int memberCheckTimer = 0;

    @Override
    public void performWork(NationNPC npc) {
        if (!isValidForWork(npc)) return;

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

        // 获取工作地点和国家
        Building workplace = npc.getWorkplace();
        Nation nation = workplace.getNation();

        // 执行管理工作
        if (npc.getState() != WorkState.WORKING) {
            enterWorkState(npc);
        }

        // 1. 收税
        if (++taxCollectionTimer >= TAX_COLLECTION_INTERVAL) {
            collectTaxes(npc, nation);
            taxCollectionTimer = 0;
        }

        // 2. 检查成员
        if (++memberCheckTimer >= MEMBER_CHECK_INTERVAL) {
            checkMembers(npc, nation);
            memberCheckTimer = 0;
        }

        // 3. 处理日常事务
        handleDailyTasks(npc, nation);

        // 消耗体力
        npc.setEnergy(npc.getEnergy() - 1);
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

        // 收取税收
        if (totalTax > 0) {
            nation.deposit(totalTax);
            NationsCore.getInstance().getNationManager().recordTransaction(
                nation, null, Transaction.TransactionType.DEPOSIT, totalTax, 
                "税收收入 (效率: " + String.format("%.1f%%", efficiency * 100) + ")"
            );
            
            // 给予经验
            npc.gainExperience((int)(totalTax / 10));
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
            building.addEfficiencyBonus("manager", bonus);
        }
        
        // 2. 管理NPC心情
        if (random.nextDouble() < 0.1) { // 10%概率触发
            for (NationNPC worker : NationsCore.getInstance().getNPCManager().getBuildingWorkers(npc.getWorkplace())) {
                if (worker != npc) {
                    int happinessBonus = 5 + (npc.getLevel() / 2); // 基础5点+等级加成
                    worker.setHappiness(Math.min(100, worker.getHappiness() + happinessBonus));
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
        
        // 心情影响（低于50时开始降低效率）
        if (npc.getHappiness() < 50) {
            efficiency *= (0.5 + (npc.getHappiness() / 100.0));
        }
        
        // 体力影响（低于30时开始降低效率）
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