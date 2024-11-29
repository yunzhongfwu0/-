package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class EconomyGUI extends BaseGUI {
    private final Nation nation;
    
    public EconomyGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6经济管理 - " + nation.getName(), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示当前余额
        setItem(13, createItem(Material.GOLD_BLOCK,
            "§6国库余额: §f" + nation.getBalance(),
            "§7当前国库余额"
        ), null);
        
        // 存款按钮
        setItem(11, createItem(Material.EMERALD,
            "§a存入金币",
            "§7点击向国库存入金币",
            "§7你的余额: §f" + plugin.getVaultEconomy().getBalance(player)
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请在聊天栏输入要存入的金额，或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消操作。");
                    return;
                }
                try {
                    double amount = Double.parseDouble(input);
                    if (amount <= 0) {
                        p.sendMessage("§c金额必须大于0！");
                        return;
                    }
                    if (plugin.getNationManager().deposit(nation, p, amount)) {
                        p.sendMessage("§a成功向国库存入 " + amount + " 金币！");
                        p.sendMessage("§a当前国库余额: " + nation.getBalance());
                    } else {
                        p.sendMessage("§c存入失败！请确保你有足够的金币。");
                    }
                } catch (NumberFormatException e) {
                    p.sendMessage("§c无效的金额！");
                }
            });
        });
        
        // 取款按钮（仅对有权限的玩家显示）
        if (nation.hasPermission(player.getUniqueId(), "nation.withdraw")) {
            setItem(15, createItem(Material.GOLD_INGOT,
                "§6取出金币",
                "§7点击从国库取出金币",
                "§7国库余额: §f" + nation.getBalance()
            ), p -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天栏输入要取出的金额，或输入 'cancel' 取消");
                ChatInputManager.awaitChatInput(p, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage("§c已取消操作。");
                        return;
                    }
                    try {
                        double amount = Double.parseDouble(input);
                        if (amount <= 0) {
                            p.sendMessage("§c金额必须大于0！");
                            return;
                        }
                        if (plugin.getNationManager().withdraw(nation, p, amount)) {
                            p.sendMessage("§a成功从国库取出 " + amount + " 金币！");
                            p.sendMessage("§a当前国库余额: " + nation.getBalance());
                        } else {
                            p.sendMessage("§c取出失败！请确保国库有足够的金币。");
                        }
                    } catch (NumberFormatException e) {
                        p.sendMessage("§c无效的金额！");
                    }
                });
            });
        }
        
        // 交易记录按钮
        setItem(22, createItem(Material.BOOK,
            "§6交易记录",
            "§7点击查看交易记录"
        ), p -> new TransactionLogGUI(plugin, p, nation).open());
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
} 