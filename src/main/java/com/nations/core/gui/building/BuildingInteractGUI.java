package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.Building;
import com.nations.core.models.NationNPC;
import com.nations.core.models.NPCType;
import com.nations.core.models.WorkState;
import com.nations.core.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildingInteractGUI extends BaseGUI {
    private final Building building;

    public BuildingInteractGUI(NationsCore plugin, Player player, Building building) {
        super(plugin, player, "§6建筑互动", 6);
        this.building = building;
        initialize();
    }

    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);

        switch (building.getType()) {
            case FARM -> initializeFarmGUI();
            // 其他建筑类型
        }
    }

    private void initializeFarmGUI() {
        // 工人管理按钮
        setItem(11, createWorkerManageItem(), p -> new WorkerManageGUI(plugin, p, building).open());
        
        // 作物选择按钮
        setItem(13, createCropSelectItem(), p -> new CropSelectGUI(plugin, p, building).open());
        
        // 自动化设置按钮
        setItem(15, createAutomationItem(), p -> new AutomationGUI(plugin, p, building).open());
        
        // 显示当前状态
        setItem(31, createStatusItem(), null);
    }

    private ItemStack createWorkerManageItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7当前工人:");
        
        // 获取所有工人
        List<NationNPC> workers = plugin.getNPCManager().getBuildingWorkers(building);
        
        // 显示工人信息
        workers.forEach(npc -> {
            String status = switch(npc.getState()) {
                case WORKING -> "§a工作中";
                case RESTING -> "§e休息中";
                case TRAVELING -> "§b移动中";
                default -> "§7空闲";
            };
            
            lore.add(String.format("§7- %s §f(Lv.%d) %s",
                npc.getType().getDisplayName(),
                npc.getLevel(),
                status
            ));
        });
        
        // 显示空余位置
        Map<NPCType, Integer> slots = building.getType().getWorkerSlots();
        int totalSlots = slots.values().stream().mapToInt(Integer::intValue).sum();
        int usedSlots = workers.size();
        
        lore.add("");
        lore.add(String.format("§7工位: §f%d/%d", usedSlots, totalSlots));
        lore.add("");
        lore.add("§e点击管理工人");
        
        return createItem(Material.PLAYER_HEAD,
            "§6工人管理",
            lore.toArray(new String[0]));
    }

    private ItemStack createCropSelectItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7当前作物: §f小麦");
        lore.add("");
        lore.add("§e点击切换作物类型");
        
        return createItem(Material.WHEAT,
            "§6作物选择",
            lore.toArray(new String[0]));
    }

    private ItemStack createAutomationItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7自动收获: §c关闭");
        lore.add("§7自动补种: §c关闭");
        lore.add("");
        lore.add("§e点击设置自动化");
        
        return createItem(Material.COMPARATOR,
            "§6自动化设置",
            lore.toArray(new String[0]));
    }

    private ItemStack createStatusItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7工作效率: §f" + calculateEfficiency() + "%");
        lore.add("§7产量加成: §f" + calculateBonus() + "%");
        lore.add("");
        lore.add("§7当前状态:");
        plugin.getNPCManager().getBuildingWorkers(building).forEach(npc -> {
            lore.add("§7- " + npc.getType().getDisplayName() + ": " + 
                getStatusColor(npc.getState()) + npc.getState().getDisplayName());
        });
        
        return createItem(Material.PAPER,
            "§6建筑状态",
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

    private double calculateEfficiency() {
        return plugin.getNPCManager().getBuildingWorkers(building).stream()
            .mapToDouble(NationNPC::getEfficiency)
            .average()
            .orElse(0.0) * 100;
    }

    private double calculateBonus() {
        return building.getLevel() * 10;
    }
}