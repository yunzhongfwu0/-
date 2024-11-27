package com.nations.core.models;

import lombok.Data;
import org.bukkit.Location;
import java.util.UUID;

@Data
public class Nation {
    private final long id;
    private String name;
    private UUID ownerUUID;
    private int level;
    private double balance;
    private Location spawnPoint;
    private String serverId;      // 服务器ID
    private int serverPort;       // 服务器端口
    private boolean isLocalServer; // 是否是本服国家
    
    public Nation(long id, String name, UUID ownerUUID, int level, double balance,
                 String serverId, int serverPort, boolean isLocalServer) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.level = level;
        this.balance = balance;
        this.serverId = serverId;
        this.serverPort = serverPort;
        this.isLocalServer = isLocalServer;
    }
    
    public Nation(long id, String name, UUID ownerUUID, int level, double balance,
                 String worldName, double x, double y, double z, float yaw, float pitch,
                 String serverId, int serverPort, boolean isLocalServer) {
        this(id, name, ownerUUID, level, balance, serverId, serverPort, isLocalServer);
        if (isLocalServer && worldName != null) {
            this.spawnPoint = new Location(
                org.bukkit.Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        }
    }
} 