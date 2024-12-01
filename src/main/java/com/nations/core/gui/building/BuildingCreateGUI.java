package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.models.Building;
import com.nations.core.models.BuildingType;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;
import com.nations.core.utils.ItemNameUtil;
import com.nations.core.utils.MessageUtil;
import com.nations.core.utils.BuildingBorderUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BuildingCreateGUI extends BaseGUI {
    private final Nation nation;
    
    public BuildingCreateGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6建造新建筑", 4);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        int slot = 10;
        for (BuildingType type : BuildingType.values()) {
            setItem(slot, createBuildingTypeItem(type),
                p -> {
                    if (canBuild(type)) {
                        handleBuildingCreate(type);
                    } else {
                        p.sendMessage(MessageUtil.error("无法建造！请先满足建造要求"));
                    }
                });
            
            slot++;
            if (slot % 9 == 8) slot += 3;
        }
        
        // 返回按钮
        setItem(31, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new BuildingMainGUI(plugin, p, nation).open());
    }
    
    private boolean canBuild(BuildingType type) {
        // 检查国家等级要求
        if (nation.getLevel() < type.getMinNationLevel()) {
            return false;
        }
        
        // 检查前置建筑
        if (type.getRequiredBuilding() != null) {
            Building required = nation.getBuilding(type.getRequiredBuilding());
            if (required == null || required.getLevel() < type.getRequiredBuildingLevel()) {
                return false;
            }
        }
        
        return true;
    }
    
    private void handleBuildingCreate(BuildingType type) {
        // 打开确认界面
        player.closeInventory();
        
        // 创建新的确认GUI
        Inventory confirmGui = Bukkit.createInventory(null, 27, "§6确认建造 - " + type.getDisplayName());
        
        // 填充边框
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                confirmGui.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }
        
        // 设置确认按钮
        confirmGui.setItem(13, createConfirmItem(type));
        
        // 返回按钮
        confirmGui.setItem(26, createItem(Material.ARROW, "§f返回", "§7点击返回"));
        
        // 注册点击事件
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onClick(InventoryClickEvent event) {
                if (event.getInventory() != confirmGui) return;
                event.setCancelled(true);
                
                if (event.getSlot() == 13) {
                    // 点击确认按钮
                    if (hasEnoughResources(type)) {
                        player.closeInventory();
                        startLocationSelection(type);
                    } else {
                        player.sendMessage(MessageUtil.error("资源不足！"));
                    }
                } else if (event.getSlot() == 26) {
                    // 返回上一级
                    new BuildingCreateGUI(plugin, player, nation).open();
                }
            }
            
            @EventHandler
            public void onClose(InventoryCloseEvent event) {
                if (event.getInventory() == confirmGui) {
                    HandlerList.unregisterAll(this);
                }
            }
        }, plugin);
        
        player.openInventory(confirmGui);
    }
    
    private boolean hasEnoughResources(BuildingType type) {
        Map<Material, Integer> costs = type.getBuildCosts();
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            if (countPlayerItems(player, cost.getKey()) < cost.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    private void startLocationSelection(BuildingType type) {
        player.sendMessage(MessageUtil.info("请在聊天栏输入 confirm 确认在当前位置建造，或输入 cancel 取消"));
        
        // 显示预览边界
        BuildingBorderUtil.showPlacementBorder(player.getLocation(), type.getBaseSize());
        
        ChatInputManager.awaitChatInput(player, response -> {
            if (response.equalsIgnoreCase("confirm")) {
                Location loc = player.getLocation();
                if (plugin.getBuildingManager().createBuilding(nation, type, loc)) {
                    player.sendMessage(MessageUtil.success("成功建造 " + type.getDisplayName()));
                    new BuildingMainGUI(plugin, player, nation).open();
                } else {
                    player.sendMessage(MessageUtil.error("建造失败！请确保位置合适"));
                }
            } else {
                player.sendMessage(MessageUtil.info("已取消建造"));
                new BuildingMainGUI(plugin, player, nation).open();
            }
        });
    }
    
    private ItemStack createBuildingTypeItem(BuildingType type) {
        List<String> lore = new ArrayList<>();
        
        // 添加建筑描述
        lore.addAll(Arrays.asList(type.getDescription()));
        lore.add("");
        
        // 检查建造条件
        boolean canBuild = true;
        List<String> requirements = new ArrayList<>();
        
        // 检查国家等级
        if (nation.getLevel() < type.getMinNationLevel()) {
            canBuild = false;
            requirements.add("§c✘ 需要国家等级: " + type.getMinNationLevel());
            requirements.add("  §7当前等级: " + nation.getLevel());
        } else {
            requirements.add("§a✔ 国家等级已满足");
        }
        
        // 检查前置建筑
        if (type.getRequiredBuilding() != null) {
            Building required = nation.getBuilding(type.getRequiredBuilding());
            if (required == null || required.getLevel() < type.getRequiredBuildingLevel()) {
                canBuild = false;
                requirements.add("§c✘ 需要建筑: " + type.getRequiredBuilding().getDisplayName() + 
                    " Lv." + type.getRequiredBuildingLevel());
                if (required != null) {
                    requirements.add("  §7当前等级: " + required.getLevel());
                } else {
                    requirements.add("  §7尚未建造");
                }
            } else {
                requirements.add("§a✔ 前置建筑已满足");
            }
        }
        
        // 添加建造要求
        lore.add("§e建造要求:");
        lore.addAll(requirements);
        lore.add("");
        
        // 添加建造费用
        lore.add("§e建造费用:");
        Map<Material, Integer> costs = type.getBuildCosts();
        costs.forEach((material, amount) -> {
            int playerHas = countPlayerItems(player, material);
            String chineseName = ItemNameUtil.getName(material);
            lore.add("§7" + chineseName + ": §f" + amount + 
                (playerHas >= amount ? " §a✔" : " §c✘ (" + playerHas + ")"));
        });
        
        lore.add("");
        lore.add("§e建筑信息:");
        lore.add("§7- 基础大小: §f" + type.getBaseSize() + "x" + type.getBaseSize());
        lore.add("");
        
        if (canBuild) {
            lore.add("§a✔ 点击建造此建筑");
        } else {
            lore.add("§c✘ 不满足建造条件");
        }
        
        return createItem(
            getBuildingMaterial(type),
            (canBuild ? "§6" : "§8") + type.getDisplayName(),
            lore.toArray(new String[0])
        );
    }
    
    private ItemStack createConfirmItem(BuildingType type) {
        List<String> lore = new ArrayList<>();
        lore.add("§7建筑类型: §f" + type.getDisplayName());
        lore.add("§7建筑等级: §f1");
        lore.add("");
        
        // 检查资源是否足够
        Map<Material, Integer> costs = type.getBuildCosts();
        boolean hasAll = true;
        
        if (!costs.isEmpty()) {
            lore.add("§7建造所需资源:");
            for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
                int playerHas = countPlayerItems(player, cost.getKey());
                String chineseName = ItemNameUtil.getName(cost.getKey());
                lore.add("§7" + chineseName + ": §f" + cost.getValue() + 
                    (playerHas >= cost.getValue() ? " §a✔" : " §c✘ (" + playerHas + ")"));
                if (playerHas < cost.getValue()) {
                    hasAll = false;
                }
            }
        }
        
        return createItem(Material.EMERALD,
            hasAll ? "§a确认建造" : "§c资源不足",
            lore.toArray(new String[0]));
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