package dev.claimtax.util;

import dev.claimtax.ClaimTaxPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class TaxLogger {

    private final ClaimTaxPlugin plugin;
    private final File logFile;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TaxLogger(ClaimTaxPlugin plugin) {
        this.plugin  = plugin;
        this.logFile = new File(plugin.getDataFolder(), plugin.getTaxConfig().getLogFile());
        if (!logFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { logFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Không thể tạo file log thuế", e);
            }
        }
    }

    public void log(String message) {
        if (!plugin.getTaxConfig().isLoggingEnabled()) return;

        String line = "[" + LocalDateTime.now().format(FMT) + "] " + message;
        plugin.getLogger().info(message);

        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            pw.println(line);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi ghi log thuế", e);
        }
    }

    public void logTaxSuccess(String playerName, int blocks, double amount) {
        log(String.format("[THU THUẾ] %s | %d blocks | -%.0f tiền", playerName, blocks, amount));
    }

    public void logTaxFailed(String playerName, int blocks, double needed, double balance) {
        log(String.format("[THIẾU TIỀN] %s | %d blocks | Cần %.0f | Có %.0f",
                playerName, blocks, needed, balance));
    }

    public void logTaxExempt(String playerName, int blocks, String reason) {
        log(String.format("[MIỄN THUẾ] %s | %d blocks | Lý do: %s", playerName, blocks, reason));
    }

    public void logCycleSummary(int total, int taxed, int exempt, int failed, double revenue) {
        log("=== KẾT QUẢ CHU KỲ THUẾ ===");
        log(String.format("  Tổng người chơi: %d | Thu thuế: %d | Miễn: %d | Thiếu tiền: %d",
                total, taxed, exempt, failed));
        log(String.format("  Tổng doanh thu: %.0f tiền", revenue));
        log("============================");
    }
}
