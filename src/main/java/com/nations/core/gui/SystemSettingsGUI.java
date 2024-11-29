package com.nations.core.gui;

import com.nations.core.NationsCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SystemSettingsGUI extends BaseGUI {
    
    public SystemSettingsGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6系统设置", 3);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 重载配置
        setItem(11, createItem(Material.REDSTONE,
            "§6重载配置",
            "§7点击重新加载配置文件"
        ), p -> {
            plugin.reloadConfig();
            p.sendMessage("§a配置文件已重新加载！");
        });
        
        // 性能监控
        setItem(13, createItem(Material.CLOCK,
            "§6性能监控",
            "§7点击查看性能数据",
            "",
            "§e显示:",
            "§7- TPS",
            "§7- 内存使用",
            "§7- 缓存状态"
        ), p -> {
            // TODO: 显示性能数据
        });
        
        // 数据备份
        setItem(15, createItem(Material.BOOK,
            "§6数据备份",
            "§7点击备份数据",
            "",
            "§e包括:",
            "§7- 国家数据",
            "§7- 领土数据",
            "§7- 交易记录"
        ), p -> {
            // TODO: 执行数据备份
        });
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
} 