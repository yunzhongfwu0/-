package com.nations.core.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.util.*;

public class Nation {
    private final long id;
    private String name;
    private UUID ownerUUID;
    private int level;
    private double balance;
    private Location spawnPoint;
    private String serverId;
    private int serverPort;
    private boolean isLocalServer;
    private Territory territory;
    private final Map<UUID, NationMember> members = new HashMap<>();
    private final Map<UUID, Long> invites = new HashMap<>();
    private final long createdTime;
    
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
        this.createdTime = System.currentTimeMillis();
    }
    
    public Nation(long id, String name, UUID ownerUUID, int level, double balance,
                 String worldName, double x, double y, double z, float yaw, float pitch,
                 String serverId, int serverPort, boolean isLocalServer) {
        this(id, name, ownerUUID, level, balance, serverId, serverPort, isLocalServer);
        if (isLocalServer && worldName != null) {
            this.spawnPoint = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        }
    }
    
    public boolean canInviteMore() {
        return true; // 暂时不限制成员数量
    }
    
    public boolean isInvited(UUID playerUuid) {
        Long expireTime = invites.get(playerUuid);
        if (expireTime == null) return false;
        if (System.currentTimeMillis() > expireTime) {
            invites.remove(playerUuid);
            return false;
        }
        return true;
    }
    
    public void invite(UUID playerUuid) {
        invites.put(playerUuid, System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }
    
    public void cancelInvite(UUID playerUuid) {
        invites.remove(playerUuid);
    }
    
    public boolean addMember(UUID playerUuid, String rankStr) {
        if (members.containsKey(playerUuid)) return false;
        
        NationRank rank = NationRank.fromString(rankStr);
        if (rank == null) rank = NationRank.MEMBER;
        
        members.put(playerUuid, new NationMember(
            this.id,
            rank,
            new Date()
        ));
        invites.remove(playerUuid);
        return true;
    }
    
    public boolean removeMember(UUID playerUuid) {
        return members.remove(playerUuid) != null;
    }
    
    public boolean promoteMember(UUID playerUuid, NationRank newRank) {
        NationMember member = members.get(playerUuid);
        if (member == null) return false;
        member.setRank(newRank);
        return true;
    }
    
    public NationRank getMemberRank(UUID playerUuid) {
        if (playerUuid.equals(ownerUUID)) return NationRank.OWNER;
        NationMember member = members.get(playerUuid);
        return member != null ? member.getRank() : null;
    }
    
    public boolean hasPermission(UUID playerUuid, String permission) {
        if (playerUuid.equals(ownerUUID)) {
            return true; // 国主拥有所有权限
        }
        NationRank rank = getMemberRank(playerUuid);
        return rank != null && rank.hasPermission(permission);
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public boolean isMember(UUID playerUuid) {
        return playerUuid.equals(ownerUUID) || members.containsKey(playerUuid);
    }
    
    public boolean isInTerritory(Location location) {
        return territory != null && territory.contains(location);
    }
    
    public int getMaxRadius() {
        // 根据国家等级返回最大领土半径
        return switch (level) {
            case 1 -> 15;  // 30*30
            case 2 -> 25;  // 50*50
            case 3 -> 35;  // 70*70
            case 4 -> 50;  // 100*100
            default -> 15;
        };
    }
    
    public long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public double getBalance() {
        return balance;
    }
    
    public void setBalance(double balance) {
        this.balance = balance;
    }
    
    public Location getSpawnPoint() {
        return spawnPoint;
    }
    
    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
    }
    
    public Territory getTerritory() {
        return territory;
    }
    
    public void setTerritory(Territory territory) {
        this.territory = territory;
    }
    
    public Map<UUID, NationMember> getMembers() {
        return members;
    }
    
    public boolean isLocalServer() {
        return isLocalServer;
    }
    
    public String getServerId() {
        return serverId;
    }
} 