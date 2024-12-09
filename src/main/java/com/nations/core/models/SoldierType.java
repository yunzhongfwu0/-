package com.nations.core.models;

public enum SoldierType {
    WARRIOR("战士", "近战单位", 100, 15, 5),
    ARCHER("弓箭手", "远程单位", 80, 12, 3),
    SUPPORT("辅助", "治疗单位", 70, 8, 8),
    GENERAL("武将", "指挥单位", 150, 20, 10);
    
    private final String displayName;
    private final String description;
    private final int baseHealth;
    private final int baseAttack;
    private final int baseDefense;
    
    SoldierType(String displayName, String description, int baseHealth, int baseAttack, int baseDefense) {
        this.displayName = displayName;
        this.description = description;
        this.baseHealth = baseHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getBaseHealth() { return baseHealth; }
    public int getBaseAttack() { return baseAttack; }
    public int getBaseDefense() { return baseDefense; }
} 