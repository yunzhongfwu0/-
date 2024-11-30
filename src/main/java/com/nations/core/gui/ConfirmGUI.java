package com.nations.core.gui;

import com.nations.core.NationsCore;
import com.nations.core.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class ConfirmGUI extends BaseGUI {
    
    private final String title;
    private final String confirmText;
    private final String[] warningText;
    private final Consumer<Player> onConfirm;
    private final Consumer<Player> onCancel;
    
    public ConfirmGUI(NationsCore plugin, Player player, 
                      String title, String confirmText, String[] warningText,
                      Consumer<Player> onConfirm, Consumer<Player> onCancel) {
        super(plugin, player, MessageUtil.title(title), 3);
        this.title = title;
        this.confirmText = confirmText;
        this.warningText = warningText;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        initialize();
    }
    
    private void initialize() {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // 确认按钮
        setItem(11, createItem(Material.LIME_WOOL,
            MessageUtil.title(confirmText),
            MessageUtil.createLore("确认操作",
                warningText
            ).toArray(new String[0])
        ), onConfirm);
        
        // 取消按钮
        setItem(15, createItem(Material.RED_WOOL,
            MessageUtil.title("取消"),
            MessageUtil.subtitle("点击取消操作")
        ), onCancel);
    }
    
    /**
     * 创建一个确认界面的快捷方法
     */
    public static void open(NationsCore plugin, Player player,
                          String title, String confirmText, String[] warningText,
                          Consumer<Player> onConfirm, Consumer<Player> onCancel) {
        new ConfirmGUI(plugin, player, title, confirmText, warningText, onConfirm, onCancel).open();
    }
} 