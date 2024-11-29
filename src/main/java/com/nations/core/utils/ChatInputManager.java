package com.nations.core.utils;

import com.nations.core.NationsCore;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputManager {
    private static final Map<UUID, Consumer<String>> inputHandlers = new HashMap<>();
    private static NationsCore plugin;
    
    public static void init(NationsCore instance) {
        plugin = instance;
    }
    
    public static void awaitChatInput(Player player, Consumer<String> handler) {
        inputHandlers.put(player.getUniqueId(), handler);
        
        // 30秒后自动取消等待
        new BukkitRunnable() {
            @Override
            public void run() {
                if (inputHandlers.remove(player.getUniqueId()) != null) {
                    player.sendMessage("§c输入超时，操作已取消。");
                    // 重新打开之前的GUI
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.performCommand("nadmin gui");
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskLater(plugin, 600L); // 30秒 = 600 ticks
    }
    
    public static boolean handleChatInput(Player player, String message) {
        Consumer<String> handler = inputHandlers.remove(player.getUniqueId());
        if (handler != null) {
            if (!message.equalsIgnoreCase("cancel")) {
                // 在主线程中执行处理
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        handler.accept(message);
                    }
                }.runTask(plugin);
            } else {
                player.sendMessage("§c已取消操作。");
                // 重新打开之前的GUI
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.performCommand("nadmin gui");
                    }
                }.runTask(plugin);
            }
            return true;
        }
        return false;
    }
    
    public static void cancelInput(Player player) {
        if (inputHandlers.remove(player.getUniqueId()) != null) {
            player.sendMessage("§c操作已取消。");
        }
    }
    
    public static boolean isWaitingForInput(Player player) {
        return inputHandlers.containsKey(player.getUniqueId());
    }
} 