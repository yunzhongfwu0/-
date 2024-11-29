package com.nations.core.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemNameUtil {
    private static final Map<Material, String> ITEM_NAMES = new HashMap<>();
    
    static {
        // 初始化所有物品的中文名称
        for (Material material : Material.values()) {
            if (material.isItem()) {
                String name = formatMaterialName(material.name());
                ITEM_NAMES.put(material, name);
            }
        }
        
        // 手动覆盖一些特殊物品的名称
        ITEM_NAMES.put(Material.DIAMOND, "钻石");
        ITEM_NAMES.put(Material.EMERALD, "绿宝石");
        ITEM_NAMES.put(Material.NETHERITE_INGOT, "下界合金锭");
        ITEM_NAMES.put(Material.GOLD_INGOT, "金锭");
        ITEM_NAMES.put(Material.IRON_INGOT, "铁锭");
        ITEM_NAMES.put(Material.DIAMOND_BLOCK, "钻石块");
        ITEM_NAMES.put(Material.EMERALD_BLOCK, "绿宝石块");
        ITEM_NAMES.put(Material.GOLD_BLOCK, "金块");
        ITEM_NAMES.put(Material.IRON_BLOCK, "铁块");
        ITEM_NAMES.put(Material.ENDER_PEARL, "末影珍珠");
        ITEM_NAMES.put(Material.BLAZE_ROD, "烈焰棒");
        ITEM_NAMES.put(Material.NETHER_STAR, "下界之星");
        ITEM_NAMES.put(Material.STONE, "石头");
        ITEM_NAMES.put(Material.GRASS_BLOCK, "草方块");
        ITEM_NAMES.put(Material.DIRT, "泥土");
        ITEM_NAMES.put(Material.COBBLESTONE, "圆石");
        ITEM_NAMES.put(Material.OAK_LOG, "橡木原木");
        ITEM_NAMES.put(Material.OAK_PLANKS, "橡木木板");
        ITEM_NAMES.put(Material.SPRUCE_LOG, "云杉原木");
        ITEM_NAMES.put(Material.SPRUCE_PLANKS, "云杉木板");
        ITEM_NAMES.put(Material.BIRCH_LOG, "白桦原木");
        ITEM_NAMES.put(Material.BIRCH_PLANKS, "白桦木板");
        ITEM_NAMES.put(Material.JUNGLE_LOG, "丛林原木");
        ITEM_NAMES.put(Material.JUNGLE_PLANKS, "丛林木板");
        ITEM_NAMES.put(Material.ACACIA_LOG, "金合欢原木");
        ITEM_NAMES.put(Material.ACACIA_PLANKS, "金合欢木板");
        ITEM_NAMES.put(Material.DARK_OAK_LOG, "深色橡木原木");
        ITEM_NAMES.put(Material.DARK_OAK_PLANKS, "深色橡木木板");
        ITEM_NAMES.put(Material.COAL, "煤炭");
        ITEM_NAMES.put(Material.COAL_BLOCK, "煤炭块");
        ITEM_NAMES.put(Material.REDSTONE, "红石粉");
        ITEM_NAMES.put(Material.REDSTONE_BLOCK, "红石块");
        ITEM_NAMES.put(Material.LAPIS_LAZULI, "青金石");
        ITEM_NAMES.put(Material.LAPIS_BLOCK, "青金石块");
        // ... 可以继续添加更多常用物品的中文名称
    }
    
    /**
     * 获取物品的中文名称
     */
    public static String getChineseName(Material material) {
        return ITEM_NAMES.getOrDefault(material, formatMaterialName(material.name()));
    }
    
    /**
     * 获取物品的中文名称
     */
    public static String getChineseName(ItemStack item) {
        if (item == null) return "空";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return getChineseName(item.getType());
    }
    
    /**
     * 格式化物品名称
     * 例如: DIAMOND_SWORD -> 钻石剑
     */
    private static String formatMaterialName(String name) {
        // 移除物品名称中的下划线，并将单词首字母大写
        String[] words = name.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());
                }
            }
        }
        
        // 添加一些常见后缀的翻译
        String result = formatted.toString();
        result = result.replace("Block", "块");
        result = result.replace("Ingot", "锭");
        result = result.replace("Sword", "剑");
        result = result.replace("Pickaxe", "镐");
        result = result.replace("Axe", "斧");
        result = result.replace("Shovel", "锹");
        result = result.replace("Hoe", "锄");
        result = result.replace("Helmet", "头盔");
        result = result.replace("Chestplate", "胸甲");
        result = result.replace("Leggings", "护腿");
        result = result.replace("Boots", "靴子");
        
        return result;
    }
    
    /**
     * 添加自定义物品名称
     */
    public static void addCustomName(Material material, String name) {
        ITEM_NAMES.put(material, name);
    }
    
    /**
     * 获取所有已注册的物品名称
     */
    public static Map<Material, String> getAllNames() {
        return new HashMap<>(ITEM_NAMES);
    }
} 