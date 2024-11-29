package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MemberActionGUI extends BaseGUI {
    private final Nation nation;
    private final UUID targetUUID;
    
    public MemberActionGUI(NationsCore plugin, Player player, Nation nation, UUID targetUUID) {
        super(plugin, player, "§6成员操作", 3);
        this.nation = nation;
        this.targetUUID = targetUUID;
        initialize();
    }
    
    private void initialize() {
        super.fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 踢出成员
        setItem(11, createItem(Material.BARRIER,
            "§c踢出成员",
            "§7点击踢出该成员"
        ), p -> {
            if (plugin.getNationManager().removeMember(nation, targetUUID)) {
                p.sendMessage("§a成功踢出成员！");
                new MemberManageGUI(plugin, p, nation).open();
            }
        });
        
        // 返回按钮
        setItem(15, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new MemberManageGUI(plugin, p, nation).open());
    }
} 