package com.nations.core.combat;

import com.nations.core.models.Soldier;
import java.util.List;
import lombok.Getter;

@Getter
public class BattleResult {
    private final boolean attackersWin;
    private final List<Soldier> casualties;
    private final double attackPower;
    private final double defensePower;
    
    public BattleResult(boolean attackersWin, List<Soldier> casualties, double attackPower, double defensePower) {
        this.attackersWin = attackersWin;
        this.casualties = casualties;
        this.attackPower = attackPower;
        this.defensePower = defensePower;
    }
    
    public String getFormattedResult() {
        StringBuilder result = new StringBuilder();
        result.append("§6战斗结果:\n");
        result.append(attackersWin ? "§a进攻方胜利！" : "§c防守方胜利！").append("\n");
        result.append("§7进攻力量: §f").append(String.format("%.1f", attackPower)).append("\n");
        result.append("§7防御力量: §f").append(String.format("%.1f", defensePower)).append("\n");
        result.append("§7阵亡数量: §f").append(casualties.size());
        return result.toString();
    }
    
    public double getWinnerPowerAdvantage() {
        return attackersWin ? 
            attackPower / defensePower : 
            defensePower / attackPower;
    }
    
    public double getCasualtyRate() {
        return (double) casualties.size() / (attackPower + defensePower);
    }
} 