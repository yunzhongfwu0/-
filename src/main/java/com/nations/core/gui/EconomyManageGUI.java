package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EconomyManageGUI extends BaseGUI {
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public EconomyManageGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6经济管理", 6);
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
            setItem(10 + i + (i/7)*2, createItem(Material.GOLD_INGOT,
                "§6" + nation.getName(),
                "§7余额: §f" + nation.getBalance(),
                "",
                "§e左键 §7- 设置余额",
                "§e右键 §7- 查看交易记录"
            ), p -> {
                p.closeInventory();
                p.sendMessage("§a请输入新的余额，或输入 'cancel' 取消");
                ChatInputManager.awaitChatInput(p, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage("§c已取消操作。");
                        new EconomyManageGUI(plugin, p).open();
                        return;
                    }
                    try {
                        double amount = Double.parseDouble(input);
                        if (amount < 0) {
                            p.sendMessage("§c余额不能为负数！");
                            return;
                        }
                        if (plugin.getNationManager().setBalance(nation, amount)) {
                            p.sendMessage("§a成功设置国家余额为: " + amount);
                            new EconomyManageGUI(plugin, p).open();
                        } else {
                            p.sendMessage("§c设置余额失败！");
                        }
                    } catch (NumberFormatException e) {
                        p.sendMessage("§c无效的金额！");
                    }
                });
            });
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
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            "§f返回管理面板",
            "§7点击返回"
        ), p -> new AdminGUI(plugin, p).open());
    }
}