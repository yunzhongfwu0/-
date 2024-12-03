package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.NationNPC;
import com.nations.core.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class NPCManageGUI extends BaseGUI {
    private final NationNPC npc;
    private final boolean isOwner;

    public NPCManageGUI(NationsCore plugin, Player player, NationNPC npc, boolean isOwner) {
        super(plugin, player, "NPC管理 - " + npc.getCitizensNPC().getName(), 3);
        this.npc = npc;
        this.isOwner = isOwner;
        initialize();
    }

    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);

        // NPC信息
        setItem(4, createItem(npc.getType().getIcon(),
            "§6" + npc.getCitizensNPC().getName(),
            "§7类型: §f" + npc.getType().getDisplayName(),
            "§7等级: §f" + npc.getLevel(),
            "§7经验: §f" + npc.getExperience(),
            "§7心情: §f" + npc.getHappiness() + "%",
            "§7体力: §f" + npc.getEnergy() + "%"
        ), p -> {});

        // 技能按钮
        setItem(11, createItem(Material.BOOK,
            "§6技能管理",
            "§7管理NPC的技能",
            "",
            "§e点击打开技能界面"
        ), p -> new NPCSkillGUI(plugin, p, npc).open());

        // 背包按钮
        setItem(13, createItem(Material.CHEST,
            "§6背包管理",
            "§7管理NPC的背包",
            "",
            "§e点击打开背包"
        ), p -> p.openInventory(npc.getInventory()));


        // 返回按钮
        setItem(22, createItem(Material.ARROW,
            "§6返回",
            "§7返回上一级菜单"
        ), p -> p.closeInventory());
    }
}
