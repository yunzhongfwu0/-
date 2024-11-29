package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class InviteGUI extends BaseGUI {
    private final Nation nation;
    
    public InviteGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6邀请玩家", 6);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        List<Player> availablePlayers = new ArrayList<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!nation.isMember(p.getUniqueId()) && 
                plugin.getNationManager().getNationByPlayer(p).isEmpty()) {
                availablePlayers.add(p);
            }
        }
        
        int slot = 10;
        for (Player target : availablePlayers) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName("§f" + target.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7点击邀请该玩家");
            meta.setLore(lore);
            head.setItemMeta(meta);
            
            setItem(slot, head, p -> {
                plugin.getServer().dispatchCommand(p, "nation invite " + target.getName());
                p.closeInventory();
            });
            
            slot++;
            if (slot % 9 == 8) slot += 3;
        }
        
        // 返回按钮
        setItem(49, createItem(Material.ARROW,
            "§f返回",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
} 