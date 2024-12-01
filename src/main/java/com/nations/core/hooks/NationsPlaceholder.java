package com.nations.core.hooks;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.models.NationMember;
import com.nations.core.models.NationRank;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NationsPlaceholder extends PlaceholderExpansion {
    
    private final NationsCore plugin;
    
    public NationsPlaceholder(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "nations";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return "YourName";
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return "1.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) return "";
        
        Optional<Nation> nationOpt = plugin.getNationManager().getNationByUUID(player.getUniqueId());
        
        // 基础信息
        switch (identifier.toLowerCase()) {
            case "has_nation":
                return nationOpt.isPresent() ? "true" : "false";
                
            case "nation_name":
                return nationOpt.map(Nation::getName).orElse("");
                
            case "nation_level":
                return nationOpt.map(n -> String.valueOf(n.getLevel())).orElse("0");
                
            case "nation_balance":
                return nationOpt.map(n -> String.format("%.2f", n.getBalance())).orElse("0");
                
            case "nation_members_count":
                return nationOpt.map(n -> String.valueOf(n.getMembers().size())).orElse("0");
                
            case "nation_owner":
                return nationOpt.map(n -> Bukkit.getOfflinePlayer(n.getOwnerUUID()).getName()).orElse("");
                
            case "member_rank":
                return nationOpt.map(n -> {
                    if (n.getOwnerUUID().equals(player.getUniqueId())) {
                        return NationRank.OWNER.getDisplayName();
                    }
                    NationMember member = n.getMembers().get(player.getUniqueId());
                    return member != null ? member.getRank().getDisplayName() : "";
                }).orElse("");
                
            case "is_owner":
                return nationOpt.map(n -> n.getOwnerUUID().equals(player.getUniqueId()) ? "true" : "false").orElse("false");
        }
        
        // 领地信息
        if (identifier.startsWith("territory_")) {
            String subIdentifier = identifier.substring(10);
            switch (subIdentifier.toLowerCase()) {
                case "has":
                    return nationOpt.map(n -> n.getTerritory() != null ? "true" : "false").orElse("false");
                    
                case "size":
                    return nationOpt.map(n -> n.getTerritory() != null ? 
                        String.valueOf(n.getTerritory().getRadius() * 2) : "0").orElse("0");
                    
                case "world":
                    return nationOpt.map(n -> n.getTerritory() != null ? 
                        n.getTerritory().getWorldName() : "").orElse("");
                    
                case "center_x":
                    return nationOpt.map(n -> n.getTerritory() != null ? 
                        String.valueOf(n.getTerritory().getCenterX()) : "0").orElse("0");
                    
                case "center_z":
                    return nationOpt.map(n -> n.getTerritory() != null ? 
                        String.valueOf(n.getTerritory().getCenterZ()) : "0").orElse("0");
            }
        }
        
        // 传送点信息
        if (identifier.startsWith("spawn_")) {
            String subIdentifier = identifier.substring(6);
            switch (subIdentifier.toLowerCase()) {
                case "has":
                    return nationOpt.map(n -> n.getSpawnPoint() != null ? "true" : "false").orElse("false");
                    
                case "world":
                    return nationOpt.map(n -> n.getSpawnWorldName() != null ? n.getSpawnWorldName() : "").orElse("");
                    
                case "x":
                    return nationOpt.map(n -> n.getSpawnPoint() != null ? 
                        String.valueOf(n.getSpawnPoint().getX()) : "0").orElse("0");
                    
                case "y":
                    return nationOpt.map(n -> n.getSpawnPoint() != null ? 
                        String.valueOf(n.getSpawnPoint().getY()) : "0").orElse("0");
                    
                case "z":
                    return nationOpt.map(n -> n.getSpawnPoint() != null ? 
                        String.valueOf(n.getSpawnPoint().getZ()) : "0").orElse("0");
            }
        }
        
        // 全局统计
        if (identifier.startsWith("total_")) {
            String subIdentifier = identifier.substring(6);
            switch (subIdentifier.toLowerCase()) {
                case "nations":
                    return String.valueOf(plugin.getNationManager().getAllNations().size());
                    
                case "members":
                    return String.valueOf(plugin.getNationManager().getTotalPlayers());
                    
                case "territory":
                    return String.valueOf(plugin.getNationManager().getTotalTerritoryArea());
                    
                case "balance":
                    return String.format("%.2f", plugin.getNationManager().getTotalBalance());
            }
        }
        
        return null;
    }
} 