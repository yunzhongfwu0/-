package com.nations.core.models;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum NPCType {
    FARMER("农民", "种植和收获作物", Material.IRON_HOE, 50),
    GUARD("守卫", "保护建筑", Material.SHIELD, 60),
    TRADER("商人", "进行交易", Material.EMERALD, 70),
    MANAGER("管理员", "管理国家事务", Material.GOLDEN_HELMET, 100);

    private final String displayName;
    private final String description;
    private final Material icon;
    private final int baseSalary;

    NPCType(String displayName, String description, Material icon, int baseSalary) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.baseSalary = baseSalary;
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
} 