package com.nations.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

public class MessageUtil {
    
    private static final String LINE = "§7§m                                                ";
    private static final String SMALL_LINE = "§7§m                    ";
    
    /**
     * 创建一个标准的成功消息
     */
    public static String success(String message) {
        return "\n" + LINE + "\n" +
               "§a✔ §f" + message + "\n" +
               LINE;
    }
    
    /**
     * 创建一个标准的错误消息
     */
    public static String error(String message) {
        return "\n" + LINE + "\n" +
               "§c✘ §f" + message + "\n" +
               LINE;
    }
    
    /**
     * 创建一个标准的警告消息
     */
    public static String warn(String message) {
        return "\n" + LINE + "\n" +
               "§e⚠ §f" + message + "\n" +
               LINE;
    }
    
    /**
     * 创建一个标准的信息消息
     */
    public static String info(String message) {
        return "\n" + SMALL_LINE + "\n" +
               "§b✦ §f" + message + "\n" +
               SMALL_LINE;
    }
    
    /**
     * 创建一个标准的提示消息
     */
    public static String tip(String message) {
        return "§d✎ §f" + message;
    }
    
    /**
     * 创建一个标准的操作消息
     */
    public static String action(String message) {
        return "§6➤ §f" + message;
    }
    
    /**
     * 创建一个标准的广播消息
     */
    public static String broadcast(String message) {
        return "\n" + LINE + "\n" +
               "§e✉ §6国家公告\n" +
               "§f" + message + "\n" +
               LINE;
    }
    
    /**
     * 创建一个标准的标题
     */
    public static String title(String title) {
        return "§6§l" + title + " §r";
    }
    
    /**
     * 创建一个标准的子标题
     */
    public static String subtitle(String subtitle) {
        return "§7" + subtitle;
    }
    
    /**
     * 创建一个带有框的消息
     */
    public static String box(String title, String... messages) {
        StringBuilder sb = new StringBuilder("\n" + LINE + "\n");
        sb.append("§6✦ §l").append(title).append("\n");
        for (String msg : messages) {
            sb.append("§f").append(msg).append("\n");
        }
        sb.append(LINE);
        return sb.toString();
    }
    
    /**
     * 创建一个简单的描述列表
     */
    public static List<String> createSimpleLore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty()) {
                lore.add("");
            } else {
                lore.add("§7" + line);
            }
        }
        return lore;
    }
    
    /**
     * 创建一个带有标题的描述列表
     */
    public static List<String> createLore(String title, String... descriptions) {
        List<String> lore = new ArrayList<>();
        lore.add("§6" + title);
        lore.add("§8" + SMALL_LINE);
        for (String desc : descriptions) {
            if (desc.isEmpty()) {
                lore.add("");
            } else {
                lore.add("§7" + desc);
            }
        }
        return lore;
    }
    
    /**
     * 创建一个带有要求的描述列表
     */
    public static List<String> createRequirementLore(String title, String... requirements) {
        List<String> lore = new ArrayList<>();
        lore.add("§6" + title);
        lore.add("§8" + SMALL_LINE);
        lore.add("§e要求:");
        for (String req : requirements) {
            lore.add("§7▪ " + req);
        }
        return lore;
    }
    
    /**
     * 创建一个带有状态的描述列表
     */
    public static List<String> createStatusLore(String title, String... status) {
        List<String> lore = new ArrayList<>();
        lore.add("§6" + title);
        lore.add("§8" + SMALL_LINE);
        lore.add("§e当前状态:");
        for (String stat : status) {
            lore.add("§7▪ " + stat);
        }
        return lore;
    }
    
    /**
     * 创建一个带有操作说明的描述列表
     */
    public static List<String> createActionLore(String title, String... actions) {
        List<String> lore = new ArrayList<>();
        lore.add("§6" + title);
        lore.add("§8" + SMALL_LINE);
        for (String action : actions) {
            if (action.startsWith("左键")) {
                lore.add("§a➜ " + action);
            } else if (action.startsWith("右键")) {
                lore.add("§c➜ " + action);
            } else if (action.startsWith("Shift")) {
                lore.add("§e➜ " + action);
            } else {
                lore.add("§7▪ " + action);
            }
        }
        return lore;
    }
    
    /**
     * 创建一个进度条
     */
    public static String createProgressBar(double current, double max, int length) {
        StringBuilder bar = new StringBuilder("§8[");
        double progress = current / max;
        int filledLength = (int) (length * progress);
        
        for (int i = 0; i < length; i++) {
            if (i < filledLength) {
                bar.append("§a■");
            } else {
                bar.append("§7■");
            }
        }
        
        bar.append("§8] §f").append((int)(progress * 100)).append("%");
        return bar.toString();
    }
    
    /**
     * 创建一个带有统计信息的描述列表
     */
    public static List<String> createStatisticsLore(String title, String... statistics) {
        List<String> lore = new ArrayList<>();
        lore.add("§6" + title);
        lore.add("§8" + SMALL_LINE);
        lore.add("§e统计信息:");
        for (String stat : statistics) {
            String[] parts = stat.split(":");
            if (parts.length == 2) {
                lore.add("§7" + parts[0] + ": §f" + parts[1].trim());
            } else {
                lore.add("§7▪ " + stat);
            }
        }
        return lore;
    }
    
    public static String formatResourceRequirement(Material material, int required, int has) {
        String itemName = ItemNameUtil.getName(material);
        return "§7- " + itemName + ": §f需要 " + required + "，拥有 " + has;
    }
    
    /**
     * 格式化多个资源的需求列表
     * @param requirements Map<Material, Integer> 资源需求，key为物品类型，value为需求数量
     * @param inventory Map<Material, Integer> 玩家拥有的资源，key为物品类型，value为拥有数量
     * @return 格式化后的资源需求列表字符串
     */
    public static String formatResourceRequirements(Map<Material, Integer> requirements, Map<Material, Integer> inventory) {
        StringBuilder sb = new StringBuilder();
        requirements.forEach((material, required) -> {
            int has = inventory.getOrDefault(material, 0);
            sb.append(formatResourceRequirement(material, required, has)).append("\n");
        });
        return sb.toString();
    }
} 