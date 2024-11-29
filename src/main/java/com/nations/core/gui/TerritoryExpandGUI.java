package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TerritoryExpandGUI extends BaseGUI {
    private final Nation nation;
    
    public TerritoryExpandGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6扩展领土", 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        super.fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // TODO: 实现领土扩展界面
        
        // 返回按钮
        setItem(22, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new TerritoryGUI(plugin, p, nation).open());
    }
} 