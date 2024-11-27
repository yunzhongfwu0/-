package com.nations.core.commands;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NationAdminCommand implements CommandExecutor {
    
    private final NationsCore plugin;
    
    public NationAdminCommand(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("nations.admin")) {
            sender.sendMessage("§c你没有权限使用此命令！");
            return true;
        }
        
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "setmoney" -> handleSetMoney(sender, args);
            case "addmoney" -> handleAddMoney(sender, args);
            case "takemoney" -> handleTakeMoney(sender, args);
            case "delete" -> handleForceDelete(sender, args);
            case "transfer" -> handleForceTransfer(sender, args);
            case "info" -> handleDetailedInfo(sender, args);
            case "help" -> showAdminHelp(sender);
            default -> {
                sender.sendMessage("§c未知命令！使用 /nadmin help 查看帮助");
                return false;
            }
        }
        
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().loadConfigs();
        sender.sendMessage("§a配置文件已重新加载！");
    }
    
    private void handleSetMoney(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /nadmin setmoney <玩家名> <金额>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
        if (nation.isEmpty()) {
            sender.sendMessage("§c该玩家没有国家！");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount < 0) {
                sender.sendMessage("§c金额不能为负数！");
                return;
            }
            
            if (plugin.getNationManager().setBalance(nation.get(), amount)) {
                sender.sendMessage("§a已将玩家 " + target.getName() + " 的国家余额设置为 " + amount);
                target.sendMessage("§a你的国家余额已被管理员设置为 " + amount);
            } else {
                sender.sendMessage("§c设置余额失败！");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的金额！");
        }
    }
    
    private void handleAddMoney(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /nadmin addmoney <玩家名> <金额>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
        if (nation.isEmpty()) {
            sender.sendMessage("§c该玩家没有国家！");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                sender.sendMessage("§c金额必须大于0！");
                return;
            }
            
            if (plugin.getNationManager().addBalance(nation.get(), amount)) {
                sender.sendMessage("§a已向玩家 " + target.getName() + " 的国家余额添加 " + amount + " 金额");
                target.sendMessage("§a你的国家余额已被管理员添加 " + amount + " 金额");
            } else {
                sender.sendMessage("§c添加金额失败！");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的金额！");
        }
    }
    
    private void handleTakeMoney(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /nadmin takemoney <玩家名> <金额>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
        if (nation.isEmpty()) {
            sender.sendMessage("§c该玩家没有国家！");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                sender.sendMessage("§c金额必须大于0！");
                return;
            }
            
            if (plugin.getNationManager().takeBalance(nation.get(), amount)) {
                sender.sendMessage("§a已从玩家 " + target.getName() + " 的国家余额扣除 " + amount + " 金额");
                target.sendMessage("§a你的国家余额已被管理员扣除 " + amount + " 金额");
            } else {
                sender.sendMessage("§c扣除金额失败！余额不足或发生错误。");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的金额！");
        }
    }
    
    private void handleForceDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /nadmin delete <玩家名>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
        if (nation.isEmpty()) {
            sender.sendMessage("§c该玩家没有国家！");
            return;
        }
        
        if (plugin.getNationManager().deleteNation(nation.get())) {
            sender.sendMessage("§a已强制删除玩家 " + target.getName() + " 的国家");
        } else {
            sender.sendMessage("§c删除国家失败！");
        }
    }
    
    private void handleForceTransfer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /nadmin transfer <当前玩家名> <新玩家名>");
            return;
        }
        
        Player currentPlayer = Bukkit.getPlayer(args[1]);
        if (currentPlayer == null) {
            sender.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Player newPlayer = Bukkit.getPlayer(args[2]);
        if (newPlayer == null) {
            sender.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(currentPlayer);
        if (nation.isEmpty()) {
            sender.sendMessage("§c该玩家没有国家！");
            return;
        }
        
        if (plugin.getNationManager().transferOwnership(nation.get(), newPlayer)) {
            sender.sendMessage("§a已将玩家 " + currentPlayer.getName() + " 的国家转让给 " + newPlayer.getName());
        } else {
            sender.sendMessage("§c转让国家失败！");
        }
    }
    
    private void handleDetailedInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /nadmin info <玩家名>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
        if (nation.isEmpty()) {
            sender.sendMessage("§c该玩家没有国家！");
            return;
        }
        
        Nation n = nation.get();
        sender.sendMessage("§6========== 国家详细信息 ==========");
        sender.sendMessage("§e国家ID: §f" + n.getId());
        sender.sendMessage("§e国家名称: §f" + n.getName());
        sender.sendMessage("§e创建者UUID: §f" + n.getOwnerUUID());
        sender.sendMessage("§e创建者名称: §f" + Bukkit.getOfflinePlayer(n.getOwnerUUID()).getName());
        sender.sendMessage("§e国等级: §f" + n.getLevel());
        sender.sendMessage("§e国库余额: §f" + n.getBalance());
        if (n.getSpawnPoint() != null) {
            Location spawn = n.getSpawnPoint();
            sender.sendMessage("§e传送点: §f" + String.format("%.2f, %.2f, %.2f (%s)", 
                spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getWorld().getName()));
        }
        sender.sendMessage("§6================================");
    }
    
    private void showAdminHelp(CommandSender sender) {
        sender.sendMessage("§6========== 国家管理系统帮助 ==========");
        sender.sendMessage("§e/nadmin reload §f- 重新加载配置文件");
        sender.sendMessage("§e/nadmin setmoney <玩家名> <金额> §f- 设置玩家国家余额");
        sender.sendMessage("§e/nadmin addmoney <玩家名> <金额> §f- 增加玩家国家余额");
        sender.sendMessage("§e/nadmin takemoney <玩家名> <金额> §f- 扣除玩家国家余额");
        sender.sendMessage("§e/nadmin delete <玩家名> §f- 强制删除玩家的国家");
        sender.sendMessage("§e/nadmin transfer <当前玩家名> <新玩家名> §f- 强制转让国家");
        sender.sendMessage("§e/nadmin info <玩家名> §f- 查看玩家的国家详细信息");
        sender.sendMessage("§6===================================");
    }
} 