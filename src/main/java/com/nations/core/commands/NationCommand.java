package com.nations.core.commands;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public class NationCommand implements CommandExecutor {
    
    private final NationsCore plugin;
    
    public NationCommand(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行！");
            return true;
        }
        
        if (args.length == 0) {
            showInfo(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "info" -> handleInfo(player, args);
            case "delete" -> handleDelete(player);
            case "rename" -> handleRename(player, args);
            case "setspawn" -> handleSetSpawn(player);
            case "spawn" -> handleSpawn(player, args);
            case "pay" -> handlePay(player, args);
            case "balance" -> handleBalance(player);
            case "help" -> showHelp(player);
            case "list" -> handleList(player);
            default -> {
                player.sendMessage("§c未知命令！使用 /nation help 查看帮助");
                return false;
            }
        }
        
        return true;
    }
    
    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation create <国家名称>");
            return;
        }
        
        String nationName = args[1];
        
        // 检查玩家是否已有国家
        if (plugin.getNationManager().getNationByPlayer(player).isPresent()) {
            player.sendMessage("§c你已经拥有一个国家了！");
            return;
        }
        
        // 检查名称长度
        int minLength = plugin.getConfig().getInt("nations.min-name-length", 2);
        int maxLength = plugin.getConfig().getInt("nations.max-name-length", 16);
        if (nationName.length() < minLength || nationName.length() > maxLength) {
            player.sendMessage("§c国家名称长度必须在 " + minLength + " 到 " + maxLength + " 个字符之间！");
            return;
        }
        
        // 检查名称格式
        String nameRegex = plugin.getConfig().getString("nations.name-regex", "^[\u4e00-\u9fa5a-zA-Z0-9_]+$");
        if (!nationName.matches(nameRegex)) {
            player.sendMessage("§c国家名称只能包含中文、字母、数字和下划线！");
            return;
        }
        
        // 创建国家
        if (plugin.getNationManager().createNation(player, nationName)) {
            player.sendMessage("§a成功创建国家 " + nationName + "！");
        } else {
            player.sendMessage("§c创建国家失败！该名称可能已被使用。");
        }
    }
    
    private void handleInfo(Player player, String[] args) {
        Optional<Nation> nation;
        if (args.length > 1) {
            nation = plugin.getNationManager().getNationByName(args[1]);
        } else {
            nation = plugin.getNationManager().getNationByPlayer(player);
        }
        
        if (nation.isEmpty()) {
            player.sendMessage("§c未找到指定的国家！");
            return;
        }
        
        Nation n = nation.get();
        player.sendMessage("§6========== 国家信息 ==========");
        player.sendMessage("§e国家名称: §f" + n.getName());
        player.sendMessage("§e国家等级: §f" + n.getLevel());
        player.sendMessage("§e国库余额: §f" + n.getBalance());
        player.sendMessage("§e创建者: §f" + plugin.getServer().getOfflinePlayer(n.getOwnerUUID()).getName());
        player.sendMessage("§e服务器: §f" + (n.isLocalServer() ? "本服" : "子服-" + n.getServerId()));
        if (n.getSpawnPoint() != null && n.isLocalServer()) {
            player.sendMessage("§e传送点: §f已设置");
        }
        player.sendMessage("§6===========================");
    }
    
    private void handleDelete(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        // 删除国家
        if (plugin.getNationManager().deleteNation(nation.get())) {
            player.sendMessage("§a成功删除国家！");
        } else {
            player.sendMessage("§c删除国家失败！");
        }
    }
    
    private void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation rename <新名称>");
            return;
        }
        
        Optional<Nation> nationOpt = plugin.getNationManager().getNationByPlayer(player);
        if (nationOpt.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        Nation nation = nationOpt.get();
        
        // 检查是否是国家所有者
        if (!player.getUniqueId().equals(nation.getOwnerUUID())) {
            player.sendMessage("§c只有国家领袖才能重命名国家！");
            return;
        }
        
        String newName = args[1];
        
        // 检查名称长度
        int minLength = plugin.getConfig().getInt("nations.min-name-length", 2);
        int maxLength = plugin.getConfig().getInt("nations.max-name-length", 16);
        if (newName.length() < minLength || newName.length() > maxLength) {
            player.sendMessage("§c国家名称长度必须在 " + minLength + " 到 " + maxLength + " 个字符之间！");
            return;
        }
        
        // 检查名称格式
        String nameRegex = plugin.getConfig().getString("nations.name-regex", "^[\u4e00-\u9fa5a-zA-Z0-9_]+$");
        if (!newName.matches(nameRegex)) {
            player.sendMessage("§c国家名称只能包含中文、字母、数字和下划线！");
            return;
        }
        
        // 检查费用
        double renameCost = plugin.getConfig().getDouble("nations.rename-cost", 5000.0);
        if (nation.getBalance() < renameCost) {
            player.sendMessage("§c国库余额不足！重命名需要 " + renameCost + " 金币，当前余额：" + nation.getBalance());
            return;
        }
        
        // 扣除费用并重命名
        if (plugin.getNationManager().renameNation(nation, newName, renameCost)) {
            player.sendMessage("§a成功将国家重命名为 " + newName + "！");
            player.sendMessage("§a花费: " + renameCost + " 金币");
            player.sendMessage("§a当前国库余额: " + nation.getBalance());
        } else {
            player.sendMessage("§c重命名失败！该名称可能已被使用。");
        }
    }
    
    private void handleSetSpawn(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!player.getUniqueId().equals(nation.get().getOwnerUUID())) {
            player.sendMessage("§c只有国家领袖才能设置传送点！");
            return;
        }
        
        if (plugin.getNationManager().setSpawnPoint(nation.get(), player.getLocation())) {
            player.sendMessage("§a成功设置国家传送点！");
        } else {
            player.sendMessage("§c设置传送点失败！");
        }
    }
    
    private void handleSpawn(Player player, String[] args) {
        Optional<Nation> nation;
        if (args.length > 1) {
            // 传送到指定国家
            nation = plugin.getNationManager().getNationByName(args[1]);
        } else {
            // 传送到自己的国家
            nation = plugin.getNationManager().getNationByPlayer(player);
        }
        
        if (nation.isEmpty()) {
            player.sendMessage("§c未找到指定的国家！");
            return;
        }
        
        plugin.getNationManager().teleportToNation(player, nation.get());
    }
    
    private void handlePay(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /nation pay <玩家名> <金额>");
            return;
        }
        
        Optional<Nation> senderNation = plugin.getNationManager().getNationByPlayer(player);
        if (senderNation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        Optional<Nation> targetNation = plugin.getNationManager().getNationByPlayer(target);
        if (targetNation.isEmpty()) {
            player.sendMessage("§c目标玩家没有国家！");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            if (amount <= 0) {
                player.sendMessage("§c转账金额必须大于0！");
                return;
            }
            
            if (senderNation.get().getBalance() < amount) {
                player.sendMessage("§c你的国家余额不足！");
                return;
            }
            
            if (plugin.getNationManager().transferMoney(senderNation.get(), targetNation.get(), amount)) {
                player.sendMessage("§a成功向 " + target.getName() + " 的国家转账 " + amount + " 金币！");
                player.sendMessage("§a当前余额: " + senderNation.get().getBalance());
                target.sendMessage("§a收到来自 " + player.getName() + " 的国家转账 " + amount + " 金币！");
                target.sendMessage("§a当前余额: " + targetNation.get().getBalance());
            } else {
                player.sendMessage("§c转账失败！");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的金额！");
        }
    }
    
    private void handleBalance(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        player.sendMessage("§a你的国家余额: " + nation.get().getBalance());
    }
    
    private void showInfo(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你还没有国家！使用 /nation create <名称> 创建一个国家。");
            return;
        }
        handleInfo(player, new String[]{"info"});
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6========== 国家系统帮助 ==========");
        player.sendMessage("§e/nation create <名称> §f- 创建一个新国家");
        player.sendMessage("§e/nation info [玩家名] §f- 查看国家信息");
        player.sendMessage("§e/nation delete §f- 删除你的国家");
        player.sendMessage("§e/nation rename <新名称> §f- 重命名你的国家");
        player.sendMessage("§e/nation setspawn §f- 设置国家传送点");
        player.sendMessage("§e/nation spawn [玩家名] §f- 传送到国家传送点");
        player.sendMessage("§e/nation pay <玩家名> <金额> §f- 向其他国家转账");
        player.sendMessage("§e/nation balance §f- 查看国家余额");
        player.sendMessage("§e/nation list §f- 查看国家列表");
        player.sendMessage("§6================================");
    }
    
    private void handleList(Player player) {
        Collection<Nation> localNations = plugin.getNationManager().getLocalNations();
        Collection<Nation> remoteNations = plugin.getNationManager().getRemoteNations();
        
        player.sendMessage("§6========== 国家列表 ==========");
        player.sendMessage("§e本服国家:");
        for (Nation n : localNations) {
            player.sendMessage("§f- " + n.getName() + " §7(等级:" + n.getLevel() + ")");
        }
        player.sendMessage("§e其他服国家:");
        for (Nation n : remoteNations) {
            player.sendMessage("§f- " + n.getName() + " §7(服务器:" + n.getServerId() + ")");
        }
        player.sendMessage("§6===========================");
    }
} 