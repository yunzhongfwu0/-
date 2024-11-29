package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.models.NationRank;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class RankSelectGUI extends BaseGUI {
    private final Nation nation;
    private final UUID targetUUID;
    
    public RankSelectGUI(NationsCore plugin, Player player, Nation nation, UUID targetUUID) {
        super(plugin, player, "§6选择职位", 3);
        this.nation = nation;
        this.targetUUID = targetUUID;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示所有可用职位
        int slot = 10;
        for (NationRank rank : NationRank.values()) {
            if (rank != NationRank.OWNER) { // 不允许设置为国家拥有者
                ArrayList<String> lore = new ArrayList<>();
                lore.add("§7点击设置为此职位");
                lore.add("");
                lore.add("§e权限:");
                rank.getPermissions().forEach(perm -> lore.add("§7- " + perm));
                
                setItem(slot++, createItem(Material.NAME_TAG,
                    "§6" + rank.getDisplayName(),
                    lore.toArray(new String[0])
                ), p -> {
                    if (plugin.getNationManager().setMemberRank(nation, targetUUID, rank)) {
                        p.sendMessage(MessageUtil.success("成功设置玩家职位为: " + rank.getDisplayName()));
                        new MemberManageGUI(plugin, p, nation).open();
                    } else {
                        p.sendMessage(MessageUtil.error("设置职位失败！"));
                    }
                });
            }
        }
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回成员管理",
            "§7点击返回"
        ), p -> new MemberManageGUI(plugin, p, nation).open());
    }
} 