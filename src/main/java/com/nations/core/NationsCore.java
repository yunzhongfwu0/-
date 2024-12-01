package com.nations.core;

import com.nations.core.managers.ConfigManager;
import com.nations.core.managers.DatabaseManager;
import com.nations.core.managers.NationManager;
import com.nations.core.commands.NationCommand;
import com.nations.core.cache.CacheManager;
import com.nations.core.commands.NationAdminCommand;
import com.nations.core.listeners.TerritoryProtectionListener;
import com.nations.core.listeners.ChatInputListener;
import com.nations.core.listeners.GUIListener;
import com.nations.core.utils.ChatInputManager;
import com.nations.core.utils.PerformanceMonitor;
import com.nations.core.utils.TaskManager;
import com.nations.core.hooks.NationsPlaceholder;
import com.nations.core.managers.BuildingManager;
import com.nations.core.managers.WorldManager;
import com.nations.core.utils.ItemNameUtil;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NationsCore extends JavaPlugin {
    
    @Getter
    private static NationsCore instance;
    @Getter
    private ConfigManager configManager;
    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private NationManager nationManager;
    @Getter
    private Economy economy;
    private BuildingManager buildingManager;
    @Getter
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化性能监控
        PerformanceMonitor.startMonitoring(this);
        
        // 保存默认配置文件
        saveDefaultConfig();
        saveResource("zh_cn.json", false);  // 只保存语言文件
        
        // 初始化配置文件
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();
        
        // 初始化数据库连接池
        this.databaseManager = new DatabaseManager(this);
        if (!this.databaseManager.connect()) {
            getLogger().severe("数据库连接失败！插件将被禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化Vault经济
        if (!setupEconomy()) {
            getLogger().severe("未找到Vault经济插件！插件将被禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化世界管理器（移到前面）
        this.worldManager = new WorldManager(this);
        
        // 初始化其他管理器
        this.nationManager = new NationManager(this);
        this.buildingManager = new BuildingManager(this);
        
        // 初始化任务管理器
        TaskManager.init(this);
        
        // 初始化聊天输入管理器
        ChatInputManager.init(this);
        
        // 初始化物品名称工具
        ItemNameUtil.init(this);
        
        // 注册命令和监听器
        registerCommands();
        registerListeners();
        
        // 注册 PlaceholderAPI 扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NationsPlaceholder(this).register();
            getLogger().info("PlaceholderAPI 扩展已注册！");
        }
        
        getLogger().info("国家系统插件已成功启动！");
    }

    @Override
    public void onDisable() {
        // 关闭任务管理器
        TaskManager.shutdown();
        
        // 清理缓存
        CacheManager.cleanup();
        
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        getLogger().info("国家系统插件已关闭！");
    }

    private void registerCommands() {
        getCommand("nation").setExecutor(new NationCommand(this));
        getCommand("nadmin").setExecutor(new NationAdminCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new TerritoryProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(), this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getVaultEconomy() {
        return economy;
    }

    public BuildingManager getBuildingManager() {
        return buildingManager;
    }
} 