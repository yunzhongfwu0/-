package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.commands.NationCommand;
import com.nations.core.utils.ChatInputManager;
import com.nations.core.utils.ItemNameUtil;
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
        
        if (items != null && !items.getKeys(false).isEmpty()) {
            lore.add("");
            lore.add("§e所需物品:");
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int required = items.getInt(itemName);
                int playerHas = countPlayerItems(player, material);
                String chineseName = ItemNameUtil.getChineseName(material);
                lore.add("§7" + chineseName + ": §f" + required + 
                    (playerHas >= required ? " §a✔" : " §c✘ (" + playerHas + ")"));
            }
        }
        
        // 创建国家按钮
        setItem(13, createItem(Material.EMERALD,
            "§6点击创建国家",
            lore.toArray(new String[0])
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请在聊天栏输入国家名称，或输入 'cancel' 取消");
            
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消创建国家。");
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
} 