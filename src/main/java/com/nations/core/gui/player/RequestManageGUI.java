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
import java.util.UUID;

public class RequestManageGUI extends BaseGUI {
    private final Nation nation;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;
    
    public RequestManageGUI(NationsCore plugin, Player player, Nation nation) {
        super(plugin, player, MessageUtil.title("申请管理 - " + nation.getName()), 6);
        this.nation = nation;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        List<UUID> requests = plugin.getNationManager().getJoinRequests(nation);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, requests.size());
        
        // 显示申请列表
        for (int i = 0; i < endIndex - startIndex; i++) {
            UUID uuid = requests.get(startIndex + i);
            String playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
            
            int currentSlot = 10 + i + (i/7)*2;
            setItem(currentSlot, createItem(Material.PLAYER_HEAD,
                MessageUtil.title(playerName),
                MessageUtil.createActionLore("申请处理",
                    "左键 - 同意申请",
                    "右键 - 拒绝申请"
                ).toArray(new String[0])
            ), p -> {
                // 左键处理 - 同意申请
                if (plugin.getNationManager().getNationByUUID(uuid).isPresent()) {
                    p.sendMessage(MessageUtil.error("该玩家已加入其他国家！"));
                    plugin.getNationManager().removeJoinRequest(nation, uuid);
                    new RequestManageGUI(plugin, p, nation).open();
                    return;
                }
                
                if (plugin.getNationManager().addMember(nation, uuid, "MEMBER")) {
                    p.sendMessage(MessageUtil.success("已同意 " + playerName + " 的加入申请"));
                    
                    Player target = plugin.getServer().getPlayer(uuid);
                    if (target != null) {
                        target.sendMessage(MessageUtil.success("恭喜！你加入国家 " + nation.getName() + " 的申请已通过！"));
                        target.sendMessage(MessageUtil.tip("输入 /nation 打开国家菜单"));
                    }
                    
                    for (UUID memberId : nation.getMembers().keySet()) {
                        Player member = plugin.getServer().getPlayer(memberId);
                        if (member != null && !member.getUniqueId().equals(p.getUniqueId())) {
                            member.sendMessage(MessageUtil.broadcast("欢迎新成员 " + playerName + " 加入国家！"));
                        }
                    }
                } else {
                    p.sendMessage(MessageUtil.error("添加成员失败，请重试！"));
                }
                
                new RequestManageGUI(plugin, p, nation).open();
            }, p -> {
                // 右键处理 - 拒绝申请
                plugin.getNationManager().removeJoinRequest(nation, uuid);
                p.sendMessage(MessageUtil.info("已拒绝 " + playerName + " 的加入申请"));
                
                Player target = plugin.getServer().getPlayer(uuid);
                if (target != null) {
                    target.sendMessage(MessageUtil.error("你加入国家 " + nation.getName() + " 的申请被拒绝了。"));
                }
                
                new RequestManageGUI(plugin, p, nation).open();
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
        
        if (endIndex < requests.size()) {
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