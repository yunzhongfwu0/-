package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.Building;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AutomationGUI extends BaseGUI {
    private final Building building;
    
    public AutomationGUI(NationsCore plugin, Player player, Building building) {
        super(plugin, player, "§6自动化设置", 3);
        this.building = building;
        initialize();
    }
    
    private void initialize() {
        // ... 实现初始化逻辑
    }
} 