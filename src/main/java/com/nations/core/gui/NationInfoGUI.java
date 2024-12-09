package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class NationInfoGUI extends BaseGUI {
    private final Nation nation;
    
    public NationInfoGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6国家信息 - " + nation.getName(), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 基本信息
        setItem(13, createItem(Material.BEACON,
            "§6" + nation.getName(),
            "§7等级: §f" + nation.getLevel(),
            "§7成员: §f" + nation.getCurrentMembers() + "/" + nation.getMaxMembers(),
            "§7余额: §f" + nation.getBalance(),
            "§7创建时间: §f" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                .format(new java.util.Date(nation.getCreatedTime()))
        ), null);
        
        // 等级信息
        setItem(12, createItem(Material.EXPERIENCE_BOTTLE,
            "§6国家等级: §f" + nation.getLevel(),
            "§7最大成员数: §f" + nation.getMaxMembers(),
            "§7最大领土范围: §f" + nation.getMaxRadius() * 2 + "x" + nation.getMaxRadius() * 2
        ), null);
        
        // 经济信息
        setItem(14, createItem(Material.GOLD_INGOT,
            "§6国库余额: §f" + nation.getBalance(),
            "§7点击查看经济详情"
        ), p -> new EconomyGUI(plugin, p, nation).open());
        
        // 领土信息
        if (nation.getTerritory() != null) {
            setItem(16, createItem(Material.MAP,
                "§6领土信息",
                "§7中心坐标: §f" + nation.getTerritory().getCenterX() + ", " + nation.getTerritory().getCenterZ(),
                "§7当前范围: §f" + nation.getTerritory().getRadius() * 2 + "x" + nation.getTerritory().getRadius() * 2,
                "§7点击管理领土"
            ), p -> new TerritoryGUI(plugin, p, nation).open());
        }
        
        // 返回按钮
        setItem(22, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
} 