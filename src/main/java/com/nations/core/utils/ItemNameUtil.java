package com.nations.core.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ItemNameUtil {
    private static final Map<Material, String> ITEM_NAMES = new HashMap<>();
    private static final Map<String, String> LANG_MAP = new HashMap<>();
    
    public static void init(JavaPlugin plugin) {
        try {
            File langFile = new File(plugin.getDataFolder(), "zh_cn.json");
            if (!langFile.exists()) {
                // 尝试从 jar 中复制文件
                try {
                    plugin.saveResource("zh_cn.json", false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无法找到语言文件，将使用备用名称");
                    loadFallbackNames();
                    return;
                }
            }
            
            try (FileReader reader = new FileReader(langFile, StandardCharsets.UTF_8)) {
                Map<String, String> langMap = new Gson().fromJson(
                    reader,
                    new TypeToken<Map<String, String>>(){}.getType()
                );
                LANG_MAP.putAll(langMap);
                
                // 初始化所有物品的中文名称
                for (Material material : Material.values()) {
                    String materialName = material.name().toLowerCase();
                    
                    // 尝试获取物品名称
                    String itemKey = "item.minecraft." + materialName;
                    String blockKey = "block.minecraft." + materialName;
                    
                    String name = LANG_MAP.get(itemKey);
                    if (name == null) {
                        name = LANG_MAP.get(blockKey);
                    }
                    
                    if (name != null) {
                        ITEM_NAMES.put(material, name);
                    } else {
                        // 如果找不到翻译，使用默认的格式化名称
                        name = formatMaterialName(material.name());
                        ITEM_NAMES.put(material, name);
                    }
                }
                
                plugin.getLogger().info("成功加载 " + ITEM_NAMES.size() + " 个物品的中文名称");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载物品语言文件失败: " + e.getMessage());
            loadFallbackNames();
        }
    }
    
    private static String formatMaterialName(String name) {
        // 将大写下划线格式转换为更友好的显示格式
        String[] words = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }
    
    private static void loadFallbackNames() {
        // 基础物品的备用名称
        ITEM_NAMES.put(Material.DIAMOND, "钻石");
        ITEM_NAMES.put(Material.GOLD_BLOCK, "金块");
        ITEM_NAMES.put(Material.IRON_BLOCK, "铁块");
        ITEM_NAMES.put(Material.EMERALD_BLOCK, "绿宝石块");
        ITEM_NAMES.put(Material.CHEST, "箱子");
        ITEM_NAMES.put(Material.CRAFTING_TABLE, "工作台");
        // ... 添加其他基础物品
    }
    
    public static String getName(Material material) {
        return ITEM_NAMES.getOrDefault(material, material.name());
    }
    
    // 添加这个方法来解决编译错误
    public static String getChineseName(Material material) {
        return getName(material);
    }
    
    public static String getResourceList(Map<Material, Integer> resources) {
        StringBuilder sb = new StringBuilder();
        resources.forEach((material, amount) -> 
            sb.append("§7- ").append(getName(material)).append(": §f").append(amount).append("\n")
        );
        return sb.toString();
    }
} 