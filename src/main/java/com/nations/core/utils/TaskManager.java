package com.nations.core.utils;

import com.nations.core.NationsCore;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TaskManager {
    private static final Executor ASYNC_EXECUTOR = Executors.newFixedThreadPool(3);
    private static NationsCore plugin;
    
    public static void init(NationsCore instance) {
        plugin = instance;
    }
    
    public static <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ASYNC_EXECUTOR);
    }
    
    public static void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
    
    public static <T> CompletableFuture<T> runAsyncThenSync(
            Supplier<T> asyncSupplier, 
            Consumer<T> syncConsumer) {
        return CompletableFuture.supplyAsync(asyncSupplier, ASYNC_EXECUTOR)
            .thenApplyAsync(result -> {
                runSync(() -> syncConsumer.accept(result));
                return result;
            });
    }
    
    public static void runSyncDelayed(Runnable task, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLater(plugin, delayTicks);
    }
    
    public static void runAsyncDelayed(Runnable task, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                CompletableFuture.runAsync(task, ASYNC_EXECUTOR);
            }
        }.runTaskLater(plugin, delayTicks);
    }
    
    public static void shutdown() {
        if (ASYNC_EXECUTOR instanceof java.util.concurrent.ExecutorService service) {
            service.shutdown();
            try {
                if (!service.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    service.shutdownNow();
                }
            } catch (InterruptedException e) {
                service.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
} 