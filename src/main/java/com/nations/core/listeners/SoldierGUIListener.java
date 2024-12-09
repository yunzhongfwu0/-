package com.nations.core.listeners;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import com.nations.core.training.TrainingSystem;
import com.nations.core.gui.SoldierRecruitGUI;
import com.nations.core.gui.SoldierManageGUI;
import com.nations.core.gui.TrainingSelectGUI;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SoldierGUIListener implements Listener {
    private final NationsCore plugin;
    
    public SoldierGUIListener(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals("§6士兵管理") && !title.equals("§6招募士兵")) return;
        
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        if (title.equals("§6士兵管理")) {
            handleManageGUI(player, clicked, event.getSlot(), event);
        } else if (title.equals("§6招募士兵")) {
            handleRecruitGUI(player, clicked, event.getSlot());
        }
    }
    
    private void handleManageGUI(Player player, ItemStack clicked, int slot, InventoryClickEvent event) {
        if (slot == 49) {
            // 打开招募界面
            Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
            if (nation.isPresent()) {
                new SoldierRecruitGUI(plugin, player, nation.get()).open();
            } else {
                player.sendMessage("§c你没有国家！");
            }
        } else if (slot < 45) {
            // 点击士兵
            if (clicked.hasItemMeta()) {
                if (event.isRightClick() && event.isShiftClick()) {
                    handleDismiss(player, clicked);
                } else if (event.isRightClick()) {
                    handleTrain(player, clicked);
                } else {
                    handleInfo(player, clicked);
                }
            }
        }
    }
    
    private void handleRecruitGUI(Player player, ItemStack clicked, int slot) {
        if (slot == 26) {
            // 返回按钮
            Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
            if (nation.isPresent()) {
                new SoldierManageGUI(plugin, player, nation.get()).open();
            }
            return;
        }
        
        // 处理招募
        ItemMeta meta = clicked.getItemMeta();
        if (meta != null && meta.getDisplayName().startsWith("§6")) {
            final String typeName = meta.getDisplayName().substring(2);
            try {
                SoldierType type = Arrays.stream(SoldierType.values())
                    .filter(t -> t.getDisplayName().equals(typeName))
                    .findFirst()
                    .orElse(null);
                
                if (type != null) {
                    Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
                    if (nation.isPresent()) {
                        Set<Building> barracks = nation.get().getBuildingsByType(BuildingType.BARRACKS);
                        if (!barracks.isEmpty()) {
                            Building barrack = barracks.iterator().next(); // 获取第一个兵营
                            String name = type.getDisplayName() + "#" + 
                                (plugin.getSoldierManager().getSoldiersByPlayer(player.getUniqueId()).size() + 1);
                                
                            if (plugin.getSoldierManager().recruitSoldier(player, barrack, type, name)) {
                                player.sendMessage("§a成功招募 " + type.getDisplayName() + "！");
                                new SoldierManageGUI(plugin, player, nation.get()).open();
                            } else {
                                player.sendMessage("§c招募失败！请检查兵营等级和容量。");
                            }
                        } else {
                            player.sendMessage("§c你的国家没有兵营！");
                        }
                    } else {
                        player.sendMessage("§c你没有国家！");
                    }
                }
            } catch (Exception e) {
                player.sendMessage("§c招募失败！");
            }
        }
    }
    
    private void handleDismiss(Player player, ItemStack item) {
        String name = item.getItemMeta().getDisplayName().substring(2); // 移除颜色代码
        
        // 获取玩家的所有士兵
        Set<Soldier> soldiers = plugin.getSoldierManager().getSoldiersByPlayer(player.getUniqueId());
        
        // 查找匹配的士兵
        Soldier target = soldiers.stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
        
        if (target != null) {
            if (plugin.getSoldierManager().dismissSoldier(target)) {
                player.sendMessage("§a已解雇士兵 " + name + "！");
                player.closeInventory();
            } else {
                player.sendMessage("§c解雇失败！");
            }
        }
    }
    
    private void handleTrain(Player player, ItemStack item) {
        String name = item.getItemMeta().getDisplayName().substring(2);
        
        Set<Soldier> soldiers = plugin.getSoldierManager().getSoldiersByPlayer(player.getUniqueId());
        Soldier target = soldiers.stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
        
        if (target != null) {
            Optional<Nation> nation = plugin.getNationManager().getNationByPlayer(player);
            if (nation.isPresent()) {
                new TrainingSelectGUI(plugin, player, nation.get(), target).open();
            } else {
                player.sendMessage("§c你没有国家！");
            }
        }
    }
    
    private void handleInfo(Player player, ItemStack item) {
        
        String name = item.getItemMeta().getDisplayName().substring(2);
        
        Set<Soldier> soldiers = plugin.getSoldierManager().getSoldiersByPlayer(player.getUniqueId());
        Soldier target = soldiers.stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
        
        if (target != null) {
            player.closeInventory();
            
            player.sendMessage("§6=== 士兵详情 ===");
            player.sendMessage("§7名称: §f" + target.getName());
            player.sendMessage("§7类型: §f" + target.getType().getDisplayName());
            player.sendMessage("§7等级: §f" + target.getLevel());
            player.sendMessage("§7经验: §f" + target.getExperience() + "/" + (target.getLevel() * 100));
            
            Map<String, Double> attrs = target.getAttributes();
            player.sendMessage("§7生命值: §f" + String.format("%.1f", attrs.get("health")));
            player.sendMessage("§7攻击力: §f" + String.format("%.1f", attrs.get("attack")));
            player.sendMessage("§7防御力: §f" + String.format("%.1f", attrs.get("defense")));
        }
    }
} 