package com.nations.core.listeners;

import com.nations.core.NationsCore;
import com.nations.core.gui.NPCManageGUI;
import com.nations.core.models.Nation;
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
            if (npc.getCitizensNPC().getId() == event.getNPC().getId()) {
                if (npc.getWorkplace() == null) {
                    player.sendMessage("§c该NPC没有工作场所！");
                    return;
                }
                
                Nation nation = npc.getWorkplace().getNation();
                
                // 检查是否有权限查看NPC界面
                if (!nation.isMember(player.getUniqueId()) && !player.hasPermission("nations.admin")) {
                    if (!nation.getOwnerUUID().equals(player.getUniqueId())) {
                        player.sendMessage("§c你不是该NPC所属国家的成员！");
                        return;
                    }
                }
                
                // 打开NPC管理界面
                new NPCManageGUI(plugin, player, npc, nation.getOwnerUUID().equals(player.getUniqueId())).open();
                return;
            }
        }
    }
}