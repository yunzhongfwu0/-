package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DeleteConfirmGUI extends BaseGUI {
    private final Nation nation;
    
    public DeleteConfirmGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§c确认删除国家？", 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.RED_STAINED_GLASS_PANE);
        
        // 确认删除
        setItem(11, createItem(Material.LIME_WOOL,
            "§a确认删除",
            "§7点击确认删除国家",
            "§c警告：此操作不可逆！"
        ), p -> {
            if (plugin.getNationManager().deleteNation(nation)) {
                p.closeInventory();
                p.sendMessage("§a成功删除国家！");
            } else {
                p.sendMessage("§c删除国家失败！");
            }
        });
        
        // 取消删除
        setItem(15, createItem(Material.RED_WOOL,
            "§c取消删除",
            "§7点击取消"
        ), p -> new SettingsGUI(plugin, p, nation).open());
    }
} 