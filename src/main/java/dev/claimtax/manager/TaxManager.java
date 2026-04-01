package dev.claimtax.manager;

import dev.claimtax.ClaimTaxPlugin;
import dev.claimtax.data.TaxRecord;
import dev.claimtax.manager.ClaimAnalyzer.PlayerClaimInfo;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TaxManager {

    private final ClaimTaxPlugin plugin;
    private final ClaimAnalyzer  analyzer;

    public TaxManager(ClaimTaxPlugin plugin) {
        this.plugin   = plugin;
        this.analyzer = new ClaimAnalyzer(plugin);
    }

    public void runTaxCycle(boolean silent) {
        if (!silent) plugin.getLogger().info("=== BAT DAU CHU KY THU THUE ===");

        Set<UUID> owners = analyzer.getAllClaimOwners();
        int countTaxed = 0, countExempt = 0, countPending = 0;
        double totalRevenue = 0.0;

        for (UUID uuid : owners) {
            TaxResult result = processTax(uuid, false);
            if (result == null) continue;
            switch (result.status) {
                case TAXED   -> { countTaxed++;  totalRevenue += result.amount; }
                case EXEMPT  -> countExempt++;
                case PENDING -> countPending++;
            }
        }

        plugin.getDataStore().saveAll();
        plugin.getTaxLogger().logCycleSummary(owners.size(), countTaxed, countExempt, countPending, totalRevenue);

        if (!silent) {
            plugin.getLogger().info(String.format(
                    "[ClaimTax] Hoan tat: %d thu | %d mien | %d dang canh bao | %.0f coins",
                    countTaxed, countExempt, countPending, totalRevenue));
        }
    }

    public enum TaxStatus { TAXED, EXEMPT, PENDING, ERROR }

    public static class TaxResult {
        public final TaxStatus status;
        public final double    amount;
        public final int       blocks;
        public final String    reason;

        public TaxResult(TaxStatus status, double amount, int blocks, String reason) {
            this.status = status;
            this.amount = amount;
            this.blocks = blocks;
            this.reason = reason;
        }
    }

    public TaxResult processTax(UUID uuid, boolean notifyNow) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            plugin.getLogger().severe("[ClaimTax] Vault Economy chua khoi tao!");
            return new TaxResult(TaxStatus.ERROR, 0, 0, "Economy null");
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
        Player onlinePlayer = Bukkit.getPlayer(uuid);

        if (onlinePlayer != null && onlinePlayer.hasPermission("claimtax.exempt")) {
            plugin.getTaxLogger().logTaxExempt(playerName, 0, "claimtax.exempt permission");
            return new TaxResult(TaxStatus.EXEMPT, 0, 0, "permission");
        }

        PlayerClaimInfo info = analyzer.analyze(uuid);
        if (info == null) return new TaxResult(TaxStatus.ERROR, 0, 0, "analyze failed");

        int totalBlocks = info.totalBlocks;

        TaxRecord record = plugin.getDataStore().getOrCreate(uuid, playerName);
        record.setPlayerName(playerName);
        record.setLastClaimedBlocks(totalBlocks);
        record.setLastTaxTime(System.currentTimeMillis());

        int minBlocks = plugin.getTaxConfig().getMinimumBlocks();
        if (totalBlocks < minBlocks) {
            plugin.getTaxLogger().logTaxExempt(playerName, totalBlocks, "duoi " + minBlocks + " blocks");
            if (notifyNow && onlinePlayer != null) {
                onlinePlayer.sendMessage(plugin.getTaxConfig().getTaxExemptBlocksMsg(totalBlocks));
            }
            plugin.getDiscordNotifier().notifyExempt(uuid, totalBlocks, minBlocks);

            plugin.getDataStore().save(record);
            return new TaxResult(TaxStatus.EXEMPT, 0, totalBlocks, "below minimum");
        }

        double taxAmount = plugin.getTaxConfig().calculateTax(totalBlocks);
        double balance   = economy.getBalance(offlinePlayer);

        if (balance < taxAmount) {
            if (!plugin.getDebtTracker().hasDebt(uuid)) {
                plugin.getDebtTracker().addDebt(uuid, taxAmount, totalBlocks);
                plugin.getTaxLogger().logTaxFailed(playerName, totalBlocks, taxAmount, balance);

                long warnMins = Math.round(plugin.getTaxConfig().getWarningIntervalTicks() / 1200.0);
                int  maxWarn  = plugin.getTaxConfig().getMaxWarnings();

                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage(plugin.getTaxConfig().getPrefix()
                            + "§cBan thieu §e" + String.format("%,.0f", taxAmount - balance)
                            + " §ctien thue! Hay nap them ngay!"
                            + " §7Canh bao moi §e" + warnMins + " §7phut, sau §e"
                            + maxWarn + " §7lan se xoa claim!");
                    if (plugin.getTaxConfig().isUseBossBar()) {
                        showBossBar(onlinePlayer,
                                ChatColor.RED + "Thieu tien thue! Can: " + String.format("%,.0f", taxAmount),
                                BarColor.RED);
                    }
                }

                plugin.getDiscordNotifier().notifyTaxPending(
                        uuid, playerName, totalBlocks, taxAmount, balance, maxWarn, warnMins);
            }

            plugin.getDataStore().save(record);
            return new TaxResult(TaxStatus.PENDING, taxAmount, totalBlocks, "pending warning");
        }

        plugin.getDebtTracker().clearDebt(uuid);

        EconomyResponse response = economy.withdrawPlayer(offlinePlayer, taxAmount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("[ClaimTax] Vault tu choi: " + playerName + " | " + response.errorMessage);
            plugin.getDataStore().save(record);
            return new TaxResult(TaxStatus.ERROR, taxAmount, totalBlocks, response.errorMessage);
        }

        record.addPaid(taxAmount);
        record.resetWarnings();
        record.clearDebt();

        plugin.getTaxLogger().logTaxSuccess(playerName, totalBlocks, taxAmount);

        double remaining = economy.getBalance(offlinePlayer);

        if (onlinePlayer != null) {
            onlinePlayer.sendMessage(plugin.getTaxConfig().getTaxCollectedMsg(taxAmount, totalBlocks));
            if (plugin.getTaxConfig().isUseBossBar()) {
                showBossBar(onlinePlayer,
                        ChatColor.GREEN + "Da nop thue " + String.format("%,.0f", taxAmount),
                        BarColor.GREEN);
            }
            if (plugin.getTaxConfig().isShowClaimDetails() && !info.details.isEmpty()) {
                onlinePlayer.sendMessage(plugin.getTaxConfig().getPrefix() + "§7Chi tiet claims:");
                for (ClaimAnalyzer.ClaimDetail d : info.details) {
                    onlinePlayer.sendMessage(String.format("§8  • §e%s §7- §a%d blocks",
                            d.getLocationString(), d.area));
                }
            }
        }

        plugin.getDiscordNotifier().notifyTaxCollected(uuid, playerName, totalBlocks, taxAmount, remaining);

        plugin.getDataStore().save(record);
        return new TaxResult(TaxStatus.TAXED, taxAmount, totalBlocks, "success");
    }

    private void showBossBar(Player player, String title, BarColor color) {
        BossBar bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setProgress(1.0);
        Bukkit.getScheduler().runTaskLater(plugin, bar::removeAll,
                plugin.getTaxConfig().getBossBarDuration() * 20L);
    }

    public ClaimAnalyzer getAnalyzer() { return analyzer; }
}
