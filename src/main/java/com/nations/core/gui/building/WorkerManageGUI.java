package com.nations.core.gui.building;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.gui.NPCSkillGUI;
import com.nations.core.models.*;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WorkerManageGUI extends BaseGUI {
    private final Building building;
    private final NationNPC selectedNPC;

    public WorkerManageGUI(NationsCore plugin, Player player, Building building) {
        this(plugin, player, building, null);
    }

    public WorkerManageGUI(NationsCore plugin, Player player, Building building, NationNPC selectedNPC) {
        super(plugin, player, "§6工人管理", 6);
        this.building = building;
        this.selectedNPC = selectedNPC;
        initialize();
    }

    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);

        if (selectedNPC == null) {
            showWorkerList();
        } else {
            showWorkerDetails();
        }
    }

    private void showWorkerList() {
        // 显示建筑信息
        List<String> lore = new ArrayList<>();
        lore.add("§7等级: §f" + building.getLevel());
        lore.add("§7工人上限:");
        building.getType().getWorkerSlots().forEach((type, count) -> 
            lore.add(String.format("§7- %s: §f%d", type.getDisplayName(), count)));

        setItem(4, createItem(building.getType().getIcon(),
            "§6" + building.getType().getDisplayName(),
            lore.toArray(new String[0])
        ), null);

        // 获取当前工人
        List<NationNPC> workers = plugin.getNPCManager().getBuildingWorkers(building);
        Map<NPCType, Integer> slots = building.getType().getWorkerSlots();

        // 显示工人列表
        int slot = 19;
        for (NationNPC worker : workers) {
            setItem(slot++, createWorkerItem(worker), 
                p -> new WorkerManageGUI(plugin, p, building, worker).open());
        }

        // 显示可雇佣按钮
        for (Map.Entry<NPCType, Integer> entry : slots.entrySet()) {
            NPCType type = entry.getKey();
            int maxSlots = entry.getValue();
            long currentCount = workers.stream().filter(w -> w.getType() == type).count();

            if (currentCount < maxSlots) {
                setItem(slot++, createHireItem(type), 
                    p -> handleHire(p, type));
            }
        }

        // 返回按钮
        setItem(49, createItem(Material.ARROW,
            "§f返回",
            new String[]{"§7返回建筑管理"}
        ), p -> new BuildingInteractGUI(plugin, p, building).open());
    }

    private void showWorkerDetails() {
        // 基本信息 - 简化显示
        List<String> basicLore = Arrays.asList(
            "§7类型: §f" + selectedNPC.getType().getDisplayName(),
            "§7等级: §f" + selectedNPC.getLevel(),
            "§7经验: §f" + selectedNPC.getExperience() + "/" + selectedNPC.getRequiredExperience()
        );

        setItem(4, createItem(selectedNPC.getType().getIcon(),
            "§6" + selectedNPC.getCitizensNPC().getName(),
            basicLore.toArray(new String[0])
        ), null);

        // 状态信息
        List<String> statusLore = Arrays.asList(
            "§7心情: §f" + selectedNPC.getHappiness() + "%",
            "§7体力: §f" + selectedNPC.getEnergy() + "%",
            "§7状态: " + getStateDisplay(selectedNPC.getState())
        );

        setItem(19, createItem(Material.PAPER,
            "§6状态信息",
            statusLore.toArray(new String[0])
        ), null);

        // 技能信息
        List<String> skillLore = new ArrayList<>();
        skillLore.add("§7已解锁技能:");

        Map<NPCSkill, NPCSkillData> skills = plugin.getNPCSkillManager().getSkills(selectedNPC);
        if (skills.isEmpty()) {
            skillLore.add("§7- 暂无技能");
        } else {
            skills.forEach((skill, data) -> {
                if (data.isUnlocked()) {
                    skillLore.add(String.format("§7- %s §f(Lv.%d)",
                        skill.getDisplayName(),
                        data.getLevel()
                    ));
                }
            });
        }

        skillLore.add("");
        skillLore.add("§e点击管理技能");

        setItem(21, createItem(Material.BOOK,
            "§6技能管理",
            skillLore.toArray(new String[0])
        ), p -> new NPCSkillGUI(plugin, p, selectedNPC).open());

        // 背包管理
        List<String> invLore = Arrays.asList(
            "§7已使用: §f" + getUsedSlots(selectedNPC) + "/27",
            "",
            "§e点击打开背包"
        );

        setItem(23, createItem(Material.CHEST,
            "§6背包管理",
            invLore.toArray(new String[0])
        ), p -> p.openInventory(selectedNPC.getInventory()));

        // 工作位置
        List<String> locLore = Arrays.asList(
            "§7当前位置: §f" + formatLocation(selectedNPC.getWorkPosition()),
            "",
            "§e点击传送"
        );

        setItem(25, createItem(Material.COMPASS,
            "§6工作位置",
            locLore.toArray(new String[0])
        ), p -> {
            if (selectedNPC.getWorkPosition() != null) {
                p.teleport(selectedNPC.getWorkPosition());
                p.sendMessage(MessageUtil.success("已传送到工人工作位置"));
            }
        });

        // 解雇按钮
        List<String> fireLore = Arrays.asList(
            "§7将工人解雇",
            "",
            "§c⚠ 此操作不可撤销"
        );

        setItem(40, createItem(Material.BARRIER,
            "§c解雇工人",
            fireLore.toArray(new String[0])
        ), p -> handleFire(p));

        // 返回按钮
        setItem(49, createItem(Material.ARROW,
            "§f返回",
            new String[]{"§7返回工人列表"}
        ), p -> new WorkerManageGUI(plugin, p, building).open());
    }

    private ItemStack createWorkerItem(NationNPC npc) {
        List<String> lore = Arrays.asList(
            "§7类型: §f" + npc.getType().getDisplayName(),
            "§7等级: §f" + npc.getLevel(),
            "§7状态: " + getStateDisplay(npc.getState()),
            "",
            "§e点击查看详情"
        );

        return createItem(npc.getType().getIcon(),
            "§6" + npc.getCitizensNPC().getName(),
            lore.toArray(new String[0])
        );
    }

    private ItemStack createHireItem(NPCType type) {
        return createItem(Material.VILLAGER_SPAWN_EGG,
            "§a雇佣 " + type.getDisplayName(),
            "§7基础工资: §f" + type.getBaseSalary(),
            "§7解锁等级: §f" + type.getUnlockLevel(),
            "",
            "§7描述:",
            "§7" + type.getDescription(),
            "",
            "§e点击雇佣"
        );
    }

    private void handleHire(Player player, NPCType type) {
        // 检查权限
        if (!building.getNation().hasPermission(player.getUniqueId(), "nation.hire")) {
            player.sendMessage(MessageUtil.error("你没有雇佣工人的权限！"));
            return;
        }

        // 检查国家等级
        if (building.getNation().getLevel() < type.getUnlockLevel()) {
            player.sendMessage(MessageUtil.error("需要国家等级达到 " + type.getUnlockLevel() + " 级！"));
            return;
        }

        // 检查金币
        if (!plugin.getVaultEconomy().has(player, type.getBaseSalary())) {
            player.sendMessage(MessageUtil.error("你需要 " + type.getBaseSalary() + " 金币来雇佣工人！"));
            return;
        }

        // 创建NPC
        NationNPC npc = plugin.getNPCManager().createNPC(type, building);
        if (npc != null) {
            plugin.getVaultEconomy().withdrawPlayer(player, type.getBaseSalary());
            player.sendMessage(MessageUtil.success("成功雇佣了一名 " + type.getDisplayName() + "！"));
            new WorkerManageGUI(plugin, player, building).open();
        } else {
            player.sendMessage(MessageUtil.error("雇佣失败！"));
        }
    }

    private void handleFire(Player player) {
        // 检查权限
        if (!building.getNation().hasPermission(player.getUniqueId(), "nation.fire")) {
            player.sendMessage(MessageUtil.error("你没有解雇工人的权限！"));
            return;
        }

        // 解雇NPC
        plugin.getNPCManager().dismissWorker(selectedNPC);
        player.sendMessage(MessageUtil.success("已解雇工人！"));
        new WorkerManageGUI(plugin, player, building).open();
    }

    private String getStateDisplay(WorkState state) {
        return switch (state) {
            case WORKING -> "§a工作中";
            case RESTING -> "§e休息中";
            case TRAVELING -> "§b移动中";
            default -> "§7空闲";
        };
    }

    private int getUsedSlots(NationNPC npc) {
        return (int) Arrays.stream(npc.getInventory().getContents())
            .filter(item -> item != null && item.getType() != Material.AIR)
            .count();
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "无";
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    private double calculateSalary(NationNPC npc) {
        double base = npc.getType().getBaseSalary();
        double levelBonus = base * (npc.getLevel() * 0.1); // 每级增加10%
        return base + levelBonus;
    }
} 