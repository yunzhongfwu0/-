package com.nations.core.listeners;

import com.nations.core.gui.BaseGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;

public class GUIListener implements Listener {
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGUI gui) {
            // 取消事件以防止物品被移动
            event.setCancelled(true);
            
            // 确保点击的是GUI界面而不是玩家背包
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                // 根据点击类型调用不同的处理器
                if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    gui.handleRightClick(event);
                } else {
                    gui.handleLeftClick(event);
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGUI gui) {
            gui.handleDrag(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGUI gui) {
            gui.handleClose(event);
        }
    }
} 