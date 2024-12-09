package com.nations.core.npc.behaviors;

import com.nations.core.NationsCore;
import com.nations.core.managers.NPCManager;
import com.nations.core.models.*;
import com.nations.core.npc.NPCBehavior;
import com.nations.core.npc.behaviors.AbstractNPCBehavior;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.ai.Navigator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GuardBehavior extends AbstractNPCBehavior {
    private static final Random random = new Random();
    private static final int PATROL_INTERVAL = 100; // 5秒
    private static final double DETECTION_RANGE = 20.0; // 20格检测范围
    private static final double ATTACK_RANGE = 3.0; // 3格攻击范围
    private int stateTimer = 0;
    private Entity currentTarget = null;
    private int combatTimer = 0;
    private static final int MAX_CHASE_TIME = 200; // 10秒追击时间限制
    private BukkitTask workTask;
    private long lastPatrolTime = 0; // 添加巡逻时间记录
    private double lastDistance = 0;
    private int stuckTicks = 0;

    @Override
    public void performWork(NationNPC npc) {
        try {
            if (!isValidForWork(npc)) {
                NationsCore.getInstance().getLogger().info(
                    String.format("守卫 %s 不满足工作条件", 
                        npc.getCitizensNPC().getName())
                );
                return;
            }

            // 检查是否在领地范围内
            Nation nation = npc.getWorkplace().getNation();
            if (nation == null) {
                NationsCore.getInstance().getLogger().info("守卫所属国家为空");
                return;
            }

            Location npcLoc = npc.getCitizensNPC().getEntity().getLocation();
            if (!nation.isInTerritory(npcLoc)) {
                Location workPos = npc.getWorkPosition();
                if (workPos != null) {
                    npc.getCitizensNPC().teleport(workPos, TeleportCause.PLUGIN);
                    NationsCore.getInstance().getLogger().info(
                        String.format("守卫 %s 超出领地范围，已传送回工作地点",
                            npc.getCitizensNPC().getName())
                    );
                }
                return;
            }

            // 状态计时器
            stateTimer++;
            
            // 检查体力和状态转换
            if (npc.getEnergy() <= 10) {
                stateTimer = 0;
                currentTarget = null;
                enterRestState(npc);
            }

            // 处理休息状态
            if (npc.getState() == WorkState.RESTING) {
                handleRest(npc);
                currentTarget = null;
                if (npc.getEnergy() >= 80) {
                    enterWorkState(npc);
                }
                NPCManager.updateHologram(npc, npc.getEnergy());
                return;
            }

            NPCManager.updateHologram(npc, npc.getEnergy());

            // 工作状态下的行为
            if (npc.getState() == WorkState.WORKING) {
                // 检查当前目标是否仍然有效
                if (currentTarget != null) {
                    if (!isValidTarget(currentTarget, npc, nation)) {
                        NationsCore.getInstance().getLogger().info(
                            String.format("守卫 %s 的目标无效，重置目标", 
                                npc.getCitizensNPC().getName())
                        );
                        currentTarget = null;
                        combatTimer = 0;
                    } else {
                        // 更新战斗计时器
                        combatTimer++;
                        if (combatTimer >= MAX_CHASE_TIME) {
                            // 超时，放弃追击
                            NationsCore.getInstance().getLogger().info(
                                String.format("守卫 %s 追击超时，放弃目标", 
                                    npc.getCitizensNPC().getName())
                            );
                            currentTarget = null;
                            combatTimer = 0;
                        } else {
                            // 持续更新战斗状态
                            handleCombat(npc, currentTarget);
                            // 每3秒消耗1点体力
                            if (System.currentTimeMillis() - lastEnergyConsumption > 1000) {
                                consumeEnergy(npc, 2, "战斗");
                                lastEnergyConsumption = System.currentTimeMillis();
                            }
                            return;
                        }
                    }
                }
                
                // 只有在没有当前目标时才寻找新目标
                if (currentTarget == null) {
                    Entity target = findNearestEnemy(npc, npcLoc, nation);
                    if (target != null) {
                        currentTarget = target;
                        combatTimer = 0;
                        lastEnergyConsumption = System.currentTimeMillis();
                        NationsCore.getInstance().getLogger().info(
                            String.format("守卫 %s 发现新目标: %s", 
                                npc.getCitizensNPC().getName(),
                                target.getType().name())
                        );
                        handleCombat(npc, currentTarget);
                        return;
                    }
                }

                // 没有目标时进行巡逻
                if (currentTarget == null) {
                    NationsCore.getInstance().getLogger().info(
                        String.format("守卫 %s 准备巡逻，当前位置: (x=%.1f, y=%.1f, z=%.1f)", 
                            npc.getCitizensNPC().getName(),
                            npcLoc.getX(),
                            npcLoc.getY(),
                            npcLoc.getZ())
                    );
                    handlePatrol(npc, npcLoc, nation.getTerritory());
                }
            }
        } catch (Exception e) {
            NationsCore.getInstance().getLogger().warning(
                String.format("守卫 %s 行为更新时发生错误: %s", 
                    npc.getCitizensNPC().getName(),
                    e.getMessage())
            );
            e.printStackTrace();
        }
    }

    private Entity findNearestEnemy(NationNPC npc, Location guardLoc, Nation nation) {
        Entity nearestEnemy = null;
        double closestDistance = Double.MAX_VALUE;

        // 获取附近所有实体
        for (Entity entity : guardLoc.getWorld().getNearbyEntities(guardLoc, DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE)) {
            // 跳过非生物实体
            if (!(entity instanceof LivingEntity)) continue;
            // 跳过自己
            if (entity.equals(npc.getCitizensNPC().getEntity())) continue;
            // 跳过其他NPC
            if (entity.hasMetadata("NPC")) continue;
            
            // 检查是否在领地范围内
            if (!nation.isInTerritory(entity.getLocation())) continue;

            // 检查目标是否可到达
            if (!isTargetReachable(guardLoc, entity.getLocation())) continue;

            boolean isEnemy = false;
            if (entity instanceof Player) {
                // 检查玩家是否是敌人
                isEnemy = !nation.isMember(((Player) entity).getUniqueId());
            } else {
                // 检查是否是敌对生物
                isEnemy = isHostileMob(entity);
            }

            if (isEnemy) {
                double distance = entity.getLocation().distance(guardLoc);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nearestEnemy = entity;
                }
            }
        }

        return nearestEnemy;
    }

    private boolean isTargetReachable(Location guardLoc, Location targetLoc) {
        // 检查Y轴差距
        double yDiff = Math.abs(guardLoc.getY() - targetLoc.getY());
        if (yDiff > 5) { // 如果高度差超过5格，认为无法到达
            return false;
        }

        // 检查目标位置是否在地下
        Location targetHighest = targetLoc.getWorld().getHighestBlockAt(targetLoc).getLocation();
        if (targetLoc.getY() < targetHighest.getY() - 2) { // 如果目标在地表以下2格以上，认为在地下
            return false;
        }

        // 检查路径是否有障碍
        Location checkLoc = guardLoc.clone();
        Vector direction = targetLoc.toVector().subtract(guardLoc.toVector()).normalize();
        double distance = guardLoc.distance(targetLoc);
        
        // 每隔2格检查一次是否有障碍
        for (double d = 0; d < distance; d += 2) {
            checkLoc.add(direction.clone().multiply(2));
            
            // 检查这个位置是否有一个2格高的空间
            Block block = checkLoc.getBlock();
            Block above = block.getRelative(BlockFace.UP);
            Block below = block.getRelative(BlockFace.DOWN);
            
            // 如果路径被完全堵住（前后都是实心方块），认为无法到达
            if (!block.getType().isAir() && !above.getType().isAir() && 
                !block.getRelative(BlockFace.NORTH).getType().isAir() &&
                !block.getRelative(BlockFace.SOUTH).getType().isAir() &&
                !block.getRelative(BlockFace.EAST).getType().isAir() &&
                !block.getRelative(BlockFace.WEST).getType().isAir()) {
                return false;
            }
        }

        return true;
    }

    private boolean isHostileMob(Entity entity) {
        // 只检测地表常见的敌对生物
        return (entity instanceof Zombie && !(entity instanceof PigZombie)) || // 普通僵尸（不包括僵尸猪人）
               entity instanceof Skeleton || // 骷髅
               entity instanceof Spider || // 蜘蛛
               entity instanceof Creeper || // 苦力怕
               entity instanceof Witch || // 女巫
               (entity instanceof Illager) || // 灾厄村民系列
               entity instanceof Phantom; // 幻翼（夜晚出现）
    }

    private void handleRest(NationNPC npc) {
        // 在休息状态下恢复体力
        if (npc.getEnergy() < 100) {
            // 每秒恢复5点体力
            if (System.currentTimeMillis() - lastEnergyRecovery > 1000) {
                npc.setEnergy(Math.min(100, npc.getEnergy() + 5));
                lastEnergyRecovery = System.currentTimeMillis();
            }
        }
    }

    private void enterWorkState(NationNPC npc) {
        npc.setState(WorkState.WORKING);
        stateTimer = 0;
    }

    private void enterRestState(NationNPC npc) {
        npc.setState(WorkState.RESTING);
        stateTimer = 0;
    }

    @Override
    public void onSpawn(NationNPC npc) {
        setupEquipment(npc);
        // 启动独立的行为控制任务
        workTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!npc.getCitizensNPC().isSpawned()) {
                        this.cancel();
                        return;
                    }
                    performWork(npc);
                } catch (Exception e) {
                    NationsCore.getInstance().getLogger().warning(
                        String.format("守卫 %s 行为更新时发生错误: %s", 
                            npc.getCitizensNPC().getName(),
                            e.getMessage())
                    );
                }
            }
        }.runTaskTimer(NationsCore.getInstance(), 12L, 12L); // 每5tick执行一次，减少服务器负担
    }

    @Override
    public void onDespawn(NationNPC npc) {
        if (workTask != null) {
            workTask.cancel();
            workTask = null;
        }
        // 确保清理导航状态
        try {
            Navigator navigator = npc.getCitizensNPC().getNavigator();
            if (navigator != null && navigator.isNavigating()) {
                navigator.cancelNavigation();
            }
        } catch (Exception e) {
            // 忽略清理错误
        }
        currentTarget = null;
        stateTimer = 0;
        combatTimer = 0;
        lastPatrolTime = 0;
    }

    @Override
    public void setupEquipment(NationNPC npc) {
        Equipment equipment = npc.getCitizensNPC().getTrait(Equipment.class);
        // 设置守卫的装备
        equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.IRON_SWORD));
        equipment.set(Equipment.EquipmentSlot.OFF_HAND, new ItemStack(Material.SHIELD));
        equipment.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.IRON_HELMET));
        equipment.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.IRON_CHESTPLATE));
        equipment.set(Equipment.EquipmentSlot.LEGGINGS, new ItemStack(Material.IRON_LEGGINGS));
        equipment.set(Equipment.EquipmentSlot.BOOTS, new ItemStack(Material.IRON_BOOTS));
    }

    private boolean isValidForWork(NationNPC npc) {
        return npc != null && 
               npc.getCitizensNPC() != null && 
               npc.getCitizensNPC().isSpawned() &&
               npc.getWorkplace() != null &&
               npc.getCitizensNPC().getNavigator() != null;
    }

    private void handlePatrol(NationNPC npc, Location currentLoc, Territory territory) {
        if (!isValidForWork(npc)) return;
        
        Navigator navigator = npc.getCitizensNPC().getNavigator();
        if (navigator == null) return;

        try {
            // 检查当前导航状态
            if (navigator.isNavigating()) {
                Location targetLoc = navigator.getTargetAsLocation();
                if (targetLoc != null) {
                    double distance = currentLoc.distance(targetLoc);
                    NationsCore.getInstance().getLogger().info(
                        String.format("守卫 %s 正在移动，距离目标: %.1f", 
                            npc.getCitizensNPC().getName(),
                            distance)
                    );
                    
                    if (distance < 2.0) {
                        navigator.cancelNavigation();
                        lastPatrolTime = 0;
                    }
                }
                return;
            }

            // 获取巡逻点
            Location patrolPoint = getPatrolPoint(npc);
            if (patrolPoint != null) {
                NationsCore.getInstance().getLogger().info(
                    String.format("守卫 %s 尝试设置巡逻点: (x=%.1f, y=%.1f, z=%.1f)", 
                        npc.getCitizensNPC().getName(),
                        patrolPoint.getX(),
                        patrolPoint.getY(),
                        patrolPoint.getZ())
                );

                if (!territory.contains(patrolPoint)) {
                    NationsCore.getInstance().getLogger().info("巡逻点不在领地范围内");
                    return;
                }

                if (!isLocationSafe(patrolPoint)) {
                    NationsCore.getInstance().getLogger().info("巡逻点不安全");
                    return;
                }

                if (currentLoc.distance(patrolPoint) <= 5.0) {
                    NationsCore.getInstance().getLogger().info("巡逻点太近");
                    return;
                }

                // 设置导航参数
                navigator.getLocalParameters()
                    .baseSpeed(1.0f)
                    .range(20)
                    .attackRange(ATTACK_RANGE)
                    .distanceMargin(2.0)
                    .stationaryTicks(100);

                // 设置新的导航目标
                navigator.setTarget(patrolPoint);
                lastPatrolTime = System.currentTimeMillis();
                
                NationsCore.getInstance().getLogger().info(
                    String.format("守卫 %s 成功设置巡逻目标: (x=%.1f, y=%.1f, z=%.1f)", 
                        npc.getCitizensNPC().getName(), 
                        patrolPoint.getX(), 
                        patrolPoint.getY(), 
                        patrolPoint.getZ())
                );
            }
        } catch (Exception e) {
            NationsCore.getInstance().getLogger().warning(
                String.format("守卫 %s 巡逻时发生错误: %s", 
                    npc.getCitizensNPC().getName(),
                    e.getMessage())
            );
            e.printStackTrace();
        }
    }

    private Location getPatrolPoint(NationNPC npc) {
        try {
            Location buildingLoc = npc.getWorkplace().getBaseLocation();
            if (buildingLoc == null) {
                NationsCore.getInstance().getLogger().warning("建筑位置为空");
                return null;
            }

            Territory territory = npc.getWorkplace().getNation().getTerritory();
            if (territory == null) {
                NationsCore.getInstance().getLogger().warning("领地范围为空");
                return null;
            }

            // 在领地范围内随机选择一个点
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * territory.getRadius(); // 在半径范围内随机选择距离
            
            double x = territory.getCenterX() + Math.cos(angle) * distance;
            double z = territory.getCenterZ() + Math.sin(angle) * distance;
            
            Location target = new Location(buildingLoc.getWorld(), x, buildingLoc.getY(), z);
            
            // 找到最高的可通行方块
            int maxY = buildingLoc.getBlockY() + 5; // 建筑位置上下5格范围内
            int minY = buildingLoc.getBlockY() - 5;
            
            for (int y = maxY; y >= minY; y--) {
                target.setY(y);
                Block block = target.getBlock();
                Block above = block.getRelative(BlockFace.UP);
                Block below = block.getRelative(BlockFace.DOWN);
                
                // 检查是否是安全的位置：当前位置和上方是空气，下方是实心方块
                if (block.getType().isAir() && 
                    above.getType().isAir() && 
                    !below.getType().isAir() && 
                    below.getType().isSolid() &&
                    !below.isLiquid()) {
                    
                    target.setY(y);
                    return target;
                }
            }
            
            NationsCore.getInstance().getLogger().warning("未找到有效的地面");
            return null;
        } catch (Exception e) {
            NationsCore.getInstance().getLogger().warning("生成巡逻点时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void collectDrops(NationNPC npc, Location location) {
        location.getWorld().getNearbyEntities(location, 5, 5, 5).stream()
            .filter(e -> e instanceof Item)
            .map(e -> (Item) e)
            .forEach(item -> {
                ItemStack stack = item.getItemStack();
                npc.getInventory().addItem(stack);
                item.remove();
                
                NationsCore.getInstance().getLogger().info(
                    String.format("守卫 %s 收集了掉落物: %s x%d", 
                        npc.getCitizensNPC().getName(),
                        stack.getType().name(),
                        stack.getAmount())
                );
            });
    }

    private void consumeEnergy(NationNPC npc, int amount, String action) {
        int oldEnergy = npc.getEnergy();
        int newEnergy = Math.max(0, oldEnergy - amount);
        npc.setEnergy(newEnergy);
        
        // 每降低10点体力记录一次
        if (newEnergy % 10 == 0 || newEnergy == 0) {
            NationsCore.getInstance().getLogger().info(
                String.format("守卫 %s %s消耗体力 %d%% -> %d%%", 
                    npc.getCitizensNPC().getName(), 
                    action,
                    oldEnergy,
                    newEnergy)
            );
        }
    }

    private boolean isValidTarget(Entity target, NationNPC npc, Nation nation) {
        if (target == null || !target.isValid() || target.isDead()) {
            return false;
        }
        
        // 检查是否是NPC
        if (target.hasMetadata("NPC")) {
            return false;
        }
        
        // 检查目标是否可到达
        Location guardLoc = npc.getCitizensNPC().getEntity().getLocation();
        if (!isTargetReachable(guardLoc, target.getLocation())) {
            return false;
        }
        
        // 检查目标是否在检测范围内
        double distance = target.getLocation().distance(guardLoc);
        if (distance > DETECTION_RANGE * 1.5) { // 允许追击超出一些检测范围
            return false;
        }
        
        // 检查目标是否在领地内
        if (!nation.isInTerritory(target.getLocation())) {
            return false;
        }
        
        return true;
    }

    private void handleCombat(NationNPC npc, Entity target) {
        if (!isValidForWork(npc) || target == null) return;
        
        try {
            Location guardLoc = npc.getCitizensNPC().getEntity().getLocation();
            Location targetLoc = target.getLocation();
            double distance = guardLoc.distance(targetLoc);
            Navigator navigator = npc.getCitizensNPC().getNavigator();
            
            // 如果目标在攻击范围内
            if (distance <= ATTACK_RANGE) {
                // 取消导航，专注于战斗
                if (navigator.isNavigating()) {
                    navigator.cancelNavigation();
                }
                
                // 让NPC面向目标
                guardLoc.setDirection(targetLoc.subtract(guardLoc).toVector());
                npc.getCitizensNPC().getEntity().teleport(guardLoc);
                
                // 进行攻击
                if (target instanceof LivingEntity) {
                    LivingEntity livingTarget = (LivingEntity) target;
                    LivingEntity guard = (LivingEntity) npc.getCitizensNPC().getEntity();
                    
                    // 计算伤害
                    double damage = 2.0; // 基础伤害2心
                    if (target instanceof Player) {
                        try {
                            Player player = (Player) target;
                            // 对敌对玩家造成伤害和击退
                            livingTarget.damage(damage, guard);
                            Vector knockback = player.getLocation().subtract(guard.getLocation()).toVector().normalize().multiply(0.5);
                            player.setVelocity(knockback);
                            
                            // 发送警告消息
                            if (!player.hasMetadata("guard_damage_warning")) {
                                player.sendMessage("§c警告：你正在遭受守卫的攻击！");
                                player.setMetadata("guard_damage_warning", new FixedMetadataValue(NationsCore.getInstance(), true));
                                
                                // 10秒后移除警告标记
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (player.isOnline()) {
                                                player.removeMetadata("guard_damage_warning", NationsCore.getInstance());
                                            }
                                        } catch (Exception e) {
                                            // 忽略移除标记时的错误
                                        }
                                    }
                                }.runTaskLater(NationsCore.getInstance(), 200L);
                            }
                        } catch (Exception e) {
                            NationsCore.getInstance().getLogger().warning(
                                String.format("守卫 %s 攻击玩家时发生错误: %s", 
                                    npc.getCitizensNPC().getName(),
                                    e.getMessage())
                            );
                        }
                    } else {
                        // 对怪物造成伤害
                        livingTarget.damage(damage, guard);
                    }
                    
                    // 记录战斗
                    NationsCore.getInstance().getLogger().info(
                        String.format("守卫 %s 攻击了 %s，造成 %.1f 点伤害，距离: %.1f", 
                            npc.getCitizensNPC().getName(),
                            target instanceof Player ? ((Player)target).getName() : target.getType().name(),
                            damage,
                            distance)
                    );
                }
            } else {
                // 目标不在攻击范围内，需要追击
                if (navigator != null) {
                    // 如果距离太远，尝试找到一个更好的路径点
                    Location moveTarget = distance > 15 ? findBetterPathPoint(guardLoc, targetLoc) : targetLoc;
                    
                    // 设置导航参数
                    navigator.getLocalParameters()
                        .baseSpeed(1.2f) // 追击时速度稍快
                        .range(30)
                        .attackRange(ATTACK_RANGE)
                        .distanceMargin(2.0)
                        .stationaryTicks(100);

                    // 每次都更新目标位置
                    navigator.setTarget(targetLoc);
                    
                    // 如果没有在移动，强制开始移动
                    if (!navigator.isNavigating()) {
                        // 尝试找到一个更好的路径点
                        Location midPoint = null;
                        if (distance > 10) {
                            Vector direction = targetLoc.clone().subtract(guardLoc).toVector().normalize();
                            midPoint = guardLoc.clone().add(direction.multiply(5));
                            midPoint.setY(midPoint.getWorld().getHighestBlockYAt(midPoint));
                            navigator.setTarget(midPoint);
                        }
                        
                        NationsCore.getInstance().getLogger().info(
                            String.format("守卫 %s 正在追击目标 %s，距离: %.1f，设置中间点: %s", 
                                npc.getCitizensNPC().getName(),
                                target.getType().name(),
                                distance,
                                midPoint != null ? String.format("(x=%.1f, y=%.1f, z=%.1f)", 
                                    midPoint.getX(), midPoint.getY(), midPoint.getZ()) : "无")
                        );
                    }
                    
                    // 追击消耗体力（每3秒消耗1点体力）
                    if (System.currentTimeMillis() - lastEnergyConsumption > 3000) {
                        consumeEnergy(npc, 1, "追击");
                        lastEnergyConsumption = System.currentTimeMillis();
                    }
                }
            }
            
            // 如果目标是玩家，发出警告
            if (target instanceof Player && !target.hasMetadata("guard_warning")) {
                try {
                    Player player = (Player) target;
                    player.sendMessage("§c警告：你正在被守卫盯上了！");
                    player.setMetadata("guard_warning", new FixedMetadataValue(NationsCore.getInstance(), true));
                    
                    // 10秒后移除警告标记
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            try {
                                if (player.isOnline()) {
                                    player.removeMetadata("guard_warning", NationsCore.getInstance());
                                }
                            } catch (Exception e) {
                                // 忽略移除标记时的错误
                            }
                        }
                    }.runTaskLater(NationsCore.getInstance(), 200L);
                } catch (Exception e) {
                    // 忽略警告消息发送失败
                }
            }
        } catch (Exception e) {
            NationsCore.getInstance().getLogger().warning(
                String.format("守卫 %s 战斗时发生错误: %s", 
                    npc.getCitizensNPC().getName(),
                    e.getMessage())
            );
            e.printStackTrace();
        }
    }

    private boolean isLocationSafe(Location loc) {
        Block block = loc.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        Block below = block.getRelative(BlockFace.DOWN);
        
        return block.getType().isAir() && 
               above.getType().isAir() && 
               !below.getType().isAir() && 
               !below.isLiquid();
    }

    private Location findSafeTeleportLocation(Location guardLoc, Location targetLoc) {
        Vector direction = targetLoc.toVector().subtract(guardLoc.toVector()).normalize();
        double targetDistance = Math.min(guardLoc.distance(targetLoc) * 0.7, 10); // 传送到目标70%距离处，最远10格
        
        Location teleportLoc = targetLoc.clone().subtract(direction.multiply(targetDistance));
        teleportLoc.setY(teleportLoc.getWorld().getHighestBlockYAt(teleportLoc));
        
        // 确保位置安全
        if (isLocationSafe(teleportLoc)) {
            return teleportLoc;
        }
        
        // 尝试在周围找一个安全位置
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double x = teleportLoc.getX() + Math.cos(angle) * 2;
            double z = teleportLoc.getZ() + Math.sin(angle) * 2;
            Location testLoc = new Location(teleportLoc.getWorld(), x, teleportLoc.getY(), z);
            testLoc.setY(testLoc.getWorld().getHighestBlockYAt(testLoc));
            
            if (isLocationSafe(testLoc)) {
                return testLoc;
            }
        }
        
        return null;
    }

    private Location findBetterPathPoint(Location guardLoc, Location targetLoc) {
        double distance = guardLoc.distance(targetLoc);
        if (distance <= 15) return targetLoc;
        
        Vector direction = targetLoc.toVector().subtract(guardLoc.toVector()).normalize();
        Location midPoint = guardLoc.clone().add(direction.multiply(10)); // 向目标方向前进10格
        
        // 找到安全的地面位置
        midPoint.setY(midPoint.getWorld().getHighestBlockYAt(midPoint));
        
        // 确保位置安全
        if (!isLocationSafe(midPoint)) {
            // 尝试在周围找一个安全位置
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                double x = midPoint.getX() + Math.cos(angle) * 2;
                double z = midPoint.getZ() + Math.sin(angle) * 2;
                Location testLoc = new Location(midPoint.getWorld(), x, midPoint.getY(), z);
                testLoc.setY(testLoc.getWorld().getHighestBlockYAt(testLoc));
                
                if (isLocationSafe(testLoc)) {
                    return testLoc;
                }
            }
        }
        
        return midPoint;
    }

    private long lastEnergyConsumption = 0;
    private long lastEnergyRecovery = 0;

    /**
     * 安全地取消导航，避免空指针异常
     */
    private void safelyCancelNavigation(Navigator navigator) {
        try {
            if (navigator != null && navigator.isNavigating()) {
                // 在取消导航前先设置一个临时目标，避免空指针
                if (navigator.getNPC() != null && navigator.getNPC().getEntity() != null) {
                    Location currentLoc = navigator.getNPC().getEntity().getLocation();
                    if (currentLoc != null) {
                        navigator.setTarget(currentLoc);
                    }
                }
                navigator.cancelNavigation();
            }
        } catch (Exception e) {
            // 略取消导航时的错误，确保不影响其他功能
            NationsCore.getInstance().getLogger().warning(
                "取消导航时发生错误: " + e.getMessage()
            );
        }
    }
} 