package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.Building;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CropSelectGUI extends BaseGUI {
    private final Building building;
    
    public CropSelectGUI(NationsCore plugin, Player player, Building building) {
        super(plugin, player, "§6选择作物", 3);
        this.building = building;
        initialize();
    }
    
    private void initialize() {
        // ... 实现初始化逻辑
    }
} 