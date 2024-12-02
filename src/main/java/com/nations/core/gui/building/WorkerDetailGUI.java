package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.gui.ConfirmGUI;
import com.nations.core.models.NationNPC;
import com.nations.core.models.WorkState;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WorkerDetailGUI extends BaseGUI {
    private final NationNPC worker;
    
    public WorkerDetailGUI(NationsCore plugin, Player player, NationNPC worker) {
        super(plugin, player, "§6工人详情", 3);
        this.worker = worker;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 基本信息
        setItem(13, createWorkerInfoItem(), null);
        
        // 解雇按钮
        setItem(15, createItem(Material.BARRIER,
            "§c解雇工人",
            "§7- 将返还一半雇佣费用",
            "§7- 工人将立即离开",
            "",
            "§c警告: 此操作不可撤销！",
            "",
            "§e点击解雇"
        ), this::handleDismiss);
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new WorkerManageGUI(plugin, p, worker.getWorkplace()).open());
    }
    
    private ItemStack createWorkerInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7职位: §f" + worker.getType().getDisplayName());
        lore.add("§7等级: §f" + worker.getLevel());
        lore.add("§7经验: §f" + worker.getExperience() + "/1000");
        lore.add("");
        lore.add("§7状态:");
        lore.add("§7- 心情: §f" + worker.getHappiness() + "%");
        lore.add("§7- 体力: §f" + worker.getEnergy() + "%");
        lore.add("§7- 工作状态: " + getStatusColor(worker.getState()) + worker.getState().getDisplayName());
        lore.add("");
        lore.add("§7效率: §f" + String.format("%.1f%%", worker.getEfficiency() * 100));
        lore.add("§7工资: §f" + worker.getCurrentSalary());
        
        return createItem(worker.getType().getIcon(),
            "§6" + worker.getType().getDisplayName(),
            lore.toArray(new String[0]));
    }
    
    private void handleDismiss(Player player) {
        new ConfirmGUI(plugin, player,
            "确认解雇",
            "解雇工人",
            new String[]{
                "§c此操作不可撤销！",
                "§7将返还:",
                "§7- " + (worker.getCurrentSalary()) + " 金币",
                "",
                "§7工人将立即离开",
                "§7所有数据将被清除"
            },
            this::performDismiss,
            p -> new WorkerDetailGUI(plugin, p, worker).open()
        ).open();
    }
    
    private void performDismiss(Player player) {
        // 返还一半雇佣费用
        worker.getWorkplace().getNation().deposit(worker.getCurrentSalary());
        
        // 解雇工人
        plugin.getNPCManager().dismissWorker(worker);
        
        player.sendMessage(MessageUtil.success("成功解雇工人！"));
        new WorkerManageGUI(plugin, player, worker.getWorkplace()).open();
    }
    
    private String getStatusColor(WorkState state) {
        return switch (state) {
            case WORKING -> "§a";
            case RESTING -> "§e";
            case TRAVELING -> "§b";
            default -> "§7";
        };
    }
} 