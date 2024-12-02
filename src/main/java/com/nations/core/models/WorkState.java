package com.nations.core.models;

import lombok.Getter;

@Getter
public enum WorkState {
    IDLE("空闲"),
    WORKING("工作中"),
    RESTING("休息中"),
    TRAVELING("移动中");
    
    private final String displayName;
    
    WorkState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
} 