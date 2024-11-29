package com.nations.core.gui;

import com.nations.core.NationsCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AdminHelpGUI extends BaseGUI {
    
    public AdminHelpGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6管理员帮助", 3);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 基础命令
        setItem(11, createItem(Material.BOOK,
            "§6基础命令",
            "§7/nadmin - 打开管理面板",
            "§7/nadmin reload - 重载配置",
            "§7/nadmin help - 查看帮助"
        ), null);
        
        // 国家管理
        setItem(13, createItem(Material.BEACON,
            "§6国家管理",
            "§7/nadmin delete <国家> - 删除国家",
            "§7/nadmin transfer <国家> <玩家> - 转让国家",
            "§7/nadmin setlevel <国家> <等级> - 设置等级"
        ), null);
        
        // 经济管理
        setItem(15, createItem(Material.GOLD_INGOT,
            "§6经济管理",
            "§7/nadmin setmoney <国家> <金额> - 设置余额",
            "§7/nadmin addmoney <国家> <金额> - 增加余额",
            "§7/nadmin takemoney <国家> <金额> - 减少余额"
        ), null);
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
} 