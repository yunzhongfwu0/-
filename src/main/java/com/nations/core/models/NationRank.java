package com.nations.core.models;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;

@Getter
public enum NationRank {
    OWNER("国主", Arrays.asList(
        "nation.admin",
        "nation.invite",
        "nation.kick",
        "nation.promote",
        "nation.demote",
        "nation.withdraw",
        "nation.deposit",
        "nation.setspawn",
        "nation.upgrade",
        "nation.rename",
        "nation.delete",
        "nation.territory",
        "nation.transfer"
    )),
    
    MINISTER("大臣", Arrays.asList(
        "nation.invite",
        "nation.kick",
        "nation.promote",
        "nation.demote",
        "nation.withdraw",
        "nation.deposit",
        "nation.setspawn"
    )),
    
    OFFICER("官员", Arrays.asList(
        "nation.invite",
        "nation.deposit",
        "nation.setspawn"
    )),
    
    MEMBER("成员", Arrays.asList(
        "nation.deposit",
        "nation.spawn"
    ));
    
    private final String displayName;
    private final List<String> permissions;
    
    NationRank(String displayName, List<String> permissions) {
        this.displayName = displayName;
        this.permissions = permissions;
    }
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission) || permissions.contains("nation.admin");
    }
    
    public static NationRank fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getPermissions() {
        return permissions;
    }
} 