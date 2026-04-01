package dev.claimtax.task;

import dev.claimtax.ClaimTaxPlugin;
import org.bukkit.scheduler.BukkitTask;

public class TaxScheduler {

    private final ClaimTaxPlugin plugin;
    private BukkitTask currentTask;

    public TaxScheduler(ClaimTaxPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();

        long interval = plugin.getTaxConfig().getIntervalTicks();

        currentTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getLogger().info("[ClaimTax] Bắt đầu thu thuế định kỳ...");
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                plugin.getTaxManager().runTaxCycle(false)
            );
        }, interval, interval);

        plugin.getLogger().info(String.format(
                "[ClaimTax] Đã lên lịch thu thuế mỗi %d ticks (%.1f giờ / %.1f ngày)",
                interval,
                interval / 72000.0,
                interval / 1728000.0));
    }

    public void stop() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel();
            currentTask = null;
            plugin.getLogger().info("[ClaimTax] Đã dừng lịch thu thuế.");
        }
    }

    public void restart() {
        stop();
        start();
    }

    public boolean isRunning() {
        return currentTask != null && !currentTask.isCancelled();
    }
}
