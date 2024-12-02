package com.nations.core.listeners;

import com.nations.core.NationsCore;
import com.nations.core.models.NationNPC;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NPCInteractListener implements Listener {
    private final NationsCore plugin;

    public NPCInteractListener(NationsCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        Player player = event.getClicker();
        
        // 遍历所有NPC找到被点击的那个
        for (NationNPC npc : plugin.getNPCManager().getAllNPCs()) {
            if (npc.getCitizensNPC() == event.getNPC()) {
                // 检查玩家是否有权限查看NPC背包
                if (npc.getWorkplace().getNation().isMember(player.getUniqueId())) {
                    // 打开NPC背包
                    player.openInventory(npc.getInventory());
                    player.sendMessage("§a打开 " + npc.getCitizensNPC().getName() + " 的背包");
                } else {
                    player.sendMessage("§c你没有权限查看这个NPC的背包");
                }
                break;
            }
        }
    }
} 