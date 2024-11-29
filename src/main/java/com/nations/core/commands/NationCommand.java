package com.nations.core.commands;

import com.nations.core.NationsCore;
import com.nations.core.gui.*;
import com.nations.core.models.Nation;
import com.nations.core.models.Territory;
import com.nations.core.models.NationRank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.bukkit.configuration.ConfigurationSection;

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
            new MainGUI(plugin, player).open();
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "gui" -> new MainGUI(plugin, player).open();
            case "create" -> handleCreate(player, args);
            case "info" -> handleInfo(player, args);
            case "delete" -> handleDelete(player);
            case "rename" -> handleRename(player, args);
            case "setspawn" -> handleSetSpawn(player);
            case "spawn" -> handleSpawn(player, args);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "kick" -> handleKick(player, args);
            case "leave" -> handleLeave(player);
            case "promote" -> handlePromote(player, args);
            case "demote" -> handleDemote(player, args);
            case "territory" -> handleTerritory(player);
            case "showborder" -> handleShowBorder(player);
            case "help" -> showHelp(player);
            case "deposit" -> handleDeposit(player, args);
            case "withdraw" -> handleWithdraw(player, args);
            case "upgrade" -> handleUpgrade(player);
            default -> {
                player.sendMessage("§c未知命令！使用 /nation help 查看帮助");
                return false;
            }
        }
        
        return true;
    }

    // ... 其他已有的处理方法保持不变 ...

    public void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation invite <玩家名>");
            return;
        }

        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c找不到指定的玩家！");
            return;
        }

        if (plugin.getNationManager().getNationByPlayer(target).isPresent()) {
            player.sendMessage("§c该玩家已经有国家了！");
            return;
        }

        // 发送带点击事件的邀请消息
        target.sendMessage(Component.text("§6========== 国家邀请 =========="));
        target.sendMessage(Component.text("§e" + player.getName() + " 邀请你加入国家 " + nation.get().getName()));
        target.sendMessage(Component.text("§a[点击接受]")
            .clickEvent(ClickEvent.runCommand("/nation accept " + nation.get().getName()))
            .append(Component.text(" §7或 "))
            .append(Component.text("§c[点击拒绝]")
                .clickEvent(ClickEvent.runCommand("/nation deny " + nation.get().getName()))));
        target.sendMessage(Component.text("§6============================"));

        player.sendMessage("§a已向 " + target.getName() + " 发送邀请！");
    }

    public void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation accept <国家名>");
            return;
        }

        Optional<Nation> nation = plugin.getNationManager().getNationByName(args[1]);
        if (nation.isEmpty()) {
            player.sendMessage("§c找不到指定的国家！");
            return;
        }

        if (plugin.getNationManager().addMember(nation.get(), player.getUniqueId(), "MEMBER")) {
            player.sendMessage("§a你已加入国家 " + nation.get().getName() + "！");
            plugin.getServer().broadcast(
                Component.text("§e" + player.getName() + " 加入了国家 " + nation.get().getName() + "！")
            );
        } else {
            player.sendMessage("§c加入国家失败！");
        }
    }

    public void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation deny <国家名>");
            return;
        }

        Optional<Nation> nation = plugin.getNationManager().getNationByName(args[1]);
        if (nation.isEmpty()) {
            player.sendMessage("§c找不到指定的国家！");
            return;
        }

        player.sendMessage("§a你已拒绝加入国家 " + nation.get().getName());
        Player owner = plugin.getServer().getPlayer(nation.get().getOwnerUUID());
        if (owner != null) {
            owner.sendMessage("§e" + player.getName() + " 拒绝了加入国家的邀请。");
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("§6========== 国家系统帮助 ==========");
        player.sendMessage("§e基础命令:");
        player.sendMessage("§e/nation §f- 打开国家系统GUI");
        player.sendMessage("§e/nation create <名称> §f- 创建一个新国家");
        player.sendMessage("§e/nation info [玩家名] §f- 查看国家信息");
        player.sendMessage("§e/nation delete §f- 删除你的国家");
        
        player.sendMessage("§e成员管理:");
        player.sendMessage("§e/nation invite <玩家名> §f- 邀请玩家加入国家");
        player.sendMessage("§e/nation kick <玩家名> §f- 将玩家踢出国家");
        player.sendMessage("§e/nation promote <玩家名> §f- 提升玩家职位");
        player.sendMessage("§e/nation demote <玩家名> §f- 降低玩家职位");
        player.sendMessage("§e/nation leave §f- 离开当前国家");
        
        player.sendMessage("§e领地管理:");
        player.sendMessage("§e/nation territory §f- 查看领地信息");
        player.sendMessage("§e/nation showborder §f- 显示领地边界");
        player.sendMessage("§e/nation setspawn §f- 设置国家传送点");
        player.sendMessage("§e/nation spawn [国家名] §f- 传送到国家传送点");
        
        player.sendMessage("§e经济管理:");
        player.sendMessage("§e/nation deposit <金额> §f- 向国库存入金钱");
        player.sendMessage("§e/nation withdraw <金额> §f- 从国库取出金钱");
        
        player.sendMessage("§e升级管理:");
        player.sendMessage("§e/nation upgrade §f- 升级国家");
        
        player.sendMessage("§6================================");
    }

    public void handleCreate(Player player, String[] args) {
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
            player.sendMessage("§c国家名称只能包含中文、字母、数字和下划线");
            return;
        }
        
        // 获取玩家当前位置作为国家中心
        Location center = player.getLocation();
        
        // 创建国家
        if (plugin.getNationManager().createNationWithTerritory(player, nationName, center)) {
            player.sendMessage("§a成功创建国家 " + nationName + "！");
            player.sendMessage("§a国家领土已以你当前位置为中心建立！");
            player.sendMessage("§a初始领土范围: 30x30");
            player.sendMessage("§a使用 /nation territory 查看领土信息");
            player.sendMessage("§a使用 /nation showborder 显示领土边界");
            
            // 广播消息
            plugin.getServer().broadcast(
                Component.text("§e" + player.getName() + " 创建了新的国家: " + nationName + "！")
            );
            
            // 自动显示领土边界
            plugin.getServer().dispatchCommand(player, "nation showborder");
        } else {
            player.sendMessage("§c创建国家失败！该名称可能已被使用，或与其他国家领土重叠。");
        }
    }

    public void handleInfo(Player player, String[] args) {
        Optional<Nation> nation;
        if (args.length > 1) {
            // 查看指定家的国家
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target != null) {
                nation = plugin.getNationManager().getNationByPlayer(target);
            } else {
                // 如果找不到玩家，尝试按国家名查找
                nation = plugin.getNationManager().getNationByName(args[1]);
            }
        } else {
            // 查看自己的国家
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
        player.sendMessage("§e所属服务器: §f" + (n.isLocalServer() ? "本服" : "子服-" + n.getServerId()));
        if (n.getSpawnPoint() != null) {
            Location spawn = n.getSpawnPoint();
            player.sendMessage("§e传送点: §f" + String.format("%.0f, %.0f, %.0f", 
                spawn.getX(), spawn.getY(), spawn.getZ()));
        }
        player.sendMessage("§6================================");
    }

    public void handleDelete(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!player.getUniqueId().equals(nation.get().getOwnerUUID())) {
            player.sendMessage("§c只有国家领袖才能删除国家！");
            return;
        }
        
        if (plugin.getNationManager().deleteNation(nation.get())) {
            player.sendMessage("§a成功删除国家！");
        } else {
            player.sendMessage("§c删除国家失败！");
        }
    }

    public void handleRename(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation rename <新名称>");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!player.getUniqueId().equals(nation.get().getOwnerUUID())) {
            player.sendMessage("§c只有国家领袖才能重命名国家！");
            return;
        }
        
        String newName = args[1];
        double renameCost = plugin.getConfig().getDouble("nations.rename-cost", 5000.0);
        
        if (nation.get().getBalance() < renameCost) {
            player.sendMessage("§c国库余额不足！重命名需要 " + renameCost + " 金币。");
            return;
        }
        
        if (plugin.getNationManager().renameNation(nation.get(), newName, renameCost)) {
            player.sendMessage("§a成功将国家重命名为 " + newName + "！");
            player.sendMessage("§a花费: " + renameCost + " 金币");
        } else {
            player.sendMessage("§c重命名失败！该名称可能已被使用。");
        }
    }

    public void handleSetSpawn(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!nation.get().hasPermission(player.getUniqueId(), "nation.setspawn")) {
            player.sendMessage("§c你没有设置传送点的权限！");
            return;
        }
        
        Location location = player.getLocation();
        if (!nation.get().isInTerritory(location)) {
            player.sendMessage("§c只能在国家领地内设置传送点！");
            return;
        }
        
        if (plugin.getNationManager().setSpawnPoint(nation.get(), location)) {
            player.sendMessage("§a成功设置国家传送点！");
        } else {
            player.sendMessage("§c设置传送点失败！");
        }
    }

    public void handleSpawn(Player player, String[] args) {
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
        
        plugin.getNationManager().teleportToNation(player, nation.get());
    }

    public void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation kick <玩家名>");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!nation.get().hasPermission(player.getUniqueId(), "nation.kick")) {
            player.sendMessage("§c你没有踢出成员的权限！");
            return;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        if (target.getUniqueId().equals(nation.get().getOwnerUUID())) {
            player.sendMessage("§c你不能踢出国家领袖！");
            return;
        }
        
        if (plugin.getNationManager().removeMember(nation.get(), target.getUniqueId())) {
            player.sendMessage("§a已将 " + target.getName() + " 踢出国家！");
            target.sendMessage("§c你已被踢出国家 " + nation.get().getName() + "！");
        } else {
            player.sendMessage("§c踢出成员失败！该玩家可能不是国家成员。");
        }
    }

    public void handleLeave(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (player.getUniqueId().equals(nation.get().getOwnerUUID())) {
            player.sendMessage("§c国家领袖不能离开国家！请先转让国家或删除国家。");
            return;
        }
        
        if (plugin.getNationManager().removeMember(nation.get(), player.getUniqueId())) {
            player.sendMessage("§a你已离开国家 " + nation.get().getName() + "！");
        } else {
            player.sendMessage("§c离开国家失败！");
        }
    }

    public void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation promote <玩家名>");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!nation.get().hasPermission(player.getUniqueId(), "nation.promote")) {
            player.sendMessage("§c你没有提升职位的权限！");
            return;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        NationRank currentRank = nation.get().getMemberRank(target.getUniqueId());
        if (currentRank == null || currentRank == NationRank.OWNER) {
            player.sendMessage("§c无法提升该玩家的职位！");
            return;
        }
        
        NationRank newRank = NationRank.values()[currentRank.ordinal() - 1];
        if (plugin.getNationManager().promoteMember(nation.get(), target.getUniqueId(), newRank)) {
            player.sendMessage("§a已将 " + target.getName() + " 的职位提升为 " + newRank.getDisplayName());
            target.sendMessage("§a你在国家 " + nation.get().getName() + " 中的职位已被提升为 " + newRank.getDisplayName());
        } else {
            player.sendMessage("§c提升职位失败！");
        }
    }

    public void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation demote <玩家名>");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!nation.get().hasPermission(player.getUniqueId(), "nation.promote")) {
            player.sendMessage("§c你没有降低职位的权限");
            return;
        }
        
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§c找不到指定的玩家！");
            return;
        }
        
        NationRank currentRank = nation.get().getMemberRank(target.getUniqueId());
        if (currentRank == null || currentRank == NationRank.MEMBER) {
            player.sendMessage("§c无法降低该玩家的职位！");
            return;
        }
        
        NationRank newRank = NationRank.values()[currentRank.ordinal() + 1];
        if (plugin.getNationManager().promoteMember(nation.get(), target.getUniqueId(), newRank)) {
            player.sendMessage("§a已将 " + target.getName() + " 的职位降为 " + newRank.getDisplayName());
            target.sendMessage("§c你在国家 " + nation.get().getName() + " 中的职位已被降为 " + newRank.getDisplayName());
        } else {
            player.sendMessage("§c降低职位失败！");
        }
    }

    public void handleTerritory(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        Territory territory = nation.get().getTerritory();
        if (territory == null) {
            player.sendMessage("§c你的国家还没有领土！");
            return;
        }
        
        player.sendMessage("§6========== 国家领土信息 ==========");
        player.sendMessage("§e中心坐标: §f" + territory.getCenterX() + ", " + territory.getCenterZ());
        player.sendMessage("§e当前范围: §f" + territory.getRadius() * 2 + "x" + territory.getRadius() * 2);
        player.sendMessage("§e最大范围: §f" + nation.get().getMaxRadius() * 2 + "x" + nation.get().getMaxRadius() * 2);
        player.sendMessage("§e所在世界: §f" + territory.getWorldName());
        player.sendMessage("§6================================");
    }

    public void handleShowBorder(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        Territory territory = nation.get().getTerritory();
        if (territory == null) {
            player.sendMessage("§c你的国家还没有领土！");
            return;
        }
        
        // 使用粒子效果显示边界，而不是创建实体方块
        territory.showBorderParticles(player);
        player.sendMessage("§a正在显示领土边界...");
    }

    private void handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation deposit <金额>");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage("§c金额必须大于0！");
                return;
            }
            
            if (plugin.getNationManager().deposit(nation.get(), player, amount)) {
                player.sendMessage("§a成功向国库存入 " + amount + " 金币！");
                player.sendMessage("§a当前国库余额: " + nation.get().getBalance());
            } else {
                player.sendMessage("§c存入失败！请确保你有足够的金币。");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的金额！");
        }
    }

    private void handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c用法: /nation withdraw <金额>");
            return;
        }
        
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!nation.get().hasPermission(player.getUniqueId(), "nation.withdraw")) {
            player.sendMessage("§c你没有从国库取钱的权限！");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                player.sendMessage("§c金额必须大于0！");
                return;
            }
            
            if (plugin.getNationManager().withdraw(nation.get(), player, amount)) {
                player.sendMessage("§a成功从国库取出 " + amount + " 金币！");
                player.sendMessage("§a当前国库余额: " + nation.get().getBalance());
            } else {
                player.sendMessage("§c取出失败！请确保国库有足够的金币。");
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c无效的金额！");
        }
    }

    private void handleUpgrade(Player player) {
        Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
        if (nation.isEmpty()) {
            player.sendMessage("§c你没有国家！");
            return;
        }
        
        if (!nation.get().hasPermission(player.getUniqueId(), "nation.upgrade")) {
            player.sendMessage("§c你没有升级国家的权限！");
            return;
        }
        
        int nextLevel = nation.get().getLevel() + 1;
        ConfigurationSection levelConfig = plugin.getConfig().getConfigurationSection("nations.levels." + nextLevel);
        
        if (levelConfig == null) {
            player.sendMessage("§c你的国家已经达到最高等级！");
            return;
        }
        
        // 打开升级GUI
        new UpgradeGUI(plugin, player, nation.get()).open();
    }
} 