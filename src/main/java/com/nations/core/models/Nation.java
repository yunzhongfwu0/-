package com.nations.core.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.nations.core.NationsCore;

import java.util.*;

public class Nation {
    private final long id;
    private String name;
    private UUID ownerUUID;
    private int level;
    private double balance;
    private Location spawnPoint;
    private String spawnWorldName;
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw, spawnPitch;
    private boolean hasSpawnCoordinates = false;
    private String serverId;
    private int serverPort;
    private boolean isLocalServer;
    private Territory territory;
    private final Map<UUID, NationMember> members = new HashMap<>();
    private final Set<UUID> invites = new HashSet<>();
    private final long createdTime;
    private Set<Building> buildings = new HashSet<>();
    
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
        return invites.contains(playerUuid);
    }
    
    public void addInvite(UUID playerUuid) {
        invites.add(playerUuid);
    }
    
    public void removeInvite(UUID playerUuid) {
        invites.remove(playerUuid);
    }
    
    public void clearInvites() {
        invites.clear();
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
        if (ownerUUID.equals(playerUuid)) {
            return NationRank.OWNER;
        }
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
        return ownerUUID.equals(playerUuid) || members.containsKey(playerUuid);
    }
    
    public boolean isInTerritory(Location location) {
        return territory != null && territory.contains(location);
    }
    
    public int getMaxRadius() {
        // 根据国家等级返回最大领土径
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
        // 如果传送点为空但有世界名称和坐标，尝试重新加载
        if (spawnPoint == null && spawnWorldName != null && hasSpawnCoordinates) {
            spawnPoint = NationsCore.getInstance().getWorldManager()
                .createLocation(spawnWorldName, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
            
            if (spawnPoint != null) {
                NationsCore.getInstance().getLogger().info(
                    "已为国家 " + name + " 重新加载传送点: " + 
                    String.format("%.1f, %.1f, %.1f in %s", spawnX, spawnY, spawnZ, spawnWorldName)
                );
            }
        }
        return spawnPoint;
    }
    
    public void setSpawnPoint(Location location) {
        if (location != null) {
            this.spawnWorldName = location.getWorld().getName();
            this.spawnX = location.getX();
            this.spawnY = location.getY();
            this.spawnZ = location.getZ();
            this.spawnYaw = location.getYaw();
            this.spawnPitch = location.getPitch();
            this.hasSpawnCoordinates = true;
            this.spawnPoint = location.clone();
        }
    }
    
    /**
     * 检查传送点是否有效
     */
    public boolean isSpawnPointValid() {
        return NationsCore.getInstance().getWorldManager()
            .isLocationValid(getSpawnPoint());
    }
    
    /**
     * 尝试修复无效的传送点
     */
    public boolean fixSpawnPoint() {
        if (!hasSpawnCoordinates || spawnWorldName == null) return false;
        
        Location fixed = NationsCore.getInstance().getWorldManager()
            .createLocation(spawnWorldName, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
        
        if (fixed != null) {
            this.spawnPoint = fixed;
            return true;
        }
        return false;
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
    
    /**
     * 获取出生点世界名称
     */
    public String getSpawnWorldName() {
        return spawnWorldName;
    }
    
    public void setSpawnWorldName(String worldName) {
        this.spawnWorldName = worldName;
    }
    
    /**
     * 设置传送点坐标（用于世界未加载时）
     */
    public void setSpawnCoordinates(double x, double y, double z, float yaw, float pitch) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.spawnYaw = yaw;
        this.spawnPitch = pitch;
        this.hasSpawnCoordinates = true;
    }
    
    // 添加这些getter方法用于保存数据
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public double getSpawnZ() { return spawnZ; }
    public float getSpawnYaw() { return spawnYaw; }
    public float getSpawnPitch() { return spawnPitch; }
    public boolean hasSpawnCoordinates() { return hasSpawnCoordinates; }
    
    public boolean hasBuilding(BuildingType type) {
        return buildings.stream()
            .anyMatch(b -> b.getType() == type);
    }
    
    public Building getBuilding(BuildingType type) {
        return buildings.stream()
            .filter(b -> b.getType() == type)
            .findFirst()
            .orElse(null);
    }
    
    public boolean hasBuildingLevel(BuildingType type, int level) {
        Building building = getBuilding(type);
        return building != null && building.getLevel() >= level;
    }
    
    public void addBuilding(Building building) {
        buildings.add(building);
    }
    
    public void removeBuilding(Building building) {
        buildings.remove(building);
    }
    
    // 获取建筑加成
    public double getBuildingBonus(String bonusType) {
        return buildings.stream()
            .mapToDouble(b -> b.getBonuses().getOrDefault(bonusType, 0.0))
            .sum();
    }
    
    public Set<Building> getBuildings() {
        return buildings;
    }
    
    public int getMaxMembers() {
        // 使用 NationsCore.getInstance() 获取插件实例
        NationsCore plugin = NationsCore.getInstance();
        
        // 从基础配置获取等级对应的成员上限
        int baseLimit = plugin.getConfig().getInt("nations.levels." + level + ".max-members", 10);
        
        // 计算建筑加成
        int buildingBonus = (int)getBuildingBonus("max_members");
        
        return baseLimit + buildingBonus;
    }
    
    public int getCurrentMembers() {
        return members.size() + 1; // +1 是因为包括国主
    }
} 