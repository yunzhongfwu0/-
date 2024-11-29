package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.models.NationMember;
import com.nations.core.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MemberListGUI extends BaseGUI {
    private final Nation nation;
    
    public MemberListGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, MessageUtil.title("成员列表 - " + nation.getName()), 6);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示国家所有者
        ItemStack ownerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta ownerMeta = (SkullMeta) ownerHead.getItemMeta();
        ownerMeta.setOwningPlayer(Bukkit.getOfflinePlayer(nation.getOwnerUUID()));
        ownerMeta.setDisplayName(MessageUtil.title("国家领袖"));
        ownerMeta.setLore(MessageUtil.createStatusLore("领袖信息",
            "玩家: " + Bukkit.getOfflinePlayer(nation.getOwnerUUID()).getName(),
            "职位: 国主"
        ));
        ownerHead.setItemMeta(ownerMeta);
        setItem(4, ownerHead, null);
        
        // 显示成员列表
        int slot = 19;
        for (Map.Entry<UUID, NationMember> entry : nation.getMembers().entrySet()) {
            UUID memberUUID = entry.getKey();
            NationMember member = entry.getValue();
            
            ItemStack memberHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta memberMeta = (SkullMeta) memberHead.getItemMeta();
            memberMeta.setOwningPlayer(Bukkit.getOfflinePlayer(memberUUID));
            memberMeta.setDisplayName("§f" + Bukkit.getOfflinePlayer(memberUUID).getName());
            
            List<String> memberLore = new ArrayList<>();
            memberLore.addAll(MessageUtil.createStatusLore("成员信息",
                "职位: " + member.getRank().getDisplayName(),
                "加入时间: " + member.getFormattedJoinDate()
            ));
            
            if (nation.hasPermission(player.getUniqueId(), "nation.promote")) {
                memberLore.add("");
                memberLore.addAll(MessageUtil.createActionLore("可用操作",
                    "左键 - 提升职位",
                    "右键 - 踢出成员"
                ));
            }
            
            memberMeta.setLore(memberLore);
            memberHead.setItemMeta(memberMeta);
            
            setItem(slot++, memberHead, 
                // 左键 - 提升职位
                p -> {
                    if (nation.hasPermission(p.getUniqueId(), "nation.promote")) {
                        new RankSelectGUI(plugin, p, nation, memberUUID).open();
                    }
                },
                // 右键 - 踢出成员
                p -> {
                    if (plugin.getNationManager().removeMember(nation, memberUUID)) {
                        p.sendMessage(MessageUtil.success("成功将玩家踢出国家！"));
                        
                        // 通知被踢出的玩家
                        Player kickedPlayer = plugin.getServer().getPlayer(memberUUID);
                        if (kickedPlayer != null) {
                            kickedPlayer.sendMessage(MessageUtil.error("你已被踢出国家 " + nation.getName()));
                        }
                        
                        // 通知其他在线成员
                        for (UUID memberId : nation.getMembers().keySet()) {
                            if (!memberId.equals(p.getUniqueId())) {
                                Player othermember = plugin.getServer().getPlayer(memberId);
                                if (othermember != null) {
                                    othermember.sendMessage(MessageUtil.broadcast(
                                        "玩家 " + Bukkit.getOfflinePlayer(memberUUID).getName() + 
                                        " 已被 " + p.getName() + " 踢出国家"
                                    ));
                                }
                            }
                        }
                        
                        initialize();
                    } else {
                        p.sendMessage(MessageUtil.error("踢出玩家失败！"));
                    }
                }
            );
        }
        
        // 返回按钮
        setItem(49, createItem(Material.ARROW,
            MessageUtil.title("返回"),
            MessageUtil.subtitle("点击返回主菜单")
        ), p -> new MainGUI(plugin, p).open());
    }
} 