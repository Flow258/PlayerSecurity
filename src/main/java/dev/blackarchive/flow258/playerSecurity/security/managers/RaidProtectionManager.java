package dev.blackarchive.flow258.playerSecurity.security.managers;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Detects bot-raid join floods by counting how many players
 * join within a rolling time window. If the rate exceeds the
 * configured threshold, lockdown mode is activated and all new
 * incoming connections are refused until the flood subsides.
 */
public class RaidProtectionManager {

    private final PlayerSecurityPlugin plugin;

    // Sliding window of join timestamps (ms)
    private final Deque<Long> joinTimestamps = new ArrayDeque<>();

    private boolean lockdownActive = false;
    private long lockdownStartTime = 0L;

    // Config keys
    private int joinsPerSecondThreshold;
    private int windowMillis;
    private int lockdownDurationSeconds;
    private boolean enabled;

    public RaidProtectionManager(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        this.enabled                 = plugin.getConfig().getBoolean("RAID_PROTECTION.ENABLED", true);
        this.joinsPerSecondThreshold = plugin.getConfig().getInt("RAID_PROTECTION.JOINS_PER_SECOND_THRESHOLD", 5);
        this.windowMillis            = plugin.getConfig().getInt("RAID_PROTECTION.WINDOW_SECONDS", 3) * 1000;
        this.lockdownDurationSeconds = plugin.getConfig().getInt("RAID_PROTECTION.LOCKDOWN_DURATION_SECONDS", 30);
    }

    /**
     * Call on every PlayerLoginEvent (before allowing in).
     * Returns true if the connection should be blocked (flood detected or lockdown active).
     */
    public boolean checkJoin() {
        if (!enabled) return false;

        long now = System.currentTimeMillis();

        // If lockdown is active, check if it should expire
        if (lockdownActive) {
            if ((now - lockdownStartTime) >= lockdownDurationSeconds * 1000L) {
                endLockdown();
            } else {
                return true; // still in lockdown
            }
        }

        // Slide the window
        joinTimestamps.addLast(now);
        while (!joinTimestamps.isEmpty() && (now - joinTimestamps.peekFirst()) > windowMillis) {
            joinTimestamps.pollFirst();
        }

        int joinCount = joinTimestamps.size();

        if (joinCount >= joinsPerSecondThreshold) {
            activateLockdown(joinCount);
            return true;
        }

        return false;
    }

    private void activateLockdown(int count) {
        if (lockdownActive) return;
        lockdownActive = true;
        lockdownStartTime = System.currentTimeMillis();

        String alert = plugin.getMessagesConfig().get("MESSAGES.RAID.ALERT")
                .replace("{count}", String.valueOf(count));
        String lockdownMsg = plugin.getMessagesConfig().get("MESSAGES.RAID.LOCKDOWN_START");

        plugin.getAlertManager().broadcastAlert(alert);
        plugin.getAlertManager().broadcastAlert(lockdownMsg);
        plugin.getLogManager().log("[RAID] Join flood detected! Count=" + count + ". Lockdown activated.");

        // Schedule auto-lift
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (lockdownActive) endLockdown();
        }, lockdownDurationSeconds * 20L);
    }

    private void endLockdown() {
        lockdownActive = false;
        joinTimestamps.clear();
        String msg = plugin.getMessagesConfig().get("MESSAGES.RAID.LOCKDOWN_END");
        plugin.getAlertManager().broadcastAlert(msg);
        plugin.getLogManager().log("[RAID] Lockdown lifted.");
    }

    /**
     * Staff can manually toggle lockdown via /security lockdown.
     */
    public void setLockdown(boolean active) {
        if (active && !lockdownActive) {
            lockdownActive = true;
            lockdownStartTime = System.currentTimeMillis();
            plugin.getLogManager().log("[RAID] Manual lockdown activated by staff.");
        } else if (!active && lockdownActive) {
            endLockdown();
            plugin.getLogManager().log("[RAID] Manual lockdown deactivated by staff.");
        }
    }

    public boolean isLockdownActive() { return lockdownActive; }

    public int getJoinCount() { return joinTimestamps.size(); }
}
