package com.nations.core.commands;

import com.nations.core.NationsCore;
import com.nations.core.gui.AdminGUI;
import com.nations.core.gui.CostSettingsGUI;
import com.nations.core.models.Nation;
import org.bukkit.Bukkit;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行！");
            return true;
        }
        
        if (!player.hasPermission("nations.admin")) {
            player.sendMessage("§c你没有权限执行此命令！");
            return true;
        }
        
        if (args.length == 0) {
            new AdminGUI(plugin, player).open();
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "gui" -> new AdminGUI(plugin, player).open();
            case "reload" -> {
                plugin.reloadConfig();
                player.sendMessage("§a配置文件已重新加载！");
            }
            case "setcreationcost" -> new CostSettingsGUI(plugin, player, true, 0).open();
            case "setupgradecost" -> {
                if (args.length < 2) {
                    player.sendMessage("§c用法: /nadmin setupgradecost <等级>");
                    return true;
                }
                try {
                    int level = Integer.parseInt(args[1]);
                    if (level < 2 || level > 4) {
                        player.sendMessage("§c等级必须在 2-4 之间！");
                        return true;
                    }
                    new CostSettingsGUI(plugin, player, false, level).open();
                } catch (NumberFormatException e) {
                    player.sendMessage("§c无效的等级！");
                }
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage("§c用法: /nadmin delete <玩家名>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§c找不到指定的玩家！");
                    return true;
                }
                Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
                if (nation.isEmpty()) {
                    player.sendMessage("§c该玩家没有国家！");
                    return true;
                }
                if (plugin.getNationManager().deleteNation(nation.get())) {
                    player.sendMessage("§a成功删除玩家 " + target.getName() + " 的国家！");
                } else {
                    player.sendMessage("§c删除国家失败！");
                }
            }
            case "transfer" -> {
                if (args.length < 3) {
                    player.sendMessage("§c用法: /nadmin transfer <当前玩家名> <新玩家名>");
                    return true;
                }
                Player from = Bukkit.getPlayer(args[1]);
                Player to = Bukkit.getPlayer(args[2]);
                if (from == null || to == null) {
                    player.sendMessage("§c找不到指定的玩家！");
                    return true;
                }
                Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(from);
                if (nation.isEmpty()) {
                    player.sendMessage("§c该玩家没有国家！");
                    return true;
                }
                if (plugin.getNationManager().transferNation(nation.get(), to)) {
                    player.sendMessage("§a成功将国家转让给 " + to.getName() + "！");
                } else {
                    player.sendMessage("§c转让国家失败！");
                }
            }
            case "setmoney" -> {
                if (args.length < 3) {
                    player.sendMessage("§c用法: /nadmin setmoney <玩家名> <金额>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§c找不到指定的玩家！");
                    return true;
                }
                Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
                if (nation.isEmpty()) {
                    player.sendMessage("§c该玩家没有国家！");
                    return true;
                }
                try {
                    double amount = Double.parseDouble(args[2]);
                    if (amount < 0) {
                        player.sendMessage("§c金额不能为负数！");
                        return true;
                    }
                    if (plugin.getNationManager().setBalance(nation.get(), amount)) {
                        player.sendMessage("§a成功设置国家余额为: " + amount);
                    } else {
                        player.sendMessage("§c设置余额失败！");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§c无效的金额！");
                }
            }
            case "forcejoin" -> {
                if (args.length < 3) {
                    player.sendMessage("§c用法: /nadmin forcejoin <玩家名> <国家名>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§c找不到指定的玩家！");
                    return true;
                }
                Optional<Nation> nation = plugin.getNationManager().getNationByName(args[2]);
                if (nation.isEmpty()) {
                    player.sendMessage("§c找不到指定的国家！");
                    return true;
                }
                if (plugin.getNationManager().addMember(nation.get(), target.getUniqueId(), "MEMBER")) {
                    player.sendMessage("§a成功将 " + target.getName() + " 添加到国家！");
                } else {
                    player.sendMessage("§c添加成员失败！");
                }
            }
            case "forcekick" -> {
                if (args.length < 2) {
                    player.sendMessage("§c用法: /nadmin forcekick <玩家名>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§c找不到指定的玩家！");
                    return true;
                }
                Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(target);
                if (nation.isEmpty()) {
                    player.sendMessage("§c该玩家没有国家！");
                    return true;
                }
                if (plugin.getNationManager().removeMember(nation.get(), target.getUniqueId())) {
                    player.sendMessage("§a成功将 " + target.getName() + " 踢出国家！");
                } else {
                    player.sendMessage("§c踢出成员失败！");
                }
            }
            case "help" -> showHelp(player);
            default -> {
                player.sendMessage("§c未知命令！使用 /nadmin help 查看帮助");
                return false;
            }
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6========== 国家管理命令 ==========");
        player.sendMessage("§e/nadmin §f- 打开管理面板");
        player.sendMessage("§e/nadmin gui §f- 打开管理面板");
        player.sendMessage("§e/nadmin reload §f- 重新加载配置");
        player.sendMessage("§e/nadmin setcreationcost §f- 设置创建国家费用");
        player.sendMessage("§e/nadmin setupgradecost <等级> §f- 设置升级费用");
        player.sendMessage("§e/nadmin delete <玩家名> §f- 删除玩家的国家");
        player.sendMessage("§e/nadmin transfer <当前玩家名> <新玩家名> §f- 转让国家");
        player.sendMessage("§e/nadmin setmoney <玩家名> <金额> §f- 设置国家余额");
        player.sendMessage("§e/nadmin forcejoin <玩家名> <国家名> §f- 强制玩家加入国家");
        player.sendMessage("§e/nadmin forcekick <玩家名> §f- 强制踢出玩家");
        player.sendMessage("§6================================");
    }
} 