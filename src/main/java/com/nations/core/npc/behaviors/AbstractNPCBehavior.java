package com.nations.core.npc.behaviors;

import com.nations.core.models.NationNPC;
import com.nations.core.npc.NPCBehavior;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public abstract class AbstractNPCBehavior implements NPCBehavior {
    @Override
    public void lookAtNearestPlayer(NationNPC npc, double range) {
        if (npc == null || !npc.getCitizensNPC().isSpawned()) return;
        
        NPC citizensNPC = npc.getCitizensNPC();
        Entity npcEntity = citizensNPC.getEntity();
        Location npcLoc = npcEntity.getLocation();
        Player nearest = null;
        double closestDistance = range;

        // 寻找最近的玩家
        for (Entity entity : npcLoc.getWorld().getNearbyEntities(npcLoc, range, range, range)) {
            if (entity instanceof Player && !entity.hasMetadata("NPC")) {
                double distance = entity.getLocation().distance(npcLoc);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nearest = (Player) entity;
                }
            }
        }

        // 如果找到玩家，让NPC看向玩家
        if (nearest != null) {
            final Player target = nearest; // 创建final引用供lambda使用
            Location playerLoc = target.getLocation().clone();
            
            // 计算朝向
            Vector direction = playerLoc.subtract(npcLoc).toVector();
            Location lookLoc = npcLoc.clone();
            lookLoc.setDirection(direction);

            // 使用Citizens的API来设置朝向
            if (!citizensNPC.getNavigator().isNavigating()) {
                // 如果NPC没有在移动，直接设置朝向
                citizensNPC.faceLocation(playerLoc);
            } else {
                // 如果NPC在移动，只在停下来时更新朝向
                Navigator navigator = citizensNPC.getNavigator();
                navigator.getLocalParameters()
                    .lookAtFunction((npc1) -> target.getLocation())
                    .attackRange(5.0);
            }
        }
    }
} 