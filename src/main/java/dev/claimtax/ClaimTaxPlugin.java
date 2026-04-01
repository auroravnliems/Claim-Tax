package dev.claimtax;

import dev.claimtax.command.TaxCommand;
import dev.claimtax.data.DebtTracker;
import dev.claimtax.data.TaxDataStore;
import dev.claimtax.discord.DiscordNotifier;
import dev.claimtax.manager.TaxManager;
import dev.claimtax.task.TaxScheduler;
import dev.claimtax.task.WarningScheduler;
import dev.claimtax.util.TaxConfig;
import dev.claimtax.util.TaxLogger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ClaimTaxPlugin extends JavaPlugin {

    private static ClaimTaxPlugin instance;

    private Economy          economy;
    private TaxConfig        taxConfig;
    private TaxLogger        taxLogger;
    private TaxDataStore     dataStore;
    private DebtTracker      debtTracker;
    private DiscordNotifier  discordNotifier;
    private TaxManager       taxManager;
    private TaxScheduler     scheduler;
    private WarningScheduler warningScheduler;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        taxConfig = new TaxConfig(this);

        if (!setupEconomy()) {
            getLogger().severe("Khong tim thay Vault Economy! Tat plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("GriefPrevention") == null) {
            getLogger().severe("Khong tim thay GriefPrevention! Tat plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        taxLogger       = new TaxLogger(this);
        dataStore       = new TaxDataStore(this);
        debtTracker     = new DebtTracker();
        discordNotifier = new DiscordNotifier(this);
        taxManager      = new TaxManager(this);

        TaxCommand taxCmd = new TaxCommand(this);
        getCommand("claimtax").setExecutor(taxCmd);
        getCommand("claimtax").setTabCompleter(taxCmd);

        scheduler        = new TaxScheduler(this);
        warningScheduler = new WarningScheduler(this);
        scheduler.start();
        warningScheduler.start();

        getLogger().info("ClaimTax da khoi dong!");
        getLogger().info(String.format("  Thue suat   : %.0f / 1000 blocks / tuan",
                taxConfig.getAmountPer1000Blocks()));
        getLogger().info(String.format("  Nguong mien : %d blocks", taxConfig.getMinimumBlocks()));
        getLogger().info(String.format("  Canh bao    : moi %.0f phut x %d lan",
                taxConfig.getWarningIntervalTicks() / 1200.0, taxConfig.getMaxWarnings()));
        getLogger().info("  Discord DM  : " +
                (discordNotifier.isAvailable() ? "BAT (DiscordSRV)" : "TAT (chua co DiscordSRV)"));
    }

    @Override
    public void onDisable() {
        if (warningScheduler != null) warningScheduler.stop();
        if (scheduler != null)        scheduler.stop();
        if (dataStore != null) {
            dataStore.saveAll();
            getLogger().info("Da luu " + dataStore.getRecordCount() + " ban ghi thue.");
        }
        getLogger().info("ClaimTax da tat.");
    }

    public void reloadAll() {
        taxConfig.reload();
        scheduler.restart();
        warningScheduler.restart();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static ClaimTaxPlugin getInstance() { return instance; }
    public Economy          getEconomy()         { return economy; }
    public TaxConfig        getTaxConfig()        { return taxConfig; }
    public TaxLogger        getTaxLogger()        { return taxLogger; }
    public TaxDataStore     getDataStore()        { return dataStore; }
    public DebtTracker      getDebtTracker()      { return debtTracker; }
    public DiscordNotifier  getDiscordNotifier()  { return discordNotifier; }
    public TaxManager       getTaxManager()       { return taxManager; }
    public TaxScheduler     getScheduler()        { return scheduler; }
    public WarningScheduler getWarningScheduler() { return warningScheduler; }
}
