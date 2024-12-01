package com.nations.core.managers;

import com.nations.core.NationsCore;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class WorldManager {
    private final NationsCore plugin;
    private final Logger logger;

    public WorldManager(NationsCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 获取世界，如果世界未加载则尝试加载
     * @param worldName 世界名称
     * @return 世界对象，如果加载失败返回null
     */
    public World getWorld(String worldName) {
        if (worldName == null) return null;
        
        World world = Bukkit.getWorld(worldName);
        if (world != null) return world;

        logger.info("正在加载世界: " + worldName);
        try {
            world = Bukkit.createWorld(new WorldCreator(worldName));
            if (world != null) {
                logger.info("成功加载世界: " + worldName);
                return world;
            }
        } catch (Exception e) {
            logger.warning("加载世界失败: " + worldName);
            logger.warning(e.getMessage());
        }
        return null;
    }

    /**
     * 异步加载世界
     * @param worldName 世界名称
     * @return CompletableFuture<World>
     */
    public CompletableFuture<World> loadWorldAsync(String worldName) {
        return CompletableFuture.supplyAsync(() -> getWorld(worldName));
    }

    /**
     * 创建位置对象，确保世界已加载
     * @param worldName 世界名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @return 位置对象，如果世界加载失败返回null
     */
    public Location createLocation(String worldName, double x, double y, double z) {
        World world = getWorld(worldName);
        return world != null ? new Location(world, x, y, z) : null;
    }

    /**
     * 创建位置对象，确保世界已加载
     * @param worldName 世界名称
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param yaw 偏航角
     * @param pitch 俯仰角
     * @return 位置对象，如果世界加载失败返回null
     */
    public Location createLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        World world = getWorld(worldName);
        return world != null ? new Location(world, x, y, z, yaw, pitch) : null;
    }

    /**
     * 检查位置是否有效（世界是否已加载）
     * @param location 位置对象
     * @return 如果位置有效返回true
     */
    public boolean isLocationValid(Location location) {
        return location != null && location.getWorld() != null;
    }

    /**
     * 尝试修复无效的位置（重新加载世界）
     * @param location 位置对象
     * @param worldName 世界名称
     * @return 修复后的位置对象，如果修复失败返回null
     */
    public Location fixLocation(Location location, String worldName) {
        if (location == null || worldName == null) return null;
        
        World world = getWorld(worldName);
        if (world == null) return null;

        location.setWorld(world);
        return location;
    }
} 