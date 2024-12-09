package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.gui.MainGUI;
import com.nations.core.models.Building;
import com.nations.core.models.Nation;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuildingMainGUI extends BaseGUI {
    private final Nation nation;
    
    public BuildingMainGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6建筑管理", 5);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示已有建筑
        Set<Building> buildings = nation.getBuildings();
        int slot = 10;
        for (Building building : buildings) {
            setItem(slot, createBuildingItem(building), 
                p -> new BuildingDetailGUI(plugin, p, nation, building).open());
            
            slot++;
            if (slot % 9 == 8) slot += 3;
        }
        
        // 新建建筑按钮
        setItem(40, createItem(Material.EMERALD_BLOCK,
            "§a建造新建筑",
            "§7点击查看可建造的建筑"
        ), p -> new BuildingCreateGUI(plugin, p, nation).open());
        
        // 返回按钮
        setItem(44, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
    
    private ItemStack createBuildingItem(Building building) {
        List<String> lore = new ArrayList<>();
        lore.add("§7等级: §f" + building.getLevel());
        lore.add("");
        lore.add("§7建筑加成:");
        
        Map<String, Double> bonuses = building.getBonuses();
        switch (building.getType()) {
            case TOWN_HALL -> {
                double taxRate = bonuses.getOrDefault("tax_rate", 0.0) * 100;
                double maxMembers = bonuses.getOrDefault("max_members", 0.0);
                lore.add(String.format("§7- 税率: §f+%.1f%%", taxRate));
                lore.add(String.format("§7- 人口上限: §f+%.0f", maxMembers));
            }
            case BARRACKS -> {
                double slots = bonuses.getOrDefault("training_slots", 0.0);
                double bonus = bonuses.getOrDefault("training_bonus", 0.0) * 100;
                double speed = bonuses.getOrDefault("training_speed", 0.0) * 100;
                lore.add(String.format("§7- 训练位数量: §f%.0f", slots));
                lore.add(String.format("§7- 训练加成: §f+%.1f%%", bonus));
                lore.add(String.format("§7- 训练速度: §f-%.1f%%", speed));
            }
            case MARKET -> {
                double discount = bonuses.getOrDefault("trade_discount", 0.0) * 100;
                double income = bonuses.getOrDefault("income_bonus", 0.0) * 100;
                lore.add(String.format("§7- 交易折扣: §f%.1f%%", discount));
                lore.add(String.format("§7- 收入加成: §f+%.1f%%", income));
            }
            case WAREHOUSE -> {
                double storage = bonuses.getOrDefault("storage_size", 0.0);
                lore.add(String.format("§7- 存储容量: §f%.0f", storage));
            }
            case FARM -> {
                double production = bonuses.getOrDefault("food_production", 0.0);
                lore.add(String.format("§7- 食物产量: §f%.0f/小时", production));
            }
        }

        // 显示效率加成
        double efficiency = building.getTotalEfficiencyBonus() * 100 - 100;
        if (efficiency > 0) {
            lore.add("");
            lore.add(String.format("§e效率加成: §f+%.1f%%", efficiency));
        }

        // 显示工人信息
        lore.add("");
        lore.add("§7工人:");
        building.getType().getWorkerSlots().forEach((type, count) -> {
            int current = (int) plugin.getNPCManager().getBuildingWorkers(building).stream()
                .filter(npc -> npc.getType() == type)
                .count();
            lore.add(String.format("§7- %s: §f%d/%d", type.getDisplayName(), current, count));
        });

        lore.add("");
        lore.add("§e点击查看详情");
        
        return createItem(building.getType().getIcon(),
            "§6" + building.getType().getDisplayName(),
            lore.toArray(new String[0]));
    }
} 