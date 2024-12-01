package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.commands.NationCommand;
import com.nations.core.utils.ChatInputManager;
import com.nations.core.utils.ItemNameUtil;
import com.nations.core.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CreateNationGUI extends BaseGUI {
    
    private final NationCommand nationCommand;
    
    public CreateNationGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6创建国家", 3);
        this.nationCommand = new NationCommand(plugin);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 获取创建费用配置
        ConfigurationSection costConfig = plugin.getConfig().getConfigurationSection("nations.creation");
        double money = costConfig.getDouble("money", 0);
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        
        // 创建说明文本
        List<String> lore = new ArrayList<>();
        lore.add("§b默认选取以你为中心的 30*30 区域 为国家领土");
        lore.add("§7在聊天栏输入国家名称");
        lore.add("§7输入 'cancel' 取消操作");
        lore.add("");
        lore.add("§e要求:");
        lore.add("§7- 名称长度: " + plugin.getConfig().getInt("nations.min-name-length") + 
            " - " + plugin.getConfig().getInt("nations.max-name-length") + " 个字符");
        lore.add("§7- 允许中文、字母、数字和下划线");
        lore.add("");
        lore.add("§e创建费用:");
        lore.add("§7金币: §f" + money + (plugin.getVaultEconomy().has(player, money) ? " §a✔" : " §c✘"));
        
        // 使用 final 修饰
        final boolean[] hasEnoughResources = {plugin.getVaultEconomy().has(player, money)};
        
        if (items != null && !items.getKeys(false).isEmpty()) {
            lore.add("");
            lore.add("§e所需物品:");
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int required = items.getInt(itemName);
                int playerHas = countPlayerItems(player, material);
                String chineseName = ItemNameUtil.getName(material);
                lore.add("§7" + chineseName + ": §f" + required + 
                    (playerHas >= required ? " §a✔" : " §c✘ (" + playerHas + ")"));
                if (playerHas < required) {
                    hasEnoughResources[0] = false;
                }
            }
        }
        
        // 创建国家按钮
        setItem(13, createItem(Material.EMERALD,
            hasEnoughResources[0] ? "§a点击创建国家" : "§c资源不足",
            lore.toArray(new String[0])
        ), p -> {
            if (!hasEnoughResources[0]) {
                p.sendMessage(MessageUtil.error("创建失败！资源不足"));
                return;
            }
            p.closeInventory();
            p.sendMessage("§a请在聊天栏输入国家名称，或输入 'cancel' 取消");
            
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage(MessageUtil.info("已取消创建国家。"));
                    return;
                }
                
                // 检查名称长度
                int minLength = plugin.getConfig().getInt("nations.min-name-length", 2);
                int maxLength = plugin.getConfig().getInt("nations.max-name-length", 16);
                if (input.length() < minLength || input.length() > maxLength) {
                    p.sendMessage(MessageUtil.error("国家名称长度必须在 " + minLength + " 到 " + maxLength + " 个字符之间！"));
                    new CreateNationGUI(plugin, p).open();
                    return;
                }
                
                // 检查名称格式
                String nameRegex = plugin.getConfig().getString("nations.name-regex", "^[\u4e00-\u9fa5a-zA-Z0-9_]+$");
                if (!input.matches(nameRegex)) {
                    p.sendMessage(MessageUtil.error("国家名称只能包含中文、字母、数字和下划线！"));
                    new CreateNationGUI(plugin, p).open();
                    return;
                }
                
                nationCommand.handleCreate(p, new String[]{"create", input});
            });
        });
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
    
    private int countPlayerItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    private ItemStack createCostItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7创建国家需要以下资源：");
        lore.add("");
        
        // 获取金币费用
        double money = plugin.getConfigManager().getCreateNationMoney();
        lore.add("§7金币: §f" + money + 
            (plugin.getVaultEconomy().has(player, money) ? " §a✔" : " §c✘"));
        lore.add("");
        
        // 获取物品费用
        ConfigurationSection items = plugin.getConfigManager().getCreateNationCost();
        boolean hasAll = plugin.getVaultEconomy().has(player, money);
        
        if (items != null && !items.getKeys(false).isEmpty()) {
            lore.add("§7所需物品：");
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int required = items.getInt(itemName);
                int playerHas = countPlayerItems(player, material);
                String chineseName = ItemNameUtil.getName(material);
                lore.add("§7" + chineseName + ": §f" + required + 
                    (playerHas >= required ? " §a✔" : " §c✘ (" + playerHas + ")"));
                if (playerHas < required) hasAll = false;
            }
        }
        
        return createItem(Material.EMERALD,
            hasAll ? "§a点击创建国家" : "§c资源不足",
            lore.toArray(new String[0]));
    }
} 