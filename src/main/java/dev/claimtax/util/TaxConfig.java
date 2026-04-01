package dev.claimtax.util;

import dev.claimtax.ClaimTaxPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class TaxConfig {

    private final ClaimTaxPlugin plugin;

    private double amountPer1000Blocks;
    private int    minimumBlocks;
    private long   intervalTicks;
    private String onDebt;
    private int    maxWarnings;
    private long   warningIntervalTicks;
    private boolean countSubClaims;
    private boolean showClaimDetails;
    private boolean useBossBar;
    private int    bossBarDuration;

    private boolean discordNotifyEnabled;
    private boolean discordNotifyExempt;

    private String prefix;
    private String msgTaxCollected;
    private String msgTaxFailed;
    private String msgTaxExemptBlocks;
    private String msgTaxExemptPerm;
    private String msgClaimDeleted;

    private boolean loggingEnabled;
    private String  logFile;
    private boolean logPerPlayer;

    public TaxConfig(ClaimTaxPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        amountPer1000Blocks  = cfg.getDouble("tax.amount-per-1000-blocks", 20000.0);
        minimumBlocks        = cfg.getInt("tax.minimum-blocks", 500);
        intervalTicks        = cfg.getLong("tax.interval-ticks", 12096000L);
        onDebt               = cfg.getString("tax.on-debt", "DELETE").toUpperCase();
        maxWarnings          = cfg.getInt("tax.max-warnings", 3);
        warningIntervalTicks = cfg.getLong("tax.warning-interval-ticks", 6000L);
        countSubClaims       = cfg.getBoolean("advanced.count-sub-claims", true);
        showClaimDetails     = cfg.getBoolean("advanced.show-claim-details", false);
        useBossBar           = cfg.getBoolean("advanced.use-bossbar", false);
        bossBarDuration      = cfg.getInt("advanced.bossbar-duration", 10);

        discordNotifyEnabled = cfg.getBoolean("discord.notify-enabled", true);
        discordNotifyExempt  = cfg.getBoolean("discord.notify-exempt", false);

        prefix               = color(cfg.getString("messages.prefix", "&8[&6ClaimTax&8] "));
        msgTaxCollected      = cfg.getString("messages.tax-collected", "");
        msgTaxFailed         = cfg.getString("messages.tax-failed", "");
        msgTaxExemptBlocks   = cfg.getString("messages.tax-exempt-blocks", "");
        msgTaxExemptPerm     = cfg.getString("messages.tax-exempt-perm", "");
        msgClaimDeleted      = cfg.getString("messages.claim-deleted", "");

        loggingEnabled       = cfg.getBoolean("logging.enabled", true);
        logFile              = cfg.getString("logging.file", "tax_log.txt");
        logPerPlayer         = cfg.getBoolean("logging.log-per-player", true);
    }

    private String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }

    public double  getAmountPer1000Blocks()  { return amountPer1000Blocks; }
    public int     getMinimumBlocks()        { return minimumBlocks; }
    public long    getIntervalTicks()        { return intervalTicks; }
    public String  getOnDebt()               { return onDebt; }
    public int     getMaxWarnings()          { return maxWarnings; }
    public long    getWarningIntervalTicks() { return warningIntervalTicks; }
    public boolean isCountSubClaims()        { return countSubClaims; }
    public boolean isShowClaimDetails()      { return showClaimDetails; }
    public boolean isUseBossBar()            { return useBossBar; }
    public int     getBossBarDuration()      { return bossBarDuration; }
    public String  getPrefix()               { return prefix; }
    public boolean isLoggingEnabled()        { return loggingEnabled; }
    public String  getLogFile()              { return logFile; }
    public boolean isLogPerPlayer()          { return logPerPlayer; }
    public boolean isDiscordNotifyEnabled()  { return discordNotifyEnabled; }
    public boolean isDiscordNotifyExempt()   { return discordNotifyExempt; }

    public double calculateTax(int totalBlocks) {
        return (totalBlocks / 1000.0) * amountPer1000Blocks;
    }

    public String getTaxCollectedMsg(double amount, int blocks) {
        return prefix + color(msgTaxCollected
                .replace("{amount}", String.format("%,.0f", amount))
                .replace("{blocks}", String.valueOf(blocks)));
    }

    public String getTaxFailedMsg(double amount, double balance, int warnings) {
        return prefix + color(msgTaxFailed
                .replace("{amount}", String.format("%,.0f", amount))
                .replace("{balance}", String.format("%,.0f", balance))
                .replace("{warnings}", String.valueOf(warnings))
                .replace("{max}", String.valueOf(maxWarnings)));
    }

    public String getTaxExemptBlocksMsg(int blocks) {
        return prefix + color(msgTaxExemptBlocks
                .replace("{blocks}", String.valueOf(blocks))
                .replace("{min}", String.valueOf(minimumBlocks)));
    }

    public String getTaxExemptPermMsg() {
        return prefix + color(msgTaxExemptPerm);
    }

    public String getClaimDeletedMsg(String location, double amount) {
        return prefix + color(msgClaimDeleted
                .replace("{location}", location)
                .replace("{amount}", String.format("%,.0f", amount)));
    }
}
