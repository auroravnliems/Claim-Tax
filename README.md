# 💰 ClaimTax

Plugin thu thuế claim blocks hàng tuần cho Minecraft, tích hợp **GriefPrevention** + **Vault** + **DiscordSRV**.

---

## 📋 Yêu cầu

| Plugin | Bắt buộc | Ghi chú |
|---|---|---|
| **Spigot / Paper** | ✅ | 1.20+ |
| **GriefPrevention** | ✅ | Nguồn dữ liệu claim |
| **Vault** | ✅ | Xử lý giao dịch tiền tệ |
| **EssentialsX / CMI / ...** | ✅ | Bất kỳ economy plugin nào hỗ trợ Vault |
| **DiscordSRV** | ❌ | Tùy chọn — để gửi thông báo DM Discord |
| **Java** | ✅ | 17+ |

---


## ⚙️ config.yml

```yaml
tax:
  # Thuế suất: số tiền / 1000 blocks / tuần
  amount-per-1000-blocks: 20000.0

  # Ngưỡng tối thiểu — dưới mức này được miễn thuế
  minimum-blocks: 500

  # Chu kỳ thu thuế (ticks, 12096000 = 7 ngày)
  interval-ticks: 12096000

  # Hành động khi thiếu tiền: WARN hoặc DELETE
  on-debt: DELETE

  # Số lần cảnh báo tối đa (chỉ dùng ở WARN mode)
  max-warnings: 3

  # Khoảng thời gian giữa các lần cảnh báo (ticks, 6000 = 5 phút)
  warning-interval-ticks: 6000

discord:
  # Bật/tắt thông báo Discord DM (cần DiscordSRV)
  notify-enabled: true

  # Gửi DM khi được miễn thuế (thường tắt để tránh spam)
  notify-exempt: false

advanced:
  # Tính tổng cả sub-claims (ngăn chia nhỏ claim để trốn thuế)
  count-sub-claims: true

  # Hiển thị chi tiết từng claim khi thông báo
  show-claim-details: false

  # Dùng BossBar thay vì chat
  use-bossbar: false
  bossbar-duration: 10

logging:
  enabled: true
  file: "tax_log.txt"
  log-per-player: true
```

---

## 💡 Công thức tính thuế

```
Thuế = (Tổng blocks / 1000) × amount-per-1000-blocks

Ví dụ:
  5000 blocks × 20,000 / 1000 = 100,000 coins / tuần
  500  blocks               → Miễn thuế (dưới ngưỡng)
```

---

## 🛡️ Chống gian lận (Anti-Fraud)

- Dữ liệu claim lấy **thẳng từ GriefPrevention DataStore** — không qua input người chơi
- Tổng hợp **tất cả claim** của một người (kể cả chia nhỏ ra nhiều mảnh)
- Sub-claims được đếm nhưng **không double-count** để tránh tính sai
- Người chơi **không thể giả mạo** số blocks đã claim

---

## 🔔 Thông báo Discord DM

> Yêu cầu DiscordSRV và người chơi đã `/discord link` tài khoản.

| Sự kiện | Màu | Nội dung |
|---|---|---|
| Thu thuế thành công | 🟢 Xanh lá | Blocks, số tiền, số dư còn lại |
| Thiếu tiền lần đầu | 🟠 Cam | Số còn thiếu, lịch cảnh báo |
| Cảnh báo định kỳ | 🟠/🔴 | Lần X/Y, thời gian còn lại |
| Claim bị xóa | 🔴 Đỏ | Số claim xóa, số nợ, không hoàn lại |
| Tự thu sau cảnh báo | 🟢 Xanh lá | Đã tự thu khi có đủ tiền |
| Miễn thuế | 🔵 Xanh dương | Dưới ngưỡng block (tắt mặc định) |

**Hoạt động cả khi người chơi offline** — chỉ cần đã link Discord.

---

## 🎮 Lệnh

| Lệnh | Quyền | Mô tả |
|---|---|---|
| `/claimtax info` | `claimtax.info` | Xem thông tin thuế của bản thân |
| `/claimtax info <player>` | `claimtax.admin` | Xem thông tin của người chơi khác |
| `/claimtax run` | `claimtax.admin` | Thu thuế toàn server ngay lập tức |
| `/claimtax run <player>` | `claimtax.admin` | Thu thuế một người chơi |
| `/claimtax setrate <số>` | `claimtax.admin` | Đổi thuế suất (lưu vào config) |
| `/claimtax reload` | `claimtax.admin` | Reload config + restart scheduler |
| `/claimtax status` | `claimtax.admin` | Xem trạng thái plugin |
| `/claimtax top` | `*` | Top người nộp thuế nhiều nhất |
| `/claimtax help` | `*` | Xem danh sách lệnh |

Alias: `/ctx`, `/tax`

---

## 🔑 Quyền

| Quyền | Mặc định | Mô tả |
|---|---|---|
| `claimtax.admin` | OP | Toàn quyền quản lý |
| `claimtax.exempt` | `false` | Miễn thuế vĩnh viễn |
| `claimtax.info` | `true` | Xem thông tin thuế bản thân |

---

## 🧪 Cấu hình test nhanh

Chỉnh `config.yml` để test mà không cần chờ 7 ngày:

```yaml
tax:
  interval-ticks: 300          # 15 giây = 1 chu kỳ thu thuế
  warning-interval-ticks: 100  # 5 giây = 1 lần cảnh báo
  max-warnings: 2              # 2 lần cảnh báo rồi xóa
  minimum-blocks: 10           # Ngưỡng thấp để dễ test
```

Sau đó dùng `/claimtax run` để thu thuế thủ công ngay.

---
