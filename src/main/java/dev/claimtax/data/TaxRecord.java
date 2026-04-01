package dev.claimtax.data;

import java.util.UUID;

public class TaxRecord {

    private final UUID playerUUID;
    private String playerName;

    private double totalPaid;

    private double totalDebt;

    private int warningCount;

    private long lastTaxTime;

    private int lastClaimedBlocks;

    public TaxRecord(UUID playerUUID, String playerName) {
        this.playerUUID     = playerUUID;
        this.playerName     = playerName;
        this.totalPaid      = 0.0;
        this.totalDebt      = 0.0;
        this.warningCount   = 0;
        this.lastTaxTime    = 0L;
        this.lastClaimedBlocks = 0;
    }


    public UUID   getPlayerUUID()        { return playerUUID; }
    public String getPlayerName()        { return playerName; }
    public void   setPlayerName(String n){ this.playerName = n; }

    public double getTotalPaid()         { return totalPaid; }
    public void   addPaid(double amount) { this.totalPaid += amount; }

    public double getTotalDebt()         { return totalDebt; }
    public void   addDebt(double amount) { this.totalDebt += amount; }
    public void   clearDebt()            { this.totalDebt = 0; }

    public int    getWarningCount()      { return warningCount; }
    public void   addWarning()           { this.warningCount++; }
    public void   resetWarnings()        { this.warningCount = 0; }

    public long   getLastTaxTime()       { return lastTaxTime; }
    public void   setLastTaxTime(long t) { this.lastTaxTime = t; }

    public int    getLastClaimedBlocks() { return lastClaimedBlocks; }
    public void   setLastClaimedBlocks(int b) { this.lastClaimedBlocks = b; }
}
