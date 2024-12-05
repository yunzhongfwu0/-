package com.nations.core.models;

import org.bukkit.Material;

public enum NPCSkill {
    // 农民技能
    EFFICIENT_FARMING("高效种植", "提高作物生长速度", Material.WHEAT, NPCType.FARMER),
    HARVEST_MASTER("收获大师", "增加作物产量", Material.GOLDEN_HOE, NPCType.FARMER),
    CROP_EXPERT("作物专家", "提高稀有作物产出概率", Material.DIAMOND_HOE, NPCType.FARMER),
    
    // 守卫技能
    COMBAT_MASTERY("战斗精通", "提高攻击力", Material.IRON_SWORD, NPCType.GUARD),
    DEFENSIVE_STANCE("防御姿态", "提高防御力", Material.SHIELD, NPCType.GUARD),
    PATROL_EFFICIENCY("巡逻效率", "提高巡逻范围和效率", Material.COMPASS, NPCType.GUARD),
    
    // 商人技能
    BARGAINING("议价能力", "提高交易获得的金币", Material.GOLD_INGOT, NPCType.TRADER),
    MARKET_INSIGHT("市场洞察", "解锁更多交易选项", Material.EMERALD, NPCType.TRADER),
    TRADE_NETWORK("贸易网络", "提高交易频率", Material.MAP, NPCType.TRADER),
    
    // 管理者技能
    LEADERSHIP("领导力", "提高其他NPC的工作效率", Material.GOLDEN_HELMET, NPCType.MANAGER),
    RESOURCE_MANAGEMENT("资源管理", "降低NPC维护成本", Material.CHEST, NPCType.MANAGER),
    CRISIS_HANDLING("危机处理", "提高建筑防御和修复速度", Material.BELL, NPCType.MANAGER),
    
    // 仓库管理员技能
    ORGANIZATION("物品整理", "提高仓库整理效率", Material.HOPPER, NPCType.WAREHOUSE_KEEPER),
    STORAGE_EXPERT("存储专家", "增加仓库存储容量", Material.BARREL, NPCType.WAREHOUSE_KEEPER),
    LOGISTICS("物流管理", "提高物品存取速度", Material.MINECART, NPCType.WAREHOUSE_KEEPER);

    private final String displayName;
    private final String description;
    private final Material icon;
    private final NPCType npcType;

    NPCSkill(String displayName, String description, Material icon, NPCType npcType) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.npcType = npcType;
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

    public NPCType getNpcType() {
        return npcType;
    }

    // 获取技能效果值(基于等级)
    public double getEffectValue(int level) {
        if (level <= 0) return 0;
        
        switch (this) {
            // 农民技能
            case EFFICIENT_FARMING:
                return 0.05 * level; // 每级提高5%生长速度和工作范围
            case HARVEST_MASTER:
                return 0.08 * level; // 每级提高8%额外收获概率
            case CROP_EXPERT:
                return 0.03 * level; // 每级提高3%稀有作物概率
                
            // 守卫技能
            case COMBAT_MASTERY:
                return 0.1 * level; // 每级提高10%攻击力
            case DEFENSIVE_STANCE:
                return 0.1 * level; // 每级提高10%防御力
            case PATROL_EFFICIENCY:
                return 1.0 * level; // 每级提高1格巡逻范围
                
            // 商人技能    
            case BARGAINING:
                return 0.05 * level; // 每级提高5%交易金币
            case MARKET_INSIGHT:
                return 1.0 * level; // 每级解锁1个新交易
            case TRADE_NETWORK:
                return 0.1 * level; // 每级提高10%交易频率
                
            // 管理者技能
            case LEADERSHIP:
                return 0.03 * level; // 每级提高3%效率
            case RESOURCE_MANAGEMENT:
                return 0.05 * level; // 每级降低5%成本
            case CRISIS_HANDLING:
                return 0.05 * level; // 每级提高5%防御和修复
                
            // 仓库管理员技能
            case ORGANIZATION:
                return 0.05 * level; // 每级提高5%仓库整理效率
            case STORAGE_EXPERT:
                return 0.05 * level; // 每级提高5%仓库存储容量
            case LOGISTICS:
                return 0.05 * level; // 每级提高5%物品存取速度
                
            default:
                return 0;
        }
    }

    // 获取技能解锁所需金币
    public int getUnlockCost() {
        switch (this) {
            // 一级技能 - 基础价格
            case EFFICIENT_FARMING:
            case COMBAT_MASTERY:
            case BARGAINING:
            case LEADERSHIP:
                return 1000;
                
            // 二级技能 - 中等价格
            case HARVEST_MASTER:
            case DEFENSIVE_STANCE:
            case MARKET_INSIGHT:
            case RESOURCE_MANAGEMENT:
                return 2000;
                
            // 三级技能 - 高级价格
            case CROP_EXPERT:
            case PATROL_EFFICIENCY:
            case TRADE_NETWORK:
            case CRISIS_HANDLING:
                return 3000;
                
            // 仓库管理员技能
            case ORGANIZATION:
            case STORAGE_EXPERT:
            case LOGISTICS:
                return 2000;
                
            default:
                return 1000;
        }
    }

    // 获取技能升级所需金币
    public int getUpgradeCost(int currentLevel) {
        if (currentLevel >= 10) return 0; // 已达到最高等级
        
        int baseCost = getUnlockCost() / 2; // 基础升级费用是解锁费用的一半
        double multiplier = Math.pow(1.5, currentLevel); // 每级费用增加50%
        
        return (int)(baseCost * multiplier);
    }

    // 获取技能升级所需经验
    public int getRequiredExperience(int currentLevel) {
        return (currentLevel + 1) * 100; // 每级需要(等级+1)*100经验
    }
}
