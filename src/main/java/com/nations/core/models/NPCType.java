package com.nations.core.models;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum NPCType {
    FARMER("农民", "种植和收获作物", Material.IRON_HOE, 50, 1),
    GUARD("守卫", "保护建筑和领土", Material.SHIELD, 60, 2),
    TRADER("商人", "进行交易和资源收集", Material.EMERALD, 70, 2),
    MANAGER("管理员", "管理国家事务", Material.GOLDEN_HELMET, 100, 3),
    MINER("矿工", "开采矿物资源", Material.IRON_PICKAXE, 55, 1),
    BUILDER("建筑师", "加速建筑建造", Material.BRICK, 80, 2),
    RESEARCHER("研究员", "提升科技发展", Material.BOOK, 90, 3),
    DIPLOMAT("外交官", "处理国家外交", Material.PAPER, 85, 3);

    private final String displayName;
    private final String description;
    private final Material icon;
    private final int baseSalary;
    private final int unlockLevel; // 解锁所需的国家等级

    NPCType(String displayName, String description, Material icon, int baseSalary, int unlockLevel) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.baseSalary = baseSalary;
        this.unlockLevel = unlockLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public int getBaseSalary() {
        return baseSalary;
    }

    public int getUnlockLevel() {
        return unlockLevel;
    }

    public boolean isUnlocked(Nation nation) {
        return nation.getLevel() >= unlockLevel;
    }
} 