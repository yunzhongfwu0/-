package com.nations.core.combat;

import com.nations.core.models.*;
import java.util.*;
import java.util.stream.Collectors;

public class BattleSystem {
    private static final Random random = new Random();
    
    public static BattleResult simulateBattle(List<Soldier> attackers, List<Soldier> defenders) {
        // 计算双方总战斗力
        double attackPower = calculateTeamPower(attackers);
        double defensePower = calculateTeamPower(defenders) * 1.2; // 防守方有20%加成
        
        // 计算武将加成
        applyGeneralBonus(attackers, defenders);
        
        // 模拟战斗
        double totalPower = attackPower + defensePower;
        double winChance = attackPower / totalPower;
        
        boolean attackersWin = random.nextDouble() < winChance;
        
        // 计算伤亡
        List<Soldier> casualties = calculateCasualties(attackersWin ? defenders : attackers);
        
        return new BattleResult(
            attackersWin,
            casualties,
            attackPower,
            defensePower
        );
    }
    
    private static double calculateTeamPower(List<Soldier> soldiers) {
        return soldiers.stream()
            .mapToDouble(BattleSystem::calculateSoldierPower)
            .sum();
    }
    
    private static double calculateSoldierPower(Soldier soldier) {
        Map<String, Double> attrs = soldier.getAttributes();
        return (attrs.get("attack") * 0.6 + attrs.get("defense") * 0.4) * 
               (1 + (soldier.getLevel() - 1) * 0.1);
    }
    
    private static void applyGeneralBonus(List<Soldier> attackers, List<Soldier> defenders) {
        applyGeneralBonusToTeam(attackers);
        applyGeneralBonusToTeam(defenders);
    }
    
    private static void applyGeneralBonusToTeam(List<Soldier> team) {
        team.stream()
            .filter(s -> s.getType() == SoldierType.GENERAL)
            .forEach(general -> {
                double bonus = 0.1 * general.getLevel();
                team.forEach(soldier -> {
                    if (soldier != general) {
                        soldier.getAttributes().replaceAll((k, v) -> v * (1 + bonus));
                    }
                });
            });
    }
    
    private static List<Soldier> calculateCasualties(List<Soldier> losers) {
        return losers.stream()
            .filter(s -> random.nextDouble() < 0.3) // 30%阵亡率
            .collect(Collectors.toList());
    }
} 