package com.nations.core.managers;

import com.nations.core.NationsCore;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final NationsCore plugin;
    @Getter private FileConfiguration config;
    
    public ConfigManager(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfigs() {
        // 保存默认配置
        plugin.saveDefaultConfig();
        
        // 加载配置
        config = plugin.getConfig();
    }
    
    public ConfigurationSection getDatabase() {
        return config.getConfigurationSection("database");
    }
} 