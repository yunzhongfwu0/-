package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TrainingSelectGUI extends BaseGUI {
    private final Soldier soldier;
    
    public TrainingSelectGUI(NationsCore plugin, Player player, Nation nation, Soldier soldier) {
        super(plugin, player, "§6选择训练兵营", 6);  // 54格界面
        this.soldier = soldier;
        initialize(nation);
    }
    
    private void initialize(Nation nation) {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        Set<Building> barracksList = nation.getBuildingsByType(BuildingType.BARRACKS);
        
        int slot = 10;
        for (Building barrack : barracksList) {
            int usedSlots = plugin.getSoldierManager().getTrainingSlots(barrack);
            double maxSlotsDouble = barrack.getBonuses().getOrDefault("training_slots", 2.0);
            int maxSlots = (int)Math.floor(maxSlotsDouble);
            double bonus = barrack.getBonuses().getOrDefault("training_bonus", 0.0) * 100;
            double speedReduction = barrack.getBonuses().getOrDefault("training_speed", 0.0) * 100;
            
            int baseTime = 15 + (soldier.getLevel() - 1) * 5;
            int actualTime = (int)(baseTime * (1 - barrack.getBonuses().getOrDefault("training_speed", 0.0)));
            
            List<String> lore = new ArrayList<>();
            lore.add("§7等级: §f" + barrack.getLevel());
            lore.add("§7训练位: §f" + usedSlots + "/" + maxSlots);
            lore.add("§7训练加成: §f+" + String.format("%.1f%%", bonus));
            lore.add("§7训练速度: §f-" + String.format("%.1f%%", speedReduction));
            lore.add("");
            lore.add("§7训练时间: §f" + actualTime + "分钟");
            
            if (usedSlots < maxSlots) {
                lore.add("§e点击开始训练");
                setItem(slot, createItem(Material.IRON_BLOCK,
                    "§6兵营 #" + barrack.getId(),
                    lore.toArray(new String[0])
                ), p -> {
                    if (plugin.getSoldierManager().startTraining(soldier, barrack)) {
                        p.sendMessage("§a开始训练士兵 " + soldier.getName());
                        p.closeInventory();
                    } else {
                        p.sendMessage("§c训练失败！训练位已满。");
                    }
                });
            } else {
                lore.add("§c训练位已满");
                setItem(slot, createItem(Material.RED_STAINED_GLASS,
                    "§c兵营 #" + barrack.getId(),
                    lore.toArray(new String[0])
                ), null);
            }
            
            slot++;
            if ((slot + 1) % 9 == 0) slot += 2;
        }
        
        setItem(49, createItem(Material.BARRIER, "§c返回"), 
            p -> new SoldierManageGUI(plugin, p, nation).open());
    }
} 