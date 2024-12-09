package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.gui.ConfirmGUI;
import com.nations.core.models.Building;
import com.nations.core.models.BuildingType;
import com.nations.core.models.Nation;
import com.nations.core.models.Transaction.TransactionType;
import com.nations.core.utils.ItemNameUtil;
import com.nations.core.utils.MessageUtil;
import com.nations.core.utils.HologramUtil;
import com.nations.core.utils.BuildingBorderUtil;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingDetailGUI extends BaseGUI {
    private final Nation nation;
    private final Building building;
    
    public BuildingDetailGUI(NationsCore plugin, Player player, Nation nation, Building building) {
        super(plugin, player, "§6建筑详情", 5);
        this.nation = nation;
        this.building = building;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 建筑基本信息
        setItem(13, createBuildingInfoItem(), null);
        
        // 工人管理按钮
        setItem(29, createItem(Material.PLAYER_HEAD,
            "§6工人管理",
            "§7当前工人: §f" + plugin.getNPCManager().getBuildingWorkers(building).size() + "/" + 
                building.getType().getWorkerSlots().values().stream().mapToInt(Integer::intValue).sum(),
            "",
            "§7- 雇佣新工人",
            "§7- 管理现有工人",
            "§7- 查看工作状态",
            "",
            "§e点击管理工人"
        ), p -> new WorkerManageGUI(plugin, p, building).open());
        
        // 建筑互动按钮
        setItem(31, createItem(Material.CRAFTING_TABLE,
            "§6建筑互动",
            "§7- 设置自动化",
            "§7- 选择作物类型",
            "§7- 查看生产状态",
            "",
            "§e点击进行互动"
        ), p -> new BuildingInteractGUI(plugin, p, building).open());
        
        // 建筑升级按钮
        if (building.getLevel() < 5) {
            setItem(33, createUpgradeItem(), p -> handleUpgrade(p));
        }
        
        // 建筑拆除按钮
        setItem(44, createItem(Material.TNT,
            "§c拆除建筑",
            "§7- 将返还部分资源",
            "§7- 工人将被解雇",
            "§7- 所有数据将被清除",
            "",
            "§c警告: 此操作不可撤销！",
            "",
            "§e点击拆除"
        ), this::handleDemolish);
        
        // 返回按钮
        setItem(40, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new BuildingMainGUI(plugin, p, nation).open());
    }
    
    private ItemStack createBuildingInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7类型: §f" + building.getType().getDisplayName());
        lore.add("§7等级: §f" + building.getLevel());
        lore.add("");
        lore.add("§7建筑加成:");
        
        // 获取基础加成和等级加成
        Map<String, Double> baseBonuses = building.getType().getBaseBonus();
        Map<String, Double> levelBonuses = building.getType().getLevelBonus();
        
        switch (building.getType()) {
            case TOWN_HALL -> {
                double baseTax = baseBonuses.getOrDefault("tax_rate", 0.0) * 100;
                double levelTax = levelBonuses.getOrDefault("tax_rate", 0.0) * (building.getLevel() - 1) * 100;
                double baseMembers = baseBonuses.getOrDefault("max_members", 0.0);
                double levelMembers = levelBonuses.getOrDefault("max_members", 0.0) * (building.getLevel() - 1);
                
                lore.add(String.format("§7- 基础税率: §f+%.1f%%", baseTax));
                if (levelTax > 0) lore.add(String.format("§7- 等级加成: §f+%.1f%%", levelTax));
                lore.add(String.format("§7- 基础人口上限: §f%.0f", baseMembers));
                if (levelMembers > 0) lore.add(String.format("§7- 等级加成: §f+%.0f", levelMembers));
            }
            case BARRACKS -> {
                double baseSlots = baseBonuses.getOrDefault("training_slots", 0.0);
                double levelSlots = levelBonuses.getOrDefault("training_slots", 0.0) * (building.getLevel() - 1);
                double baseBonusTraining = baseBonuses.getOrDefault("training_bonus", 0.0) * 100;
                double levelBonusTraining = levelBonuses.getOrDefault("training_bonus", 0.0) * (building.getLevel() - 1) * 100;
                double baseSpeed = baseBonuses.getOrDefault("training_speed", 0.0) * 100;
                double levelSpeed = levelBonuses.getOrDefault("training_speed", 0.0) * (building.getLevel() - 1) * 100;
                double baseStrength = baseBonuses.getOrDefault("strength", 0.0);
                double levelStrength = levelBonuses.getOrDefault("strength", 0.0) * (building.getLevel() - 1);
                double baseDefense = baseBonuses.getOrDefault("defense", 0.0);
                double levelDefense = levelBonuses.getOrDefault("defense", 0.0) * (building.getLevel() - 1);
                
                lore.add(String.format("§7- 基础训练位: §f%.0f", baseSlots));
                if (levelSlots > 0) lore.add(String.format("§7- 等级加成: §f+%.0f", levelSlots));
                lore.add(String.format("§7- 基础训练加成: §f+%.1f%%", baseBonusTraining));
                if (levelBonusTraining > 0) lore.add(String.format("§7- 等级加成: §f+%.1f%%", levelBonusTraining));
                lore.add(String.format("§7- 基础训练速度: §f-%.1f%%", baseSpeed));
                if (levelSpeed > 0) lore.add(String.format("§7- 等级加成: §f-%.1f%%", levelSpeed));
                lore.add(String.format("§7- 基础战斗力: §f+%.1f", baseStrength));
                if (levelStrength > 0) lore.add(String.format("§7- 等级加成: §f+%.1f", levelStrength));
                lore.add(String.format("§7- 基础防御力: §f+%.1f", baseDefense));
                if (levelDefense > 0) lore.add(String.format("§7- 等级加成: §f+%.1f", levelDefense));
            }
            case MARKET -> {
                double baseDiscount = baseBonuses.getOrDefault("trade_discount", 0.0) * 100;
                double levelDiscount = levelBonuses.getOrDefault("trade_discount", 0.0) * (building.getLevel() - 1) * 100;
                double baseIncome = baseBonuses.getOrDefault("income_bonus", 0.0) * 100;
                double levelIncome = levelBonuses.getOrDefault("income_bonus", 0.0) * (building.getLevel() - 1) * 100;
                
                lore.add(String.format("§7- 基础交易折扣: §f%.1f%%", baseDiscount));
                if (levelDiscount > 0) lore.add(String.format("§7- 等级加成: §f+%.1f%%", levelDiscount));
                lore.add(String.format("§7- 基础收入加成: §f+%.1f%%", baseIncome));
                if (levelIncome > 0) lore.add(String.format("§7- 等级加成: §f+%.1f%%", levelIncome));
            }
            case WAREHOUSE -> {
                double baseStorage = baseBonuses.getOrDefault("storage_size", 0.0);
                double levelStorage = levelBonuses.getOrDefault("storage_size", 0.0) * (building.getLevel() - 1);
                
                lore.add(String.format("§7- 基础存储容量: §f%.0f", baseStorage));
                if (levelStorage > 0) lore.add(String.format("§7- 等级加成: §f+%.0f", levelStorage));
            }
            case FARM -> {
                double baseProduction = baseBonuses.getOrDefault("food_production", 0.0);
                double levelProduction = levelBonuses.getOrDefault("food_production", 0.0) * (building.getLevel() - 1);
                
                lore.add(String.format("§7- 基础食物产量: §f%.0f/小时", baseProduction));
                if (levelProduction > 0) lore.add(String.format("§7- 等级加成: §f+%.0f/小时", levelProduction));
            }
        }
        
        // 显示总加成
        lore.add("");
        lore.add("§e当前总加成:");
        Map<String, Double> finalBonuses = building.getBonuses();
        switch (building.getType()) {
            case TOWN_HALL -> {
                lore.add(String.format("§7- 总税率: §f+%.1f%%", finalBonuses.getOrDefault("tax_rate", 0.0) * 100));
                lore.add(String.format("§7- 总人口上限: §f%.0f", finalBonuses.getOrDefault("max_members", 0.0)));
            }
            case BARRACKS -> {
                lore.add(String.format("§7- 总训练位: §f%.0f", finalBonuses.getOrDefault("training_slots", 0.0)));
                lore.add(String.format("§7- 总训练加成: §f+%.1f%%", finalBonuses.getOrDefault("training_bonus", 0.0) * 100));
                lore.add(String.format("§7- 总训练速度: §f-%.1f%%", finalBonuses.getOrDefault("training_speed", 0.0) * 100));
                lore.add(String.format("§7- 总战斗力: §f+%.1f", finalBonuses.getOrDefault("strength", 0.0)));
                lore.add(String.format("§7- 总防御力: §f+%.1f", finalBonuses.getOrDefault("defense", 0.0)));
            }
            case MARKET -> {
                lore.add(String.format("§7- 总交易折扣: §f%.1f%%", finalBonuses.getOrDefault("trade_discount", 0.0) * 100));
                lore.add(String.format("§7- 总收入加成: §f+%.1f%%", finalBonuses.getOrDefault("income_bonus", 0.0) * 100));
            }
            case WAREHOUSE -> {
                lore.add(String.format("§7- 总存储容量: §f%.0f", finalBonuses.getOrDefault("storage_size", 0.0)));
            }
            case FARM -> {
                lore.add(String.format("§7- 总食物产量: §f%.0f/h", finalBonuses.getOrDefault("food_production", 0.0)));
            }
        }
        
        // 显示效率加成
        double efficiency = building.getTotalEfficiencyBonus() * 100 - 100;
        if (efficiency > 0) {
            lore.add("");
            lore.add(String.format("§e效率加成: §f+%.1f%%", efficiency));
        }
        
        lore.add("");
        lore.add("§7工人:");
        building.getType().getWorkerSlots().forEach((type, count) -> 
            lore.add("§7- " + type.getDisplayName() + ": §f" + 
                plugin.getNPCManager().getBuildingWorkers(building).stream()
                    .filter(npc -> npc.getType() == type)
                    .count() + "/" + count)
        );
        
        return createItem(building.getType().getIcon(),
            "§6" + building.getType().getDisplayName(),
            lore.toArray(new String[0]));
    }
    
    private ItemStack createUpgradeItem() {
        int nextLevel = building.getLevel() + 1;
        List<String> lore = new ArrayList<>();
        lore.add("§7当前等级: §f" + building.getLevel());
        lore.add("§7下一等级: §f" + nextLevel);
        lore.add("");
        lore.add("§7升级需要:");
        lore.add("§7- 金币: §f" + getUpgradeCost());
        
        // 获取升级所需资源
        Map<Material, Integer> upgradeCosts = getUpgradeResourceCosts();
        if (!upgradeCosts.isEmpty()) {
            upgradeCosts.forEach((material, amount) -> {
                boolean hasEnough = plugin.getNationManager().hasEnoughItems(player, material, amount);
                lore.add("§7- " + ItemNameUtil.getName(material) + ": §f" + amount + (hasEnough ? " §a✔" : " §c✘"));
            });
        }
        
        lore.add("");
        lore.add("§7升级后效果:");
        lore.add("§7- 工作效率提升 10%");
        lore.add("§7- 工人上限 +1");
        lore.add("§7- 特殊效果增强");
        lore.add("");
        lore.add("§e点击升级");
        
        return createItem(Material.EXPERIENCE_BOTTLE,
            "§6升级建筑",
            lore.toArray(new String[0]));
    }
    
    private Map<Material, Integer> getUpgradeResourceCosts() {
        Map<Material, Integer> costs = new HashMap<>();
        int nextLevel = building.getLevel() + 1;
        
        // 这里可以根据建筑类型和等级来设置不同的资源需求
        switch (building.getType()) {
            case FARM -> {
                costs.put(Material.WHEAT, 32 * nextLevel);
                costs.put(Material.IRON_HOE, nextLevel);
            }
            case BARRACKS -> {
                costs.put(Material.IRON_SWORD, nextLevel);
                costs.put(Material.IRON_CHESTPLATE, nextLevel);
            }
            case MARKET -> {
                costs.put(Material.EMERALD, 16 * nextLevel);
                costs.put(Material.CHEST, 2 * nextLevel);
            }
            case WAREHOUSE -> {
                costs.put(Material.CHEST, 4 * nextLevel);
                costs.put(Material.IRON_BLOCK, 2 * nextLevel);
            }
            case TOWN_HALL -> {
                costs.put(Material.DIAMOND, 8 * nextLevel);
                costs.put(Material.EMERALD_BLOCK, 2 * nextLevel);
            }
        }
        
        return costs;
    }
    
    private void handleDemolish(Player player) {
        new ConfirmGUI(plugin, player,
            "确认拆除",
            "拆除建筑",
            new String[]{
                "§c此操作不可撤销！",
                "§7将返还以下资源:",
                "§7- 50% 建造材料",
                "",
                "§7同时会:",
                "§7- 解雇所有工人",
                "§7- 清除所有数据"
            },
            this::demolishBuilding,
            p -> new BuildingDetailGUI(plugin, p, nation, building).open()
        ).open();
    }
    
    private void demolishBuilding(Player player) {
        if (plugin.getBuildingManager().demolishBuilding(nation, building)) {
            player.sendMessage(MessageUtil.success("成功拆除建筑！"));
            new BuildingMainGUI(plugin, player, nation).open();
        } else {
            player.sendMessage(MessageUtil.error("拆除建筑失败！"));
            new BuildingDetailGUI(plugin, player, nation, building).open();
        }
    }
    
    private String formatBonus(String key, Double value) {
        return switch (key) {
            case "tax_rate" -> String.format("税收加成: +%.1f%%", value * 100);
            case "max_members" -> String.format("成员上限: +%.0f", value);
            case "strength" -> String.format("战斗力: +%.1f", value);
            case "defense" -> String.format("防御力: +%.1f", value);
            case "trade_discount" -> String.format("交易折扣: %.1f%%", value * 100);
            case "income_bonus" -> String.format("收入加成: +%.1f%%", value * 100);
            case "storage_size" -> String.format("存储空间: +%.0f", value);
            case "food_production" -> String.format("食物产量: +%.1f/h", value);
            default -> key + ": " + value;
        };
    }
    
    private void handleUpgrade(Player player) {
        int nextLevel = building.getLevel() + 1;
        
        // 检查升级条件
        if (!canUpgrade()) {
            player.sendMessage(MessageUtil.error("不满足升级条件！"));
            return;
        }
        
        // 显示确认界面
        new ConfirmGUI(plugin, player,
            "确��升级",
            "升级建筑",
            new String[]{
                "§7升级到 " + nextLevel + " 级需要:",
                "§7- " + getUpgradeCost() + " 金币",
                "",
                "§7升级后效果:",
                "§7- 工作效率提升 10%",
                "§7- 工人上限 +1",
                "§7- 特殊效果增强",
                "",
                "§e点击确认升级"
            },
            this::performUpgrade,
            p -> new BuildingDetailGUI(plugin, p, nation, building).open()
        ).open();
    }
    
    private boolean canUpgrade() {
        int nextLevel = building.getLevel() + 1;
        
        // 检查最高等级
        if (nextLevel > 5) {
            return false;
        }
        
        // 检查国家等级要求
        if (nation.getLevel() < nextLevel) {
            return false;
        }
        
        // 检查资金
        if (nation.getBalance() < getUpgradeCost()) {
            return false;
        }
        
        return true;
    }
    
    private double getUpgradeCost() {
        return 5000 * Math.pow(2, building.getLevel());
    }
    
    private void performUpgrade(Player player) {
        double cost = getUpgradeCost();
        
        // 扣除费用
        nation.withdraw(cost);
        
        // 记录交易
        plugin.getNationManager().recordTransaction(
            nation,
            null,
            TransactionType.WITHDRAW,
            cost,
            "升级建筑: " + building.getType().getDisplayName() + " 到 " + (building.getLevel() + 1) + " 级"
        );
        
        // 升级建筑
        building.setLevel(building.getLevel() + 1);
        plugin.getBuildingManager().saveBuilding(building);
        
        // 发送消息
        player.sendMessage(MessageUtil.success("成功将建筑升级到 " + building.getLevel() + " 级！"));
        
        // 刷新界面
        new BuildingDetailGUI(plugin, player, nation, building).open();
    }
}