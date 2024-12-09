package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.BuildingType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BaseGUI implements InventoryHolder {
    protected final NationsCore plugin;
    protected final Player player;
    protected final Inventory inventory;
    protected final Map<Integer, Consumer<Player>> leftClickHandlers = new HashMap<>();
    protected final Map<Integer, Consumer<Player>> rightClickHandlers = new HashMap<>();
    protected ClickType lastClickType;
    
    public BaseGUI(NationsCore plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    protected void fillBorder(Material material) {
        int size = inventory.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, new ItemStack(material));
            }
        }
    }
    
    protected void setItem(int slot, ItemStack item, Consumer<Player> leftClickHandler) {
        setItem(slot, item, leftClickHandler, null);
    }
    
    protected void setItem(int slot, ItemStack item, Consumer<Player> leftClickHandler, Consumer<Player> rightClickHandler) {
        inventory.setItem(slot, item);
        if (leftClickHandler != null) {
            leftClickHandlers.put(slot, leftClickHandler);
        }
        if (rightClickHandler != null) {
            rightClickHandlers.put(slot, rightClickHandler);
        }
    }
    
    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
    
    public void handleLeftClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        Consumer<Player> handler = leftClickHandlers.get(slot);
        if (handler != null) {
            handler.accept((Player) event.getWhoClicked());
        }
    }
    
    public void handleRightClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        lastClickType = event.getClick();
        Consumer<Player> handler = rightClickHandlers.get(slot);
        if (handler != null) {
            handler.accept((Player) event.getWhoClicked());
        }
    }
    
    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
    
    public void handleClose(InventoryCloseEvent event) {
        // 默认的关闭处理，子类可以重写此方法
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public void close() {
        if (player.getOpenInventory().getTopInventory().equals(inventory)) {
            player.closeInventory();
        }
    }
    
    protected Material getBuildingMaterial(BuildingType type) {
        return switch (type) {
            case TOWN_HALL -> Material.BEACON;
            case BARRACKS -> Material.IRON_SWORD;
            case MARKET -> Material.EMERALD;
            case WAREHOUSE -> Material.CHEST;
            case FARM -> Material.WHEAT;
        };
    }
} 