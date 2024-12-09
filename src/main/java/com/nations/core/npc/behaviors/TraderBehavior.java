package com.nations.core.npc.behaviors;

import com.nations.core.npc.NPCBehavior;
import com.nations.core.NationsCore;
import com.nations.core.models.NationNPC;
import com.nations.core.models.WorkState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Equipment.EquipmentSlot;

public class TraderBehavior extends AbstractNPCBehavior {

    @Override
    public void performWork(NationNPC npc) {
        // 添加调试日志
        NationsCore.getInstance().getLogger().info(String.format(
            "商人 %s 执行工作检查, 当前状态: %s, 体力: %d, 时间: %d",
            npc.getCitizensNPC().getName(),
            npc.getState(),
            npc.getEnergy(),
            npc.getCitizensNPC().getEntity().getWorld().getTime()
        ));

        if(npc.getState() != WorkState.WORKING) {
            NationsCore.getInstance().getLogger().info(String.format(
                "商人 %s 不在工作状态，跳过工作", 
                npc.getCitizensNPC().getName()
            ));
            return;
        }
        
        // 更新交易列表
        updateTrades(npc);
        
        // 自动交易逻辑
        if(npc.getInventory().containsAtLeast(new ItemStack(Material.WHEAT), 20)) {
            // 尝试卖出小麦
            sellItems(npc);
        }
    }

    @Override
    public void onSpawn(NationNPC npc) {
        setupTrades(npc);
    }

    @Override
    public void onDespawn(NationNPC npc) {
        npc.getInventory().clear();
    }

    @Override
    public void setupEquipment(NationNPC npc) {
        Equipment equipment = npc.getCitizensNPC().getOrAddTrait(Equipment.class);
        equipment.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.LEATHER_CHESTPLATE));
    }

    private void setupTrades(NationNPC npc) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        
        // 添加基础交易
        MerchantRecipe wheatTrade = new MerchantRecipe(
            new ItemStack(Material.EMERALD),
            20
        );
        wheatTrade.addIngredient(new ItemStack(Material.WHEAT, 20));
        recipes.add(wheatTrade);
        
        // 设置交易列表
        Merchant merchant = (Merchant)npc.getCitizensNPC().getEntity();
        merchant.setRecipes(recipes);
    }

    private void updateTrades(NationNPC npc) {
        // 根据经验等级更新交易
        int level = npc.getLevel();
        // 实现交易更新逻辑
    }

    private void sellItems(NationNPC npc) {
        // 实现自动销售逻辑
    }
} 