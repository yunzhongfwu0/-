package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.Building;
import com.nations.core.models.Nation;
import com.nations.core.utils.MessageUtil;
import com.nations.core.utils.ItemNameUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WarehouseGUI extends BaseGUI {
    private final Building building;
    private final int size = 6; // 6行的界面
    
    public WarehouseGUI(NationsCore plugin, Player player, Building building) {
        super(plugin, player, "仓库管理", 6);
        this.building = building;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示仓库信息
        int maxStorage = building.getLevel() * 100;
        int currentStorage = 0;
        List<ItemStack> items = new ArrayList<>();
        
        // 获取仓库物品
        Block block = building.getBaseLocation().getBlock();
        if (block.getState() instanceof Container container) {
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    currentStorage++;
                    items.add(item);
                }
            }
        }
        
        // 显示存储信息
        setItem(4, createItem(Material.CHEST,
            "§6仓库信息",
            "§7等级: §f" + building.getLevel(),
            "§7存储空间: §f" + currentStorage + "/" + maxStorage,
            "§7物品种类: §f" + items.size(),
            "",
            "§7点击查看详细信息"
        ), null);
        
        // 显示物品列表
        int slot = 9;
        for (ItemStack item : items) {
            if (slot >= size * 9 - 9) break;
            
            List<String> lore = new ArrayList<>();
            lore.add("§7数量: §f" + item.getAmount());
            if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                lore.addAll(item.getItemMeta().getLore());
            }
            
            setItem(slot++, createItem(
                item.getType(),
                "§f" + ItemNameUtil.getName(item.getType()),
                lore.toArray(new String[0])
            ), null);
        }
        
        // 返回按钮
        setItem(size * 9 - 1, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new BuildingInteractGUI(plugin, p, building).open());
    }
} 