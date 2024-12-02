package com.nations.core.listeners;

import com.nations.core.NationsCore;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CitizensLoadListener implements Listener {
    private final NationsCore plugin;

    public CitizensLoadListener(NationsCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCitizensEnable(CitizensEnableEvent event) {
        // 在下一个tick执行，确保Citizens完全加载
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("Citizens 已加载，开始加载 NPC...");
            plugin.getNPCManager().loadNPCs();
        });
    }
} 