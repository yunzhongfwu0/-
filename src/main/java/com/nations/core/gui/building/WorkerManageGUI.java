package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.Building;
import com.nations.core.models.NationNPC;
import com.nations.core.models.Transaction.TransactionType;
import com.nations.core.models.NPCType;
import com.nations.core.models.WorkState;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WorkerManageGUI extends BaseGUI {
    private final Building building;
    
    public WorkerManageGUI(NationsCore plugin, Player player, Building building) {
        super(plugin, player, "§6工人管理", 6);
        this.building = building;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示当前工人
        List<NationNPC> workers = plugin.getNPCManager().getBuildingWorkers(building);
        final int baseSlot = 10;
        
        // 显示现有工人
        for (int i = 0; i < workers.size(); i++) {
            final NationNPC worker = workers.get(i);
            setItem(baseSlot + i, createWorkerItem(worker), p -> handleWorkerClick(worker));
        }
        
        // 显示可雇佣的工人类型
        Map<NPCType, Integer> workerSlots = building.getType().getWorkerSlots();
        final int hireStartSlot = baseSlot + workers.size();
        
        // 使用索引遍历而不是 forEach
        int index = 0;
        for (Map.Entry<NPCType, Integer> entry : workerSlots.entrySet()) {
            final NPCType type = entry.getKey();
            final int maxCount = entry.getValue();
            
            // 计算当前这种类型的工人数量
            final long currentCount = workers.stream()
                .filter(w -> w.getType() == type)
                .count();
                
            // 如果还没达到上限，显示雇佣按钮
            if (currentCount < maxCount) {
                final int slot = hireStartSlot + index;
                setItem(slot, createHireItem(type), p -> handleHire(type));
            }
            index++;
        }
        
        // 返回按钮
        setItem(49, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new BuildingDetailGUI(plugin, p, building.getNation(), building).open());
    }
    
    private void handleWorkerClick(NationNPC worker) {
        // 打开工人详情界面
        new WorkerDetailGUI(plugin, player, worker).open();
    }
    
    private ItemStack createWorkerItem(NationNPC worker) {
        List<String> lore = new ArrayList<>();
        lore.add("§7等级: §f" + worker.getLevel());
        lore.add("§7经验: §f" + worker.getExperience());
        lore.add("§7心情: §f" + worker.getHappiness() + "%");
        lore.add("§7体力: §f" + worker.getEnergy() + "%");
        lore.add("");
        lore.add("§7效率: §f" + String.format("%.1f%%", worker.getEfficiency() * 100));
        lore.add("§7工资: §f" + worker.getCurrentSalary());
        lore.add("");
        lore.add("§7状态: " + getStatusColor(worker.getState()) + worker.getState().getDisplayName());
        lore.add("");
        lore.add("§e左键查看详情");
        lore.add("§c右键解雇");
        
        return createItem(worker.getType().getIcon(),
            "§6" + worker.getType().getDisplayName(),
            lore.toArray(new String[0]));
    }
    
    private ItemStack createHireItem(NPCType type) {
        List<String> lore = new ArrayList<>();
        lore.add("§7职位: §f" + type.getDisplayName());
        lore.add("");
        lore.add("§7说明:");
        lore.addAll(Arrays.asList(type.getDescription().split("\n")));
        lore.add("");
        lore.add("§7基础工资: §f" + type.getBaseSalary());
        lore.add("§7雇佣费用: §f" + (type.getBaseSalary() * 2));
        lore.add("");
        lore.add("§e点击雇佣");
        
        return createItem(type.getIcon(),
            "§a雇佣 " + type.getDisplayName(),
            lore.toArray(new String[0]));
    }
    
    private String getStatusColor(WorkState state) {
        return switch (state) {
            case WORKING -> "§a";
            case RESTING -> "§e";
            case TRAVELING -> "§b";
            default -> "§7";
        };
    }
    
    private void handleHire(NPCType type) {
        // 检查资金
        double cost = type.getBaseSalary() * 2;
        if (building.getNation().getBalance() >= cost) {
            building.getNation().withdraw(cost);
            // 记录交易
            plugin.getNationManager().recordTransaction(
                building.getNation(),
                null,
                TransactionType.WITHDRAW,
                cost,
                "雇佣工人: " + type.getDisplayName()
            );
            NationNPC npc = plugin.getNPCManager().createNPC(type, building);
            if (npc != null) {
                player.sendMessage(MessageUtil.success("成功雇佣 " + type.getDisplayName()));
                initialize(); // 刷新界面
            }
        } else {
            player.sendMessage(MessageUtil.error("资金不足！需要 " + cost + " 金币"));
        }
    }
} 