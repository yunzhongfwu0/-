package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NationManageGUI extends BaseGUI {
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public NationManageGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6国家管理", 6);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        Collection<Nation> nations = plugin.getNationManager().getAllNations();
        List<Nation> nationList = new ArrayList<>(nations);
        
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, nationList.size());
        
        // 显示国家列表
        for (int i = 0; i < endIndex - startIndex; i++) {
            Nation nation = nationList.get(startIndex + i);
            setItem(10 + i + (i/7)*2, createItem(Material.BEACON,
                "§6" + nation.getName(),
                "§7等级: §f" + nation.getLevel(),
                "§7余额: §f" + nation.getBalance(),
                "§7成员: §f" + nation.getCurrentMembers() + "/" + nation.getMaxMembers(),
                "",
                "§e左键 §7- 管理国家",
                "§c右键 §7- 删除国家"
            ), p -> new NationDetailGUI(plugin, p, nation).open());
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
        
        if (endIndex < nationList.size()) {
            setItem(53, createItem(Material.ARROW,
                "§f下一页",
                "§7点击查看下一页"
            ), p -> {
                page++;
                initialize();
            });
        }
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
} 