package com.nations.core;

import com.nations.core.managers.ConfigManager;
import com.nations.core.managers.DatabaseManager;
import com.nations.core.managers.NationManager;
import com.nations.core.commands.NationCommand;
import lombok.Getter;
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

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置文件
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();
        
        // 初始化数据库连接
        this.databaseManager = new DatabaseManager(this);
        if (!this.databaseManager.connect()) {
            getLogger().severe("数据库连接失败！插件将被禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化管理器
        this.nationManager = new NationManager(this);
        
        // 注册命令和监听器
        registerCommands();
        registerListeners();
        
        getLogger().info("国家系统插件已成功启动！");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("国家系统插件已关闭！");
    }

    private void registerCommands() {
        getCommand("nation").setExecutor(new NationCommand(this));
    }

    private void registerListeners() {
        // TODO: 实现监听器注册
    }
} 