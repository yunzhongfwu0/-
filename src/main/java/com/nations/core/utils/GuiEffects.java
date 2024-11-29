package com.nations.core.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.bukkit.enchantments.Enchantment;

public class GuiEffects {
    
    // 打开GUI时的动画效果
    public static void playOpenAnimation(Plugin plugin, Inventory inventory, Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= inventory.getSize() || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                if (inventory.getItem(tick) != null) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.2f, 1.5f);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    // 点击按钮的音效
    public static void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    // 成功操作的音效
    public static void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    // 错误操作的音效
    public static void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
    
    // 确认操作的音效
    public static void playConfirmSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }
    
    private static void removeAllEnchants(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            for (Enchantment enchantment : item.getEnchantments().keySet()) {
                item.removeEnchantment(enchantment);
            }
        }
    }
    
    // 高亮物品动画
    public static void playHighlightAnimation(Plugin plugin, Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) return;
        
        new BukkitRunnable() {
            int tick = 0;
            boolean glow = false;
            
            @Override
            public void run() {
                if (tick >= 6 || inventory.getItem(slot) != item) {
                    cancel();
                    return;
                }
                
                if (glow) {
                    removeAllEnchants(item);
                } else {
                    item.addUnsafeEnchantment(Enchantment.LUCK, 1);
                }
                glow = !glow;
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
} 