package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TransferGUI extends BaseGUI {
    private final Nation nation;
    
    public TransferGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6转账", 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        super.fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // TODO: 实现转账界面
        
        // 返回按钮
        setItem(22, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new EconomyGUI(plugin, p, nation).open());
    }
} 