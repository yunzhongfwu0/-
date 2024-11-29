package com.nations.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

public class FeedbackManager {
    
    public static void sendSuccess(Player player, String message) {
        player.sendMessage("§a✔ " + message);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        ParticleEffects.playCircleEffect(player.getServer().getPluginManager().getPlugin("NationsCore"), 
            player, Particle.VILLAGER_HAPPY);
    }
    
    public static void sendError(Player player, String message) {
        player.sendMessage("§c✘ " + message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        ParticleEffects.playCircleEffect(player.getServer().getPluginManager().getPlugin("NationsCore"), 
            player, Particle.VILLAGER_ANGRY);
    }
    
    public static void sendWarning(Player player, String message) {
        player.sendMessage("§e⚠ " + message);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }
    
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Title.Times times = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),
            Duration.ofMillis(stay * 50L),
            Duration.ofMillis(fadeOut * 50L)
        );
        
        Title titleObj = Title.title(
            Component.text(title),
            Component.text(subtitle),
            times
        );
        
        player.showTitle(titleObj);
    }
    
    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }
    
    public static void playProgressSound(Player player, double progress) {
        float pitch = (float) (0.5 + (progress * 1.5));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, pitch);
    }
} 