package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.models.NationRank;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RankSelectGUI extends BaseGUI {
    private final Nation nation;
    private final UUID targetUUID;
    
    public RankSelectGUI(NationsCore plugin, Player player, Nation nation, UUID targetUUID) {
        super(plugin, player, MessageUtil.title("选择职位"), 3);
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
                // 先构建权限列表
                List<String> permissionList = new ArrayList<>();
                permissionList.add("点击设置为此职位");
                permissionList.add("");
                permissionList.add("权限:");
                rank.getPermissions().forEach(perm -> permissionList.add("- " + perm));
                
                setItem(slot++, createItem(Material.NAME_TAG,
                    MessageUtil.title(rank.getDisplayName()),
                    MessageUtil.createActionLore("职位信息",
                        permissionList.toArray(new String[0])
                    ).toArray(new String[0])
                ), p -> {
                    if (plugin.getNationManager().setMemberRank(nation, targetUUID, rank)) {
                        p.sendMessage(MessageUtil.success("成功设置玩家职位为: " + rank.getDisplayName()));
                        
                        // 通知被设置的玩家
                        Player target = plugin.getServer().getPlayer(targetUUID);
                        if (target != null) {
                            target.sendMessage(MessageUtil.success("你的职位已被设置为: " + rank.getDisplayName()));
                        }
                        
                        // 通知其他在线成员
                        for (UUID memberId : nation.getMembers().keySet()) {
                            if (!memberId.equals(p.getUniqueId()) && !memberId.equals(targetUUID)) {
                                Player member = plugin.getServer().getPlayer(memberId);
                                if (member != null) {
                                    member.sendMessage(MessageUtil.broadcast(
                                        "玩家 " + plugin.getServer().getOfflinePlayer(targetUUID).getName() + 
                                        " 的职位已被 " + p.getName() + " 设置为 " + rank.getDisplayName()
                                    ));
                                }
                            }
                        }
                        
                        new MemberManageGUI(plugin, p, nation).open();
                    } else {
                        p.sendMessage(MessageUtil.error("设置职位失败！"));
                    }
                });
            }
        }
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            MessageUtil.title("返回"),
            MessageUtil.subtitle("点击返回成员管理")
        ), p -> new MemberManageGUI(plugin, p, nation).open());
    }
} 