package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import com.nations.core.utils.ChatInputManager;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class SettingsGUI extends BaseGUI {
    private final Nation nation;
    
    public SettingsGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6国家设置 - " + nation.getName(), 3);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 重命名国家
        if (nation.hasPermission(player.getUniqueId(), "nation.rename")) {
            setItem(10, createItem(Material.NAME_TAG,
                "§6重命名国家",
                "§7点击重命名国家",
                "",
                "§e要求:",
                "§7- 名称长度: " + plugin.getConfig().getInt("nations.min-name-length") + 
                    " - " + plugin.getConfig().getInt("nations.max-name-length") + " 个字符",
                "§7- 允许中文、字母、数字和下划线",
                "§7- 不能与已有国家重名"
            ), p -> {
                p.closeInventory();
                p.sendMessage("§a请在聊天栏输入新的国家名称，或输入 'cancel' 取消");
                ChatInputManager.awaitChatInput(p, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage("§c已取消重命名。");
                        new SettingsGUI(plugin, p, nation).open();
                        return;
                    }
                    
                    // 检查名称长度
                    int minLength = plugin.getConfig().getInt("nations.min-name-length", 2);
                    int maxLength = plugin.getConfig().getInt("nations.max-name-length", 16);
                    if (input.length() < minLength || input.length() > maxLength) {
                        p.sendMessage("§c国家名称长度必须在 " + minLength + " 到 " + maxLength + " 个字符之间！");
                        new SettingsGUI(plugin, p, nation).open();
                        return;
                    }
                    
                    // 检查名称格式
                    String nameRegex = plugin.getConfig().getString("nations.name-regex", "^[\u4e00-\u9fa5a-zA-Z0-9_]+$");
                    if (!input.matches(nameRegex)) {
                        p.sendMessage("§c国家名称只能包含中文、字母、数字和下划线！");
                        new SettingsGUI(plugin, p, nation).open();
                        return;
                    }
                    
                    // 检查是否已存在同名国家
                    if (plugin.getNationManager().getNationByName(input).isPresent()) {
                        p.sendMessage("§c已存在同名的国家！");
                        new SettingsGUI(plugin, p, nation).open();
                        return;
                    }
                    
                    // 执行重命名
                    if (plugin.getNationManager().renameNation(nation, input, 0)) {
                        p.sendMessage("§a成功将国家重命名为: " + input);
                        // 广播消息
                        plugin.getServer().broadcast(
                            Component.text("§e国家 " + nation.getName() + " 已更名为 " + input)
                        );
                        new SettingsGUI(plugin, p, nation).open();
                    } else {
                        p.sendMessage("§c重命名失败！");
                        new SettingsGUI(plugin, p, nation).open();
                    }
                });
            });
        }
        
        // 设置传送点
        if (nation.hasPermission(player.getUniqueId(), "nation.setspawn")) {
            List<String> spawnLore = new ArrayList<>();
            spawnLore.add("§7点击设置传送点");
            spawnLore.add("");
            
            Location currentSpawn = nation.getSpawnPoint();
            if (currentSpawn != null) {
                spawnLore.add("§e当前传送点:");
                spawnLore.add("§7世界: §f" + currentSpawn.getWorld().getName());
                spawnLore.add(String.format("§7坐标: §f%.1f, %.1f, %.1f", 
                    currentSpawn.getX(), currentSpawn.getY(), currentSpawn.getZ()));
            } else {
                spawnLore.add("§c尚未设置传送点");
            }
            
            spawnLore.add("");
            spawnLore.add("§e要求:");
            spawnLore.add("§7- 必须在国家领地内");
            spawnLore.add("§7- 必须站在安全的位置");
            
            setItem(13, createItem(Material.ENDER_PEARL,
                "§6设置传送点",
                spawnLore.toArray(new String[0])
            ), p -> {
                // 检查是否在领地内
                if (nation.getTerritory() == null || !nation.getTerritory().contains(p.getLocation())) {
                    p.sendMessage("§c你必须在国家领地内设置传送点！");
                    return;
                }
                
                // 检查位置是否安全
                Location loc = p.getLocation();
                if (!isSafeLocation(loc)) {
                    p.sendMessage("§c当前位置不安全，请选择一个安全的位置！");
                    return;
                }
                
                // 设置传送点
                if (plugin.getNationManager().setSpawnPoint(nation, loc)) {
                    p.sendMessage("§a成功设置国家传送点！");
                    new SettingsGUI(plugin, p, nation).open();
                } else {
                    p.sendMessage("§c设置传送点失败！");
                }
            });
        }
        
        // 删除国家
        if (player.getUniqueId().equals(nation.getOwnerUUID())) {
            setItem(16, createItem(Material.BARRIER,
                "§c删除国家",
                "§7点击删除国家",
                "§c警告：此操作不可逆！"
            ), p -> new DeleteConfirmGUI(plugin, p, nation).open());
        }
        
        // 返回按钮
        setItem(22, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
    
    /**
     * 检查位置是否安全
     */
    private boolean isSafeLocation(Location loc) {
        // 检查脚下的方块
        Location ground = loc.clone().subtract(0, 1, 0);
        if (!ground.getBlock().getType().isSolid()) {
            return false;
        }
        
        // 检查玩家位置和头顶的方块
        return loc.getBlock().getType().isAir() && 
               loc.clone().add(0, 1, 0).getBlock().getType().isAir();
    }
} 