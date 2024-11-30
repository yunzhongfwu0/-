package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;
import com.nations.core.utils.MessageUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TerritoryDetailGUI extends BaseGUI {
    private final Nation nation;
    
    public TerritoryDetailGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, MessageUtil.title("领土管理 - " + nation.getName()), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示当前领土信息
        if (nation.getTerritory() != null) {
            setItem(13, createItem(Material.MAP,
                MessageUtil.title("当前领土信息"),
                MessageUtil.createStatusLore("领土状态",
                    "中心: " + nation.getTerritory().getCenterX() + ", " + nation.getTerritory().getCenterZ(),
                    "范围: " + nation.getTerritory().getRadius() * 2 + "x" + nation.getTerritory().getRadius() * 2,
                    "世界: " + nation.getTerritory().getWorldName()
                ).toArray(new String[0])
            ), null);
        } else {
            setItem(13, createItem(Material.BARRIER,
                MessageUtil.title("未设置领土"),
                MessageUtil.createStatusLore("领土状态",
                    "该国家还没有领土"
                ).toArray(new String[0])
            ), null);
        }
        
        // 修改领土范围
        setItem(11, createItem(Material.REDSTONE,
            MessageUtil.title("修改领土范围"),
            MessageUtil.createLore("范围设置",
                "点击修改领土范围",
                "",
                "当前范围: " + (nation.getTerritory() != null ? 
                    nation.getTerritory().getRadius() * 2 + "x" + nation.getTerritory().getRadius() * 2 : "未设置"),
                "",
                "§e要求:",
                "- 范围必须在 10-1000 之间",
                "- 不能与其他国家重叠"
            ).toArray(new String[0])
        ), p -> {
            p.closeInventory();
            p.sendMessage(MessageUtil.tip("请输入新的领土范围（边长），或输入 'cancel' 取消"));
            ChatInputManager.awaitChatInput(p, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    p.sendMessage(MessageUtil.info("已取消操作。"));
                    new TerritoryDetailGUI(plugin, p, nation).open();
                    return;
                }
                try {
                    int size = Integer.parseInt(input);
                    if (size < 10 || size > 1000) {
                        p.sendMessage(MessageUtil.error("范围必须在 10-1000 之间！"));
                        return;
                    }
                    if (plugin.getNationManager().setTerritorySize(nation, size/2)) {
                        p.sendMessage(MessageUtil.success("成功设置领土范围为: " + size + "x" + size));
                        new TerritoryDetailGUI(plugin, p, nation).open();
                    } else {
                        p.sendMessage(MessageUtil.error("设置领土范围失败！可能与其他国家领土重叠。"));
                    }
                } catch (NumberFormatException e) {
                    p.sendMessage(MessageUtil.error("无效的数字！"));
                }
            });
        });
        
        // 重设领土中心
        setItem(15, createItem(Material.COMPASS,
            MessageUtil.title("重设领土中心"),
            MessageUtil.createLore("中心设置",
                "点击将领土中心设为当前位置",
                "",
                "§e要求:",
                "- 必须在安全的位置",
                "- 不能与其他国家重叠"
            ).toArray(new String[0])
        ), p -> {
            if (plugin.getNationManager().setTerritoryCenter(nation, p.getLocation())) {
                p.sendMessage(MessageUtil.success("成功将领土中心设置为当前位置！"));
                new TerritoryDetailGUI(plugin, p, nation).open();
            } else {
                p.sendMessage(MessageUtil.error("设置领土中心失败！可能与其他国家领土重叠。"));
            }
        });
        
        // 返回按钮
        setItem(26, createItem(Material.ARROW,
            MessageUtil.title("返回"),
            MessageUtil.subtitle("点击返回领土列表")
        ), p -> new TerritoryManageGUI(plugin, p).open());
    }
} 