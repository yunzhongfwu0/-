package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import com.nations.core.npc.NPCBehavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.citizensnpcs.api.trait.trait.Equipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarehouseKeeperBehavior implements NPCBehavior {
    private BukkitRunnable sortingTask;
    private static final int SORT_INTERVAL = 1200; // 每分钟整理一次
    
    @Override
    public void performWork(NationNPC npc) {
        if (!isValidForWork(npc)) {
            NationsCore.getInstance().getLogger().info("仓库管理员NPC无效，跳过工作");
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

        // 获取工作地点
        Building workplace = npc.getWorkplace();
        if (workplace == null || workplace.getType() != BuildingType.WAREHOUSE) {
            return;
        }

        // 进入工作状态
        if (npc.getState() != WorkState.WORKING) {
            enterWorkState(npc);
            startSortingTask(npc);
        }
    }

    private void startSortingTask(NationNPC npc) {
        // 取消旧的任务
        if (sortingTask != null) {
            sortingTask.cancel();
        }

        // 创建新的整理任务
        sortingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValidForWork(npc)) {
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
                double logisticsBonus = NationsCore.getInstance().getNPCSkillManager()
                    .getSkillEffectiveness(npc, NPCSkill.LOGISTICS);

                // 整理所有箱子
                Location baseLoc = warehouse.getBaseLocation();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location chestLoc = baseLoc.clone().add(x, 1, z);
                        Block block = chestLoc.getBlock();
                        if (block.getState() instanceof Container container) {
                            sortContainer(container, warehouse, organizationBonus, storageBonus);
                        }
                    }
                }

                // 应用物流加成
                if (logisticsBonus > 0) {
                    warehouse.addEfficiencyBonus("logistics", logisticsBonus);
                }

                // 消耗体力（根据效率加成减少消耗）
                int energyCost = (int) Math.ceil(5 / (1 + organizationBonus));
                npc.setEnergy(Math.max(0, npc.getEnergy() - energyCost));

                // 获得经验
                npc.gainExperience(1);
            }
        };

        // 启动任务
        sortingTask.runTaskTimer(NationsCore.getInstance(), SORT_INTERVAL, SORT_INTERVAL);
    }

    private void sortContainer(Container container, Building warehouse, double organizationBonus, double storageBonus) {
        // 获取仓库容量上限（考虑存储专家技能加成）
        int baseStorage = warehouse.getLevel() * 100;
        int maxStorage = (int)(baseStorage * (1 + storageBonus));
        
        // 获取当前物品
        ItemStack[] contents = container.getInventory().getContents();
        List<ItemStack> items = new ArrayList<>();
        
        // 统计物品
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                items.add(item);
            }
        }
        
        // 如果超出容量,发出警告
        if (items.size() > maxStorage) {
            NationsCore.getInstance().getLogger().warning(String.format(
                "仓库 %d 存储超出上限! 当前: %d, 上限: %d",
                warehouse.getId(), items.size(), maxStorage
            ));
            return;
        }
        
        // 按物品类型分类
        items.sort((a, b) -> {
            // 首先按材质分类
            int typeCompare = a.getType().compareTo(b.getType());
            if (typeCompare != 0) return typeCompare;
            
            // 其次按数量降序
            return Integer.compare(b.getAmount(), a.getAmount());
        });
        
        // 清空容器
        container.getInventory().clear();
        
        // 重新放入排序后的物品
        for (ItemStack item : items) {
            container.getInventory().addItem(item);
        }
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