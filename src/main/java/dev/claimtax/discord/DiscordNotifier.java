package dev.claimtax.discord;

import dev.claimtax.ClaimTaxPlugin;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

public class DiscordNotifier {

    private final ClaimTaxPlugin plugin;
    private boolean discordSrvAvailable = false;

    private static final Color COLOR_SUCCESS = new Color(0x2ECC71);
    private static final Color COLOR_WARNING = new Color(0xE67E22);
    private static final Color COLOR_DANGER  = new Color(0xE74C3C);
    private static final Color COLOR_INFO    = new Color(0x3498DB);

    public DiscordNotifier(ClaimTaxPlugin plugin) {
        this.plugin = plugin;
        checkDiscordSRV();
    }

    private void checkDiscordSRV() {
        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            discordSrvAvailable = true;
            plugin.getLogger().info("[ClaimTax] Đã kết nối DiscordSRV - thông báo Discord DM đã bật.");
        } else {
            plugin.getLogger().warning("[ClaimTax] DiscordSRV chưa bật - Discord DM sẽ bị tắt.");
        }
    }

    public boolean isAvailable() {
        return discordSrvAvailable;
    }

    private User getDiscordUser(UUID uuid) {
        if (!discordSrvAvailable) return null;
        try {
            String discordId = DiscordSRV.getPlugin()
                    .getAccountLinkManager()
                    .getDiscordId(uuid);

            if (discordId == null) return null;

            return DiscordSRV.getPlugin()
                    .getJda()
                    .retrieveUserById(discordId)
                    .complete();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "[ClaimTax] Lỗi lấy Discord user của UUID: " + uuid, e);
            return null;
        }
    }

    private void sendDM(UUID uuid, EmbedBuilder embed) {
        if (!discordSrvAvailable) return;
        if (!plugin.getTaxConfig().isDiscordNotifyEnabled()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                User user = getDiscordUser(uuid);
                if (user == null) return;

                embed.setTimestamp(Instant.now());
                embed.setFooter("ClaimTax • " + Bukkit.getServer().getName());

                user.openPrivateChannel().queue(
                        channel -> channel.sendMessageEmbeds(embed.build()).queue(
                                success -> { /* Gửi thành công */ },
                                error   -> plugin.getLogger().warning(
                                        "[ClaimTax] Không gửi được DM cho " + user.getName()
                                                + ": " + error.getMessage())
                        ),
                        error -> plugin.getLogger().warning(
                                "[ClaimTax] Không mở được kênh DM: " + error.getMessage())
                );
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[ClaimTax] Lỗi gửi Discord DM", e);
            }
        });
    }

    public void notifyTaxCollected(UUID uuid, String playerName, int blocks, double amount, double remaining) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(COLOR_SUCCESS)
                .setTitle("✅ Thuế claim đã được thu")
                .setDescription("Thuế claim hàng tuần của bạn đã được tự động thu.")
                .addField("📦 Tổng blocks claim", blocks + " blocks", true)
                .addField("💳 Số tiền thuế", String.format("%,.0f", amount) + " coins", true)
                .addField("💰 Số dư còn lại", String.format("%,.0f", remaining) + " coins", true)
                .addField("ℹ️ Thuế suất",
                        String.format("%,.0f", plugin.getTaxConfig().getAmountPer1000Blocks())
                                + " coins / 1000 blocks / tuần", false);
        sendDM(uuid, embed);
    }

    public void notifyTaxPending(UUID uuid, String playerName, int blocks,
                                 double taxAmount, double balance, int maxWarnings, long intervalMinutes) {
        double shortage = taxAmount - balance;
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(COLOR_WARNING)
                .setTitle("⚠️ Thiếu tiền nộp thuế!")
                .setDescription("Bạn không đủ tiền để nộp thuế claim. Hãy nạp thêm tiền ngay!")
                .addField("📦 Tổng blocks claim", blocks + " blocks", true)
                .addField("💳 Thuế phải nộp", String.format("%,.0f", taxAmount) + " coins", true)
                .addField("💰 Số dư hiện tại", String.format("%,.0f", balance) + " coins", true)
                .addField("❌ Còn thiếu", String.format("%,.0f", shortage) + " coins", true)
                .addField("⏱️ Lịch cảnh báo",
                        "Kiểm tra lại mỗi **" + intervalMinutes + " phút**\n"
                                + "Sau **" + maxWarnings + " lần** cảnh báo → claim bị xóa!", false)
                .addField("💡 Mẹo",
                        "Hãy kiểm tra số dư và nạp thêm tiền trước lần cảnh báo tiếp theo.", false);
        sendDM(uuid, embed);
    }

    public void notifyWarning(UUID uuid, String playerName, int warningNum, int maxWarnings,
                              double taxAmount, double balance, long intervalMinutes) {
        double shortage  = taxAmount - balance;
        int    remaining = maxWarnings - warningNum;
        Color  color     = remaining <= 1 ? COLOR_DANGER : COLOR_WARNING;
        String urgency   = remaining <= 1 ? "🚨 KHẨN CẤP!" : "⚠️ Cảnh báo";

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(color)
                .setTitle(urgency + " Cảnh báo thuế [" + warningNum + "/" + maxWarnings + "]")
                .setDescription(remaining > 0
                        ? "Bạn vẫn chưa đủ tiền nộp thuế! Còn **" + remaining + " lần** trước khi claim bị xóa."
                        : "**Đây là lần cảnh báo cuối cùng!** Claim sẽ bị xóa trong **" + intervalMinutes + " phút** nữa!")
                .addField("💳 Thuế phải nộp", String.format("%,.0f", taxAmount) + " coins", true)
                .addField("💰 Số dư hiện tại", String.format("%,.0f", balance) + " coins", true)
                .addField("❌ Còn thiếu", String.format("%,.0f", shortage) + " coins", true);

        if (remaining == 0) {
            embed.addField("⏰ Thời gian còn lại",
                    "Khoảng **" + intervalMinutes + " phút** trước khi bị xóa!", false);
        }

        sendDM(uuid, embed);
    }

    public void notifyClaimDeleted(UUID uuid, String playerName, int deletedCount,
                                   double taxAmount, double balance) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(COLOR_DANGER)
                .setTitle("🚫 Claim đã bị xóa!")
                .setDescription("Toàn bộ claim của bạn đã bị xóa do không nộp thuế đúng hạn. **Không hoàn lại.**")
                .addField("🏘️ Số claim bị xóa", deletedCount + " claims", true)
                .addField("💳 Thuế còn nợ", String.format("%,.0f", taxAmount) + " coins", true)
                .addField("💰 Số dư của bạn", String.format("%,.0f", balance) + " coins", true)
                .addField("ℹ️ Lý do",
                        "Bạn đã nhận đủ số lần cảnh báo mà không nộp thuế.", false)
                .addField("📋 Lưu ý",
                        "Liên hệ admin nếu bạn có thắc mắc.", false);
        sendDM(uuid, embed);
    }

    public void notifyAutoCollected(UUID uuid, String playerName, double amount, double remaining) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(COLOR_SUCCESS)
                .setTitle("✅ Đã tự động thu thuế!")
                .setDescription("Bạn đã có đủ tiền! Thuế đã được tự động thu và xóa nợ của bạn.")
                .addField("💳 Đã thu", String.format("%,.0f", amount) + " coins", true)
                .addField("💰 Còn lại", String.format("%,.0f", remaining) + " coins", true);
        sendDM(uuid, embed);
    }

    public void notifyExempt(UUID uuid, int blocks, int minBlocks) {
        if (!plugin.getTaxConfig().isDiscordNotifyExempt()) return;
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(COLOR_INFO)
                .setTitle("🟢 Bạn được miễn thuế tuần này")
                .setDescription("Số blocks claim của bạn dưới ngưỡng thu thuế.")
                .addField("📦 Blocks của bạn", blocks + " blocks", true)
                .addField("📋 Ngưỡng miễn thuế", "< " + minBlocks + " blocks", true);
        sendDM(uuid, embed);
    }
}