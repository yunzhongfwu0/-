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
import com.nations.core.managers.NPCManager;
import com.nations.core.listeners.CitizensLoadListener;
import com.nations.core.listeners.NPCInteractListener;
import com.nations.core.managers.NPCSkillManager;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class NationsCore extends JavaPlugin {
    
    private static NationsCore instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private NationManager nationManager;
    private Economy economy;
    private BuildingManager buildingManager;
    private WorldManager worldManager;
    private NPCManager npcManager;
    private NPCSkillManager npcSkillManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化性能监控
        PerformanceMonitor.startMonitoring(this);
        
        // 保存默认配置文件
        saveDefaultConfig();
        saveResource("zh_cn.json", false);
        
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
        
        // 初始化世界管理器
        this.worldManager = new WorldManager(this);
        
        // 初始化其他管理器
        this.nationManager = new NationManager(this);
        this.buildingManager = new BuildingManager(this);
        this.npcManager = new NPCManager(this);
        this.npcSkillManager = new NPCSkillManager(this);
        this.npcSkillManager.createTables();
        this.npcSkillManager.loadSkills();
        
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
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new NationsPlaceholder(this).register();
            getLogger().info("PlaceholderAPI 扩展已注册！");
        }
        
        // 注册Citizens加载监听器
        getServer().getPluginManager().registerEvents(
            new CitizensLoadListener(this), 
            this
        );
        
        getLogger().info("国家系统插件已成功启动！");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.onDisable();
        }
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
        getServer().getPluginManager().registerEvents(new NPCInteractListener(this), this);
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

    public static NationsCore getInstance() {
        return instance;
    }

    /**
     * 获取 Vault 经济实例
     */
    public Economy getVaultEconomy() {
        return economy;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NationManager getNationManager() {
        return nationManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public BuildingManager getBuildingManager() {
        return buildingManager;
    }

    public NPCSkillManager getNPCSkillManager() {
        return npcSkillManager;
    }
} 