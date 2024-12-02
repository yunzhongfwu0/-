package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import com.nations.core.utils.MessageUtil;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NPCSkillGUI extends BaseGUI {
    private final NationNPC npc;
    private final Map<NPCSkill, NPCSkillData> skills;

    public NPCSkillGUI(NationsCore plugin, Player player, NationNPC npc) {
        super(plugin, player, "NPC技能 - " + npc.getCitizensNPC().getName(), 6);
        this.npc = npc;
        Map<NPCSkill, NPCSkillData> loadedSkills = plugin.getNPCSkillManager().getNPCSkills(npc.getId());
        this.skills = loadedSkills != null ? loadedSkills : new ConcurrentHashMap<>();
        initialize();
    }

    public void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        int slot = 19;
        Map<NPCSkill, NPCSkillData> skills = plugin.getNPCSkillManager().getSkills(npc);
        
        // 显示NPC信息
        setItem(4, createItem(
            Material.PLAYER_HEAD,
            "§6" + npc.getCitizensNPC().getName(),
            "",
            "§e类型: §f" + npc.getType().getDisplayName(),
            "§e等级: §f" + npc.getLevel(),
            "§e经验: §f" + npc.getExperience() + "/" + npc.getRequiredExperience(),
            "§e可用技能点: §f" + calculateAvailableSkillPoints(),
            "",
            "§7点击返回NPC管理界面"
        ), p -> new NPCManageGUI(plugin, p, npc, true).open());

        // 显示技能列表
        for (NPCSkill skill : NPCSkill.values()) {
            if (skill.getNpcType() == npc.getType()) {
                NPCSkillData skillData = skills.computeIfAbsent(skill, NPCSkillData::new);
                ItemStack skillItem = createSkillItem(skill, skillData);
                
                setItem(slot++, skillItem, 
                    // 左键点击处理
                    p -> {
                        if (!skillData.isUnlocked()) {
                            // 解锁技能
                            if (calculateAvailableSkillPoints() > 0) {
                                if (plugin.getNPCSkillManager().unlockSkill(npc, skill, p)) {
                                    initialize(); // 刷新界面
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "没有足够的技能点！");
                            }
                        } else if (skillData.canLevelUp()) {
                            // 升级技能
                            if (calculateAvailableSkillPoints() > 0) {
                                if (plugin.getNPCSkillManager().upgradeSkill(npc, skill, p)) {
                                    initialize(); // 刷新界面
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "没有足够的技能点！");
                            }
                        }
                    }
                );
            }
        }

        // 返回按钮
        setItem(49, createItem(Material.ARROW,
            MessageUtil.title("返回"),
            MessageUtil.subtitle("点击返回NPC管理")
        ), p -> new NPCManageGUI(plugin, p, npc, true).open());
    }

    private ItemStack createSkillItem(NPCSkill skill, NPCSkillData skillData) {
        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + skill.getDisplayName());
            List<String> lore = new ArrayList<>();
            
            // 基础描述
            lore.add(ChatColor.GRAY + skill.getDescription());
            lore.add("");
            
            if (skillData.isUnlocked()) {
                // 等级和经验信息
                lore.add(ChatColor.YELLOW + "等级: " + skillData.getLevel() + "/" + NPCSkillData.MAX_LEVEL);
                if (skillData.getLevel() < NPCSkillData.MAX_LEVEL) {
                    lore.add(ChatColor.YELLOW + "经验: " + skillData.getExperience() + "/" + skillData.getRequiredExperience());
                    lore.add(ChatColor.YELLOW + "进度: " + skillData.getProgressPercentage() + "%");
                } else {
                    lore.add(ChatColor.GREEN + "已达到最高等级!");
                }
                
                // 当前效果
                lore.add("");
                lore.add(ChatColor.AQUA + "当前效果:");
                double effect = skillData.getEffectiveness();
                switch (skill) {
                    case EFFICIENT_FARMING:
                        lore.add(ChatColor.AQUA + "- 生长速度和工作范围 +" + String.format("%.1f", effect * 100) + "%");
                        break;
                    case HARVEST_MASTER:
                        lore.add(ChatColor.AQUA + "- 额外收获概率 +" + String.format("%.1f", effect * 100) + "%");
                        break;
                    case CROP_EXPERT:
                        lore.add(ChatColor.AQUA + "- 稀有作物概率 +" + String.format("%.1f", effect * 100) + "%");
                        break;
                    // ... 其他技能效果显示
                }
                
                // 升级信息
                if (skillData.canLevelUp()) {
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "升级费用: " + skill.getUpgradeCost(skillData.getLevel()) + " 金币");
                    lore.add(ChatColor.YELLOW + "需要技能点: 1");
                    if (calculateAvailableSkillPoints() > 0) {
                        lore.add(ChatColor.GREEN + "点击升级!");
                    } else {
                        lore.add(ChatColor.RED + "技能点不足!");
                    }
                }
            } else {
                // 未解锁信息
                lore.add(ChatColor.RED + "未解锁");
                lore.add(ChatColor.YELLOW + "解锁费用: " + skill.getUnlockCost() + " 金币");
                lore.add(ChatColor.YELLOW + "需要技能点: 1");
                if (calculateAvailableSkillPoints() > 0) {
                    lore.add(ChatColor.GREEN + "点击解锁!");
                } else {
                    lore.add(ChatColor.RED + "技能点不足!");
                }
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private int calculateAvailableSkillPoints() {
        // 每升一级获得1个技能点
        int totalPoints = npc.getLevel();
        
        // 减去已使用的技能点
        Map<NPCSkill, NPCSkillData> skills = plugin.getNPCSkillManager().getSkills(npc);
        int usedPoints = 0;
        for (NPCSkillData skillData : skills.values()) {
            if (skillData.isUnlocked()) {
                // 解锁和每次升级都需要1点技能点
                usedPoints += skillData.getLevel();
            }
        }
        
        return totalPoints - usedPoints;
    }
}
