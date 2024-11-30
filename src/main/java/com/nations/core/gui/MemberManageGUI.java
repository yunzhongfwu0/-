package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.gui.player.RequestManageGUI;
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

public class MemberManageGUI extends BaseGUI {
    private final Nation nation;
    
    public MemberManageGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, MessageUtil.title("成员管理 - " + nation.getName()), 6);
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
                    if (!nation.hasPermission(p.getUniqueId(), "nation.promote")) {
                        p.sendMessage(MessageUtil.error("你没有权限踢出成员！"));
                        return;
                    }
                    
                    ConfirmGUI.open(plugin, p,
                        "确认踢出成员",
                        "确认踢出",
                        new String[]{
                            "点击确认踢出成员",
                            "",
                            "§c警告:",
                            "- 该成员将失去所有职位",
                            "- 需要重新申请才能加入"
                        },
                        confirmPlayer -> {
                            if (plugin.getNationManager().removeMember(nation, memberUUID)) {
                                confirmPlayer.sendMessage(MessageUtil.success("成功将玩家踢出国家！"));
                                
                                // 通知被踢出的玩家
                                Player kickedPlayer = plugin.getServer().getPlayer(memberUUID);
                                if (kickedPlayer != null) {
                                    kickedPlayer.sendMessage(MessageUtil.error("你已被踢出国家 " + nation.getName()));
                                }
                                
                                // 通知其他在线成员
                                for (UUID memberId : nation.getMembers().keySet()) {
                                    if (!memberId.equals(confirmPlayer.getUniqueId())) {
                                        Player onlineMember = plugin.getServer().getPlayer(memberId);
                                        if (onlineMember != null) {
                                            onlineMember.sendMessage(MessageUtil.broadcast(
                                                "玩家 " + Bukkit.getOfflinePlayer(memberUUID).getName() + 
                                                " 已被 " + confirmPlayer.getName() + " 踢出国家"
                                            ));
                                        }
                                    }
                                }
                                
                                initialize();
                            } else {
                                confirmPlayer.sendMessage(MessageUtil.error("踢出玩家失败！"));
                            }
                        },
                        cancelPlayer -> new MemberManageGUI(plugin, cancelPlayer, nation).open()
                    );
                }
            );
        }
        
        // 邀请新成员
        if (nation.hasPermission(player.getUniqueId(), "nation.invite")) {
            setItem(48, createItem(Material.EMERALD,
                MessageUtil.title("邀请新成员"),
                MessageUtil.subtitle("点击邀请新成员")
            ), p -> new InviteGUI(plugin, p, nation).open());
        }
        
        // 查看申请
        if (nation.hasPermission(player.getUniqueId(), "nation.manage.requests")) {
            int requestCount = plugin.getNationManager().getJoinRequests(nation).size();
            setItem(50, createItem(Material.BOOK,
                MessageUtil.title("查看申请"),
                MessageUtil.createStatusLore("申请管理",
                    requestCount > 0 ? "当前有 " + requestCount + " 个待处理申请" : "暂无待处理申请",
                    "",
                    "点击查看详情"
                ).toArray(new String[0])
            ), p -> new RequestManageGUI(plugin, p, nation).open());
        }
        
        // 返回按钮
        setItem(49, createItem(Material.ARROW,
            MessageUtil.title("返回"),
            MessageUtil.subtitle("点击返回主菜单")
        ), p -> new MainGUI(plugin, p).open());
    }
} 