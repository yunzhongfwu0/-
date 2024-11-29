package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.TerritoryVisualizer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TerritoryGUI extends BaseGUI {
    private final Nation nation;
    
    public TerritoryGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6领土管理 - " + nation.getName(), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示领土边界
        setItem(11, createItem(Material.GLOWSTONE_DUST,
            "§6显示领土边界",
            "§7点击显示领土范围",
            nation.getTerritory() == null ? "§c你的国家还没有领土！" : "§a点击显示边界"
        ), p -> {
            p.closeInventory();
            TerritoryVisualizer.showBorders(p, nation.getTerritory());
        });
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
} 