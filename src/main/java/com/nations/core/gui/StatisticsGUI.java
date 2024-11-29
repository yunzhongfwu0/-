package com.nations.core.gui;

import com.nations.core.NationsCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class StatisticsGUI extends BaseGUI {
    
    public StatisticsGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6数据统计", 3);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 国家统计
        setItem(11, createItem(Material.BEACON,
            "§6国家统计",
            "§7总国家数: §f" + plugin.getNationManager().getAllNations().size(),
            "§7在线国家: §f" + plugin.getNationManager().getOnlineNationsCount(),
            "§7总领土面积: §f" + plugin.getNationManager().getTotalTerritoryArea()
        ), null);
        
        // 经济统计
        setItem(13, createItem(Material.GOLD_INGOT,
            "§6经济统计",
            "§7总国库余额: §f" + plugin.getNationManager().getTotalBalance(),
            "§7今日交易额: §f" + plugin.getNationManager().getTodayTransactions(),
            "§7总交易次数: §f" + plugin.getNationManager().getTotalTransactions()
        ), null);
        
        // 玩家统计
        setItem(15, createItem(Material.PLAYER_HEAD,
            "§6玩家统计",
            "§7总玩家数: §f" + plugin.getNationManager().getTotalPlayers(),
            "§7在线玩家: §f" + plugin.getServer().getOnlinePlayers().size(),
            "§7无国家玩家: §f" + plugin.getNationManager().getPlayersWithoutNation()
        ), null);
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
}