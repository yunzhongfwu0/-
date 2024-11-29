package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TerritoryDetailGUI extends BaseGUI {
    private final Nation nation;
    
    public TerritoryDetailGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6领土管理 - " + nation.getName(), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示当前领土信息
        if (nation.getTerritory() != null) {
            setItem(13, createItem(Material.MAP,
                "§6当前领土信息",
                "§7中心: §f" + nation.getTerritory().getCenterX() + ", " + nation.getTerritory().getCenterZ(),
                "§7范围: §f" + nation.getTerritory().getRadius() * 2 + "x" + nation.getTerritory().getRadius() * 2,
                "§7世界: §f" + nation.getTerritory().getWorldName()
            ), null);
        } else {
            setItem(13, createItem(Material.BARRIER,
                "§c未设置领土",
                "§7该国家还没有领土"
            ), null);
        }
        
        // 修改领土范围
        setItem(11, createItem(Material.REDSTONE,
            "§6修改领土范围",
            "§7点击修改领土范围",
            "",
            "§7当前范围: §f" + (nation.getTerritory() != null ? 
                nation.getTerritory().getRadius() * 2 + "x" + nation.getTerritory().getRadius() * 2 : "未设置")
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请输入新的领土范围（边长），或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消操作。");
                    new TerritoryDetailGUI(plugin, p, nation).open();
                    return;
                }
                try {
                    int size = Integer.parseInt(input);
                    if (size < 10 || size > 1000) {
                        p.sendMessage("§c范围必须在 10-1000 之间！");
                        return;
                    }
                    if (plugin.getNationManager().setTerritorySize(nation, size/2)) {
                        p.sendMessage("§a成功设置领土范围为: " + size + "x" + size);
                        new TerritoryDetailGUI(plugin, p, nation).open();
                    } else {
                        p.sendMessage("§c设置领土范围失败！");
                    }
                } catch (NumberFormatException e) {
                    p.sendMessage("§c无效的数字！");
                }
            });
        });
        
        // 重设领土中心
        setItem(15, createItem(Material.COMPASS,
            "§6重设领土中心",
            "§7点击将领土中心设为当前位置"
        ), p -> {
            if (plugin.getNationManager().setTerritoryCenter(nation, p.getLocation())) {
                p.sendMessage("§a成功将领土中心设置为当前位置！");
                new TerritoryDetailGUI(plugin, p, nation).open();
            } else {
                p.sendMessage("§c设置领土中心失败！");
            }
        });
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回领土列表",
            "§7点击返回"
        ), p -> new TerritoryManageGUI(plugin, p).open());
    }
} 