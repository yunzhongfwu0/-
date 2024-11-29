package com.nations.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nations.core.models.Nation;
import com.nations.core.models.Transaction;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    // 国家缓存 - 较长时间缓存，因为数据变化不频繁
    private static final Cache<String, Nation> NATION_CACHE = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .recordStats()
        .build();
    
    // 物品缓存 - GUI中频繁使用的物品
    private static final Cache<String, ItemStack> ITEM_CACHE = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .weakKeys()
        .recordStats()
        .build();
    
    // 交易记录缓存 - 短时间缓存，因为数据经常变化
    private static final Cache<String, List<Transaction>> TRANSACTION_CACHE = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .recordStats()
        .build();
    
    // 权限结果缓存 - 非常短时间缓存，因为需要频繁检查
    private static final Cache<String, Boolean> PERMISSION_CACHE = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .recordStats()
        .build();
    
    public static void cacheNation(String key, Nation nation) {
        NATION_CACHE.put(key, nation);
    }
    
    public static Nation getCachedNation(String key) {
        return NATION_CACHE.getIfPresent(key);
    }
    
    public static void cacheItem(String key, ItemStack item) {
        ITEM_CACHE.put(key, item.clone());
    }
    
    public static ItemStack getCachedItem(String key) {
        ItemStack item = ITEM_CACHE.getIfPresent(key);
        return item != null ? item.clone() : null;
    }
    
    public static void cacheTransactions(String key, List<Transaction> transactions) {
        TRANSACTION_CACHE.put(key, transactions);
    }
    
    public static List<Transaction> getCachedTransactions(String key) {
        return TRANSACTION_CACHE.getIfPresent(key);
    }
    
    public static void cachePermissionResult(String key, boolean result) {
        PERMISSION_CACHE.put(key, result);
    }
    
    public static Boolean getCachedPermissionResult(String key) {
        return PERMISSION_CACHE.getIfPresent(key);
    }
    
    public static void cleanup() {
        NATION_CACHE.cleanUp();
        ITEM_CACHE.cleanUp();
        TRANSACTION_CACHE.cleanUp();
        PERMISSION_CACHE.cleanUp();
    }
    
    public static void invalidateAll() {
        NATION_CACHE.invalidateAll();
        ITEM_CACHE.invalidateAll();
        TRANSACTION_CACHE.invalidateAll();
        PERMISSION_CACHE.invalidateAll();
    }
    
    public static String getStats() {
        return String.format(
            "Cache Stats:\n" +
            "Nations: %s\n" +
            "Items: %s\n" +
            "Transactions: %s\n" +
            "Permissions: %s",
            NATION_CACHE.stats(),
            ITEM_CACHE.stats(),
            TRANSACTION_CACHE.stats(),
            PERMISSION_CACHE.stats()
        );
    }
} 