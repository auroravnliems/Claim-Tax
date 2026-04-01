package dev.claimtax.task;

import dev.claimtax.ClaimTaxPlugin;
import dev.claimtax.data.DebtTracker;
import dev.claimtax.data.DebtTracker.DebtEntry;
import dev.claimtax.data.TaxRecord;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class WarningScheduler {

    private final ClaimTaxPlugin plugin;
    private BukkitTask task;

    public WarningScheduler(ClaimTaxPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        long intervalTicks = plugin.getTaxConfig().getWarningIntervalTicks();
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::processPendingDebts, intervalTicks, intervalTicks);
        plugin.getLogger().info(String.format(
                "[ClaimTax] Warning scheduler: moi %.0f phut, toi da %d canh bao.",
                intervalTicks / 1200.0, plugin.getTaxConfig().getMaxWarnings()));
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }

    public void restart() { stop(); start(); }

    public boolean isRunning() { return task != null && !task.isCancelled(); }

    private void processPendingDebts() {
        DebtTracker tracker = plugin.getDebtTracker();
        if (tracker.size() == 0) return;

        Economy economy = plugin.getEconomy();
        if (economy == null) return;

        long warnMins = Math.round(plugin.getTaxConfig().getWarningIntervalTicks() / 1200.0);
        int  maxWarn  = plugin.getTaxConfig().getMaxWarnings();

        plugin.getLogger().info("[ClaimTax] Kiem tra " + tracker.size() + " nguoi choi dang no thue...");

        for (Map.Entry<UUID, DebtEntry> entry : tracker.getAllDebts()) {
            UUID      uuid = entry.getKey();
            DebtEntry debt = entry.getValue();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
            Player onlinePlayer = Bukkit.getPlayer(uuid);

            double balance   = economy.getBalance(offlinePlayer);
            double taxAmount = debt.taxAmount;

            if (balance >= taxAmount) {
                EconomyResponse response = economy.withdrawPlayer(offlinePlayer, taxAmount);
                if (response.transactionSuccess()) {
                    tracker.clearDebt(uuid);

                    TaxRecord record = plugin.getDataStore().getOrCreate(uuid, playerName);
                    record.addPaid(taxAmount);
                    record.resetWarnings();
                    record.clearDebt();
                    plugin.getDataStore().save(record);

                    plugin.getTaxLogger().logTaxSuccess(playerName, debt.totalBlocks, taxAmount);

                    double remaining = economy.getBalance(offlinePlayer);

                    if (onlinePlayer != null) {
                        onlinePlayer.sendMessage(plugin.getTaxConfig().getPrefix()
                                + "§aDa tu dong thu thue §e" + String.format("%,.0f", taxAmount)
                                + " §ado ban da co du tien. Cam on!");
                    }

                    plugin.getDiscordNotifier().notifyAutoCollected(uuid, playerName, taxAmount, remaining);

                    plugin.getLogger().info("[ClaimTax] " + playerName + " da tra du thue tu dong.");
                }
                continue;
            }

            debt.incrementWarning();
            int warnings  = debt.getWarningCount();
            int remaining = maxWarn - warnings;

            plugin.getTaxLogger().log(String.format("[CANH BAO %d/%d] %s | Can: %.0f | Co: %.0f",
                    warnings, maxWarn, playerName, taxAmount, balance));

            if (warnings >= maxWarn) {
                int deleted = deleteAllClaims(uuid, playerName, taxAmount, balance, onlinePlayer);
                tracker.clearDebt(uuid);

                TaxRecord record = plugin.getDataStore().getOrCreate(uuid, playerName);
                record.resetWarnings();
                record.clearDebt();
                plugin.getDataStore().save(record);

            } else {
                String warnMsg = plugin.getTaxConfig().getPrefix()
                        + "§c[CANH BAO " + warnings + "/" + maxWarn + "] "
                        + "§cVan chua du tien! Con thieu §e"
                        + String.format("%,.0f", taxAmount - balance)
                        + " §ccoins. Con §e" + remaining + " §clan truoc khi claim bi xoa!";

                if (onlinePlayer != null) onlinePlayer.sendMessage(warnMsg);

                TaxRecord record = plugin.getDataStore().getOrCreate(uuid, playerName);
                record.addWarning();
                record.addDebt(taxAmount - balance);
                plugin.getDataStore().save(record);

                plugin.getDiscordNotifier().notifyWarning(
                        uuid, playerName, warnings, maxWarn, taxAmount, balance, warnMins);
            }
        }
    }

    private int deleteAllClaims(UUID uuid, String playerName, double taxAmount,
                                double balance, Player onlinePlayer) {
        GriefPrevention gp = GriefPrevention.instance;
        if (gp == null) return 0;

        int deletedCount = 0;
        try {
            PlayerData playerData = gp.dataStore.getPlayerData(uuid);
            List<Claim> claimsToDelete = new ArrayList<>(playerData.getClaims());
            if (claimsToDelete.isEmpty()) return 0;

            plugin.getLogger().warning(String.format(
                    "[ClaimTax] XOA %d claims cua [%s] | No: %.0f | Khong hoan tien",
                    claimsToDelete.size(), playerName, taxAmount));

            for (Claim claim : claimsToDelete) {
                if (claim == null || claim.isAdminClaim()) continue;
                try {
                    gp.dataStore.deleteClaim(claim);
                    deletedCount++;
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "[ClaimTax] Loi xoa claim cua " + playerName, e);
                }
            }

            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(plugin.getTaxConfig().getPrefix()
                        + "§c§lTOAN BO " + deletedCount + " CLAIM DA BI XOA!"
                        + " §cKhong nop §e" + String.format("%,.0f", taxAmount)
                        + " §csau " + plugin.getTaxConfig().getMaxWarnings()
                        + " lan canh bao. Khong hoan lai!");
            }

            final int finalDeleted = deletedCount;
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.broadcastMessage(plugin.getTaxConfig().getPrefix()
                            + "§e" + playerName + " §cbi xoa §e" + finalDeleted
                            + " §cclaim do khong nop thue §e"
                            + String.format("%,.0f", taxAmount) + "§c!")
            );

            plugin.getDiscordNotifier().notifyClaimDeleted(
                    uuid, playerName, deletedCount, taxAmount, balance);

            plugin.getTaxLogger().log(String.format(
                    "[XOA CLAIM] %s | Xoa %d claims | No: %.0f | Khong hoan tien",
                    playerName, deletedCount, taxAmount));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[ClaimTax] Loi nghiem trong khi xoa claims cua " + playerName, e);
        }

        return deletedCount;
    }
}
