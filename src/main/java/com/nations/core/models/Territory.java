package com.nations.core.models;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
@Setter
public class Territory {
    private final long id;
    private final long nationId;
    private String worldName;
    private int centerX;
    private int centerZ;
    private int radius;
    
    public Territory(long id, long nationId, String worldName, int centerX, int centerZ, int radius) {
        this.id = id;
        this.nationId = nationId;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
    }
    
    public static Territory createNew(String worldName, int centerX, int centerZ, int radius) {
        return new Territory(0, 0, worldName, centerX, centerZ, radius);
    }
    
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        int dx = location.getBlockX() - centerX;
        int dz = location.getBlockZ() - centerZ;
        
        return Math.abs(dx) <= radius && Math.abs(dz) <= radius;
    }
    
    public boolean overlaps(Territory other) {
        if (!worldName.equals(other.worldName)) {
            return false;
        }
        int dx = Math.abs(centerX - other.centerX);
        int dz = Math.abs(centerZ - other.centerZ);
        return dx <= (radius + other.radius) && dz <= (radius + other.radius);
    }
    
    public void markBorder(World world) {
        if (!world.getName().equals(worldName)) return;
        
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("NationsCore"), () -> {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                placeMarker(world, x, centerZ - radius);
                placeMarker(world, x, centerZ + radius);
            }
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                placeMarker(world, centerX - radius, z);
                placeMarker(world, centerX + radius, z);
            }
        });
    }
    
    private void placeMarker(World world, int x, int z) {
        Block block = world.getHighestBlockAt(x, z);
        block.getRelative(0, 1, 0).setType(Material.STONE_SLAB);
    }
    
    public void clearBorder(World world) {
        if (!world.getName().equals(worldName)) return;
        
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("NationsCore"), () -> {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                clearMarker(world, x, centerZ - radius);
                clearMarker(world, x, centerZ + radius);
            }
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                clearMarker(world, centerX - radius, z);
                clearMarker(world, centerX + radius, z);
            }
        });
    }
    
    private void clearMarker(World world, int x, int z) {
        Block block = world.getHighestBlockAt(x, z);
        if (block.getRelative(0, 1, 0).getType() == Material.STONE_SLAB) {
            block.getRelative(0, 1, 0).setType(Material.AIR);
        }
    }
    
    public void showBorderParticles(Player player) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        
        new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (tick >= 200) {
                    cancel();
                    return;
                }
                
                for (int i = 0; i < 4; i++) {
                    double progress = (tick + i * 50) % 200 / 200.0;
                    
                    // 北边
                    if (i == 0) {
                        double x = centerX - radius + (2 * radius * progress);
                        showParticle(player, x, centerZ - radius);
                    }
                    // 东边
                    else if (i == 1) {
                        double z = centerZ - radius + (2 * radius * progress);
                        showParticle(player, centerX + radius, z);
                    }
                    // 南边
                    else if (i == 2) {
                        double x = centerX + radius - (2 * radius * progress);
                        showParticle(player, x, centerZ + radius);
                    }
                    // 西边
                    else {
                        double z = centerZ + radius - (2 * radius * progress);
                        showParticle(player, centerX - radius, z);
                    }
                }
                
                tick++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("NationsCore"), 0L, 1L);
    }
    
    private void showParticle(Player player, double x, double z) {
        Location loc = new Location(
            Bukkit.getWorld(worldName),
            x + 0.5,
            player.getWorld().getHighestBlockYAt((int)x, (int)z) + 1.5,
            z + 0.5
        );
        player.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
    }
    
    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }
} 