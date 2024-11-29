package com.nations.core.utils;

import com.nations.core.NationsCore;
import com.nations.core.cache.CacheManager;

import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMonitor {
    private static final AtomicInteger activeAsyncTasks = new AtomicInteger(0);
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    public static void startMonitoring(NationsCore plugin) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024;
            
            plugin.getLogger().info(String.format(
                "Performance Stats:\n" +
                "Memory Usage: %d MB / %d MB\n" +
                "Active Async Tasks: %d\n" +
                "Cache Stats:\n%s",
                usedMemory,
                maxMemory,
                activeAsyncTasks.get(),
                CacheManager.getStats()
            ));
        }, 6000L, 6000L); // 5分钟运行一次
    }
    
    public static void incrementAsyncTasks() {
        activeAsyncTasks.incrementAndGet();
    }
    
    public static void decrementAsyncTasks() {
        activeAsyncTasks.decrementAndGet();
    }
} 