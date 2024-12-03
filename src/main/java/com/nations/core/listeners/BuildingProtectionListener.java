package com.nations.core.listeners;

import com.nations.core.NationsCore;
import com.nations.core.models.Building;
import com.nations.core.models.Nation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BuildingProtectionListener implements Listener {
    
    private final NationsCore plugin;
    
    public BuildingProtectionListener(NationsCore plugin) {
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
        
        // 检查是否在任何建筑的范围内
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            for (Building building : nation.getBuildings()) {
                if (isInBuildingArea(location, building)) {
                    // 如果不是国家成员，取消事件
                    if (!nation.getOwnerUUID().equals(player.getUniqueId())) {
                        event.setCancelled(true);
                        player.sendMessage("§c你没有权限在这个建筑中破坏方块！");
                    }
                    return;
                }
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
        
        // 检查是否在任何建筑的范围内
        for (Nation nation : plugin.getNationManager().getAllNations()) {
            for (Building building : nation.getBuildings()) {
                if (isInBuildingArea(location, building)) {
                    // 如果不是国家成员，取消事件
                    if (!nation.getOwnerUUID().equals(player.getUniqueId())) {
                        event.setCancelled(true);
                        player.sendMessage("§c你没有权限在这个建筑中放置方块！");
                    }
                    return;
                }
            }
        }
    }
    
    private boolean isInBuildingArea(Location location, Building building) {
        Location buildingLoc = building.getBaseLocation();
        if (buildingLoc.getWorld() != location.getWorld()) return false;
        
        int size = building.getSize();
        int halfSize = size / 2;
        
        int dx = Math.abs(location.getBlockX() - buildingLoc.getBlockX());
        int dz = Math.abs(location.getBlockZ() - buildingLoc.getBlockZ());
        
        return dx <= halfSize && dz <= halfSize;
    }
} 