package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.Building;
import com.nations.core.models.Nation;
import com.nations.core.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class CropManageGUI extends BaseGUI {
    private final Building building;
    
    public CropManageGUI(NationsCore plugin, Player player, Building building) {
        super(plugin, player, "农夫信息", 3);
        this.building = building;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 种植说明
        setItem(13, createItem(Material.BOOK,
            "§6农夫工作说明",
            "§7农民NPC会自动:",
            "§7- 使用背包中的种子种植作物",
            "§7- 收获成熟的作物",
            "§7- 收获物品存放在NPC背包",
            "",
            "§7工作效率: §f" + calculateEfficiency() + "%",
            "§7总产量加成: §f" + (calculateWorkerBonus() + calculateLevelBonus()) + "%"
        ), null);
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new BuildingInteractGUI(plugin, p, building).open());
    }
    
    private double calculateEfficiency() {
        return plugin.getNPCManager().getBuildingWorkers(building).stream()
            .mapToDouble(npc -> npc.getEfficiency() * 100)
            .average()
            .orElse(0.0);
    }
    
    private double calculateWorkerBonus() {
        return plugin.getNPCManager().getBuildingWorkers(building).stream()
            .mapToDouble(npc -> npc.getEfficiency() * 10)
            .sum();
    }
    
    private double calculateLevelBonus() {
        return building.getLevel() * 5;
    }
}
