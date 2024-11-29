package com.nations.core.utils;

import com.nations.core.models.Territory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TerritoryVisualizer {
    
    public static void showBorders(Player player, Territory territory) {
        if (territory == null) {
            player.sendMessage("§c该国家还没有领土！");
            return;
        }

        World world = player.getWorld();
        if (!world.getName().equals(territory.getWorldName())) {
            player.sendMessage("§c你必须在同一个世界才能查看领土边界！");
            return;
        }
        
        // 在主线程中执行粒子效果
        Bukkit.getScheduler().runTaskTimer(
            Bukkit.getPluginManager().getPlugin("NationsCore"), 
            new Runnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (ticks >= 200) { // 10秒后停止显示
                        return;
                    }
                    
                    int centerX = territory.getCenterX();
                    int centerZ = territory.getCenterZ();
                    int radius = territory.getRadius();
                    
                    // 显示四条边界线
                    for (int i = -radius; i <= radius; i++) {
                        showParticle(world, centerX + i, centerZ - radius, player); // 北边
                        showParticle(world, centerX + i, centerZ + radius, player); // 南边
                        showParticle(world, centerX - radius, centerZ + i, player); // 西边
                        showParticle(world, centerX + radius, centerZ + i, player); // 东边
                    }
                    
                    ticks += 2;
                }
            }, 0L, 2L);
    }
    
    private static void showParticle(World world, int x, int z, Player player) {
        Location loc = new Location(world, x + 0.5, 
            world.getHighestBlockYAt(x, z) + 1, z + 0.5);
        player.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
    }
} 