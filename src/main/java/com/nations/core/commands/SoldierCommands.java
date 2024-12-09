package com.nations.core.commands;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.nations.core.training.TrainingSystem;
import com.nations.core.gui.SoldierManageGUI;
import java.util.Optional;

public class SoldierCommands implements CommandExecutor {
    private final NationsCore plugin;
    
    public SoldierCommands(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "recruit" -> handleRecruit(player, args);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "train" -> handleTrain(player, args);
            case "dismiss" -> handleDismiss(player, args);
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== 士兵系统命令帮助 ===");
        player.sendMessage("§f/soldier recruit <类型> <名称> §7- 招募士兵");
        player.sendMessage("§f/soldier list §7- 管理士兵");
        player.sendMessage("§f/soldier info <ID> §7- 查看士兵详情");
        player.sendMessage("§f/soldier train <ID> §7- 训练士兵");
        player.sendMessage("§f/soldier dismiss <ID> §7- 解雇士兵");
    }
    
    private void handleRecruit(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c用法: /soldier recruit <类型> <名称>");
            return;
        }
        
        try {
            SoldierType type = SoldierType.valueOf(args[1].toUpperCase());
            String name = args[2];
            
            // 检查玩家是否在兵营附近
            Building barracks = plugin.getBuildingManager().getNearestBuilding(
                player.getLocation(), BuildingType.BARRACKS, 5
            );
            
            if (barracks == null) {
                player.sendMessage("§c你必须在兵营附近才能招募士兵！");
                return;
            }
            
            if (plugin.getSoldierManager().recruitSoldier(player, barracks, type, name)) {
                player.sendMessage("§a成功招募士兵 " + name + "！");
            } else {
                player.sendMessage("§c招募失败！请检查兵营等级和容量。");
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c无效的士兵类型！可用类型: " + 
                Arrays.stream(SoldierType.values())
                    .map(SoldierType::getDisplayName)
                    .collect(Collectors.joining(", "))
            );
        }
    }
    
    private void handleList(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        // 打开GUI
        new SoldierManageGUI(plugin, player, nation.get()).open();
    }
    
    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /soldier info <ID>");
            return;
        }
        
        try {
            long id = Long.parseLong(args[1]);
            Soldier soldier = plugin.getSoldierManager().getSoldierById(id);
            
            if (soldier == null || !soldier.getUuid().equals(player.getUniqueId())) {
                player.sendMessage("§c找不到该士兵！");
                return;
            }
            
            player.sendMessage("§6=== 士兵详情 ===");
            player.sendMessage("§7名称: §f" + soldier.getName());
            player.sendMessage("§7类型: §f" + soldier.getType().getDisplayName());
            player.sendMessage("§7等级: §f" + soldier.getLevel());
            player.sendMessage("§7经验: §f" + soldier.getExperience() + "/" + (soldier.getLevel() * 100));
            
            Map<String, Double> attrs = soldier.getAttributes();
            player.sendMessage("§7生命值: §f" + String.format("%.1f", attrs.get("health")));
            player.sendMessage("§7攻击力: §f" + String.format("%.1f", attrs.get("attack")));
            player.sendMessage("§7防御力: §f" + String.format("%.1f", attrs.get("defense")));
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的ID！");
        }
    }
    
    private void handleTrain(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /soldier train <ID>");
            return;
        }
        
        try {
            long id = Long.parseLong(args[1]);
            Soldier soldier = plugin.getSoldierManager().getSoldierById(id);
            
            if (soldier == null || !soldier.getUuid().equals(player.getUniqueId())) {
                player.sendMessage("§c找不到该士兵！");
                return;
            }
            
            TrainingSystem.startTraining(soldier);
            player.sendMessage("§a开始训练士兵 " + soldier.getName() + "！");
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的ID！");
        }
    }
    
    private void handleDismiss(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /soldier dismiss <ID>");
            return;
        }
        
        try {
            long id = Long.parseLong(args[1]);
            Soldier soldier = plugin.getSoldierManager().getSoldierById(id);
            
            if (soldier == null || !soldier.getUuid().equals(player.getUniqueId())) {
                player.sendMessage("§c找不到该士兵！");
                return;
            }
            
            if (plugin.getSoldierManager().dismissSoldier(soldier)) {
                player.sendMessage("§a已解雇士兵 " + soldier.getName() + "！");
            } else {
                player.sendMessage("§c解雇失败！");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的ID！");
        }
    }
} 