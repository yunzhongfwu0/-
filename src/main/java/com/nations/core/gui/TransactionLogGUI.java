package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.models.Transaction;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.List;

public class TransactionLogGUI extends BaseGUI {
    private final Nation nation;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 28;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    public TransactionLogGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6交易记录 - " + nation.getName(), 6);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        List<Transaction> transactions = plugin.getNationManager().getTransactions(nation, currentPage, ITEMS_PER_PAGE);
        
        // 显示交易记录
        int slot = 10;
        for (Transaction trans : transactions) {
            Material material;
            String prefix;
            switch (trans.getType()) {
                case DEPOSIT -> {
                    material = Material.EMERALD;
                    prefix = "§a+";
                }
                case WITHDRAW -> {
                    material = Material.GOLD_INGOT;
                    prefix = "§c-";
                }
                case TRANSFER_IN -> {
                    material = Material.EMERALD_BLOCK;
                    prefix = "§a+";
                }
                case TRANSFER_OUT -> {
                    material = Material.GOLD_BLOCK;
                    prefix = "§c-";
                }
                default -> {
                    material = Material.PAPER;
                    prefix = "§7";
                }
            }
            
            setItem(slot, createItem(material,
                prefix + trans.getAmount() + " 金币",
                "§7类型: §f" + trans.getType().getDisplayName(),
                "§7操作者: §f" + plugin.getServer().getOfflinePlayer(trans.getPlayerUuid()).getName(),
                "§7时间: §f" + DATE_FORMAT.format(trans.getDate()),
                "§7说明: §f" + trans.getDescription()
            ), null);
            
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        
        // 上一页按钮
        if (currentPage > 0) {
            setItem(45, createItem(Material.ARROW,
                "§f上一页",
                "§7点击查看上一页"
            ), p -> {
                currentPage--;
                initialize();
            });
        }
        
        // 下一页按钮
        if (transactions.size() >= ITEMS_PER_PAGE) {
            setItem(53, createItem(Material.ARROW,
                "§f下一页",
                "§7点击查看下一页"
            ), p -> {
                currentPage++;
                initialize();
            });
        }
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            "§f返回",
            "§7点击返回"
        ), p -> new EconomyGUI(plugin, p, nation).open());
    }
} 