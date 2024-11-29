package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.utils.ChatInputManager;
import com.nations.core.utils.ItemNameUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CostSettingsGUI extends BaseGUI {
    private final boolean isCreationCost;
    private final int level;
    private static final int[] ITEM_SLOTS = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25};
    
    public CostSettingsGUI(NationsCore plugin, Player player, boolean isCreationCost, int level) {
        super(plugin, player, isCreationCost ? "§6创建国家费用设置" : "§6升级至 " + level + " 级费用设置", 6);
        this.isCreationCost = isCreationCost;
        this.level = level;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        String basePath = isCreationCost ? "nations.creation" : "nations.levels." + level + ".upgrade-cost";
        ConfigurationSection costConfig = plugin.getConfig().getConfigurationSection(basePath);
        
        // 显示当前金钱费用
        double money = costConfig.getDouble("money", 0);
        setItem(31, createItem(Material.GOLD_INGOT,
            "§6金钱费用: §f" + money,
            "§7点击修改金钱费用",
            "",
            "§e当前设置: §f" + money + " 金币"
        ), p -> {
            p.closeInventory();
            p.sendMessage("§a请在聊天栏输入新的金钱费用，或输入 'cancel' 取消");
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage("§c已取消操作。");
                    new CostSettingsGUI(plugin, p, isCreationCost, level).open();
                    return;
                }
                try {
                    double newCost = Double.parseDouble(input);
                    if (newCost < 0) {
                        p.sendMessage("§c费用不能为负数！");
                        return;
                    }
                    plugin.getConfig().set(basePath + ".money", newCost);
                    plugin.saveConfig();
                    p.sendMessage("§a成功设置金钱费用为: " + newCost);
                    new CostSettingsGUI(plugin, p, isCreationCost, level).open();
                } catch (NumberFormatException e) {
                    p.sendMessage("§c无效的金额！");
                }
            });
        });
        
        // 物品设置说明
        List<String> explanationLore = new ArrayList<>();
        explanationLore.add("§7将需要的物品放入下方格子");
        explanationLore.add("§7关闭界面时自动保存");
        explanationLore.add("");
        explanationLore.add("§e当前要求:");
        
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        if (items != null && !items.getKeys(false).isEmpty()) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                explanationLore.add("§7- " + ItemNameUtil.getChineseName(material) + ": §f" + amount);
            }
        } else {
            explanationLore.add("§7暂无物品要求");
        }
        
        explanationLore.add("");
        explanationLore.add("§e提示:");
        explanationLore.add("§7- 放入物品设置要求");
        explanationLore.add("§7- 取出物品取消要求");
        explanationLore.add("§7- 数量代表需求数量");
        explanationLore.add("§7- 支持Shift点击和拖拽");
        
        setItem(4, createItem(Material.BOOK,
            "§6物品费用设置",
            explanationLore.toArray(new String[0])
        ), null);
        
        // 加载当前设置的物品
        if (items != null) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                ItemStack item = new ItemStack(material, amount);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§6" + ItemNameUtil.getChineseName(material));
                List<String> lore = new ArrayList<>();
                lore.add("§7需要数量: §f" + amount);
                meta.setLore(lore);
                item.setItemMeta(meta);
                
                for (int slot : ITEM_SLOTS) {
                    if (inventory.getItem(slot) == null) {
                        inventory.setItem(slot, item);
                        break;
                    }
                }
            }
        }
        
        // 保存按钮
        setItem(49, createItem(Material.EMERALD,
            "§a保存设置",
            "§7点击保存当前设置"
        ), p -> saveSettings());
    }
    
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        // 允许在物品槽位和玩家背包之间自由操作
        if (isItemSlot(slot) || event.getClickedInventory() != inventory) {
            // 只阻止非物品槽位的操作
            if (!isItemSlot(slot) && event.getClickedInventory() == inventory) {
                event.setCancelled(true);
            }
            return;
        }
        
        // 其他槽位按原方式处理
        event.setCancelled(true);
        if (event.getClickedInventory() == inventory) {
            if (event.getClick() == ClickType.RIGHT && rightClickHandlers.containsKey(slot)) {
                rightClickHandlers.get(slot).accept((Player) event.getWhoClicked());
            } else if (leftClickHandlers.containsKey(slot)) {
                leftClickHandlers.get(slot).accept((Player) event.getWhoClicked());
            }
        }
    }
    
    @Override
    public void handleDrag(InventoryDragEvent event) {
        // 检查是否所有拖拽的槽位都是允许的
        for (int slot : event.getRawSlots()) {
            if (!isItemSlot(slot) && slot < inventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @Override
    public void handleClose(InventoryCloseEvent event) {
        saveSettings();
    }
    
    private boolean isItemSlot(int slot) {
        for (int itemSlot : ITEM_SLOTS) {
            if (slot == itemSlot) return true;
        }
        return false;
    }
    
    private void saveSettings() {
        String basePath = isCreationCost ? "nations.creation.items" : 
            "nations.levels." + level + ".upgrade-cost.items";
        
        // 清除旧的物品设置
        plugin.getConfig().set(basePath, null);
        
        // 保存新的物品设置
        for (int slot : ITEM_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                plugin.getConfig().set(basePath + "." + item.getType().name(), item.getAmount());
            }
        }
        
        plugin.saveConfig();
        player.sendMessage("§a设置已保存！");
    }
} 