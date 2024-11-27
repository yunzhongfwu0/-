package com.nations.core.commands;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
            case "help" -> showHelp(player);
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
            // 查看指定国家信息
            nation = plugin.getNationManager().getNationByName(args[1]);
        } else {
            // 查看自己的国家信息
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
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        String newName = args[1];
        if (plugin.getNationManager().renameNation(nation.get(), newName)) {
            player.sendMessage("§a成功将国家重命名为 " + newName + "！");
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
        player.sendMessage("§e/nation info [国家名] §f- 查看国家信息");
        player.sendMessage("§e/nation delete §f- 删除你的国家");
        player.sendMessage("§e/nation rename <新名称> §f- 重命名你的国家");
        player.sendMessage("§e/nation setspawn §f- 设置国家传送点");
        player.sendMessage("§e/nation spawn [国家名] §f- 传送到国家传送点");
        player.sendMessage("§6================================");
    }
} 