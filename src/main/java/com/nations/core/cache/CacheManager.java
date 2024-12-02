package com.nations.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nations.core.models.Nation;
import com.nations.core.models.Building;

import java.util.concurrent.TimeUnit;

public class CacheManager {
    private static final Cache<Long, Nation> nationCache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(1000)
        .recordStats()
        .build();
        
    private static final Cache<Long, Building> buildingCache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(2000)
        .recordStats()
        .build();

    public static void cacheNation(Nation nation) {
        nationCache.put(nation.getId(), nation);
    }

    public static Nation getCachedNation(long id) {
        return nationCache.getIfPresent(id);
    }

    public static void invalidateNation(long id) {
        nationCache.invalidate(id);
    }

    public static void cacheBuilding(Building building) {
        buildingCache.put(building.getId(), building);
    }

    public static Building getCachedBuilding(long id) {
        return buildingCache.getIfPresent(id);
    }

    public static void invalidateBuilding(long id) {
        buildingCache.invalidate(id);
    }

    public static void clearAll() {
        nationCache.invalidateAll();
        buildingCache.invalidateAll();
    }

    public static void cleanup() {
        nationCache.cleanUp();
        buildingCache.cleanUp();
    }

    public static String getStats() {
        return String.format(
            "Cache Stats:\n" +
            "Nations: %s\n" +
            "Buildings: %s",
            nationCache.stats().toString(),
            buildingCache.stats().toString()
        );
    }

    public static String getNationCacheStats() {
        return nationCache.stats().toString();
    }

    public static String getBuildingCacheStats() {
        return buildingCache.stats().toString();
    }

    public static void cleanupNationCache() {
        nationCache.cleanUp();
    }

    public static void cleanupBuildingCache() {
        buildingCache.cleanUp();
    }

    public static long getNationCacheSize() {
        return nationCache.estimatedSize();
    }

    public static long getBuildingCacheSize() {
        return buildingCache.estimatedSize();
    }
} 