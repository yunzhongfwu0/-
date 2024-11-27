package com.nations.core.models;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;

import java.util.UUID;

@Data
public class Nation {
    private final long id;
    private String name;
    private UUID ownerUUID;
    private int level;
    private double balance;
    private Location spawnPoint;
    
    public Nation(long id, String name, UUID ownerUUID, int level, double balance, 
                 String worldName, double x, double y, double z, float yaw, float pitch) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.level = level;
        this.balance = balance;
        
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            this.spawnPoint = new Location(world, x, y, z, yaw, pitch);
        }
    }
    
    public Nation(long id, String name, UUID ownerUUID, int level, double balance) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.level = level;
        this.balance = balance;
        this.spawnPoint = null;
    }
} 