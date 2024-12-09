package com.nations.core.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Soldier {
    private final long id;
    private final UUID uuid;
    private final SoldierType type;
    private final Building barracks;
    private int level;
    private int experience;
    private Map<String, Double> attributes;
    private String name;
    
    public Soldier(long id, UUID uuid, SoldierType type, Building barracks, String name) {
        this.id = id;
        this.uuid = uuid;
        this.type = type;
        this.barracks = barracks;
        this.name = name;
        this.level = 1;
        this.experience = 0;
        initializeAttributes();
    }
    
    private void initializeAttributes() {
        attributes = new HashMap<>();
        attributes.put("health", (double) type.getBaseHealth());
        attributes.put("attack", (double) type.getBaseAttack());
        attributes.put("defense", (double) type.getBaseDefense());
        
        // 应用兵营加成
        double strengthBonus = barracks.getBonuses().getOrDefault("strength", 0.0);
        attributes.forEach((key, value) -> 
            attributes.put(key, value * (1 + strengthBonus))
        );
    }
    
    public void gainExperience(int amount) {
        experience += amount;
        int requiredExp = level * 100;
        
        while (experience >= requiredExp) {
            levelUp();
            experience -= requiredExp;
            requiredExp = level * 100;
        }
    }
    
    private void levelUp() {
        level++;
        // 属性提升
        attributes.replaceAll((k, v) -> v * 1.1);
    }
    
    // Getters
    public long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public SoldierType getType() { return type; }
    public Building getBarracks() { return barracks; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public Map<String, Double> getAttributes() { return attributes; }
    public String getName() { return name; }
} 