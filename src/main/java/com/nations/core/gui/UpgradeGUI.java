package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ItemNameUtil;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class UpgradeGUI extends BaseGUI {
    private final Nation nation;
    
    public UpgradeGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6升级国家 - " + nation.getName(), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        int nextLevel = nation.getLevel() + 1;
        ConfigurationSection levelConfig = plugin.getConfig().getConfigurationSection("nations.levels." + nextLevel);
        ConfigurationSection costConfig = levelConfig.getConfigurationSection("upgrade-cost");
        
        // 显示当前等级信息
        setItem(11, createItem(Material.EXPERIENCE_BOTTLE,
            "§6当前等级: §f" + nation.getLevel(),
            "§7" + plugin.getConfig().getString("nations.levels." + nation.getLevel() + ".name"),
            "",
            "§7当前成员上限: §f" + plugin.getConfig().getInt("nations.levels." + nation.getLevel() + ".max-members"),
            "§7当前领地范围: §f" + plugin.getConfig().getInt("nations.levels." + nation.getLevel() + ".max-territory") + "x" + 
                plugin.getConfig().getInt("nations.levels." + nation.getLevel() + ".max-territory")
        ), null);
        
        // 显示下一等级信息
        setItem(15, createItem(Material.NETHER_STAR,
            "§6下一等级: §f" + nextLevel,
            "§7" + levelConfig.getString("name"),
            "",
            "§7成员上限: §f" + levelConfig.getInt("max-members"),
            "§7领地范围: §f" + levelConfig.getInt("max-territory") + "x" + levelConfig.getInt("max-territory")
        ), null);
        
        // 显示升级费用
        List<String> costLore = new ArrayList<>();
        costLore.add("§7升级费用:");
        double money = costConfig.getDouble("money", 0);
        if (money > 0) {
            costLore.add("§7- 金币: §f" + money + (nation.getBalance() >= money ? " §a✔" : " §c✘"));
        }
        
        ConfigurationSection items = costConfig.getConfigurationSection("items");
        if (items != null) {
            for (String itemName : items.getKeys(false)) {
                Material material = Material.valueOf(itemName);
                int amount = items.getInt(itemName);
                boolean hasEnough = plugin.getNationManager().hasEnoughItems(player, material, amount);
                costLore.add("§7- " + ItemNameUtil.getName(material) + ": §f" + amount + (hasEnough ? " §a✔" : " §c✘"));
            }
        }
        
        costLore.add("");
        costLore.add("§7点击升级");
        
        // 升级按钮
        setItem(13, createItem(Material.EMERALD,
            "§6升级国家",
            costLore.toArray(new String[0])
        ), p -> {
            if (plugin.getNationManager().canUpgradeNation(nation, p)) {
                if (plugin.getNationManager().upgradeNation(nation, p)) {
                    p.sendMessage("§a成功将国家升级到 " + nextLevel + " 级！");
                    p.sendMessage("§a新的成员上限: " + levelConfig.getInt("max-members"));
                    p.sendMessage("§a新的领地范围: " + levelConfig.getInt("max-territory") + "x" + levelConfig.getInt("max-territory"));
                    p.closeInventory();
                } else {
                    p.sendMessage("§c升级失败！请确保满足所有升级条件。");
                }
            } else {
                p.sendMessage("§c升级失败！资源不足。");
            }
        });
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
} 