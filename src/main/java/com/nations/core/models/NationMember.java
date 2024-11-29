package com.nations.core.models;

import java.util.Date;

public class NationMember {
    private final long nationId;
    private NationRank rank;
    private final Date joined_at;
    
    public NationMember(long nationId, NationRank rank, Date joined_at) {
        this.nationId = nationId;
        this.rank = rank;
        this.joined_at = joined_at;
    }
    
    public long getNationId() {
        return nationId;
    }
    
    public NationRank getRank() {
        return rank;
    }
    
    public void setRank(NationRank rank) {
        this.rank = rank;
    }
    
    public Date getJoinDate() {
        return joined_at;
    }
    
    public String getFormattedJoinDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(joined_at);
    }
} 