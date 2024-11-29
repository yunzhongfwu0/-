package com.nations.core.models;

import lombok.Getter;
import java.util.UUID;
import java.util.Date;

@Getter
public class Transaction {
    private final long id;
    private final long nationId;
    private final UUID playerUuid;
    private final TransactionType type;
    private final double amount;
    private final String description;
    private final long timestamp;
    
    public enum TransactionType {
        DEPOSIT("存入"),
        WITHDRAW("取出"),
        TRANSFER_IN("转入"),
        TRANSFER_OUT("转出");
        
        private final String displayName;
        
        TransactionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public Transaction(long id, long nationId, UUID playerUuid, TransactionType type, 
                      double amount, String description, long timestamp) {
        this.id = id;
        this.nationId = nationId;
        this.playerUuid = playerUuid;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
    }
    
    public Date getDate() {
        return new Date(timestamp);
    }
} 