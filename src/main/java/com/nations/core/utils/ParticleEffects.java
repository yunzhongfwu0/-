package com.nations.core.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ParticleEffects {
    
    // 创建圆形粒子效果
    public static void playCircleEffect(Plugin plugin, Player player, Particle particle) {
        new BukkitRunnable() {
            double angle = 0;
            int count = 0;
            
            @Override
            public void run() {
                if (count++ >= 20) {
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                for (int i = 0; i < 4; i++) {
                    double x = Math.cos(angle + (Math.PI / 2) * i);
                    double z = Math.sin(angle + (Math.PI / 2) * i);
                    loc.add(x, 0.5, z);
                    player.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0);
                    loc.subtract(x, 0.5, z);
                }
                angle += Math.PI / 10;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    // 创建螺旋粒子效果
    public static void playSpiralEffect(Plugin plugin, Location location, Particle particle) {
        new BukkitRunnable() {
            double y = 0;
            double angle = 0;
            int count = 0;
            
            @Override
            public void run() {
                if (count++ >= 40) {
                    cancel();
                    return;
                }
                
                double x = Math.cos(angle) * 0.5;
                double z = Math.sin(angle) * 0.5;
                location.add(x, y, z);
                location.getWorld().spawnParticle(particle, location, 1, 0, 0, 0, 0);
                location.subtract(x, y, z);
                
                y += 0.1;
                angle += Math.PI / 8;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    // 创建爆炸粒子效果
    public static void playExplosionEffect(Location location, Particle particle) {
        for (int i = 0; i < 20; i++) {
            Vector direction = new Vector(
                Math.random() - 0.5,
                Math.random() - 0.5,
                Math.random() - 0.5
            ).normalize().multiply(0.5);
            
            location.getWorld().spawnParticle(
                particle,
                location,
                0,
                direction.getX(),
                direction.getY(),
                direction.getZ(),
                0.5
            );
        }
    }
    
    // 创建传送点标记效果
    public static void playSpawnPointEffect(Plugin plugin, Location location) {
        new BukkitRunnable() {
            int count = 0;
            
            @Override
            public void run() {
                if (count++ >= 100) {
                    cancel();
                    return;
                }
                
                double radius = 1.0;
                for (int i = 0; i < 4; i++) {
                    double angle = (count * Math.PI / 10) + (i * Math.PI / 2);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    location.add(x, 0, z);
                    location.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0, 0, 0, 0);
                    location.subtract(x, 0, z);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
} 