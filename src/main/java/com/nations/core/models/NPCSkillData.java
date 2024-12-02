package com.nations.core.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NPCSkillData {
    private NPCSkill skill;
    private int level;
    private int experience;
    private boolean unlocked;
    public static final int MAX_LEVEL = 10;

    public NPCSkillData(NPCSkill skill) {
        this.skill = skill;
        this.level = 0;
        this.experience = 0;
        this.unlocked = false;
    }

    public boolean addExperience(int amount) {
        if (!unlocked || level >= MAX_LEVEL) {
            return false;
        }

        this.experience += amount;
        int requiredExp = getRequiredExperience();
        boolean leveledUp = false;
        
        while (this.experience >= requiredExp && level < MAX_LEVEL) {
            level++;
            this.experience -= requiredExp;
            requiredExp = getRequiredExperience();
            leveledUp = true;
        }

        // 如果已达到最高等级，经验值归零
        if (level >= MAX_LEVEL) {
            this.experience = 0;
        }

        return leveledUp;
    }

    public int getRequiredExperience() {
        // 基础经验100，每级增加100，呈指数增长
        return (int)(100 * Math.pow(1.2, level));
    }

    public double getEffectiveness() {
        if (!unlocked) return 0;
        return skill.getEffectValue(level);
    }

    public boolean canLevelUp() {
        return unlocked && level < MAX_LEVEL;
    }

    public int getProgressPercentage() {
        if (!unlocked || level >= MAX_LEVEL) return 0;
        return (int)((double)experience / getRequiredExperience() * 100);
    }

    public boolean unlock() {
        if (!unlocked) {
            unlocked = true;
            level = 1;
            experience = 0;
            return true;
        }
        return false;
    }

    public NPCSkill getSkill() {
        return skill;
    }

    public void setSkill(NPCSkill skill) {
        this.skill = skill;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }
}
