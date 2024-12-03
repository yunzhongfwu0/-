package com.nations.core.listeners;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public class TerritoryProtectionListener implements Listener {
    
    private final NationsCore plugin;
    
    public TerritoryProtectionListener(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        // 如果是系统管理员，直接允许操作
        if (player.hasPermission("nations.admin")) {
            return;
        }
        
        Location location = event.getBlock().getLocation();
        
        // 检查是否在任何国家的领土内
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.isInTerritory(location)) {
                if (!nation.isMember(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("§c这是 " + nation.getName() + " 的领土，你不能在这里破坏方块！");
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        // 如果是系统管理员，直接允许操作
        if (player.hasPermission("nations.admin")) {
            return;
        }
        
        Location location = event.getBlock().getLocation();
        
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.isInTerritory(location)) {
                if (!nation.isMember(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("§c这是 " + nation.getName() + " 的领土，你不能在这里放置方块！");
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        
        // 如果是系统管理员，直接允许操作
        if (player.hasPermission("nations.admin")) {
            return;
        }
        
        Location location = event.getClickedBlock().getLocation();
        
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            if (nation.isInTerritory(location)) {
                if (!nation.isMember(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("§c这是 " + nation.getName() + " 的领土，你不能与这里的方块互动！");
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location to = event.getTo();
        
        // 检查是否进入或离开领土
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            boolean wasIn = nation.isInTerritory(event.getFrom());
            boolean isIn = nation.isInTerritory(to);
            
            if (!wasIn && isIn) {
                // 进入领土
                player.sendMessage("§e>> 你进入了 " + nation.getName() + " 的领土");
            } else if (wasIn && !isIn) {
                // 离开领土
                player.sendMessage("§e<< 你离开了 " + nation.getName() + " 的领土");
            }
        }
    }
} 