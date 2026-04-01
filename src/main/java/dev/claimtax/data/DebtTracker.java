package dev.claimtax.data;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DebtTracker {

    public static class DebtEntry {
        public final UUID   playerUUID;
        public final double taxAmount;
        public final int    totalBlocks;
        private int warningCount;

        public DebtEntry(UUID uuid, double taxAmount, int totalBlocks) {
            this.playerUUID  = uuid;
            this.taxAmount   = taxAmount;
            this.totalBlocks = totalBlocks;
            this.warningCount = 0;
        }

        public int  getWarningCount()   { return warningCount; }
        public void incrementWarning()  { warningCount++; }
    }

    private final Map<UUID, DebtEntry> debtMap = new ConcurrentHashMap<>();

    public void addDebt(UUID uuid, double taxAmount, int totalBlocks) {
        debtMap.putIfAbsent(uuid, new DebtEntry(uuid, taxAmount, totalBlocks));
    }

    public void clearDebt(UUID uuid) {
        debtMap.remove(uuid);
    }

    public boolean hasDebt(UUID uuid) {
        return debtMap.containsKey(uuid);
    }

    public DebtEntry getDebt(UUID uuid) {
        return debtMap.get(uuid);
    }

    public Set<Map.Entry<UUID, DebtEntry>> getAllDebts() {
        return Collections.unmodifiableSet(debtMap.entrySet());
    }

    public int size() {
        return debtMap.size();
    }
}
