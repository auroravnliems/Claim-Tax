package dev.claimtax.manager;

import dev.claimtax.ClaimTaxPlugin;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.logging.Level;

public class ClaimAnalyzer {

    private final ClaimTaxPlugin plugin;

    public ClaimAnalyzer(ClaimTaxPlugin plugin) {
        this.plugin = plugin;
    }

    public static class PlayerClaimInfo {
        public final UUID playerUUID;
        public final String playerName;
        public final int totalBlocks;
        public final int claimCount;
        public final List<ClaimDetail> details;

        public PlayerClaimInfo(UUID uuid, String name, int totalBlocks, int claimCount,
                               List<ClaimDetail> details) {
            this.playerUUID  = uuid;
            this.playerName  = name;
            this.totalBlocks = totalBlocks;
            this.claimCount  = claimCount;
            this.details     = Collections.unmodifiableList(details);
        }
    }

    public static class ClaimDetail {
        public final long claimId;
        public final int area;
        public final int subClaimArea;
        public final String worldName;
        public final int x;
        public final int z;

        public ClaimDetail(long id, int area, int subArea, String world, int x, int z) {
            this.claimId     = id;
            this.area        = area;
            this.subClaimArea = subArea;
            this.worldName   = world;
            this.x           = x;
            this.z           = z;
        }

        public String getLocationString() {
            return worldName + " (" + x + ", " + z + ")";
        }
    }

    public PlayerClaimInfo analyze(UUID uuid) {
        try {
            GriefPrevention gp = GriefPrevention.instance;
            if (gp == null) {
                plugin.getLogger().warning("GriefPrevention chưa khởi động!");
                return null;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String playerName = offlinePlayer.getName() != null
                    ? offlinePlayer.getName()
                    : uuid.toString().substring(0, 8);

            PlayerData playerData = gp.dataStore.getPlayerData(uuid);
            Vector<Claim> claims  = playerData.getClaims();

            int totalBlocks = 0;
            List<ClaimDetail> details = new ArrayList<>();

            for (Claim claim : claims) {
                if (claim.isAdminClaim()) continue;

                if (claim.parent != null) continue;

                int claimArea    = claim.getArea();
                int subClaimArea = 0;

                if (plugin.getTaxConfig().isCountSubClaims() && claim.children != null) {
                    for (Claim sub : claim.children) {
                        subClaimArea += sub.getArea();
                    }
                }

                int effectiveArea = claimArea;

                totalBlocks += effectiveArea;

                if (claim.getLesserBoundaryCorner() != null) {
                    details.add(new ClaimDetail(
                            claim.getID() != null ? claim.getID() : -1L,
                            claimArea,
                            subClaimArea,
                            claim.getLesserBoundaryCorner().getWorld() != null
                                    ? claim.getLesserBoundaryCorner().getWorld().getName()
                                    : "unknown",
                            claim.getLesserBoundaryCorner().getBlockX(),
                            claim.getLesserBoundaryCorner().getBlockZ()
                    ));
                }
            }

            return new PlayerClaimInfo(uuid, playerName, totalBlocks, details.size(), details);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi khi phân tích claim của UUID: " + uuid, e);
            return null;
        }
    }

    public Set<UUID> getAllClaimOwners() {
        Set<UUID> owners = new HashSet<>();
        try {
            GriefPrevention gp = GriefPrevention.instance;
            if (gp == null) return owners;

            Collection<Claim> allClaims = gp.dataStore.getClaims();
            for (Claim claim : allClaims) {
                if (!claim.isAdminClaim() && claim.getOwnerID() != null && claim.parent == null) {
                    owners.add(claim.getOwnerID());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Lỗi khi lấy danh sách claim owners", e);
        }
        return owners;
    }
}
