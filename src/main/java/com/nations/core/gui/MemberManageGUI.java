package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.gui.player.RequestManageGUI;
import com.nations.core.models.Nation;
import com.nations.core.models.NationMember;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MemberManageGUI extends BaseGUI {
    private final Nation nation;
    
    public MemberManageGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, "§6成员管理 - " + nation.getName(), 6);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 显示国家所有者
        ItemStack ownerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta ownerMeta = (SkullMeta) ownerHead.getItemMeta();
        ownerMeta.setOwningPlayer(Bukkit.getOfflinePlayer(nation.getOwnerUUID()));
        ownerMeta.setDisplayName("§6国家领袖");
        List<String> ownerLore = new ArrayList<>();
        ownerLore.add("§7" + Bukkit.getOfflinePlayer(nation.getOwnerUUID()).getName());
        ownerLore.add("§e职位: §f国主");
        ownerMeta.setLore(ownerLore);
        ownerHead.setItemMeta(ownerMeta);
        setItem(4, ownerHead, null);
        
        // 显示成员列表
        int slot = 19;
        for (Map.Entry<UUID, NationMember> entry : nation.getMembers().entrySet()) {
            UUID memberUUID = entry.getKey();
            NationMember member = entry.getValue();
            
            ItemStack memberHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta memberMeta = (SkullMeta) memberHead.getItemMeta();
            memberMeta.setOwningPlayer(Bukkit.getOfflinePlayer(memberUUID));
            memberMeta.setDisplayName("§f" + Bukkit.getOfflinePlayer(memberUUID).getName());
            
            List<String> memberLore = new ArrayList<>();
            memberLore.add("§e职位: §f" + member.getRank().getDisplayName());
            memberLore.add("§e加入时间: §f" + member.getFormattedJoinDate());
            
            if (nation.hasPermission(player.getUniqueId(), "nation.promote")) {
                memberLore.add("");
                memberLore.add("§a左键 - 提升职位");
                memberLore.add("§c右键 - 踢出成员");
            }
            
            memberMeta.setLore(memberLore);
            memberHead.setItemMeta(memberMeta);
            
            setItem(slot++, memberHead, p -> {
                if (nation.hasPermission(p.getUniqueId(), "nation.promote")) {
                    new RankSelectGUI(plugin, p, nation, memberUUID).open();
                }
            });
        }
        
        // 邀请新成员
        if (nation.hasPermission(player.getUniqueId(), "nation.invite")) {
            setItem(48, createItem(Material.EMERALD,
                "§a邀请新成员",
                "§7点击邀请新成员"
            ), p -> new InviteGUI(plugin, p, nation).open());
        }
        
        // 查看申请
        if (nation.hasPermission(player.getUniqueId(), "nation.manage.requests")) {
            List<String> requestLore = new ArrayList<>();
            requestLore.add("§7点击查看加入申请");
            requestLore.add("");
            int requestCount = plugin.getNationManager().getJoinRequests(nation).size();
            if (requestCount > 0) {
                requestLore.add("§e当前有 " + requestCount + " 个待处理申请");
            } else {
                requestLore.add("§7暂无待处理申请");
            }
            
            setItem(50, createItem(Material.BOOK,
                "§6查看申请",
                requestLore.toArray(new String[0])
            ), p -> new RequestManageGUI(plugin, p, nation).open());
        }
        
        // 返回按钮
        setItem(45, createItem(Material.ARROW,
            "§f返回主菜单",
            "§7点击返回"
        ), p -> new MainGUI(plugin, p).open());
    }
} 