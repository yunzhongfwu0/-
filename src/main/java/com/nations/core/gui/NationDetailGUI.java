package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class NationDetailGUI extends BaseGUI {
    private final Nation nation;
    
    public NationDetailGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6国家管理 - " + nation.getName(), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 基本信息
        setItem(4, createItem(Material.BEACON,
            "§6" + nation.getName(),
            "§7等级: §f" + nation.getLevel(),
            "§7余额: §f" + nation.getBalance(),
            "§7成员: §f" + nation.getMembers().size(),
            "§7创建者: §f" + Bukkit.getOfflinePlayer(nation.getOwnerUUID()).getName()
        ), null);
        
        // 删除国家
        setItem(10, createItem(Material.BARRIER,
            "§c删除国家",
            "§7点击删除此国家",
            "",
            "§c警告: 此操作不可撤销!"
        ), p -> {
            p.closeInventory();
            p.sendMessage("§c确认删除国家 " + nation.getName() + "？");
            p.sendMessage("§c输入 'confirm' 确认删除，或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("confirm")) {
                    if (plugin.getNationManager().deleteNation(nation)) {
                        p.sendMessage("§a成功删除国家 " + nation.getName());
                        new NationManageGUI(plugin, p).open();
                    } else {
                        p.sendMessage("§c删除国家失败！");
                    }
                } else {
                    p.sendMessage("§c已取消删除操作。");
                    new NationDetailGUI(plugin, p, nation).open();
                }
            });
        });
        
        // 修改等级
        setItem(12, createItem(Material.EXPERIENCE_BOTTLE,
            "§6修改等级",
            "§7当前等级: §f" + nation.getLevel(),
            "",
            "§7点击修改等级"
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请输入新的等级 (1-4)，或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消操作。");
                    new NationDetailGUI(plugin, p, nation).open();
                    return;
                }
                try {
                    int level = Integer.parseInt(input);
                    if (level < 1 || level > 4) {
                        p.sendMessage("§c等级必须在 1-4 之间！");
                        return;
                    }
                    nation.setLevel(level);
                    p.sendMessage("§a成功将国家等级设置为 " + level);
                    new NationDetailGUI(plugin, p, nation).open();
                } catch (NumberFormatException e) {
                    p.sendMessage("§c无效的等级！");
                }
            });
        });
        
        // 修改余额
        setItem(14, createItem(Material.GOLD_INGOT,
            "§6修改余额",
            "§7当前余额: §f" + nation.getBalance(),
            "",
            "§7点击修改余额"
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请输入新的余额，或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消操作。");
                    new NationDetailGUI(plugin, p, nation).open();
                    return;
                }
                try {
                    double balance = Double.parseDouble(input);
                    if (balance < 0) {
                        p.sendMessage("§c余额不能为负数！");
                        return;
                    }
                    if (plugin.getNationManager().setBalance(nation, balance)) {
                        p.sendMessage("§a成功设置国家余额为 " + balance);
                        new NationDetailGUI(plugin, p, nation).open();
                    } else {
                        p.sendMessage("§c设置余额失败！");
                    }
                } catch (NumberFormatException e) {
                    p.sendMessage("§c无效的金额！");
                }
            });
        });
        
        // 转让国家
        setItem(16, createItem(Material.NAME_TAG,
            "§6转让国家",
            "§7点击转让国家给其他玩家",
            "",
            "§7当前拥有者: §f" + Bukkit.getOfflinePlayer(nation.getOwnerUUID()).getName()
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请输入新拥有者的名字，或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消操作。");
                    new NationDetailGUI(plugin, p, nation).open();
                    return;
                }
                Player target = Bukkit.getPlayer(input);
                if (target == null) {
                    p.sendMessage("§c找不到指定的玩家！");
                    return;
                }
                if (plugin.getNationManager().transferNation(nation, target)) {
                    p.sendMessage("§a成功将国家转让给 " + target.getName());
                    new NationDetailGUI(plugin, p, nation).open();
                } else {
                    p.sendMessage("§c转让国家失败！");
                }
            });
        });
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回国家列表",
            "§7点击返回"
        ), p -> new NationManageGUI(plugin, p).open());
    }
} 