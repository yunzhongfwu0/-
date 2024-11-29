package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.gui.player.JoinNationGUI;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainGUI extends BaseGUI {
    
    public MainGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6国家系统 - 主菜单", 3);
        initialize();
    }
    
    public void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        Optional<Nation> nationOpt = plugin.getNationManager().getNationByPlayer(player);
        
        if (nationOpt.isEmpty()) {
            // 创建国家按钮
            setItem(11, createItem(Material.EMERALD,
                "§6创建国家",
                "§7点击创建一个新的国家",
                "",
                "§e说明:",
                "§7- 需要满足创建条件",
                "§7- 成为国家领袖",
                "§7- 拥有完整管理权限"
            ), p -> new CreateNationGUI(plugin, p).open());
            
            // 加入国家按钮
            setItem(15, createItem(Material.BOOK,
                "§6加入国家",
                "§7点击查看可加入的国家",
                "",
                "§e说明:",
                "§7- 浏览所有国家",
                "§7- 发送加入申请",
                "§7- 等待审核通过"
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
                    p.sendMessage("§c该国家还未设置传送点！");
                }
            });
        }
        
        // 帮助信息
        setItem(22, createItem(Material.BOOK,
            "§6帮助信息",
            "§7点击查看帮助"
        ), p -> new HelpGUI(plugin, p).open());
    }
} 