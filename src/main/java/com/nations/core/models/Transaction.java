package com.nations.core.models;

import java.util.Date;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Transaction {
    public enum TransactionType {
        DEPOSIT("存入"),
        WITHDRAW("支出"),
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

    private final long id;
    private final long nationId;
    private final UUID playerUuid;
    private final TransactionType type;
    private final double amount;
    private final String description;
    private final long timestamp;

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


    public long getId() { return id; }
    public long getNationId() { return nationId; }
    public UUID getPlayerUuid() { return playerUuid; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public long getTimestamp() { return timestamp; }
} 