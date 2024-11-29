package com.nations.core.gui;

import com.nations.core.NationsCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class UpgradeCostSelectGUI extends BaseGUI {
    
    public UpgradeCostSelectGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6升级费用设置 - 选择等级", 3);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 2级 - 发展中国家
        setItem(11, createItem(Material.IRON_BLOCK,
            "§62级 - 发展中国家",
            "§7点击设置升级到2级的费用",
            "",
            "§e当前设置:",
            "§7金钱: §f" + plugin.getConfig().getDouble("nations.levels.2.upgrade-cost.money"),
            "§7物品: §f" + getItemsCount(2) + " 种"
        ), p -> new CostSettingsGUI(plugin, p, false, 2).open());
        
        // 3级 - 地区强国
        setItem(13, createItem(Material.GOLD_BLOCK,
            "§63级 - 地区强国",
            "§7点击设置升级到3级的费用",
            "",
            "§e当前设置:",
            "§7金钱: §f" + plugin.getConfig().getDouble("nations.levels.3.upgrade-cost.money"),
            "§7物品: §f" + getItemsCount(3) + " 种"
        ), p -> new CostSettingsGUI(plugin, p, false, 3).open());
        
        // 4级 - 世界强国
        setItem(15, createItem(Material.DIAMOND_BLOCK,
            "§64级 - 世界强国",
            "§7点击设置升级到4级的费用",
            "",
            "§e当前设置:",
            "§7金钱: §f" + plugin.getConfig().getDouble("nations.levels.4.upgrade-cost.money"),
            "§7物品: §f" + getItemsCount(4) + " 种"
        ), p -> new CostSettingsGUI(plugin, p, false, 4).open());
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
    
    private int getItemsCount(int level) {
        String path = "nations.levels." + level + ".upgrade-cost.items";
        if (!plugin.getConfig().contains(path)) {
            return 0;
        }
        return plugin.getConfig().getConfigurationSection(path).getKeys(false).size();
    }
} 