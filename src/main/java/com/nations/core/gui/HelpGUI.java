package com.nations.core.gui;

import com.nations.core.NationsCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class HelpGUI extends BaseGUI {
    
    public HelpGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6帮助信息", 4);
        initialize();
    }
    
    private void initialize() {
        super.fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 基础命令帮助
        setItem(10, createItem(Material.BOOK,
            "§6基础命令",
            "§f/nation create <名称> §7- 创建国家",
            "§f/nation info [玩家名] §7- 查看国家信息",
            "§f/nation delete §7- 删除国家",
            "§f/nation rename <新名称> §7- 重命名国家"
        ), null);
        
        // 返回按钮
        setItem(31, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
} 