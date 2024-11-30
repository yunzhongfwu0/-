package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.gui.player.JoinNationGUI;
import com.nations.core.models.Nation;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MainGUI extends BaseGUI {
    
    public MainGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6国家系统 - 主菜单", 4);
        initialize();
    }
    
    public void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        Optional<Nation> nationOpt = plugin.getNationManager().getNationByPlayer(player);
        
        if (nationOpt.isEmpty()) {
            // 创建国家按钮
            setItem(11, createItem(Material.EMERALD,
                MessageUtil.title("创建国家"),
                MessageUtil.createLore("创建说明",
                    "点击创建一个新的国家",
                    "",
                    "要求:",
                    "- 需要满足创建条件",
                    "- 成为国家领袖",
                    "- 拥有完整管理权限"
                ).toArray(new String[0])
            ), p -> new CreateNationGUI(plugin, p).open());
            
            // 加入国家按钮
            setItem(15, createItem(Material.BOOK,
                MessageUtil.title("加入国家"),
                MessageUtil.createLore("加入说明",
                    "点击查看可加入的国家",
                    "",
                    "说明:",
                    "- 浏览所有国家",
                    "- 发送加入申请",
                    "- 等待审核通过"
                ).toArray(new String[0])
            ), p -> new JoinNationGUI(plugin, p).open());
            
            return;
        }
        
        Nation nation = nationOpt.get();
        boolean isOwner = player.getUniqueId().equals(nation.getOwnerUUID());
        
        // 国家信息
        setItem(13, createItem(Material.BEACON,
            "§6" + nation.getName(),
            "§e等级: §f" + nation.getLevel(),
            "§e国库: §f" + nation.getBalance(),
            "§e点击查看详细信息"
        ), p -> new NationInfoGUI(plugin, p, nation).open());
        
        // 成员管理
        if (nation.hasPermission(player.getUniqueId(), "nation.invite")) {
            setItem(11, createItem(Material.PLAYER_HEAD,
                "§6成员管理",
                "§7点击管理国家成员"
            ), p -> new MemberManageGUI(plugin, p, nation).open());
        }
        
        // 领土管理
        if (nation.hasPermission(player.getUniqueId(), "nation.territory")) {
            setItem(12, createItem(Material.MAP,
                "§6领土管理",
                "§7点击管理国家领土"
            ), p -> new TerritoryGUI(plugin, p, nation).open());
        }
        
        // 经济管理
        setItem(14, createItem(Material.GOLD_INGOT,
            "§6经济管理",
            "§7点击管理国家经济"
        ), p -> new EconomyGUI(plugin, p, nation).open());
        
        // 国家设置
        if (isOwner) {
            setItem(15, createItem(Material.REDSTONE_TORCH,
                "§6国家设置",
                "§7点击修改国家设置"
            ), p -> new SettingsGUI(plugin, p, nation).open());
        }
        
        // 升级按钮
        if (nation.hasPermission(player.getUniqueId(), "nation.upgrade")) {
            setItem(21, createItem(Material.EXPERIENCE_BOTTLE,
                "§6升级国家",
                "§7当前等级: §f" + nation.getLevel(),
                "§7点击查看升级信息"
            ), p -> new UpgradeGUI(plugin, p, nation).open());
        }
        
        // 传送点
        if (nation.hasPermission(player.getUniqueId(), "nation.spawn")) {
            setItem(23, createItem(Material.ENDER_PEARL,
                "§6国家传送",
                nation.getSpawnPoint() != null ? "§a已设置传送点" : "§c未设置传送点",
                "§7点击传送到国家传送点"
            ), p -> {
                if (nation.getSpawnPoint() != null) {
                    p.closeInventory();
                    plugin.getNationManager().teleportToNation(p, nation);
                } else {
                    p.sendMessage(MessageUtil.error("该国家还未设置传送点！"));
                }
            });
        }
        
        // 帮助信息
        setItem(22, createItem(Material.BOOK,
            "§6帮助信息",
            "§7点击查看帮助"
        ), p -> new HelpGUI(plugin, p).open());
        
        // 退出按钮
        if (!player.getUniqueId().equals(nation.getOwnerUUID())) {
            setItem(31, createItem(Material.BARRIER,
                MessageUtil.title("退出国家"),
                MessageUtil.createLore("退出说明",
                    "点击退出当前国家",
                    "",
                    "§c警告:",
                    "- 退出后将失去所有职位",
                    "- 需要重新申请才能加入"
                ).toArray(new String[0])
            ), p -> ConfirmGUI.open(plugin, p,
                "确认退出国家",
                "确认退出",
                new String[]{
                    "点击确认退出国家",
                    "",
                    "§c警告:",
                    "- 退出后将失去所有职位",
                    "- 需要重新申请才能加入"
                },
                confirmPlayer -> {
                    if (plugin.getNationManager().removeMember(nation, confirmPlayer.getUniqueId())) {
                        confirmPlayer.sendMessage(MessageUtil.success("你已退出国家 " + nation.getName()));
                        
                        
                        // 通知其他在线成员
                        for (UUID memberId : nation.getMembers().keySet()) {
                            Player member = plugin.getServer().getPlayer(memberId);
                            if (member != null && !member.getUniqueId().equals(confirmPlayer.getUniqueId())) {
                                member.sendMessage(MessageUtil.broadcast(
                                    "玩家 " + confirmPlayer.getName() + " 退出了国家"
                                ));
                            }
                        }
                        
                        // 额外通知国主(如果不在线)
                        Player owner = plugin.getServer().getPlayer(nation.getOwnerUUID());
                        if (owner != null && !owner.getUniqueId().equals(confirmPlayer.getUniqueId()) 
                            && !nation.getMembers().containsKey(owner.getUniqueId())) {
                            owner.sendMessage(MessageUtil.broadcast(
                                "玩家 " + confirmPlayer.getName() + " 退出了国家"
                            ));
                        }
                        
                        new MainGUI(plugin, confirmPlayer).open();
                    } else {
                        confirmPlayer.sendMessage(MessageUtil.error("退出国家失败！"));
                    }
                },
                cancelPlayer -> new MainGUI(plugin, cancelPlayer).open()
            ));
        }
    }
} 