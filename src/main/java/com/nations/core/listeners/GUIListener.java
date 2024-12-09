package com.nations.core.listeners;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.gui.SoldierManageGUI;
import com.nations.core.models.Nation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class GUIListener implements Listener {
    private final NationsCore plugin;
    private ClickType lastClickType;
    
    public GUIListener(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    public ClickType getLastClickType() {
        return lastClickType;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGUI gui) {
            lastClickType = event.getClick();
            event.setCancelled(true);
            
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
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