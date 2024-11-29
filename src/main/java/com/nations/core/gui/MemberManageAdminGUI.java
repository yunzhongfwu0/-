package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MemberManageAdminGUI extends BaseGUI {
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public MemberManageAdminGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6成员管理", 6);
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
            setItem(10 + i + (i/7)*2, createItem(Material.PLAYER_HEAD,
                "§6" + nation.getName(),
                "§7成员数: §f" + nation.getMembers().size(),
                "§7领袖: §f" + Bukkit.getOfflinePlayer(nation.getOwnerUUID()).getName(),
                "",
                "§e点击管理成员"
            ), p -> new MemberListGUI(plugin, p, nation).open());
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
        
        // 强制加入成员
        setItem(48, createItem(Material.EMERALD,
            "§a强制加入成员",
            "§7点击强制玩家加入国家"
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请按以下格式输入:");
            p.sendMessage("§e<玩家名> <国家名>");
            p.sendMessage("§7或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消操作。");
                    new MemberManageAdminGUI(plugin, p).open();
                    return;
                }
                String[] args = input.split(" ");
                if (args.length != 2) {
                    p.sendMessage("§c格式错误！");
                    return;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    p.sendMessage("§c找不到指定的玩家！");
                    return;
                }
                Nation nation = plugin.getNationManager().getNationByName(args[1]).orElse(null);
                if (nation == null) {
                    p.sendMessage("§c找不到指定的国家！");
                    return;
                }
                if (plugin.getNationManager().addMember(nation, target.getUniqueId(), "MEMBER")) {
                    p.sendMessage("§a成功将 " + target.getName() + " 加入国家 " + nation.getName());
                } else {
                    p.sendMessage("§c操作失败！");
                }
            });
        });
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
} 