package com.nations.core.models;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public enum BuildingType {
    TOWN_HALL("市政厅", 5, 1, null, 0, Material.BEACON),
    BARRACKS("兵营", 5, 2, TOWN_HALL, 1, Material.IRON_SWORD),
    MARKET("市场", 7, 2, TOWN_HALL, 1, Material.EMERALD),
    WAREHOUSE("仓库", 5, 1, TOWN_HALL, 1, Material.CHEST),
    FARM("农场", 7, 1, TOWN_HALL, 1, Material.WHEAT);

    private final String displayName;
    private final int baseSize;
    private final int minNationLevel;
    private final BuildingType requiredBuilding;
    private final int requiredBuildingLevel;
    private final Material icon;
    private BuildingImpl impl;

    BuildingType(String displayName, int baseSize, int minNationLevel, 
                BuildingType requiredBuilding, int requiredBuildingLevel, Material icon) {
        this.displayName = displayName;
        this.baseSize = baseSize;
        this.minNationLevel = minNationLevel;
        this.requiredBuilding = requiredBuilding;
        this.requiredBuildingLevel = requiredBuildingLevel;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public int getBaseSize() { return baseSize; }
    public int getMinNationLevel() { return minNationLevel; }
    public BuildingType getRequiredBuilding() { return requiredBuilding; }
    public int getRequiredBuildingLevel() { return requiredBuildingLevel; }
    public Material getIcon() { return icon; }

    public Map<Material, Integer> getBuildCosts() { return impl.getBuildCosts(); }
    public void placeStructure(Location location) { impl.placeStructure(location); }
    public String[] getDescription() { return impl.getDescription(); }
    public Map<NPCType, Integer> getWorkerSlots() { return impl.getWorkerSlots(); }
    public Map<Integer, String> getWorkSchedule() { return impl.getWorkSchedule(); }
    public List<Location> getWorkLocations(Location base) { return impl.getWorkLocations(base); }

    private void init(BuildingImpl impl) {
        this.impl = impl;
    }

    private interface BuildingImpl {
        String[] getDescription();
        Map<Material, Integer> getBuildCosts();
        void placeStructure(Location location);
        Map<NPCType, Integer> getWorkerSlots();
        Map<Integer, String> getWorkSchedule();
        List<Location> getWorkLocations(Location base);
    }

    static {
        TOWN_HALL.init(new BuildingImpl() {
            @Override
            public String[] getDescription() {
                return new String[]{
                    "§7国家核心建筑",
                    "§7- 提供基础税收",
                    "§7- 增加成员上限",
                    "§7- 每级增加 5% 税收",
                    "§7- 每级增加 5 个成员上限"
                };
            }

            @Override
            public Map<Material, Integer> getBuildCosts() {
                Map<Material, Integer> costs = new HashMap<>();
                costs.put(Material.DIAMOND_BLOCK, 5);
                costs.put(Material.EMERALD_BLOCK, 5);
                return costs;
            }

            @Override
            public void placeStructure(Location loc) {
                World world = loc.getWorld();
                // 基座
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        world.getBlockAt(loc.clone().add(x, 0, z)).setType(Material.STONE_BRICKS);
                    }
                }
                // ... 其他建筑结构代码
            }

            @Override
            public Map<NPCType, Integer> getWorkerSlots() {
                Map<NPCType, Integer> slots = new HashMap<>();
                slots.put(NPCType.MANAGER, 1);
                return slots;
            }

            @Override
            public Map<Integer, String> getWorkSchedule() {
                Map<Integer, String> schedule = new HashMap<>();
                schedule.put(6, "开始工作");
                schedule.put(12, "午休");
                schedule.put(13, "继续工作");
                schedule.put(18, "结束工作");
                return schedule;
            }

            @Override
            public List<Location> getWorkLocations(Location base) {
                List<Location> locations = new ArrayList<>();
                locations.add(base.clone().add(2, 0, 2));
                locations.add(base.clone().add(-2, 0, 2));
                locations.add(base.clone().add(2, 0, -2));
                locations.add(base.clone().add(-2, 0, -2));
                return locations;
            }
        });

        BARRACKS.init(new BuildingImpl() {
            @Override
            public String[] getDescription() {
                return new String[]{
                    "§7军事训练设施",
                    "§7- 提供战斗力加成",
                    "§7- 增加防御能力",
                    "§7- 每级增加 10% 战斗力",
                    "§7- 每级增加 5% 防御力"
                };
            }

            @Override
            public Map<Material, Integer> getBuildCosts() {
                Map<Material, Integer> costs = new HashMap<>();
                costs.put(Material.IRON_BLOCK, 10);
                costs.put(Material.DIAMOND, 5);
                return costs;
            }

            @Override
            public void placeStructure(Location loc) {
                World world = loc.getWorld();
                
                // 基座
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        world.getBlockAt(loc.clone().add(x, 0, z)).setType(Material.STONE_BRICKS);
                    }
                }
                
                // 墙壁
                for (int y = 1; y <= 3; y++) {
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                                Material material = y == 2 ? Material.DARK_OAK_LOG : Material.STONE_BRICKS;
                                world.getBlockAt(loc.clone().add(x, y, z)).setType(material);
                            }
                        }
                    }
                }
                
                // 顶
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        world.getBlockAt(loc.clone().add(x, 4, z)).setType(Material.DARK_OAK_PLANKS);
                    }
                }
                
                // 装饰
                world.getBlockAt(loc.clone().add(0, 1, -2)).setType(Material.DARK_OAK_DOOR);
                world.getBlockAt(loc.clone().add(0, 2, -2)).setType(Material.DARK_OAK_DOOR);
                world.getBlockAt(loc.clone().add(-1, 2, -2)).setType(Material.DARK_OAK_FENCE);
                world.getBlockAt(loc.clone().add(1, 2, -2)).setType(Material.DARK_OAK_FENCE);
            }

            @Override
            public Map<NPCType, Integer> getWorkerSlots() {
                Map<NPCType, Integer> slots = new HashMap<>();
                slots.put(NPCType.GUARD, 4);
                return slots;
            }

            @Override
            public Map<Integer, String> getWorkSchedule() {
                Map<Integer, String> schedule = new HashMap<>();
                schedule.put(6, "训练开始");
                schedule.put(12, "午休");
                schedule.put(13, "训练继续");
                schedule.put(18, "结束训练");
                return schedule;
            }

            @Override
            public List<Location> getWorkLocations(Location base) {
                List<Location> locations = new ArrayList<>();
                locations.add(base.clone().add(2, 0, 2));
                locations.add(base.clone().add(-2, 0, 2));
                locations.add(base.clone().add(2, 0, -2));
                locations.add(base.clone().add(-2, 0, -2));
                return locations;
            }
        });

        // 市场实现
        MARKET.init(new BuildingImpl() {
            @Override
            public String[] getDescription() {
                return new String[]{
                    "§7经济交易中心",
                    "§7- 提供交易折扣",
                    "§7- 增加收入加成",
                    "§7- 每级减少 5% 交易费用",
                    "§7- 每级增加 10% 收入加成"
                };
            }

            @Override
            public Map<Material, Integer> getBuildCosts() {
                Map<Material, Integer> costs = new HashMap<>();
                costs.put(Material.EMERALD_BLOCK, 10);
                costs.put(Material.GOLD_BLOCK, 10);
                return costs;
            }

            @Override
            public void placeStructure(Location loc) {
                World world = loc.getWorld();
                
                // 地板
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        world.getBlockAt(loc.clone().add(x, 0, z)).setType(Material.SMOOTH_STONE);
                    }
                }
                
                // 支柱
                for (int y = 1; y <= 3; y++) {
                    world.getBlockAt(loc.clone().add(-3, y, -3)).setType(Material.STRIPPED_OAK_LOG);
                    world.getBlockAt(loc.clone().add(3, y, -3)).setType(Material.STRIPPED_OAK_LOG);
                    world.getBlockAt(loc.clone().add(-3, y, 3)).setType(Material.STRIPPED_OAK_LOG);
                    world.getBlockAt(loc.clone().add(3, y, 3)).setType(Material.STRIPPED_OAK_LOG);
                }
                
                // 屋顶
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        world.getBlockAt(loc.clone().add(x, 4, z)).setType(Material.OAK_SLAB);
                    }
                }
                
                // 摊位
                for (int x = -2; x <= 2; x += 2) {
                    world.getBlockAt(loc.clone().add(x, 1, 0)).setType(Material.BARREL);
                    world.getBlockAt(loc.clone().add(x, 1, 1)).setType(Material.CHEST);
                    world.getBlockAt(loc.clone().add(x, 1, -1)).setType(Material.CRAFTING_TABLE);
                }
            }

            @Override
            public Map<NPCType, Integer> getWorkerSlots() {
                Map<NPCType, Integer> slots = new HashMap<>();
                slots.put(NPCType.TRADER, 3);
                return slots;
            }

            @Override
            public Map<Integer, String> getWorkSchedule() {
                Map<Integer, String> schedule = new HashMap<>();
                schedule.put(8, "开市");
                schedule.put(12, "午休");
                schedule.put(13, "继续营业");
                schedule.put(20, "收市");
                return schedule;
            }

            @Override
            public List<Location> getWorkLocations(Location base) {
                List<Location> locations = new ArrayList<>();
                locations.add(base.clone().add(3, 0, 0));
                locations.add(base.clone().add(-3, 0, 0));
                locations.add(base.clone().add(0, 0, 3));
                locations.add(base.clone().add(0, 0, -3));
                return locations;
            }
        });

        // 仓库实现
        WAREHOUSE.init(new BuildingImpl() {
            @Override
            public String[] getDescription() {
                return new String[]{
                    "§7资源存储设施",
                    "§7- 提供额外存储空间",
                    "§7- 每级增加 100 格存储空间",
                    "§7- 由商人管理物品",
                    "§7- 管理员负责监督"
                };
            }

            @Override
            public Map<Material, Integer> getBuildCosts() {
                Map<Material, Integer> costs = new HashMap<>();
                costs.put(Material.IRON_BLOCK, 15);
                costs.put(Material.CHEST, 10);
                return costs;
            }

            @Override
            public void placeStructure(Location loc) {
                World world = loc.getWorld();
                
                // 地基
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        world.getBlockAt(loc.clone().add(x, 0, z)).setType(Material.STONE_BRICKS);
                    }
                }
                
                // 墙壁
                for (int y = 1; y <= 4; y++) {
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                                world.getBlockAt(loc.clone().add(x, y, z)).setType(Material.SMOOTH_STONE);
                            }
                        }
                    }
                }
                
                // 屋顶
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        world.getBlockAt(loc.clone().add(x, 5, z)).setType(Material.STONE_SLAB);
                    }
                }
                
                // 主箱子(中心点)
                world.getBlockAt(loc.clone().add(0, 1, 0)).setType(Material.CHEST);
                
                // 周围的箱子
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue; // 跳过中心点，因为已经放置了主箱子
                        world.getBlockAt(loc.clone().add(x, 1, z)).setType(Material.CHEST);
                    }
                }
            }

            @Override
            public Map<NPCType, Integer> getWorkerSlots() {
                Map<NPCType, Integer> slots = new HashMap<>();
                slots.put(NPCType.WAREHOUSE_KEEPER, 3);  // 3个仓���管理员
                return slots;
            }

            @Override
            public Map<Integer, String> getWorkSchedule() {
                Map<Integer, String> schedule = new HashMap<>();
                schedule.put(7, "开始整理");
                schedule.put(12, "午休");
                schedule.put(13, "继续工作");
                schedule.put(19, "结束工作");
                return schedule;
            }

            @Override
            public List<Location> getWorkLocations(Location base) {
                List<Location> locations = new ArrayList<>();
                locations.add(base.clone().add(1, 0, 1));
                locations.add(base.clone().add(-1, 0, 1));
                locations.add(base.clone().add(1, 0, -1));
                locations.add(base.clone().add(-1, 0, -1));
                return locations;
            }
        });

        // 农场实现
        FARM.init(new BuildingImpl() {
            @Override
            public String[] getDescription() {
                return new String[]{
                    "§7食物生产设施",
                    "§7- 自动生产食物",
                    "§7- 每级每小时产出 10 个食物",
                    "§7- 提供基础食物供给",
                    "§7- 支持多种作物种植"
                };
            }

            @Override
            public Map<Material, Integer> getBuildCosts() {
                Map<Material, Integer> costs = new HashMap<>();
                costs.put(Material.HAY_BLOCK, 20);
                costs.put(Material.WATER_BUCKET, 4);
                return costs;
            }

            @Override
            public void placeStructure(Location loc) {
                World world = loc.getWorld();
                
                // 先放置地基
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        world.getBlockAt(loc.clone().add(x, -1, z)).setType(Material.DIRT);
                    }
                }
                
                // 耕地和水源
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Location blockLoc = loc.clone().add(x, 0, z);
                        if ((x + z) % 3 == 0) {
                            // 水源方块底部放置石头
                            world.getBlockAt(blockLoc.clone().add(0, -1, 0)).setType(Material.STONE);
                            world.getBlockAt(blockLoc).setType(Material.WATER);
                        } else {
                            world.getBlockAt(blockLoc).setType(Material.FARMLAND);
                            world.getBlockAt(blockLoc.add(0, 1, 0)).setType(Material.WHEAT);
                        }
                    }
                }
                
                // 围栏
                for (int x = -3; x <= 3; x++) {
                    world.getBlockAt(loc.clone().add(x, 1, -3)).setType(Material.OAK_FENCE);
                    world.getBlockAt(loc.clone().add(x, 1, 3)).setType(Material.OAK_FENCE);
                }
                for (int z = -2; z <= 2; z++) {
                    world.getBlockAt(loc.clone().add(-3, 1, z)).setType(Material.OAK_FENCE);
                    world.getBlockAt(loc.clone().add(3, 1, z)).setType(Material.OAK_FENCE);
                }
                
                // 门
                world.getBlockAt(loc.clone().add(0, 1, -3)).setType(Material.OAK_FENCE_GATE);
            }

            @Override
            public Map<NPCType, Integer> getWorkerSlots() {
                Map<NPCType, Integer> slots = new HashMap<>();
                slots.put(NPCType.FARMER, 4);
                return slots;
            }

            @Override
            public Map<Integer, String> getWorkSchedule() {
                Map<Integer, String> schedule = new HashMap<>();
                schedule.put(0, "开始作");    // 日出时开始工作
                schedule.put(12000, "休息");    // 日落时休息
                return schedule;
            }

            @Override
            public List<Location> getWorkLocations(Location base) {
                List<Location> locations = new ArrayList<>();
                // 在田各处添加工作位置
                for (int x = -2; x <= 2; x += 2) {
                    for (int z = -2; z <= 2; z += 2) {
                        locations.add(base.clone().add(x, 1, z));
                    }
                }
                return locations;
            }
        });
    }

    public Map<String, Double> getBaseBonus() {
        return switch (this) {
            case TOWN_HALL -> Map.of(
                "tax_rate", 0.05,      // 基础税率5%
                "max_members", 5.0      // 基础人口上限5人
            );
            case BARRACKS -> Map.of(
                "training_slots", 2.0,  // 基础训练位数量
                "training_bonus", 0.1,  // 基础训练属性加成 (10%)
                "training_speed", 0.0,  // 基础训练速度减少 (0%)
                "strength", 1.0,        // 基础战斗力加成
                "defense", 0.5         // 基础防御力加成
            );
            case MARKET -> Map.of(
                "trade_discount", 0.02, // 基础交易折扣2%
                "income_bonus", 0.1     // 基础收入加成10%
            );
            case WAREHOUSE -> Map.of(
                "storage_size", 27.0    // 基础存储容量27格
            );
            case FARM -> Map.of(
                "food_production", 10.0  // 基础食物产量10
            );
        };
    }

    public Map<String, Double> getLevelBonus() {
        return switch (this) {
            case TOWN_HALL -> Map.of(
                "tax_rate", 0.05,      // 每级增加5%税率
                "max_members", 5.0      // 每级增加5人口上限
            );
            case BARRACKS -> Map.of(
                "training_slots", 1.0,  // 每级+1个训练位
                "training_bonus", 0.05, // 每级+5%训练加成
                "training_speed", 0.1,  // 每级减少10%训练时间
                "strength", 1.0,        // 每级+1战斗力
                "defense", 0.5          // 每级+0.5防御力
            );
            case MARKET -> Map.of(
                "trade_discount", 0.02, // 每级增加2%交易折扣
                "income_bonus", 0.1     // 每级增加10%收入加成
            );
            case WAREHOUSE -> Map.of(
                "storage_size", 27.0    // 每级增加27格存储容量
            );
            case FARM -> Map.of(
                "food_production", 10.0  // 每级增加10食物产量
            );
        };
    }
} 