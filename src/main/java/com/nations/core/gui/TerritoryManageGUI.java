package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TerritoryManageGUI extends BaseGUI {
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public TerritoryManageGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6领土管理", 6);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        List<Nation> nations = new ArrayList<>(plugin.getNationManager().getAllNations());
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, nations.size());
        
        // 显示国家列表
        for (int i = 0; i < endIndex - startIndex; i++) {
            Nation nation = nations.get(startIndex + i);
            List<String> lore = new ArrayList<>();
            lore.add("§7领土信息:");
            if (nation.getTerritory() != null) {
                lore.add("§7中心: §f" + nation.getTerritory().getCenterX() + ", " + nation.getTerritory().getCenterZ());
                lore.add("§7范围: §f" + nation.getTerritory().getRadius() * 2 + "x" + nation.getTerritory().getRadius() * 2);
            } else {
                lore.add("§c未设置领土");
            }
            lore.add("");
            lore.add("§e点击管理领土");
            
            setItem(10 + i + (i/7)*2, createItem(Material.MAP,
                "§6" + nation.getName(),
                lore.toArray(new String[0])
            ), p -> new TerritoryDetailGUI(plugin, p, nation).open());
        }
        
        // 翻页按钮
        if (page > 0) {
            setItem(45, createItem(Material.ARROW,
                "§f上一页",
                "§7点击查看上一页"
            ), p -> {
                page--;
                initialize();
            });
        }
        
        if (endIndex < nations.size()) {
            setItem(53, createItem(Material.ARROW,
                "§f下一页",
                "§7点击查看下一页"
            ), p -> {
                page++;
                initialize();
            });
        }
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
} 