package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.utils.ChatInputManager;
import com.nations.core.utils.ItemNameUtil;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialSelectorGUI extends BaseGUI {
    private final boolean isCreationCost;
    private final int level;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public MaterialSelectorGUI(NationsCore plugin, Player player, boolean isCreationCost, int level) {
        super(plugin, player, MessageUtil.title("选择物品类型"), 6);
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
            setItem(10 + i + (i/7)*2, createItem(material,
                MessageUtil.title(ItemNameUtil.getChineseName(material)),
                MessageUtil.createLore("物品设置",
                    "点击设置此物品的需求数量",
                    "",
                    "§e提示:",
                    "- 输入数量大于0设置要求",
                    "- 输入0移除要求"
                ).toArray(new String[0])
            ), p -> {
                p.closeInventory();
                p.sendMessage(MessageUtil.tip("请输入物品数量，或输入 'cancel' 取消"));
                ChatInputManager.awaitChatInput(p, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(MessageUtil.info("已取消操作。"));
                        new MaterialSelectorGUI(plugin, p, isCreationCost, level).open();
                        return;
                    }
                    try {
                        int amount = Integer.parseInt(input);
                        if (amount < 0) {
                            p.sendMessage(MessageUtil.error("数量不能为负数！"));
                            return;
                        }
                        String path = isCreationCost ? 
                            "nations.creation.items." + material.name() :
                            "nations.levels." + level + ".upgrade-cost.items." + material.name();
                            
                        if (amount == 0) {
                            plugin.getConfig().set(path, null);
                            p.sendMessage(MessageUtil.success("已移除物品要求: " + ItemNameUtil.getChineseName(material)));
                        } else {
                            plugin.getConfig().set(path, amount);
                            p.sendMessage(MessageUtil.success("成功设置物品要求: " + 
                                ItemNameUtil.getChineseName(material) + " x" + amount));
                        }
                        
                        plugin.saveConfig();
                        new CostSettingsGUI(plugin, p, isCreationCost, level).open();
                    } catch (NumberFormatException e) {
                        p.sendMessage(MessageUtil.error("无效的数字格式！"));
                    }
                });
            });
        }
        
        // 翻页按钮
        if (page > 0) {
            setItem(45, createItem(Material.ARROW,
                MessageUtil.title("上一页"),
                MessageUtil.subtitle("点击查看上一页")
            ), p -> {
                page--;
                initialize();
            });
        }
        
        if (endIndex < materials.size()) {
            setItem(53, createItem(Material.ARROW,
                MessageUtil.title("下一页"),
                MessageUtil.subtitle("点击查看下一页")
            ), p -> {
                page++;
                initialize();
            });
        }
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            MessageUtil.title("返回"),
            MessageUtil.subtitle("点击返回费用设置")
        ), p -> new CostSettingsGUI(plugin, p, isCreationCost, level).open());
    }
} 