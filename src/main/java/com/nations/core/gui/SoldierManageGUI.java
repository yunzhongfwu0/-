package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SoldierManageGUI extends BaseGUI {
    private BukkitTask updateTask;
    
    public SoldierManageGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6士兵管理", 6);
        initialize(nation);
        startUpdateTask();
    }
    
    private void initialize(Nation nation) {
        // 获取玩家的士兵
        Set<Soldier> soldiers = plugin.getSoldierManager().getSoldiersByPlayer(player.getUniqueId());
        
        // 添加按钮
        setItem(49, createItem(Material.DIAMOND_SWORD,
            "§a招募新士兵",
            "§7点击招募新士兵",
            "§7当前: §f" + soldiers.size()
        ), p -> new SoldierRecruitGUI(plugin, p, nation).open());
        
        // 添加说明
        setItem(50, createItem(Material.BOOK,
            "§6士兵系统说明",
            "§7- 左键点击士兵查看详情",
            "§7- 右键点击士兵进行训练",
            "§7- Shift+右键解雇士兵"
        ), null);
        
        // 显示现有士兵
        int slot = 0;
        for (Soldier soldier : soldiers) {
            if (slot >= 45) break; // 最多显示45个士兵
            setItem(slot++, createSoldierItem(soldier), 
                // 左键处理器
                p -> showSoldierInfo(p, soldier),
                // 右键处理器
                p -> {
                    if (lastClickType == ClickType.SHIFT_RIGHT) {
                        // Shift+右键解雇士兵
                        if (plugin.getSoldierManager().dismissSoldier(soldier)) {
                            p.sendMessage("§a成功解雇士兵 " + soldier.getName());
                            updateDisplay();
                        } else {
                            p.sendMessage("§c解雇士兵失败！");
                        }
                    } else {
                        // 右键训练士兵
                        if (plugin.getSoldierManager().isTraining(soldier)) {
                            p.sendMessage("§c该士兵正在训练中！");
                            return;  // 直接返回，不打开训练选择GUI
                        }
                        new TrainingSelectGUI(plugin, p, nation, soldier).open();
                    }
                }
            );
        }
    }
    
    private void showSoldierInfo(Player player, Soldier soldier) {
        player.sendMessage("§6=== 士兵详情 ===");
        player.sendMessage("§7名称: §f" + soldier.getName());
        player.sendMessage("§7类型: §f" + soldier.getType().getDisplayName());
        player.sendMessage("§7等级: §f" + soldier.getLevel());
        player.sendMessage("§7经验: §f" + soldier.getExperience() + "/" + (soldier.getLevel() * 100));
        
        Map<String, Double> attrs = soldier.getAttributes();
        player.sendMessage("§7生命值: §f" + String.format("%.1f", attrs.get("health")));
        player.sendMessage("§7攻击力: §f" + String.format("%.1f", attrs.get("attack")));
        player.sendMessage("§7防御力: §f" + String.format("%.1f", attrs.get("defense")));
        
        if (plugin.getSoldierManager().isTraining(soldier)) {
            long timeLeft = plugin.getSoldierManager().getTrainingTimeLeft(soldier);
            int minutes = (int)(timeLeft / (1000 * 60));
            int seconds = (int)((timeLeft / 1000) % 60);
            player.sendMessage("");
            player.sendMessage("§e训练中...");
            player.sendMessage("§7剩余时间: §f" + String.format("%d:%02d", minutes, seconds));
        }
    }
    
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() == null || 
                    !player.getOpenInventory().getTitle().equals("§6士兵管理")) {
                    cancel();
                    return;
                }
                updateDisplay();
            }
        }.runTaskTimer(plugin, 20L, 20L);  // 秒更新一次
    }
    
    private void updateDisplay() {
        Set<Soldier> soldiers = plugin.getSoldierManager().getSoldiersByPlayer(player.getUniqueId());
        int slot = 0;
        for (Soldier soldier : soldiers) {
            if (slot >= 45) break;
            getInventory().setItem(slot++, createSoldierItem(soldier));
        }
    }
    
    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (updateTask != null) {
            updateTask.cancel();
        }
        super.handleClose(event);
    }
    
    private ItemStack createSoldierItem(Soldier soldier) {
        ItemStack item = new ItemStack(getSoldierMaterial(soldier.getType()));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + soldier.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("§7类型: §f" + soldier.getType().getDisplayName());
        lore.add("§7等级: §f" + soldier.getLevel());
        lore.add("§7经验: §f" + soldier.getExperience() + "/" + (soldier.getLevel() * 100));
        
        Map<String, Double> attrs = soldier.getAttributes();
        lore.add("§7生命值: §f" + String.format("%.1f", attrs.get("health")));
        lore.add("§7攻击力: §f" + String.format("%.1f", attrs.get("attack")));
        lore.add("§7防御力: §f" + String.format("%.1f", attrs.get("defense")));
        
        // 添加训练状态
        if (plugin.getSoldierManager().isTraining(soldier)) {
            long timeLeft = plugin.getSoldierManager().getTrainingTimeLeft(soldier);
            int minutes = (int)(timeLeft / (1000 * 60));
            int seconds = (int)((timeLeft / 1000) % 60);
            lore.add("");
            lore.add("§e训练中...");
            lore.add("§7剩余时间: §f" + String.format("%d:%02d", minutes, seconds));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private Material getSoldierMaterial(SoldierType type) {
        return switch (type) {
            case WARRIOR -> Material.IRON_SWORD;
            case ARCHER -> Material.BOW;
            case SUPPORT -> Material.GOLDEN_APPLE;
            case GENERAL -> Material.DIAMOND_SWORD;
        };
    }
} 