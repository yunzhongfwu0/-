package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.*;
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

        // 建筑详细信息
        List<String> buildingLore = new ArrayList<>();
        buildingLore.add("§7等级: §f" + building.getLevel());
        buildingLore.add("§7位置: §f" + String.format("%.1f, %.1f, %.1f", 
            building.getBaseLocation().getX(),
            building.getBaseLocation().getY(),
            building.getBaseLocation().getZ()
        ));
        buildingLore.add("");
        
        // 添加工人信息
        buildingLore.add("§7工人配置:");
        building.getType().getWorkerSlots().forEach((type, count) -> {
            long current = plugin.getNPCManager().getBuildingWorkers(building).stream()
                .filter(w -> w.getType() == type)
                .count();
            buildingLore.add(String.format("§7- %s: §f%d/%d", 
                type.getDisplayName(), current, count));
        });
        buildingLore.add("");

        // 添加工作时间表
        buildingLore.add("§7工作时间表:");
        building.getType().getWorkSchedule().forEach((time, desc) -> 
            buildingLore.add(String.format("§7%s: §f%s", time, desc)));
        buildingLore.add("");

        buildingLore.add("§e点击传送到此建筑");

        setItem(4, createItem(building.getType().getIcon(),
            "§6" + building.getType().getDisplayName(),
            buildingLore.toArray(new String[0])
        ), p -> {
            p.teleport(building.getBaseLocation());
            p.sendMessage(MessageUtil.success("已传送到 " + building.getType().getDisplayName()));
            p.closeInventory();
        });

        // 根据建筑类型添加特殊功能
        switch (building.getType()) {
            case FARM -> initializeFarmGUI();
            case BARRACKS -> initializeBarracksGUI();
            case MARKET -> initializeMarketGUI();
            case WAREHOUSE -> initializeWarehouseGUI();
            case TOWN_HALL -> initializeTownHallGUI();
        }
    }

    private void initializeFarmGUI() {
        // 农场特有功能
        setItem(40, createItem(Material.WHEAT,
            "§6农场管理",
            "§7查看农场信息",
            "",
            "§e点击查看详情"
        ), p -> new CropManageGUI(plugin, p, building).open());
    }

    private void initializeBarracksGUI() {
        // 兵营特有功能
        setItem(40, createItem(Material.IRON_SWORD,
            "§6兵营管理",
            "§7管理守卫装备和巡逻",
            "",
            "§e点击管理"
        ), p -> {
            // TODO: 实现兵营管理GUI
        });
    }

    private void initializeMarketGUI() {
        // 市场特有功能
        setItem(40, createItem(Material.EMERALD,
            "§6市场管理",
            "§7管理商品和交易",
            "",
            "§e点击管理"
        ), p -> {
            // TODO: 实现市场管理GUI
        });
    }

    private void initializeWarehouseGUI() {
        // 仓库特有功能
        setItem(40, createItem(Material.CHEST,
            "§6仓库管理",
            "§7管理仓库存储",
            "",
            "§e点击管理"
        ), p -> {
            // TODO: 实现仓库管理GUI
        });
    }

    private void initializeTownHallGUI() {
        // 市政厅特有功能
        setItem(40, createItem(Material.BEACON,
            "§6市政厅管理",
            "§7管理国家事务",
            "",
            "§e点击管理"
        ), p -> {
            // TODO: 实现市政厅管理GUI
        });
    }
}