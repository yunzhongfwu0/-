package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SoldierRecruitGUI {
    private final NationsCore plugin;
    private final Player player;
    private final Nation nation;
    
    public SoldierRecruitGUI(NationsCore plugin, Player player, Nation nation) {
        this.plugin = plugin;
        this.player = player;
        this.nation = nation;
    }
    
    public void open() {
        Inventory inv = Bukkit.createInventory(null, 27, "§6招募士兵");
        
        // 获取国家的兵营
        Set<Building> barracks = nation.getBuildingsByType(BuildingType.BARRACKS);
        if (barracks.isEmpty()) {
            player.sendMessage("§c你的国家需要先建造兵营！");
            player.closeInventory();
            return;
        }
        
        // 获取第一个兵营的等级来计算可招募数量
        Building firstBarrack = barracks.iterator().next();
        int maxSoldiers = firstBarrack.getLevel() * 5;
        int currentSoldiers = plugin.getSoldierManager()
            .getSoldiersByPlayer(player.getUniqueId()).size();
        
        // 添加各种士兵类型
        addSoldierType(inv, 10, SoldierType.WARRIOR, currentSoldiers, maxSoldiers);
        addSoldierType(inv, 12, SoldierType.ARCHER, currentSoldiers, maxSoldiers);
        addSoldierType(inv, 14, SoldierType.SUPPORT, currentSoldiers, maxSoldiers);
        addSoldierType(inv, 16, SoldierType.GENERAL, currentSoldiers, maxSoldiers);
        
        // 返回按钮
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§c返回");
        back.setItemMeta(backMeta);
        inv.setItem(26, back);
        
        player.openInventory(inv);
    }
    
    private void addSoldierType(Inventory inv, int slot, SoldierType type, int current, int max) {
        ItemStack item = new ItemStack(getSoldierMaterial(type));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + type.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add("§7" + type.getDescription());
        lore.add("");
        lore.add("§7基础属性:");
        lore.add("§7- 生命值: §f" + type.getBaseHealth());
        lore.add("§7- 攻击力: §f" + type.getBaseAttack());
        lore.add("§7- 防御力: §f" + type.getBaseDefense());
        lore.add("");
        lore.add("§7当前士兵: §f" + current + "/" + max);
        
        if (current < max) {
            lore.add("§e点击招募");
        } else {
            lore.add("§c已达到兵营容量上限");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
    
    private Material getSoldierMaterial(SoldierType type) {
        return switch (type) {
            case WARRIOR -> Material.IRON_SWORD;
            case ARCHER -> Material.BOW;
            case SUPPORT -> Material.GOLDEN_APPLE;
            case GENERAL -> Material.DIAMOND_SWORD;
        };
    }
} 