package com.nations.core.managers;

import com.nations.core.NationsCore;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

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
    
    // 添加获取创建国家费用的方法
    public ConfigurationSection getCreateNationCost() {
        return config.getConfigurationSection("nations.creation.items");
    }
    
    // 添加获取升级费用的方法
    public Map<Material, Integer> getUpgradeCost(int level) {
        ConfigurationSection costSection = config.getConfigurationSection("nations.levels." + level + ".upgrade-cost.items");
        if (costSection == null) {
            return new HashMap<>();
        }
        
        Map<Material, Integer> costs = new HashMap<>();
        for (String key : costSection.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                int amount = costSection.getInt(key);
                costs.put(material, amount);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("配置文件中存在无效的物品ID: " + key);
            }
        }
        return costs;
    }
    
    // 获取创建国家所需金币
    public double getCreateNationMoney() {
        return config.getDouble("nations.creation.money", 0);
    }
    
    // 获取升级所需金币
    public double getUpgradeMoney(int level) {
        return config.getDouble("nations.levels." + level + ".upgrade-cost.money", 0);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }

    public ConfigurationSection getDatabase() {
        return config.getConfigurationSection("database");
    }
    
} 