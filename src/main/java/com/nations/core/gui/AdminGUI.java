package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.models.Nation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AdminGUI extends BaseGUI {
    
    public AdminGUI(NationsCore plugin, Player player) {
        super(plugin, player, "§6国家系统 - 管理面板", 6);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 费用设置
        setItem(10, createItem(Material.EMERALD,
            "§6创建国家费用",
            "§7点击设置创建国家所需的费用",
            "§7包括金钱和物品要求"
        ), p -> new CostSettingsGUI(plugin, p, true, 0).open());
        
        // 升级费用设置
        setItem(11, createItem(Material.EXPERIENCE_BOTTLE,
            "§6升级费用",
            "§7点击设置各等级的升级费用",
            "",
            "§e可设置等级:",
            "§72级 - 发展中国家",
            "§73级 - 地区强国",
            "§74级 - 世界强国"
        ), p -> new UpgradeCostSelectGUI(plugin, p).open());
        
        // 国家管理
        setItem(13, createItem(Material.BEACON,
            "§6国家管理",
            "§7点击管理所有国家",
            "",
            "§e功能:",
            "§7- 删除国家",
            "§7- 转让国家",
            "§7- 修改国家等级",
            "§7- 修改国家余额"
        ), p -> new NationManageGUI(plugin, p).open());
        
        // 成员管理
        setItem(14, createItem(Material.PLAYER_HEAD,
            "§6成员管理",
            "§7点击管理国家成员",
            "",
            "§e功能:",
            "§7- 强制加入成员",
            "§7- 强制踢出成员",
            "§7- 修改成员职位"
        ), p -> new MemberManageAdminGUI(plugin, p).open());
        
        // 领土管理
        setItem(15, createItem(Material.MAP,
            "§6领土管理",
            "§7点击管理国家领土",
            "",
            "§e功能:",
            "§7- 修改领土范围",
            "§7- 重设领土中心",
            "§7- 清除领土标记"
        ), p -> new TerritoryManageGUI(plugin, p).open());
        
        // 经济管理
        setItem(16, createItem(Material.GOLD_INGOT,
            "§6经济管理",
            "§7点击管理国家经济",
            "",
            "§e功能:",
            "§7- 设置国库余额",
            "§7- 查看交易记录",
            "§7- 强制转账"
        ), p -> new EconomyManageGUI(plugin, p).open());
        
        // 系统设置
        setItem(31, createItem(Material.COMPARATOR,
            "§6系统设置",
            "§7点击管理系统设置",
            "",
            "§e功能:",
            "§7- 重载配置",
            "§7- 性能监控",
            "§7- 数据备份"
        ), p -> new SystemSettingsGUI(plugin, p).open());
        
        // 数据统计
        setItem(32, createItem(Material.BOOK,
            "§6数据统计",
            "§7点击查看系统数据",
            "",
            "§e显示:",
            "§7- 国家总数",
            "§7- 玩家总数",
            "§7- 经济流水",
            "§7- 在线时长"
        ), p -> new StatisticsGUI(plugin, p).open());
        
        // 帮助信息
        setItem(49, createItem(Material.PAPER,
            "§6管理员帮助",
            "§7点击查看管理员命令",
            "",
            "§e命令列表:",
            "§7/nadmin - 打开此界面",
            "§7/nadmin help - 查看帮助",
            "§7更多命令请点击查看"
        ), p -> new AdminHelpGUI(plugin, p).open());
    }
} 