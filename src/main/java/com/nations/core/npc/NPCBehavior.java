package com.nations.core.npc;

import com.nations.core.models.NationNPC;

public interface NPCBehavior {
    void performWork(NationNPC npc);
    void onSpawn(NationNPC npc);
    void onDespawn(NationNPC npc);
    void setupEquipment(NationNPC npc);
} 