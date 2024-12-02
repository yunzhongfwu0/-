package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import com.nations.core.models.Transaction.TransactionType;
import com.nations.core.utils.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCSkillManager {
    private final NationsCore plugin;
    private final Map<Long, Map<NPCSkill, NPCSkillData>> npcSkills;

    public NPCSkillManager(NationsCore plugin) {
        this.plugin = plugin;
        this.npcSkills = new ConcurrentHashMap<>();
        loadSkills();
    }

    public void loadSkills() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "npc_skills"
            );
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                long npcId = rs.getLong("npc_id");
                NPCSkill skill = NPCSkill.valueOf(rs.getString("skill_name"));
                NPCSkillData skillData = new NPCSkillData(skill);
                skillData.setLevel(rs.getInt("level"));
                skillData.setExperience(rs.getInt("experience"));
                skillData.setUnlocked(rs.getBoolean("unlocked"));

                npcSkills.computeIfAbsent(npcId, k -> new ConcurrentHashMap<>())
                    .put(skill, skillData);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载NPC技能数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveSkill(long npcId, NPCSkillData skillData) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "REPLACE INTO " + plugin.getDatabaseManager().getTablePrefix() +
                "npc_skills (npc_id, skill_name, level, experience, unlocked) VALUES (?, ?, ?, ?, ?)"
            );

            stmt.setLong(1, npcId);
            stmt.setString(2, skillData.getSkill().name());
            stmt.setInt(3, skillData.getLevel());
            stmt.setInt(4, skillData.getExperience());
            stmt.setBoolean(5, skillData.isUnlocked());

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存NPC技能数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 获取NPC的所有技能
    public Map<NPCSkill, NPCSkillData> getSkills(NationNPC npc) {
        return npcSkills.computeIfAbsent(npc.getId(), k -> new ConcurrentHashMap<>());
    }

    // 获取NPC的所有技能（通过ID）
    public Map<NPCSkill, NPCSkillData> getNPCSkills(long npcId) {
        return npcSkills.get(npcId);
    }

    // 解锁技能
    public boolean unlockSkill(NationNPC npc, NPCSkill skill, Player player) {
        Map<NPCSkill, NPCSkillData> skills = getSkills(npc);
        NPCSkillData skillData = skills.computeIfAbsent(skill, NPCSkillData::new);
        
        if (skillData.isUnlocked()) {
            player.sendMessage(MessageUtil.error("该技能已解锁！"));
            return false;
        }

        // 检查金币
        Nation nation = npc.getWorkplace().getNation();
        if (nation.getBalance() < skill.getUnlockCost()) {
            player.sendMessage(MessageUtil.error("国库资金不足！需要: " + skill.getUnlockCost() + " 金币"));
            return false;
        }

        // 扣除金币
        nation.setBalance(nation.getBalance() - skill.getUnlockCost());
        // 记录交易
        plugin.getNationManager().recordTransaction(
            nation, 
            null, 
            TransactionType.WITHDRAW, 
            skill.getUnlockCost(),
            "解锁NPC技能: " + skill.getDisplayName()
        );
        
        // 解锁技能
        skillData.unlock();
        saveSkill(npc.getId(), skillData);
        
        // 发送成功消息
        player.sendMessage(MessageUtil.success("成功解锁技能: " + skill.getDisplayName()));
        return true;
    }

    // 升级技能
    public boolean upgradeSkill(NationNPC npc, NPCSkill skill, Player player) {
        Map<NPCSkill, NPCSkillData> skills = getSkills(npc);
        NPCSkillData skillData = skills.get(skill);
        
        if (skillData == null || !skillData.isUnlocked()) {
            player.sendMessage(MessageUtil.error("该技能未解锁！"));
            return false;
        }

        if (!skillData.canLevelUp()) {
            player.sendMessage(MessageUtil.error("该技能已达到最高等级！"));
            return false;
        }

        int cost = skill.getUpgradeCost(skillData.getLevel());
        
        // 检查金币
        Nation nation = npc.getWorkplace().getNation();
        if (nation.getBalance() < cost) {
            player.sendMessage(MessageUtil.error("国库资金不足！需要: " + cost + " 金币"));
            return false;
        }

        // 扣除金币
        nation.setBalance(nation.getBalance() - cost);
        // 记录交易
        plugin.getNationManager().recordTransaction(
            nation, 
            null, 
            TransactionType.WITHDRAW, 
            cost,
            "升级NPC技能: " + skill.getDisplayName() + " 到 " + (skillData.getLevel() + 1) + " 级"
        );
        
        // 升级技能
        skillData.addExperience(skillData.getRequiredExperience());
        saveSkill(npc.getId(), skillData);
        
        // 发送成功消息
        player.sendMessage(MessageUtil.success("成功升级技能: " + skill.getDisplayName() + " 到 " + skillData.getLevel() + " 级"));
        return true;
    }

    public void createTables() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTablePrefix() + "npc_skills (" +
                "npc_id BIGINT NOT NULL, " +
                "skill_name VARCHAR(32) NOT NULL, " +
                "level INT NOT NULL DEFAULT 0, " +
                "experience INT NOT NULL DEFAULT 0, " +
                "unlocked BOOLEAN NOT NULL DEFAULT FALSE, " +
                "PRIMARY KEY (npc_id, skill_name), " +
                "FOREIGN KEY (npc_id) REFERENCES " + plugin.getDatabaseManager().getTablePrefix() + 
                "npcs(id) ON DELETE CASCADE" +
                ")"
            );
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("创建NPC技能表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public double getSkillEffectiveness(NationNPC npc, NPCSkill skill) {
        Map<NPCSkill, NPCSkillData> skills = npcSkills.get(npc.getId());
        if (skills == null) return 0;

        NPCSkillData skillData = skills.get(skill);
        if (skillData == null || !skillData.isUnlocked()) return 0;

        String configPath = "npc.skills." + skill.name();
        ConfigurationSection skillConfig = plugin.getConfig().getConfigurationSection(configPath);
        if (skillConfig == null) return 0;

        double baseEffectiveness = skillConfig.getDouble("effectiveness_per_level", 0.1);
        return baseEffectiveness * skillData.getLevel();
    }
}
