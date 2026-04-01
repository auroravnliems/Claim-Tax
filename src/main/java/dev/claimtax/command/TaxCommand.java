package dev.claimtax.command;

import dev.claimtax.ClaimTaxPlugin;
import dev.claimtax.data.TaxRecord;
import dev.claimtax.manager.ClaimAnalyzer;
import dev.claimtax.manager.TaxManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class TaxCommand implements CommandExecutor, TabCompleter {

    private final ClaimTaxPlugin plugin;

    private static final String LINE = ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final String GOLD  = ChatColor.GOLD + "";
    private static final String GREEN = ChatColor.GREEN + "";
    private static final String RED   = ChatColor.RED + "";
    private static final String GRAY  = ChatColor.GRAY + "";
    private static final String AQUA  = ChatColor.AQUA + "";
    private static final String YELLOW= ChatColor.YELLOW + "";

    public TaxCommand(ClaimTaxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getTaxConfig().getPrefix();

        if (args.length == 0) {
            sendHelp(sender, prefix);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help"   -> sendHelp(sender, prefix);
            case "info"   -> cmdInfo(sender, args, prefix);
            case "run"    -> cmdRun(sender, args, prefix);
            case "reload" -> cmdReload(sender, prefix);
            case "status" -> cmdStatus(sender, prefix);
            case "top"    -> cmdTop(sender, prefix);
            case "setrate"-> cmdSetRate(sender, args, prefix);
            default       -> sender.sendMessage(prefix + RED + "Lệnh không hợp lệ. Dùng /claimtax help");
        }
        return true;
    }

    private void cmdInfo(CommandSender sender, String[] args, String prefix) {
        UUID targetUUID;
        String targetName;

        if (args.length >= 2) {
            if (!sender.hasPermission("claimtax.admin")) {
                sender.sendMessage(prefix + RED + "Bạn không có quyền xem thông tin của người khác!");
                return;
            }
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            if (!op.hasPlayedBefore() && op.getPlayer() == null) {
                sender.sendMessage(prefix + RED + "Không tìm thấy người chơi: " + args[1]);
                return;
            }
            targetUUID = op.getUniqueId();
            targetName = op.getName() != null ? op.getName() : args[1];
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + RED + "Console phải chỉ định tên người chơi!");
                return;
            }
            if (!sender.hasPermission("claimtax.info")) {
                sender.sendMessage(prefix + RED + "Bạn không có quyền xem thông tin thuế!");
                return;
            }
            Player p = (Player) sender;
            targetUUID = p.getUniqueId();
            targetName = p.getName();
        }

        ClaimAnalyzer.PlayerClaimInfo info = plugin.getTaxManager().getAnalyzer().analyze(targetUUID);
        TaxRecord record = plugin.getDataStore().get(targetUUID);

        sender.sendMessage(LINE);
        sender.sendMessage(GOLD + "  💰 Thông tin thuế - " + YELLOW + targetName);
        sender.sendMessage(LINE);

        if (info != null) {
            int blocks = info.totalBlocks;
            double taxDue = plugin.getTaxConfig().calculateTax(blocks);
            int minBlocks = plugin.getTaxConfig().getMinimumBlocks();

            sender.sendMessage(GRAY + "  📦 Tổng blocks claim  : " + AQUA + blocks + " blocks");
            sender.sendMessage(GRAY + "  🏘 Số claim chính     : " + AQUA + info.claimCount);
            sender.sendMessage(GRAY + "  💵 Thuế suất          : " + AQUA +
                    String.format("%,.0f", plugin.getTaxConfig().getAmountPer1000Blocks()) +
                    " / 1000 blocks");
            sender.sendMessage(GRAY + "  💳 Thuế phải nộp      : " + YELLOW +
                    String.format("%,.0f", taxDue) + " tiền / tuần");

            if (blocks < minBlocks) {
                sender.sendMessage(GRAY + "  ✅ Trạng thái         : " + GREEN + "Miễn thuế (dưới " + minBlocks + " blocks)");
            } else {
                sender.sendMessage(GRAY + "  ⚠ Trạng thái          : " + RED + "Phải nộp thuế");
            }

            if (info.details.size() > 0 && plugin.getTaxConfig().isShowClaimDetails()) {
                sender.sendMessage(GRAY + "  📍 Các claim:");
                for (ClaimAnalyzer.ClaimDetail d : info.details) {
                    sender.sendMessage(GRAY + "     • " + AQUA + d.getLocationString() +
                            GRAY + " - " + GREEN + d.area + " blocks");
                }
            }
        } else {
            sender.sendMessage(GRAY + "  📦 Tổng blocks: " + RED + "Không lấy được dữ liệu");
        }

        if (record != null) {
            sender.sendMessage(GRAY + "  💰 Tổng đã nộp        : " + GREEN +
                    String.format("%,.0f", record.getTotalPaid()));
            sender.sendMessage(GRAY + "  ❌ Tổng nợ thuế       : " + RED +
                    String.format("%,.0f", record.getTotalDebt()));
            sender.sendMessage(GRAY + "  ⚠ Số lần cảnh báo    : " + YELLOW + record.getWarningCount());
        } else {
            sender.sendMessage(GRAY + "  📝 Chưa có lịch sử nộp thuế.");
        }

        sender.sendMessage(LINE);
    }

    private void cmdRun(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("claimtax.admin")) {
            sender.sendMessage(prefix + RED + "Bạn không có quyền admin!");
            return;
        }

        if (args.length >= 2) {
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            if (!op.hasPlayedBefore() && op.getPlayer() == null) {
                sender.sendMessage(prefix + RED + "Không tìm thấy người chơi: " + args[1]);
                return;
            }

            sender.sendMessage(prefix + YELLOW + "Đang thu thuế " + args[1] + "...");

            UUID targetUUID = op.getUniqueId();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                TaxManager.TaxResult result = plugin.getTaxManager().processTax(targetUUID, true);
                if (result != null) {
                    String statusStr = switch (result.status) {
                        case TAXED  -> GREEN + "Thành công (-" + String.format("%,.0f", result.amount) + ")";
                        case EXEMPT -> AQUA  + "Miễn thuế";
                        case PENDING -> RED   + "Thieu tien - Dang canh bao";
                        case ERROR  -> RED   + "Lỗi: " + result.reason;
                    };
                    plugin.getDataStore().save(plugin.getDataStore().getOrCreate(targetUUID, args[1]));
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(prefix + "Kết quả: " + statusStr +
                                GRAY + " (" + result.blocks + " blocks)")
                    );
                }
            });

        } else {
            sender.sendMessage(prefix + YELLOW + "Đang thu thuế tất cả người chơi có claim...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getTaxManager().runTaxCycle(false);
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(prefix + GREEN + "Hoàn tất chu kỳ thu thuế! Xem log để biết chi tiết.")
                );
            });
        }
    }

    private void cmdReload(CommandSender sender, String prefix) {
        if (!sender.hasPermission("claimtax.admin")) {
            sender.sendMessage(prefix + RED + "Bạn không có quyền admin!");
            return;
        }
        plugin.getTaxConfig().reload();
        plugin.getScheduler().restart();
        sender.sendMessage(prefix + GREEN + "Đã reload config và khởi động lại lịch thu thuế!");
        sender.sendMessage(prefix + GRAY + "Thuế suất: " + YELLOW +
                String.format("%,.0f", plugin.getTaxConfig().getAmountPer1000Blocks()) +
                GRAY + " / 1000 blocks");
        sender.sendMessage(prefix + GRAY + "Chu kỳ: " + YELLOW +
                plugin.getTaxConfig().getIntervalTicks() + " ticks");
    }

    private void cmdStatus(CommandSender sender, String prefix) {
        if (!sender.hasPermission("claimtax.admin")) {
            sender.sendMessage(prefix + RED + "Bạn không có quyền admin!");
            return;
        }
        sender.sendMessage(LINE);
        sender.sendMessage(GOLD + "  ⚙ Trạng thái ClaimTax");
        sender.sendMessage(LINE);
        sender.sendMessage(GRAY + "  🔄 Scheduler     : " +
                (plugin.getScheduler().isRunning() ? GREEN + "Đang chạy" : RED + "Đã dừng"));
        sender.sendMessage(GRAY + "  💵 Thuế suất     : " + YELLOW +
                String.format("%,.0f", plugin.getTaxConfig().getAmountPer1000Blocks()) +
                " / 1000 blocks / tuần");
        sender.sendMessage(GRAY + "  📦 Ngưỡng miễn   : " + YELLOW +
                plugin.getTaxConfig().getMinimumBlocks() + " blocks");
        sender.sendMessage(GRAY + "  ⏱ Chu kỳ        : " + YELLOW +
                plugin.getTaxConfig().getIntervalTicks() + " ticks");
        sender.sendMessage(GRAY + "  📋 Bản ghi thuế  : " + YELLOW +
                plugin.getDataStore().getRecordCount() + " người chơi");
        sender.sendMessage(GRAY + "  💰 Economy       : " +
                (plugin.getEconomy() != null ? GREEN + plugin.getEconomy().getName() : RED + "Không tìm thấy"));
        sender.sendMessage(LINE);
    }

    private void cmdTop(CommandSender sender, String prefix) {
        Collection<TaxRecord> all = plugin.getDataStore().getAllRecords();
        List<TaxRecord> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> Double.compare(b.getTotalPaid(), a.getTotalPaid()));

        sender.sendMessage(LINE);
        sender.sendMessage(GOLD + "  🏆 Top người nộp thuế nhiều nhất");
        sender.sendMessage(LINE);

        int count = Math.min(sorted.size(), 10);
        for (int i = 0; i < count; i++) {
            TaxRecord r = sorted.get(i);
            String medal = switch (i) {
                case 0 -> "🥇";
                case 1 -> "🥈";
                case 2 -> "🥉";
                default -> (i + 1) + ".";
            };
            sender.sendMessage(String.format("%s%s %s%-16s %s%,.0f tiền",
                    GRAY, medal, YELLOW, r.getPlayerName(), GREEN, r.getTotalPaid()));
        }
        if (count == 0) sender.sendMessage(GRAY + "  Chưa có dữ liệu.");
        sender.sendMessage(LINE);
    }

    private void cmdSetRate(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("claimtax.admin")) {
            sender.sendMessage(prefix + RED + "Bạn không có quyền admin!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(prefix + RED + "Cú pháp: /claimtax setrate <số tiền/1000 blocks>");
            return;
        }
        try {
            double newRate = Double.parseDouble(args[1]);
            if (newRate < 0) {
                sender.sendMessage(prefix + RED + "Thuế suất không được âm!");
                return;
            }
            plugin.getConfig().set("tax.amount-per-1000-blocks", newRate);
            plugin.saveConfig();
            plugin.getTaxConfig().reload();
            sender.sendMessage(prefix + GREEN + "Đã đổi thuế suất thành " + YELLOW +
                    String.format("%,.0f", newRate) + GREEN + " / 1000 blocks / tuần");
            sender.sendMessage(prefix + GRAY + "(Đã lưu vào config.yml)");
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + RED + "Số không hợp lệ: " + args[1]);
        }
    }

    private void sendHelp(CommandSender sender, String prefix) {
        boolean isAdmin = sender.hasPermission("claimtax.admin");
        sender.sendMessage(LINE);
        sender.sendMessage(GOLD + "  💰 ClaimTax - Hướng dẫn lệnh");
        sender.sendMessage(LINE);
        sender.sendMessage(GRAY + "  /claimtax info          " + YELLOW + "- Xem thông tin thuế của bạn");
        if (isAdmin) {
            sender.sendMessage(GRAY + "  /claimtax info <player> " + YELLOW + "- Xem thông tin của người chơi khác");
            sender.sendMessage(GRAY + "  /claimtax run           " + YELLOW + "- Thu thuế tất cả người chơi ngay");
            sender.sendMessage(GRAY + "  /claimtax run <player>  " + YELLOW + "- Thu thuế một người chơi");
            sender.sendMessage(GRAY + "  /claimtax setrate <n>   " + YELLOW + "- Đổi thuế suất (n/1000 blocks)");
            sender.sendMessage(GRAY + "  /claimtax reload        " + YELLOW + "- Reload config");
            sender.sendMessage(GRAY + "  /claimtax status        " + YELLOW + "- Xem trạng thái plugin");
        }
        sender.sendMessage(GRAY + "  /claimtax top           " + YELLOW + "- Top người nộp thuế nhiều nhất");
        sender.sendMessage(LINE);
        sender.sendMessage(GRAY + "  📌 Thuế suất: " + YELLOW +
                String.format("%,.0f", plugin.getTaxConfig().getAmountPer1000Blocks()) +
                GRAY + " / 1000 blocks | Ngưỡng miễn: " + YELLOW +
                plugin.getTaxConfig().getMinimumBlocks() + " blocks");
        sender.sendMessage(LINE);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("info", "top", "help"));
            if (sender.hasPermission("claimtax.admin")) {
                subs.addAll(Arrays.asList("run", "reload", "status", "setrate"));
            }
            StringUtil.copyPartialMatches(args[0], subs, completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("info") || sub.equals("run")) && sender.hasPermission("claimtax.admin")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    StringUtil.copyPartialMatches(args[1], List.of(p.getName()), completions);
                }
            } else if (sub.equals("setrate")) {
                completions.addAll(List.of("1000", "5000", "10000", "20000", "50000"));
            }
        }

        Collections.sort(completions);
        return completions;
    }
}
