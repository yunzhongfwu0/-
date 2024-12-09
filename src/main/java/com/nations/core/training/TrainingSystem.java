package com.nations.core.training;

import com.nations.core.NationsCore;
import com.nations.core.models.*;
import org.bukkit.scheduler.BukkitRunnable;

public class TrainingSystem {
    private static final NationsCore plugin = NationsCore.getInstance();
    
    public static void startTraining(Soldier soldier) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isValid(soldier)) {
                    cancel();
                    return;
                }
                
                // 每小时获得经验
                int baseExp = 10;
                int expGain = (int)(baseExp * (1 + soldier.getBarracks().getLevel() * 0.1));
                soldier.gainExperience(expGain);
                
                // 更新数据库
                plugin.getSoldierManager().saveSoldier(soldier);
            }
        }.runTaskTimer(plugin, 0L, 72000L); // 每小时执行一次
    }
    
    private static boolean isValid(Soldier soldier) {
        return plugin.getSoldierManager().getSoldierById(soldier.getId()) != null;
    }
} 