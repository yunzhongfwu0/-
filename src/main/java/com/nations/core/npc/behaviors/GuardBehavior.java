package com.nations.core.npc.behaviors;

import com.nations.core.npc.NPCBehavior;
import com.nations.core.models.NationNPC;
import com.nations.core.models.WorkState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

public class GuardBehavior implements NPCBehavior {

    @Override
    public void performWork(NationNPC npc) {
        if(npc.getState() != WorkState.WORKING) return;
        
        Location guardPos = npc.getWorkPosition();
        if(guardPos == null) return;

        // 搜索附近的敌对生物
        guardPos.getWorld().getNearbyEntities(guardPos, 10, 10, 10).stream()
            .filter(this::isHostile)
            .findFirst()
            .ifPresent(target -> {
                // 追击敌对生物
                npc.setState(WorkState.TRAVELING);
                npc.getCitizensNPC().getNavigator().setTarget(target.getLocation());
            });
    }

    @Override
    public void onSpawn(NationNPC npc) {
        // 初始化守卫装备
        npc.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        npc.getInventory().addItem(new ItemStack(Material.SHIELD));
    }

    @Override
    public void onDespawn(NationNPC npc) {
        npc.getInventory().clear();
    }

    @Override
    public void setupEquipment(NationNPC npc) {
        Equipment equipment = npc.getCitizensNPC().getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.IRON_SWORD));
        equipment.set(Equipment.EquipmentSlot.OFF_HAND, new ItemStack(Material.SHIELD));
        equipment.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.IRON_CHESTPLATE));
    }

    private boolean isHostile(Entity entity) {
        return entity instanceof Monster;
    }
} 