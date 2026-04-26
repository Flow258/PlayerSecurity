package dev.blackarchive.flow258.playerSecurity.security.managers;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ViolationManager {

    private final PlayerSecurityPlugin plugin;
    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private BukkitTask resetTask;

    private File dataFile;
    private FileConfiguration dataConfig;

    public ViolationManager(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "violations.yml");
        loadViolations();
    }

    // ── Core ─────────────────────────────────────────────────────────────────

    public int getViolations(UUID uuid) {
        return violations.getOrDefault(uuid, 0);
    }

    public int addViolation(UUID uuid) {
        int current = violations.getOrDefault(uuid, 0) + 1;
        violations.put(uuid, current);
        lastActivity.put(uuid, System.currentTimeMillis());

        // Check alert thresholds
        for (int threshold : plugin.getSecurityConfig().getAlertThresholds()) {
            if (current == threshold) {
                Player player = plugin.getServer().getPlayer(uuid);
                String name = player != null ? player.getName() : uuid.toString();
                String msg = plugin.getSecurityConfig().getThresholdAlertMessage()
                        .replace("{player}", name)
                        .replace("{violations}", String.valueOf(current));
                plugin.getAlertManager().broadcastAlert(msg);
            }
        }

        if (plugin.getSecurityConfig().isLogViolations()) {
            Player player = plugin.getServer().getPlayer(uuid);
            String name = player != null ? player.getName() : uuid.toString();
            plugin.getLogManager().log("[VIOLATION] " + name + " now has " + current + " violations.");
        }

        return current;
    }

    public void clearViolations(UUID uuid) {
        violations.remove(uuid);
        lastActivity.remove(uuid);
    }

    public void recordActivity(UUID uuid) {
        lastActivity.put(uuid, System.currentTimeMillis());
    }

    // ── Auto Reset ────────────────────────────────────────────────────────────

    public void startAutoResetTask() {
        int minutes = plugin.getSecurityConfig().getViolationAutoResetMinutes();
        long intervalTicks = 20L * 60 * minutes;

        resetTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long threshold = (long) minutes * 60 * 1000;

            violations.entrySet().removeIf(entry -> {
                Long last = lastActivity.get(entry.getKey());
                return last == null || (now - last) >= threshold;
            });

            if (plugin.getSecurityConfig().isDebug()) {
                plugin.getLogger().info("[Debug] Auto-reset task ran. Active records: " + violations.size());
            }
        }, intervalTicks, intervalTicks);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void loadViolations() {
        if (!plugin.getSecurityConfig().isPersistViolations()) return;
        if (!dataFile.exists()) return;

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.contains("violations")) {
            for (String key : dataConfig.getConfigurationSection("violations").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int count = dataConfig.getInt("violations." + key + ".count", 0);
                    long last  = dataConfig.getLong("violations." + key + ".last", 0);
                    violations.put(uuid, count);
                    lastActivity.put(uuid, last);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        plugin.getLogger().info("Loaded " + violations.size() + " violation records.");
    }

    public void saveViolations() {
        if (!plugin.getSecurityConfig().isPersistViolations()) return;

        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : violations.entrySet()) {
            String path = "violations." + entry.getKey();
            dataConfig.set(path + ".count", entry.getValue());
            dataConfig.set(path + ".last", lastActivity.getOrDefault(entry.getKey(), 0L));
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save violations.yml: " + e.getMessage());
        }
    }
}
