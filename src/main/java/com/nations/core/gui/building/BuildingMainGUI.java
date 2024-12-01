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
        lore.add("§7类型: §f" + building.getType().getDisplayName());
        lore.add("§7等级: §f" + building.getLevel());
        lore.add("");
        lore.add("§7加成效果:");
        building.getBonuses().forEach((key, value) -> 
            lore.add("§7- " + formatBonus(key, value)));
        lore.add("");
        lore.add("§e点击管理建筑");
        
        return createItem(getBuildingMaterial(building.getType()),
            "§6" + building.getType().getDisplayName(),
            lore.toArray(new String[0]));
    }
    
    private String formatBonus(String key, Double value) {
        return switch (key) {
            case "tax_rate" -> String.format("税收加成: +%.1f%%", value * 100);
            case "max_members" -> String.format("成员上限: +%.0f", value);
            case "strength" -> String.format("战斗力: +%.1f", value);
            case "defense" -> String.format("防御力: +%.1f", value);
            case "trade_discount" -> String.format("交易折扣: %.1f%%", value * 100);
            case "income_bonus" -> String.format("收入加成: +%.1f%%", value * 100);
            case "storage_size" -> String.format("存储空间: +%.0f", value);
            case "food_production" -> String.format("食物产量: +%.1f/h", value);
            default -> key + ": " + value;
        };
    }
} 