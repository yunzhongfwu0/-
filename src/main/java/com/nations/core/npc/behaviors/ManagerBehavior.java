package com.nations.core.npc.behaviors;

import com.nations.core.npc.NPCBehavior;
import com.nations.core.models.NationNPC;
import com.nations.core.models.WorkState;
import com.nations.core.models.Building;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

public class ManagerBehavior implements NPCBehavior {

    @Override
    public void performWork(NationNPC npc) {
        if(npc.getState() != WorkState.WORKING) return;
        
        // 提供建筑效率加成
        Building workplace = npc.getWorkplace();
        if(workplace != null) {
            double bonus = 0.1 * npc.getLevel(); // 每级10%加成
            workplace.addEfficiencyBonus("manager_" + npc.getId(), bonus);
        }
    }

    @Override
    public void onSpawn(NationNPC npc) {
        npc.getInventory().addItem(new ItemStack(Material.BOOK));
    }

    @Override
    public void onDespawn(NationNPC npc) {
        // 移除效率加成
        Building workplace = npc.getWorkplace();
        if(workplace != null) {
            workplace.removeEfficiencyBonus("manager_" + npc.getId());
        }
        npc.getInventory().clear();
    }

    @Override
    public void setupEquipment(NationNPC npc) {
        Equipment equipment = npc.getCitizensNPC().getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.GOLDEN_HELMET));
        equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.BOOK));
    }
} 