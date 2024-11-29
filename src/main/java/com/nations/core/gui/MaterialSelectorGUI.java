package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.utils.ChatInputManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialSelectorGUI extends BaseGUI {
    private final boolean isCreationCost;
    private final int level;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public MaterialSelectorGUI(NationsCore plugin, Player player, boolean isCreationCost, int level) {
        super(plugin, player, "§6选择物品类型", 6);
        this.isCreationCost = isCreationCost;
        this.level = level;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 获取所有可用的物品类型
        List<Material> materials = Arrays.stream(Material.values())
            .filter(Material::isItem)
            .filter(m -> !m.isAir())
            .collect(Collectors.toList());
        
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, materials.size());
        
        // 显示物品
        for (int i = 0; i < endIndex - startIndex; i++) {
            Material material = materials.get(startIndex + i);
            setItem(10 + i + (i/7)*2, new ItemStack(material), p -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天栏输入物品数量，或输入 'cancel' 取消");
                ChatInputManager.awaitChatInput(p, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage("§c已取消操作。");
                        new MaterialSelectorGUI(plugin, p, isCreationCost, level).open();
                        return;
                    }
                    try {
                        int amount = Integer.parseInt(input);
                        if (amount <= 0) {
                            p.sendMessage("§c数量必须大于0！");
                            return;
                        }
                        String path = isCreationCost ? 
                            "nations.creation.items." + material.name() :
                            "nations.levels." + level + ".upgrade-cost.items." + material.name();
                        plugin.getConfig().set(path, amount);
                        plugin.saveConfig();
                        p.sendMessage("§a成功添加物品要求: " + material.name() + " x" + amount);
                        new CostSettingsGUI(plugin, p, isCreationCost, level).open();
                    } catch (NumberFormatException e) {
                        p.sendMessage("§c无效的数字格式！");
                    }
                });
            });
        }
        
        // 翻页按钮
        if (page > 0) {
            setItem(45, createItem(Material.ARROW,
                "§f上一页",
                "§7点击查看上一页"
            ), p -> {
                page--;
                initialize();
            });
        }
        
        if (endIndex < materials.size()) {
            setItem(53, createItem(Material.ARROW,
                "§f下一页",
                "§7点击查看下一页"
            ), p -> {
                page++;
                initialize();
            });
        }
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            "§f返回",
            "§7点击返回"
        ), p -> new CostSettingsGUI(plugin, p, isCreationCost, level).open());
    }
} 