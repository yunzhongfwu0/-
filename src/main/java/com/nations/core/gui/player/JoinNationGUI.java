package com.nations.core.gui.player;

import com.nations.core.NationsCore;
import com.nations.core.gui.BaseGUI;
import com.nations.core.gui.MainGUI;
import com.nations.core.models.Nation;
import com.nations.core.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JoinNationGUI extends BaseGUI {
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public JoinNationGUI(NationsCore plugin, Player player) {
        super(plugin, player, MessageUtil.title("加入国家"), 6);
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        List<Nation> nations = new ArrayList<>(plugin.getNationManager().getAllNations());
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, nations.size());
        
        // 显示国家列表
        for (int i = 0; i < endIndex - startIndex; i++) {
            Nation nation = nations.get(startIndex + i);
            
            List<String> lore = new ArrayList<>();
            lore.addAll(MessageUtil.createStatusLore("国家信息",
                "等级: " + nation.getLevel(),
                "成员: " + nation.getMembers().size(),
                "领袖: " + plugin.getServer().getOfflinePlayer(nation.getOwnerUUID()).getName()
            ));
            
            lore.add("");
            if (nation.isInvited(player.getUniqueId())) {
                lore.addAll(MessageUtil.createActionLore("已收到邀请",
                    "使用 /nation accept " + nation.getName() + " 接受邀请"
                ));
            } else if (plugin.getNationManager().hasJoinRequest(nation, player.getUniqueId())) {
                lore.addAll(MessageUtil.createStatusLore("申请状态",
                    "已发送申请",
                    "等待审核中..."
                ));
            } else {
                lore.add(MessageUtil.tip("点击申请加入"));
            }
            
            setItem(10 + i + (i/7)*2, createItem(Material.BOOK,
                MessageUtil.title(nation.getName()),
                lore.toArray(new String[0])
            ), p -> {
                // 检查玩家是否已有国家
                if (plugin.getNationManager().getNationByPlayer(p).isPresent()) {
                    p.sendMessage(MessageUtil.error("你已经加入了一个国家！"));
                    p.closeInventory();
                    return;
                }
                
                if (!nation.isInvited(p.getUniqueId()) && 
                    !plugin.getNationManager().hasJoinRequest(nation, p.getUniqueId())) {
                    plugin.getNationManager().addJoinRequest(nation, p.getUniqueId());
                    p.sendMessage(MessageUtil.success("已发送加入申请，请等待审核。"));
                    
                    // 通知有权限的成员
                    for (Player member : plugin.getServer().getOnlinePlayers()) {
                        if (nation.isMember(member.getUniqueId()) && 
                            nation.hasPermission(member.getUniqueId(), "nation.manage.requests")) {
                            member.sendMessage(MessageUtil.broadcast("玩家 " + p.getName() + 
                                " 申请加入国家 " + nation.getName()));
                        }
                    }
                    
                    new JoinNationGUI(plugin, p).open();
                }
            });
        }
        
        // 翻页按钮
        if (page > 0) {
            setItem(45, createItem(Material.ARROW,
                MessageUtil.title("上一页"),
                MessageUtil.subtitle("点击查看上一页")
            ), p -> {
                page--;
                initialize();
            });
        }
        
        if (endIndex < nations.size()) {
            setItem(53, createItem(Material.ARROW,
                MessageUtil.title("下一页"),
                MessageUtil.subtitle("点击查看下一页")
            ), p -> {
                page++;
                initialize();
            });
        }
        
        // 返回按钮
        setItem(49, createItem(Material.BARRIER,
            MessageUtil.title("返回"),
            MessageUtil.subtitle("点击返回")
        ), p -> new MainGUI(plugin, p).open());
    }
} 