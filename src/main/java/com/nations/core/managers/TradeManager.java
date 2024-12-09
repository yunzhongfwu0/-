package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.Building;
import com.nations.core.models.Nation;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {
    private final NationsCore plugin;
    private final Map<Long, Map<UUID, Trade>> activeTrades = new HashMap<>();
    
    public TradeManager(NationsCore plugin) {
        this.plugin = plugin;
    }
    
    public void cancelTradesByBuilding(Building building) {
        Map<UUID, Trade> trades = activeTrades.get(building.getId());
        if (trades != null) {
            // 返还所有交易中的物品
            trades.forEach((uuid, trade) -> {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && trade.getItems() != null) {
                    for (ItemStack item : trade.getItems()) {
                        if (item != null) {
                            player.getInventory().addItem(item);
                        }
                    }
                    player.sendMessage("§c由于市场被拆除，你的交易已被取消！");
                }
            });
            
            // 从数据库删除交易记录
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                    "trades WHERE building_id = ?"
                );
                stmt.setLong(1, building.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("取消交易失败: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 清除缓存
            activeTrades.remove(building.getId());
        }
    }
    
    private static class Trade {
        private final UUID playerUuid;
        private final ItemStack[] items;
        private final double price;
        
        public Trade(UUID playerUuid, ItemStack[] items, double price) {
            this.playerUuid = playerUuid;
            this.items = items;
            this.price = price;
        }
        
        public UUID getPlayerUuid() {
            return playerUuid;
        }
        
        public ItemStack[] getItems() {
            return items;
        }
        
        public double getPrice() {
            return price;
        }
    }
} 