package com.nations.core.models;

import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class NationNPC {
    private final long id;
    private final NPCType type;
    private final Building workplace;
    private final NPC citizensNPC;
    private int level = 1;
    private int experience = 0;
    private int happiness = 100;
    private int energy = 100;
    private Location workPosition;
    private Location restPosition;
    private WorkState state = WorkState.IDLE;
    private Inventory inventory;
    
    public NationNPC(long id, NPCType type, Building workplace, NPC citizensNPC) {
        this.id = id;
        this.type = type;
        this.workplace = workplace;
        this.citizensNPC = citizensNPC;
        this.inventory = Bukkit.createInventory(null, 27, "NPC背包 - " + citizensNPC.getName());
    }
    
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public double getEfficiency() {
        return (level * 0.1 + 1.0) * (happiness / 100.0) * (energy / 100.0);
    }
    
    public int getCurrentSalary() {
        return type.getBaseSalary() * level;
    }
    
    public void gainExperience(int amount) {
        this.experience += amount;
        int nextLevel = level + 1;
        int requiredExp = nextLevel * 100;
        if (experience >= requiredExp) {
            level = nextLevel;
            experience -= requiredExp;
        }
    }
    
    public boolean isValidBasic() {
        return workplace != null && citizensNPC != null;
    }
    
    public long getId() { return id; }
    public NPCType getType() { return type; }
    public Building getWorkplace() { return workplace; }
    public NPC getCitizensNPC() { return citizensNPC; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getExperience() { return experience; }
    public void setExperience(int exp) { this.experience = exp; }
    public int getHappiness() { return happiness; }
    public void setHappiness(int happiness) { this.happiness = happiness; }
    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = energy; }
    public Location getWorkPosition() { return workPosition; }
    public void setWorkPosition(Location pos) { this.workPosition = pos; }
    public Location getRestPosition() { return restPosition; }
    public void setRestPosition(Location pos) { this.restPosition = pos; }
    public WorkState getState() { return state; }
    public void setState(WorkState state) { this.state = state; }
} 