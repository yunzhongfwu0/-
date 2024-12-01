package com.nations.core.models;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public enum BuildingType {
    TOWN_HALL("市政厅", 5, 1, null, 0) {
        @Override
        public String[] getDescription() {
            return new String[]{
                "§7国家的核心建筑",
                "§7- 提供基础税收加成",
                "§7- 增加国家成员上限",
                "§7- 每级增加 5 名成员上限",
                "§7- 每级增加 5% 税收加成"
            };
        }

        @Override
        public Map<Material, Integer> getBuildCosts() {
            Map<Material, Integer> costs = new HashMap<>();
            costs.put(Material.DIAMOND, 10);
            costs.put(Material.GOLD_BLOCK, 5);
            return costs;
        }

        @Override
        public void placeStructure(Location loc) {
            World world = loc.getWorld();
            int baseY = loc.getBlockY();
            
            // 基座
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    world.getBlockAt(loc.clone().add(x, 0, z)).setType(Material.POLISHED_ANDESITE);
                }
            }
            
            // 主体结构
            for (int y = 1; y <= 4; y++) {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        if (y == 4 && (Math.abs(x) == 2 || Math.abs(z) == 2)) continue;
                        Location blockLoc = loc.clone().add(x, y, z);
                        if (Math.abs(x) == 2 || Math.abs(z) == 2 || y == 4) {
                            world.getBlockAt(blockLoc).setType(Material.QUARTZ_PILLAR);
                        } else {
                            world.getBlockAt(blockLoc).setType(Material.AIR);
                        }
                    }
                }
            }
            
            // 屋顶
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    world.getBlockAt(loc.clone().add(x, 5, z)).setType(Material.QUARTZ_STAIRS);
                }
            }
            
            // 门
            world.getBlockAt(loc.clone().add(0, 1, -2)).setType(Material.IRON_DOOR);
            world.getBlockAt(loc.clone().add(0, 2, -2)).setType(Material.IRON_DOOR);
            
            // 装饰
            world.getBlockAt(loc.clone().add(0, 3, -2)).setType(Material.LANTERN);
            world.getBlockAt(loc.clone().add(-2, 3, 0)).setType(Material.LANTERN);
            world.getBlockAt(loc.clone().add(2, 3, 0)).setType(Material.LANTERN);
            world.getBlockAt(loc.clone().add(0, 3, 2)).setType(Material.LANTERN);
        }
    },
    
    BARRACKS("军营", 3, 2, TOWN_HALL, 2) {
        @Override
        public String[] getDescription() {
            return new String[]{
                "§7军事训练设施",
                "§7- 提升国家战斗力",
                "§7- 增加防御能力",
                "§7- 每级增加 10 点战斗力",
                "§7- 每级增加 5 点防御力"
            };
        }

        @Override
        public Map<Material, Integer> getBuildCosts() {
            Map<Material, Integer> costs = new HashMap<>();
            costs.put(Material.IRON_BLOCK, 20);
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
            
            // 屋顶
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
    },
    
    MARKET("市场", 4, 2, TOWN_HALL, 1) {
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
    },
    
    WAREHOUSE("仓库", 3, 1, TOWN_HALL, 1) {
        @Override
        public String[] getDescription() {
            return new String[]{
                "§7资源存储设施",
                "§7- 提供额外存储空间",
                "§7- 每级增加 100 格存储空间",
                "§7- 可存储各类资源",
                "§7- 支持自动分类"
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
                            world.getBlockAt(loc.clone().add(x, y, z)).setType(Material.SMOOTH_STONE);
                        }
                    }
                }
            }
            
            // 屋顶
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    world.getBlockAt(loc.clone().add(x, 4, z)).setType(Material.STONE_SLAB);
                }
            }
            
            // 箱子
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    world.getBlockAt(loc.clone().add(x, 1, z)).setType(Material.CHEST);
                }
            }
        }
    },
    
    FARM("农场", 5, 1, TOWN_HALL, 1) {
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
            
            // 耕地和水源
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    Location blockLoc = loc.clone().add(x, 0, z);
                    if ((x + z) % 3 == 0) {
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
    };

    private final String displayName;
    private final int baseSize;
    private final int minNationLevel;
    private final BuildingType requiredBuilding;
    private final int requiredBuildingLevel;

    BuildingType(String displayName, int baseSize, int minNationLevel, 
                BuildingType requiredBuilding, int requiredBuildingLevel) {
        this.displayName = displayName;
        this.baseSize = baseSize;
        this.minNationLevel = minNationLevel;
        this.requiredBuilding = requiredBuilding;
        this.requiredBuildingLevel = requiredBuildingLevel;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public int getMinNationLevel() {
        return minNationLevel;
    }
    
    public BuildingType getRequiredBuilding() {
        return requiredBuilding;
    }
    
    public int getRequiredBuildingLevel() {
        return requiredBuildingLevel;
    }
    
    public int getBaseSize() {
        return baseSize;
    }

    public abstract Map<Material, Integer> getBuildCosts();
    public abstract void placeStructure(Location location);
    public abstract String[] getDescription();

    // Getters...
} 