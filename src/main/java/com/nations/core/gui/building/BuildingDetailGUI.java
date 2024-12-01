package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.gui.ConfirmGUI;
import com.nations.core.models.Building;
import com.nations.core.models.BuildingType;
import com.nations.core.models.Nation;
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
        super(plugin, player, "§6建筑详情 - " + building.getType().getDisplayName(), 3);
        this.nation = nation;
        this.building = building;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 建筑信息
        setItem(11, createBuildingInfoItem(), null);
        
        // 升级按钮
        if (building.getLevel() < 5) {
            setItem(13, createUpgradeItem(), p -> handleUpgrade());
        }
        
        // 传送按钮
        Location location = building.getBaseLocation();
        if (location != null && location.getWorld() != null) {
            setItem(15, createItem(Material.ENDER_PEARL,
                "§6传送到建筑",
                "§7点击传送到该建筑"
            ), p -> {
                p.closeInventory();
                // 再次调用 getBaseLocation() 确保位置是最新的
                Location teleportLoc = building.getBaseLocation();
                if (teleportLoc != null && teleportLoc.getWorld() != null) {
                    // 找到安全的传送位置
                    Location safeLocation = findSafeLocation(teleportLoc);
                    if (safeLocation != null) {
                        p.teleport(safeLocation);
                        p.sendMessage(MessageUtil.success("已传送到建筑位置"));
                    } else {
                        p.sendMessage(MessageUtil.error("无法找到安全的传送位置"));
                    }
                } else {
                    p.sendMessage(MessageUtil.error("目标世界未加载，无法传送"));
                }
            });
        } else {
            // 如果位置无效，显示一个禁用的按钮
            setItem(15, createItem(Material.BARRIER,
                "§c无法传送",
                "§7建筑位置无效或世界未加载",
                "§7世界名称: §f" + building.getWorldName()
            ), null);
        }
        
        // 边界显示切换按钮
        setItem(16, createBorderToggleItem(), p -> {
            if (BuildingBorderUtil.isBorderVisible(building)) {
                BuildingBorderUtil.removeBuildingBorder(building);
                p.sendMessage(MessageUtil.info("已关闭建筑边界显示"));
            } else {
                BuildingBorderUtil.showBuildingBorder(building);
                p.sendMessage(MessageUtil.info("已开启建筑边界显示"));
            }
            // 刷新界面
            initialize();
        });
        
        // 拆除按钮
        setItem(22, createItem(Material.TNT,
            "§c拆除建筑",
            "§7点击拆除并返还部分资源",
            "",
            "§c警告: 此操作不可撤销！"
        ), p -> new ConfirmGUI(plugin, p,
            "§c确认拆除建筑",
            "§c确认拆除",
            new String[]{
                "§7你确定要拆除这个建筑吗？",
                "§7将返还部分建造资源",
                "",
                "§c此操作不可撤销！"
            },
            confirm -> {
                if (plugin.getBuildingManager().demolishBuilding(nation, building)) {
                    confirm.sendMessage(MessageUtil.success("成功拆除建筑"));
                    new BuildingMainGUI(plugin, confirm, nation).open();
                } else {
                    confirm.sendMessage(MessageUtil.error("拆除失败！"));
                }
            },
            cancel -> new BuildingDetailGUI(plugin, cancel, nation, building).open()
        ).open());
    }
    
    private ItemStack createBuildingInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7类型: §f" + building.getType().getDisplayName());
        lore.add("§7等级: §f" + building.getLevel());
        lore.add("");
        lore.add("§7加成效果:");
        building.getBonuses().forEach((key, value) -> 
            lore.add("§7- " + formatBonus(key, value)));
        
        return createItem(getBuildingMaterial(building.getType()),
            "§6建筑信息",
            lore.toArray(new String[0]));
    }
    
    private ItemStack createUpgradeItem() {
        List<String> lore = new ArrayList<>();
        lore.add("§7当前等级: §f" + building.getLevel());
        lore.add("§7升级费用:");
        Map<Material, Integer> costs = building.getType().getBuildCosts();
        costs.forEach((material, amount) -> {
            int upgradeCost = (int)(amount * (1 + building.getLevel() * 0.5));
            lore.add("§7- " + ItemNameUtil.getName(material) + ": §f" + upgradeCost);
        });
        
        return createItem(Material.EXPERIENCE_BOTTLE,
            "§6升级建筑",
            lore.toArray(new String[0]));
    }
    
    private String formatBonus(String key, double value) {
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
    
    private void handleUpgrade() {
        Map<Material, Integer> costs = building.getType().getBuildCosts();
        boolean hasAll = true;
        StringBuilder message = new StringBuilder("§c升级失败！资源不足：\n");
        
        // 检查金币
        double money = plugin.getConfigManager().getUpgradeMoney(building.getLevel() + 1);
        if (!plugin.getVaultEconomy().has(player, money)) {
            hasAll = false;
            message.append("§7- 金币: §f需要 ").append(money)
                   .append("，拥有 ").append(plugin.getVaultEconomy().getBalance(player)).append("\n");
        }
        
        // 检查物品
        for (Map.Entry<Material, Integer> cost : costs.entrySet()) {
            Material material = cost.getKey();
            int required = (int)(cost.getValue() * (1 + building.getLevel() * 0.5));
            int has = countPlayerItems(player, material);
            
            if (has < required) {
                hasAll = false;
                message.append(MessageUtil.formatResourceRequirement(material, required, has)).append("\n");
            }
        }
        
        if (!hasAll) {
            player.sendMessage(message.toString());
            return;
        }
        
        // 扣除资源
        costs.forEach((material, amount) -> 
            removeItems(player, material, amount));
        
        // 扣除金币
        plugin.getVaultEconomy().withdrawPlayer(player, money);
        
        // 计算新的建筑大小
        int newSize = calculateBuildingSize(building.getType(), building.getLevel() + 1);
        
        // 检查新大小是否会与其他建筑重叠
        if (!checkBuildingSpace(building, newSize)) {
            player.sendMessage(MessageUtil.error("升级失败！新的建筑大小会与其他建筑重叠"));
            // 返还资源
            costs.forEach((material, amount) -> 
                player.getInventory().addItem(new ItemStack(material, amount)));
            plugin.getVaultEconomy().depositPlayer(player, money);
            return;
        }
        
        // 升级建筑
        building.setLevel(building.getLevel() + 1);
        building.setSize(newSize);
        
        // 更新建筑外观
        updateBuildingStructure(building);
        
        // 更新全息显示
        HologramUtil.removeBuildingHologram(building.getBaseLocation());
        HologramUtil.createBuildingHologram(building);
        
        // 显示新的建筑边界
        BuildingBorderUtil.showBuildingBorder(building);
        
        // 保存到数据库
        plugin.getBuildingManager().saveBuilding(building);
        
        // 发送成功消息
        player.sendMessage(MessageUtil.success("成功将建筑升级到 " + building.getLevel() + " 级！"));
        
        // 刷新界面
        initialize();
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
    
    private void removeItems(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        int remaining = amount;
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    contents[i] = null;
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.getInventory().setContents(contents);
    }
    
    private Location findSafeLocation(Location base) {
        World world = base.getWorld();
        if (world == null) return null;
        
        // 从建筑中心位置向上找到第一个安全位置
        Location safe = base.clone();
        safe.setY(world.getHighestBlockYAt(safe));
        safe.add(0, 1, 0); // 确保站在方块上面
        
        // 确保脚下和头部都是空气
        Block feet = safe.getBlock();
        Block head = safe.clone().add(0, 1, 0).getBlock();
        
        if (feet.getType().isAir() && head.getType().isAir()) {
            return safe;
        }
        
        // 如果当前位置不安全，尝试周围的位置
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location check = base.clone().add(x, 0, z);
                check.setY(world.getHighestBlockYAt(check));
                check.add(0, 1, 0);
                
                feet = check.getBlock();
                head = check.clone().add(0, 1, 0).getBlock();
                
                if (feet.getType().isAir() && head.getType().isAir()) {
                    return check;
                }
            }
        }
        
        return null;
    }
    
    private int calculateBuildingSize(BuildingType type, int level) {
        return switch (type) {
            case TOWN_HALL -> 5 + level;     // 市政厅随等级增长较快
            case BARRACKS -> 3 + level;      // 兵营适中增长
            case MARKET -> 4 + level;        // 市场适中增长
            case WAREHOUSE -> 3 + level;     // 仓库适中增长
            case FARM -> 5 + level;          // 农场随等级增长较快
        };
    }
    
    private boolean checkBuildingSpace(Building building, int newSize) {
        Location center = building.getBaseLocation();
        if (center == null) return false;
        
        int halfSize = newSize / 2;
        
        // 获取该国家的所有其他建筑
        for (Building other : nation.getBuildings()) {
            if (other.getId() == building.getId()) continue;
            
            Location otherCenter = other.getBaseLocation();
            if (otherCenter == null) continue;
            
            int otherHalfSize = other.getSize() / 2;
            
            // 检查两个建筑的边界是否重叠（保留2格间距）
            if (Math.abs(center.getBlockX() - otherCenter.getBlockX()) <= (halfSize + otherHalfSize + 2) && 
                Math.abs(center.getBlockZ() - otherCenter.getBlockZ()) <= (halfSize + otherHalfSize + 2)) {
                return false;
            }
        }
        
        return true;
    }
    
    private void updateBuildingStructure(Building building) {
        Location loc = building.getBaseLocation();
        if (loc == null || loc.getWorld() == null) return;
        
        // 先清除旧的建筑结构
        int oldHalfSize = building.getSize() / 2;
        for (int x = -oldHalfSize; x <= oldHalfSize; x++) {
            for (int z = -oldHalfSize; z <= oldHalfSize; z++) {
                for (int y = 0; y < 10; y++) { // 假设建筑最高10格
                    loc.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
        
        // 放置新的建筑结构
        building.getType().placeStructure(loc);
        
        // 添加升级特效
        new BukkitRunnable() {
            double angle = 0;
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks++ >= 40) {
                    cancel();
                    return;
                }
                
                angle += Math.PI / 8;
                double radius = building.getSize() / 2.0;
                
                for (double i = 0; i < Math.PI * 2; i += Math.PI / 16) {
                    double x = Math.cos(i + angle) * radius;
                    double z = Math.sin(i + angle) * radius;
                    Location particleLoc = loc.clone().add(x, 0.5, z);
                    loc.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.TOTEM, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private ItemStack createBorderToggleItem() {
        boolean isVisible = BuildingBorderUtil.isBorderVisible(building);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7当前状态: " + (isVisible ? "§a显示" : "§c隐藏"));
        lore.add("");
        lore.add("§7点击切换边界显示状态");
        
        return createItem(
            isVisible ? Material.BARRIER : Material.END_ROD,
            isVisible ? "§c关闭边界显示" : "§a显示建筑边界",
            lore.toArray(new String[0])
        );
    }
} 