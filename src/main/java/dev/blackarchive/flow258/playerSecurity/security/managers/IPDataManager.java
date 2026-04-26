package dev.blackarchive.flow258.playerSecurity.security.managers;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Tracks which IPs each UUID has connected from, and
 * how many unique UUIDs have been seen from each IP.
 * Data is persisted to ipdata.yml across restarts.
 */
public class IPDataManager {

    private final PlayerSecurityPlugin plugin;
    private File dataFile;
    private FileConfiguration data;

    // uuid -> list of IPs seen
    private final Map<UUID, Set<String>> uuidToIps = new HashMap<>();
    // ip -> set of UUIDs seen
    private final Map<String, Set<UUID>> ipToUuids = new HashMap<>();
    // uuid -> last known name
    private final Map<UUID, String> uuidToName = new HashMap<>();
    // uuid -> last seen timestamp
    private final Map<UUID, Long> lastSeen = new HashMap<>();

    public IPDataManager(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "ipdata.yml");
        load();
    }

    // ── Core ─────────────────────────────────────────────────────────────────

    /**
     * Record a player join. Call this on every login.
     */
    public void record(UUID uuid, String playerName, String ip) {
        uuidToIps.computeIfAbsent(uuid, k -> new LinkedHashSet<>()).add(ip);
        ipToUuids.computeIfAbsent(ip, k -> new HashSet<>()).add(uuid);
        uuidToName.put(uuid, playerName);
        lastSeen.put(uuid, Instant.now().getEpochSecond());
    }

    /**
     * Returns all UUIDs ever seen from a given IP.
     */
    public Set<UUID> getUuidsForIp(String ip) {
        return ipToUuids.getOrDefault(ip, Collections.emptySet());
    }

    /**
     * Returns the number of distinct accounts from an IP.
     */
    public int getAccountCount(String ip) {
        return getUuidsForIp(ip).size();
    }

    /**
     * Returns all IPs a UUID has connected from.
     */
    public Set<String> getIpsForUuid(UUID uuid) {
        return uuidToIps.getOrDefault(uuid, Collections.emptySet());
    }

    public String getLastName(UUID uuid) {
        return uuidToName.getOrDefault(uuid, uuid.toString());
    }

    public long getLastSeen(UUID uuid) {
        return lastSeen.getOrDefault(uuid, 0L);
    }

    /**
     * Build a lookup summary for the /security lookup command.
     */
    public String buildLookupSummary(UUID uuid) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getLastName(uuid)).append("\n");
        sb.append("UUID: ").append(uuid).append("\n");
        sb.append("Known IPs (").append(getIpsForUuid(uuid).size()).append("): ")
          .append(String.join(", ", getIpsForUuid(uuid))).append("\n");
        sb.append("Last seen: ").append(Instant.ofEpochSecond(getLastSeen(uuid)).toString());
        return sb.toString();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void load() {
        if (!dataFile.exists()) return;
        data = YamlConfiguration.loadConfiguration(dataFile);

        if (data.contains("players")) {
            for (String uuidStr : data.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String path = "players." + uuidStr;

                    String name = data.getString(path + ".name", uuidStr);
                    long ts = data.getLong(path + ".lastSeen", 0L);
                    List<String> ips = data.getStringList(path + ".ips");

                    uuidToName.put(uuid, name);
                    lastSeen.put(uuid, ts);
                    Set<String> ipSet = new LinkedHashSet<>(ips);
                    uuidToIps.put(uuid, ipSet);

                    for (String ip : ipSet) {
                        ipToUuids.computeIfAbsent(ip, k -> new HashSet<>()).add(uuid);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("Loaded IP data for " + uuidToIps.size() + " players.");
    }

    public void save() {
        data = new YamlConfiguration();
        for (Map.Entry<UUID, Set<String>> entry : uuidToIps.entrySet()) {
            String path = "players." + entry.getKey();
            data.set(path + ".name", uuidToName.getOrDefault(entry.getKey(), "unknown"));
            data.set(path + ".lastSeen", lastSeen.getOrDefault(entry.getKey(), 0L));
            data.set(path + ".ips", new ArrayList<>(entry.getValue()));
        }
        try {
            plugin.getDataFolder().mkdirs();
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save ipdata.yml: " + e.getMessage());
        }
    }
}
