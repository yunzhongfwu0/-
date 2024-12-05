package com.nations.core.managers;

import com.nations.core.NationsCore;
import com.nations.core.models.Building;
import com.nations.core.models.NationNPC;
import com.nations.core.models.Transaction.TransactionType;
import com.nations.core.models.NPCType;
import com.nations.core.models.WorkState;
import com.nations.core.npc.NPCBehavior;
import com.nations.core.npc.behaviors.FarmerBehavior;
import com.nations.core.npc.behaviors.GuardBehavior;
import com.nations.core.npc.behaviors.ManagerBehavior;
import com.nations.core.npc.behaviors.TraderBehavior;
import com.nations.core.npc.behaviors.WarehouseKeeperBehavior;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.HologramTrait;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public class NPCManager {
    private final NationsCore plugin;
    private final Map<Long, NationNPC> npcs = new HashMap<>();
    private final Map<Long, Set<NationNPC>> buildingNPCs = new HashMap<>();
    private boolean loaded = false;
    private final Map<Long, Long> lastWorkCheckTime = new HashMap<>();  // 记录���次检查工作的时间
    private static final long WORK_CHECK_INTERVAL = 30 * 20L;  // 30秒 * 20ticks
    private final Map<NPCType, NPCBehavior> behaviors = new HashMap<>();
    private final Map<Long, Long> lastUpdateTime = new HashMap<>();
    
    public NPCManager(NationsCore plugin) {
        this.plugin = plugin;
        
        // 注册行为
        behaviors.put(NPCType.FARMER, new FarmerBehavior());
        behaviors.put(NPCType.GUARD, new GuardBehavior());
        behaviors.put(NPCType.TRADER, new TraderBehavior());
        behaviors.put(NPCType.MANAGER, new ManagerBehavior());
        behaviors.put(NPCType.WAREHOUSE_KEEPER, new WarehouseKeeperBehavior());
        
        startNPCTasks();
    }
    
    public List<NationNPC> getBuildingWorkers(Building building) {
        return buildingNPCs.getOrDefault(building.getId(), new HashSet<>())
            .stream()
            .toList();
    }
    
    public NationNPC createNPC(NPCType type, Building building) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false); // 开启事务
            try {
                // 获取当前建筑中该类型NPC的数量
                int currentCount = (int) buildingNPCs.getOrDefault(building.getId(), new HashSet<>())
                    .stream()
                    .filter(npc -> npc.getType() == type)
                    .count();

                // 生成编号 (从1开始)
                String npcNumber = String.format("%03d", currentCount + 1);
                
                // 先创建 Citizens NPC
                NPC citizensNPC = CitizensAPI.getNPCRegistry().createNPC(
                    EntityType.PLAYER, 
                    String.format("§6%s-%s §7- %s", 
                        type.getDisplayName(), 
                        npcNumber,
                        building.getNation().getName())
                );

                // 创建NPC记��
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                    "npcs (building_id, type, citizens_id) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                
                stmt.setLong(1, building.getId());
                stmt.setString(2, type.name());
                stmt.setInt(3, citizensNPC.getId());
                stmt.executeUpdate();
                
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    long id = rs.getLong(1);
                    
                    // 创建并保存 NPC
                    NationNPC npc = new NationNPC(id, type, building, citizensNPC);
                    npcs.put(id, npc);
                    buildingNPCs.computeIfAbsent(building.getId(), k -> new HashSet<>()).add(npc);
                    
                    // 初始化背包
                    initializeNPCInventory(conn, npc);
                    
                    // 设置初始工作和休息位置
                    assignWorkLocations(npc);
                    
                    // 生成 NPC 实体
                    spawnNPC(npc);
                    
                    conn.commit(); // 提��事务
                    return npc;
                }
            } catch (SQLException e) {
                conn.rollback(); // 发生错误时回滚
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("创建NPC失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // 初始化NPC背包
    private void initializeNPCInventory(Connection conn, NationNPC npc) throws SQLException {
        // 创建背包
        Inventory inv = Bukkit.createInventory(null, 27, "NPC背包 - " + npc.getCitizensNPC().getName());
        npc.setInventory(inv);
        
        // 如果是农民，给予初始种子
        if (npc.getType() == NPCType.FARMER) {
            ItemStack seeds = new ItemStack(Material.WHEAT_SEEDS, 64);
            inv.addItem(seeds);
            
            // 保存到数据库
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                "npc_inventories (npc_id, slot, item_type, amount) VALUES (?, ?, ?, ?)"
            );
            
            stmt.setLong(1, npc.getId());
            stmt.setInt(2, 0); // 放在第一个槽��
            stmt.setString(3, Material.WHEAT_SEEDS.name());
            stmt.setInt(4, 64);
            stmt.executeUpdate();
        }
    }
    
    private void setupNPCAppearance(NPC citizensNPC, NPCType type) {
        // 设置 NPC 的装备
        Equipment equipment = citizensNPC.getTrait(Equipment.class);
        switch (type) {
            case FARMER:
                equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.IRON_HOE));
                break;
            case GUARD:
                equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.SHIELD));
                equipment.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                break;
            case TRADER:
                equipment.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.LEATHER_CHESTPLATE));
                break;
            case MANAGER:
                equipment.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.GOLDEN_HELMET));
                break;
            case WAREHOUSE_KEEPER:
                equipment.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.LEATHER_CHESTPLATE));
                equipment.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.CHEST));
                break;
        }
    }
    
    private void spawnNPC(NationNPC npc) {
        try {
            // 确保有工作位置
            if (npc.getWorkPosition() == null) {
                plugin.getLogger().warning("NPC " + npc.getCitizensNPC().getName() + " 没有工作位置，重新分配位置...");
                assignWorkLocations(npc);
            }
            
            // 获取生成位置
            Location spawnLoc = npc.getState() == WorkState.WORKING ? 
                npc.getWorkPosition() : npc.getRestPosition();
            
            if (spawnLoc != null && spawnLoc.getWorld() != null) {
                // 确保区块已加载
                if (!spawnLoc.getChunk().isLoaded()) {
                    spawnLoc.getChunk().load();
                }
                
                // 确保位置是安全的
                spawnLoc = ensureSafeLocation(spawnLoc);
                
                plugin.getLogger().info(String.format(
                    "正在生成 NPC %s 在位置: world=%s, x=%.2f, y=%.2f, z=%.2f",
                    npc.getCitizensNPC().getName(),
                    spawnLoc.getWorld().getName(),
                    spawnLoc.getX(),
                    spawnLoc.getY(),
                    spawnLoc.getZ()
                ));

                // 生成 NPC 实体
                if (!npc.getCitizensNPC().isSpawned()) {
                    boolean success = npc.getCitizensNPC().spawn(spawnLoc);
                    if (success) {
                        plugin.getLogger().info("NPC 生成成功");
                    } else {
                        plugin.getLogger().warning("NPC 生成失败");
                    }
                } else {
                    plugin.getLogger().info("NPC 已经生成，正在传送到新位置");
                    npc.getCitizensNPC().teleport(spawnLoc, TeleportCause.PLUGIN);
                }
                
                // 设置 NPC 的 AI
                setupNPCAI(npc);
            } else {
                plugin.getLogger().severe(String.format(
                    "无法生成 NPC %s: 无效的位置或世界 (workPos=%s, restPos=%s)",
                    npc.getCitizensNPC().getName(),
                    npc.getWorkPosition(),
                    npc.getRestPosition()
                ));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("生成 NPC 时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Location ensureSafeLocation(Location loc) {
        // 确保位置是安全的（不在方块内部）
        Location safe = loc.clone();
        Block block = safe.getBlock();
        Block above = block.getRelative(0, 1, 0);
        Block below = block.getRelative(0, -1, 0);
        
        // 如果脚下是空气，向下寻找地面
        if (below.getType().isAir()) {
            for (int y = 0; y > -5; y--) {
                Block check = block.getRelative(0, y, 0);
                if (!check.getType().isAir()) {
                    safe.setY(check.getY() + 1);
                    break;
                }
            }
        }
        
        // 如果在方块内部，向上寻找空间
        while (!block.getType().isAir() || !above.getType().isAir()) {
            safe.setY(safe.getY() + 1);
            block = safe.getBlock();
            above = block.getRelative(0, 1, 0);
        }
        
        return safe;
    }
    
    private void setupNPCAI(NationNPC npc) {
        NPC citizensNPC = npc.getCitizensNPC();
        
        // 设置基本属性
        citizensNPC.setProtected(true);
        citizensNPC.setFlyable(false);
        
        // 添加全息图
        if (!citizensNPC.hasTrait(HologramTrait.class)) {
            citizensNPC.addTrait(HologramTrait.class);
        }
        // 添加寻路 AI
        Navigator navigator = citizensNPC.getNavigator();
        navigator.getLocalParameters()
            .speedModifier(1.0f)         // 设置移动速度
            .distanceMargin(1.0f)        // 设置到达目标的距离
            .range(25.0f);               // 设置最大寻路距离
        
        // 添加看向玩家的行为
        if (!citizensNPC.hasTrait(LookClose.class)) {
            citizensNPC.addTrait(LookClose.class);
        }
        LookClose lookClose = citizensNPC.getTrait(LookClose.class);
        lookClose.setRange(5);           // 设置视野范围
        lookClose.setRealisticLooking(true);
    }
    
    public void loadNPCs() {
        if (loaded) return;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 加载NPC数据
            ResultSet rs = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + "npcs"
            ).executeQuery();
            
            List<NationNPC> npcsToSpawn = new ArrayList<>();
            
            while (rs.next()) {
                long id = rs.getLong("id");
                long buildingId = rs.getLong("building_id");
                NPCType type = NPCType.valueOf(rs.getString("type"));
                int citizensId = rs.getInt("citizens_id");
                Building building = plugin.getBuildingManager().getBuildingById(buildingId);
                
                if (building == null) continue;
                
                // 尝试获取已存在的 Citizens NPC
                NPC citizensNPC = CitizensAPI.getNPCRegistry().getById(citizensId);
                if (citizensNPC == null) {
                    // 如果找不到，创建新的并保持相同的 ID
                    citizensNPC = CitizensAPI.getNPCRegistry().createNPC(
                        EntityType.PLAYER,
                        "§6" + type.getDisplayName() + " §7- " + building.getNation().getName()
                    );
                    if (citizensNPC.getId() != citizensId) {
                        plugin.getLogger().warning(String.format(
                            "NPC ID 不匹配: 期望 %d, 实际 %d",
                            citizensId,
                            citizensNPC.getId()
                        ));
                    }
                }
                
                // 创建 NPC 实例
                NationNPC npc = new NationNPC(id, type, building, citizensNPC);
                npc.setLevel(rs.getInt("level"));
                npc.setExperience(rs.getInt("experience"));
                npc.setHappiness(rs.getInt("happiness"));
                npc.setEnergy(rs.getInt("energy"));
                
                // 设置位置和状态
                setupNPCLocations(npc, rs);
                npc.setState(WorkState.valueOf(rs.getString("state")));
                
                // 设置外观和装备
                setupNPCAppearance(citizensNPC, type);
                
                // 保存到缓存
                npcs.put(id, npc);
                buildingNPCs.computeIfAbsent(buildingId, k -> new HashSet<>()).add(npc);
                
                // 创建背包
                Inventory inv = Bukkit.createInventory(null, 27, "NPC背包 - " + citizensNPC.getName());
                npc.setInventory(inv);
                
                // 加载背包数据
                loadNPCInventory(npc);
                
                npcsToSpawn.add(npc);
            }
            
            // 生成所有NPC
            for (NationNPC npc : npcsToSpawn) {
                spawnNPC(npc);
            }
            
            plugin.getLogger().info("已加载 " + npcs.size() + " 个 NPC 数据");
            loaded = true;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("加载NPC数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupNPCLocations(NationNPC npc, ResultSet rs) throws SQLException {
        // 设置工作位置
        String workWorld = rs.getString("work_position_world");
        if (workWorld != null) {
            World world = Bukkit.getWorld(workWorld);
            if (world != null) {
                npc.setWorkPosition(new Location(
                    world,
                    rs.getDouble("work_position_x"),
                    rs.getDouble("work_position_y"),
                    rs.getDouble("work_position_z")
                ));
            }
        }
        
        // 设置休息位置
        String restWorld = rs.getString("rest_position_world");
        if (restWorld != null) {
            World world = Bukkit.getWorld(restWorld);
            if (world != null) {
                npc.setRestPosition(new Location(
                    world,
                    rs.getDouble("rest_position_x"),
                    rs.getDouble("rest_position_y"),
                    rs.getDouble("rest_position_z")
                ));
            }
        }
    }
    
    private void cleanupInvalidNPCs() {
        // 获取所有有效的 NPC 名称
        Set<String> validNames = npcs.values().stream()
            .map(npc -> npc.getCitizensNPC().getName())
            .collect(Collectors.toSet());
        
        // 删除无效的 Citizens NPC
        CitizensAPI.getNPCRegistry().forEach(npc -> {
            if (npc.getName().contains("§6") && !validNames.contains(npc.getName())) {
                plugin.getLogger().info("删除无效的 NPC: " + npc.getName());
                npc.destroy();
            }
        });
    }
    
    private void startNPCTasks() {
        // 每2秒更新一次 NPC 状态，而不是每秒
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!loaded) return;
                
                // 获取当前时间，避免重复调用
                long currentTime = System.currentTimeMillis();
                
                // 使用迭代器避免并发修改异常
                Iterator<NationNPC> iterator = npcs.values().iterator();
                while (iterator.hasNext()) {
                    NationNPC npc = iterator.next();
                    try {
                        // 检查是否需要更新
                        if (currentTime - lastUpdateTime.getOrDefault(npc.getId(), 0L) >= 2000) {
                            updateNPCState(npc);
                            lastUpdateTime.put(npc.getId(), currentTime);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("更新NPC状态时发生错误: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // 40 ticks = 2秒
        
        // 工资发放任务
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!loaded) return;  // 如果NPC还没加载完成，跳过此次发放
                paySalaries();
            }
        }.runTaskTimer(plugin, 72000L, 72000L);
        
        // 添加定时保存背包数据的任务
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!loaded) return;
                saveAllNPCInventories();
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // 每5分钟保存一次
        
    }
    
    private void updateNPCState(NationNPC npc) {
        try {
            if (!npc.getCitizensNPC().isSpawned()) {
                return;
            }

            // 获取当前时间
            long time = npc.getCitizensNPC().getEntity().getWorld().getTime();
            boolean isWorkTime = time >= 0 && time < 12000; // 白天是工作时间

            // 获取当前体力
            int energy = npc.getEnergy();
            
            // 更新头顶显示
            updateHologram(npc, energy);

            // 如果不工作时间，强制进入休息状态
            if (!isWorkTime && npc.getState() != WorkState.RESTING) {
                npc.setState(WorkState.RESTING);
                plugin.getLogger().info(String.format(
                    "%s %s 下班休息了",
                    npc.getType().getDisplayName(),
                    npc.getCitizensNPC().getName()
                ));
            }
            // 如果是工作时间，执行工作逻辑
            if (isWorkTime) {
                NPCBehavior behavior = behaviors.get(npc.getType());
                if (behavior != null) {
                    behavior.performWork(npc);
                }
            }

            // 处理休息状态的移动
            if (npc.getState() == WorkState.RESTING) {
                Location npcLoc = npc.getCitizensNPC().getEntity().getLocation();
                Location buildingLoc = npc.getWorkplace().getBaseLocation();
                
                // 检查是否在建筑范围内
                double distance = npcLoc.distance(buildingLoc);
                int buildingRadius = npc.getWorkplace().getType().getBaseSize() / 2;
                
                if (distance > buildingRadius) {
                    // 如果不在建筑范围内，移动回建筑区域
                    npc.getCitizensNPC().getNavigator().setTarget(buildingLoc);
                } else if (!npc.getCitizensNPC().getNavigator().isNavigating()) {
                    // 在建筑范围内且没有在移动，随机选择新的目标点
                    Location randomLoc = getRandomLocationInBuilding(buildingLoc, buildingRadius);
                    if (randomLoc != null) {
                        npc.getCitizensNPC().getNavigator().setTarget(randomLoc);
                    }
                }
                return;
            }

            

        } catch (Exception e) {
            plugin.getLogger().severe("更新NPC状态时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Location getRandomLocationInBuilding(Location center, int radius) {
        if (center == null || center.getWorld() == null) return null;
        
        // 随机生成建筑范围内的一个点
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        // 创建新的位置
        Location randomLoc = center.clone();
        randomLoc.setX(x);
        randomLoc.setZ(z);
        
        // 找到地面
        int y = center.getWorld().getHighestBlockYAt((int)x, (int)z);
        randomLoc.setY(y + 1);
        
        return randomLoc;
    }
      
    private void paySalaries() {
        for (NationNPC npc : npcs.values()) {
            payWorkerSalary(npc);
        }
    }

    private void payWorkerSalary(NationNPC npc) {
        if (npc.getWorkplace() != null && npc.getWorkplace().getNation() != null) {
            double salary = npc.getCurrentSalary();
            if (npc.getWorkplace().getNation().getBalance() >= salary) {
                npc.getWorkplace().getNation().withdraw(salary);
                // 记录交易
                plugin.getNationManager().recordTransaction(
                    npc.getWorkplace().getNation(),
                    null,
                    TransactionType.WITHDRAW,
                    salary,
                    "支付工人工资: " + npc.getCitizensNPC().getName()
                );
                npc.setHappiness(Math.min(100, npc.getHappiness() + 10));
            } else {
                npc.setHappiness(Math.max(0, npc.getHappiness() - 20));
            }
        }
    }
    
    private void assignWorkLocations(NationNPC npc) {
        List<Location> workLocations = npc.getWorkplace().getType().getWorkLocations(
            npc.getWorkplace().getBaseLocation()
        );
        
        if (!workLocations.isEmpty()) {
            // 随机选择一个工作位置
            Location workPos = workLocations.get(new Random().nextInt(workLocations.size()));
            npc.setWorkPosition(workPos);
            
            // 设置休息位置（在工作位置附近）
            Location restPos = workPos.clone().add(
                new Random().nextDouble() - 0.5,
                0,
                new Random().nextDouble() - 0.5
            );
            npc.setRestPosition(restPos);
            
            // 保存位置到数据库
            saveNPCLocations(npc);
        }
    }

    public void dismissWorker(NationNPC worker) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 删除背包数据
                PreparedStatement invStmt = conn.prepareStatement(
                    "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                    "npc_inventories WHERE npc_id = ?"
                );
                invStmt.setLong(1, worker.getId());
                invStmt.executeUpdate();
                
                // 删除 NPC 记录
                PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                    "npcs WHERE id = ?"
                );
                stmt.setLong(1, worker.getId());
                stmt.executeUpdate();
                
                // 从缓存中移除
                npcs.remove(worker.getId());
                buildingNPCs.get(worker.getWorkplace().getId()).remove(worker);
                
                // 移除 Citizens NPC
                NPC citizensNPC = worker.getCitizensNPC();
                if (citizensNPC != null) {
                    if (citizensNPC.isSpawned()) {
                        citizensNPC.despawn();
                    }
                    CitizensAPI.getNPCRegistry().deregister(citizensNPC);
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("解雇工人失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // 添加一个调试方法
    public void debugNPCState(NationNPC npc) {
        World world = npc.getWorkplace().getBaseLocation().getWorld();
        long worldTime = world.getTime();
        int currentHour = (int) ((worldTime + 6000) % 24000) / 1000;
        
        plugin.getLogger().info(String.format(
            "NPC状态调信息:\n" +
            "名称: %s\n" +
            "当前状态: %s\n" +
            "体力: %d%%\n" +
            "世界时间: %d (约%d点)\n" +
            "工作时间表: %s",
            npc.getCitizensNPC().getName(),
            npc.getState().getDisplayName(),
            npc.getEnergy(),
            worldTime,
            currentHour,
            npc.getWorkplace().getType().getWorkSchedule()
        ));
    }


    public Collection<NationNPC> getAllNPCs() {
        return npcs.values();
    }

    private void updateHologram(NationNPC npc, int energy) {
        HologramTrait hologram = npc.getCitizensNPC().getOrAddTrait(HologramTrait.class);
        
        // 清除现有的行
        hologram.clear();
        // 添加状态和体力行
        String stateText = switch (npc.getState()) {
            case WORKING -> " §a【工作中】";
            case RESTING -> " §e【休息中】";
            default -> " §7【空闲】";
        };
        
        hologram.addLine(String.format(
            "§7体力: %s%d%%%s",
            getEnergyColor(energy),
            energy,
            stateText
        ));
    }

    private String getEnergyColor(int energy) {
        if (energy > 80) return "§a";
        if (energy > 50) return "§e";
        if (energy > 20) return "§6";
        return "§c";
    }

    // 添加背包加载方法
    private void loadNPCInventory(NationNPC npc) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "npc_inventories WHERE npc_id = ?"
            );
            stmt.setLong(1, npc.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int slot = rs.getInt("slot");
                String itemType = rs.getString("item_type");
                int amount = rs.getInt("amount");
                
                Material material = Material.valueOf(itemType);
                ItemStack item = new ItemStack(material, amount);
                npc.getInventory().setItem(slot, item);
            }
            
            plugin.getLogger().info(String.format(
                "已加载 NPC %s 的背包数据",
                npc.getCitizensNPC().getName()
            ));
            
        } catch (SQLException e) {
            plugin.getLogger().severe("加载NPC背包数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 添加背包保存方法
    private void saveNPCInventory(NationNPC npc) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 先删除旧数据
            PreparedStatement deleteStmt = conn.prepareStatement(
                "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
                "npc_inventories WHERE npc_id = ?"
            );
            deleteStmt.setLong(1, npc.getId());
            deleteStmt.executeUpdate();
            
            // 保存新数据
            PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
                "npc_inventories (npc_id, slot, item_type, amount) VALUES (?, ?, ?, ?)"
            );
            
            for (int i = 0; i < npc.getInventory().getSize(); i++) {
                ItemStack item = npc.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    insertStmt.setLong(1, npc.getId());
                    insertStmt.setInt(2, i);
                    insertStmt.setString(3, item.getType().name());
                    insertStmt.setInt(4, item.getAmount());
                    insertStmt.addBatch();
                }
            }
            
            insertStmt.executeBatch();
            plugin.getLogger().info(String.format(
                "已保存 NPC %s 的背包数据",
                npc.getCitizensNPC().getName()
            ));
            
        } catch (SQLException e) {
            plugin.getLogger().severe("保存NPC背包数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 添加保存所有NPC背包的方法
    public void saveAllNPCInventories() {
        plugin.getLogger().info("正在保存所有NPC背包数据...");
        int count = 0;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (NationNPC npc : npcs.values()) {
                    saveNPCInventory(npc, conn);
                    count++;
                }
                conn.commit();
                plugin.getLogger().info("成功保存 " + count + " 个NPC的背包数据");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存NPC背包数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 修改保存单个NPC背的方法，添加Connection参数
    private void saveNPCInventory(NationNPC npc, Connection conn) throws SQLException {
        // 先删除旧数据
        PreparedStatement deleteStmt = conn.prepareStatement(
            "DELETE FROM " + plugin.getDatabaseManager().getTablePrefix() + 
            "npc_inventories WHERE npc_id = ?"
        );
        deleteStmt.setLong(1, npc.getId());
        deleteStmt.executeUpdate();
        
        // 保存新数据
        PreparedStatement insertStmt = conn.prepareStatement(
            "INSERT INTO " + plugin.getDatabaseManager().getTablePrefix() + 
            "npc_inventories (npc_id, slot, item_type, amount) VALUES (?, ?, ?, ?)"
        );
        
        for (int i = 0; i < npc.getInventory().getSize(); i++) {
            ItemStack item = npc.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                insertStmt.setLong(1, npc.getId());
                insertStmt.setInt(2, i);
                insertStmt.setString(3, item.getType().name());
                insertStmt.setInt(4, item.getAmount());
                insertStmt.addBatch();
            }
        }
        
        insertStmt.executeBatch();
        plugin.getLogger().info(String.format(
            "已保存 NPC %s 的背包数据",
            npc.getCitizensNPC().getName()
        ));
    }

    // 添加在插件卸载时保存数据的方法
    public void onDisable() {
        if (!loaded) return;
        
        plugin.getLogger().info("正在保存NPC数据...");
        
        // 保存所有NPC的背包数据
        saveAllNPCInventories();
        
        // 清理Citizens NPC
        for (NationNPC npc : npcs.values()) {
            NPC citizensNPC = npc.getCitizensNPC();
            if (citizensNPC != null) {
                if (citizensNPC.isSpawned()) {
                    citizensNPC.despawn();
                }
            }
        }
        
        plugin.getLogger().info("NPC数据保存完成");
    }

    // 删除建筑相关的所有NPC
    public void removeAllBuildingNPCs(Building building) {
        Set<NationNPC> buildingWorkers = buildingNPCs.getOrDefault(building.getId(), new HashSet<>());
        for (NationNPC npc : new HashSet<>(buildingWorkers)) {
            dismissWorker(npc);
        }
        buildingNPCs.remove(building.getId());
        plugin.getLogger().info("已删除建筑 " + building.getId() + " 的所有NPC");
    }

    public void saveNPCLocations(NationNPC npc) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE " + plugin.getDatabaseManager().getTablePrefix() + 
                "npcs SET work_position_x = ?, work_position_y = ?, work_position_z = ?, " +
                "work_position_world = ?, rest_position_x = ?, rest_position_y = ?, " +
                "rest_position_z = ?, rest_position_world = ? WHERE id = ?"
            );
            
            Location work = npc.getWorkPosition();
            Location rest = npc.getRestPosition();
            
            if (work != null) {
                stmt.setDouble(1, work.getX());
                stmt.setDouble(2, work.getY());
                stmt.setDouble(3, work.getZ());
                stmt.setString(4, work.getWorld().getName());
            } else {
                stmt.setNull(1, Types.DOUBLE);
                stmt.setNull(2, Types.DOUBLE);
                stmt.setNull(3, Types.DOUBLE);
                stmt.setNull(4, Types.VARCHAR);
            }
            
            if (rest != null) {
                stmt.setDouble(5, rest.getX());
                stmt.setDouble(6, rest.getY());
                stmt.setDouble(7, rest.getZ());
                stmt.setString(8, rest.getWorld().getName());
            } else {
                stmt.setNull(5, Types.DOUBLE);
                stmt.setNull(6, Types.DOUBLE);
                stmt.setNull(7, Types.DOUBLE);
                stmt.setNull(8, Types.VARCHAR);
            }
            
            stmt.setLong(9, npc.getId());
            stmt.executeUpdate();
            
            plugin.getLogger().info(String.format(
                "已保存NPC %s 的位置信息 (工作位置: %s, 休息位置: %s)",
                npc.getCitizensNPC().getName(),
                work != null ? formatLocation(work) : "无",
                rest != null ? formatLocation(rest) : "无"
            ));
            
        } catch (SQLException e) {
            plugin.getLogger().severe("保存NPC位置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatLocation(Location loc) {
        return String.format("world=%s, x=%.2f, y=%.2f, z=%.2f",
            loc.getWorld().getName(),
            loc.getX(),
            loc.getY(),
            loc.getZ()
        );
    }

    public void reloadNPCs() {
        // 先保存所有NPC数据
        saveAllNPCInventories();
        
        // 取消所有NPC的任务
        for (NationNPC npc : npcs.values()) {
            if (npc.getCitizensNPC().isSpawned()) {
                npc.getCitizensNPC().despawn();
            }
        }
        
        // 清空缓存
        npcs.clear();
        buildingNPCs.clear();
        
        // 重新加载NPC数据
        loadNPCs();
        
        // 重新生成所有NPC
        for (NationNPC npc : npcs.values()) {
            spawnNPC(npc);
        }
        
        plugin.getLogger().info("已重新加载所有NPC");
    }

    public void updateNPCBehaviors() {
        for (NationNPC npc : npcs.values()) {
            if (npc.getCitizensNPC().isSpawned()) {
                // 重新设置NPC的行为
                setupNPCAI(npc);
                plugin.getLogger().info("已更新NPC " + npc.getCitizensNPC().getName() + " 的行为");
            }
        }
    }
}