package dev.claimtax.data;

import dev.claimtax.ClaimTaxPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class TaxDataStore {

    private final ClaimTaxPlugin plugin;
    private final File dataFile;
    private YamlConfiguration yaml;

    private final Map<UUID, TaxRecord> records = new HashMap<>();

    public TaxDataStore(ClaimTaxPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "tax_data.yml");
        load();
    }

    public void load() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Không thể tạo tax_data.yml", e);
            }
        }

        yaml = YamlConfiguration.loadConfiguration(dataFile);
        records.clear();

        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection sec = playersSection.getConfigurationSection(uuidStr);
                if (sec == null) continue;

                TaxRecord record = new TaxRecord(uuid, sec.getString("name", "Unknown"));
                record.addPaid(sec.getDouble("total-paid", 0));
                record.addDebt(sec.getDouble("total-debt", 0));

                int warnings = sec.getInt("warnings", 0);
                for (int i = 0; i < warnings; i++) record.addWarning();

                record.setLastTaxTime(sec.getLong("last-tax-time", 0));
                record.setLastClaimedBlocks(sec.getInt("last-claimed-blocks", 0));

                records.put(uuid, record);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("UUID không hợp lệ trong tax_data.yml: " + uuidStr);
            }
        }

        plugin.getLogger().info("Đã tải " + records.size() + " bản ghi thuế.");
    }

    public void saveAll() {
        yaml.set("players", null);

        for (Map.Entry<UUID, TaxRecord> entry : records.entrySet()) {
            String path = "players." + entry.getKey().toString();
            TaxRecord r = entry.getValue();

            yaml.set(path + ".name",                r.getPlayerName());
            yaml.set(path + ".total-paid",          r.getTotalPaid());
            yaml.set(path + ".total-debt",          r.getTotalDebt());
            yaml.set(path + ".warnings",            r.getWarningCount());
            yaml.set(path + ".last-tax-time",       r.getLastTaxTime());
            yaml.set(path + ".last-claimed-blocks", r.getLastClaimedBlocks());
        }

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Không thể lưu tax_data.yml", e);
        }
    }

    public void save(TaxRecord record) {
        String path = "players." + record.getPlayerUUID().toString();

        yaml.set(path + ".name",                record.getPlayerName());
        yaml.set(path + ".total-paid",          record.getTotalPaid());
        yaml.set(path + ".total-debt",          record.getTotalDebt());
        yaml.set(path + ".warnings",            record.getWarningCount());
        yaml.set(path + ".last-tax-time",       record.getLastTaxTime());
        yaml.set(path + ".last-claimed-blocks", record.getLastClaimedBlocks());

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Không thể lưu tax_data.yml", e);
        }
    }

    public TaxRecord getOrCreate(UUID uuid, String playerName) {
        return records.computeIfAbsent(uuid, id -> {
            TaxRecord rec = new TaxRecord(id, playerName);
            plugin.getLogger().info("Tạo bản ghi thuế mới cho: " + playerName);
            return rec;
        });
    }

    public TaxRecord get(UUID uuid) {
        return records.get(uuid);
    }

    public boolean has(UUID uuid) {
        return records.containsKey(uuid);
    }

    public Collection<TaxRecord> getAllRecords() {
        return records.values();
    }

    public int getRecordCount() {
        return records.size();
    }
}
