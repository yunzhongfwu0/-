package com.nations.core.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SoldierStats {
    private final long soldierId;
    private int kills;
    private int deaths;
    private int battlesWon;
    private int battlesLost;
    private double damageDealt;
    private double damageTaken;
    
    public SoldierStats(long soldierId) {
        this.soldierId = soldierId;
    }
    
    public double getKDRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
    
    public double getWinRate() {
        int totalBattles = battlesWon + battlesLost;
        return totalBattles == 0 ? 0 : (double) battlesWon / totalBattles;
    }
} 