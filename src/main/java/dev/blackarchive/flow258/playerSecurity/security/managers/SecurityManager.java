package dev.blackarchive.flow258.playerSecurity.security.managers;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {

    private final PlayerSecurityPlugin plugin;

    // IP -> set of UUIDs (accounts seen from this IP this session)
    private final Map<String, Set<UUID>> ipAccountMap = new ConcurrentHashMap<>();

    // UUID -> list of message timestamps (ms) for spam detection
    private final Map<UUID, List<Long>> chatTimestamps = new ConcurrentHashMap<>();

    // UUID -> list of command timestamps (ms)
    private final Map<UUID, List<Long>> commandTimestamps = new ConcurrentHashMap<>();

    // UUID -> mute expiry timestamp (ms)
    private final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();

    // UUID -> air ticks (for fly detection)
    private final Map<UUID, Integer> airTicks = new ConcurrentHashMap<>();

    // UUID -> chat violation count
    private final Map<UUID, Integer> chatViolations = new ConcurrentHashMap<>();

    // UUID -> command violation count
    private final Map<UUID, Integer> commandViolations = new ConcurrentHashMap<>();

    // UUID -> movement violation count
    private final Map<UUID, Integer> movementViolations = new ConcurrentHashMap<>();

    public SecurityManager(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Session ───────────────────────────────────────────────────────────────

    public void registerJoin(Player player) {
        String ip = getIp(player);
        if (ip == null) return;

        ipAccountMap.computeIfAbsent(ip, k -> new HashSet<>()).add(player.getUniqueId());
        airTicks.put(player.getUniqueId(), 0);
    }

    public void registerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        airTicks.remove(uuid);
        chatTimestamps.remove(uuid);
        commandTimestamps.remove(uuid);
        mutedPlayers.remove(uuid);
        chatViolations.remove(uuid);
        commandViolations.remove(uuid);
        movementViolations.remove(uuid);
    }

    // ── IP Tracking ───────────────────────────────────────────────────────────

    public int getAccountCountForIp(String ip) {
        return ipAccountMap.getOrDefault(ip, Collections.emptySet()).size();
    }

    public String getIp(Player player) {
        InetAddress address = player.getAddress() != null ? player.getAddress().getAddress() : null;
        return address != null ? address.getHostAddress() : null;
    }

    // ── Chat Spam ─────────────────────────────────────────────────────────────

    /**
     * Returns true if the player is spamming chat.
     */
    public boolean checkChatSpam(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int limit = plugin.getSecurityConfig().getChatMessagesPerSecond();

        List<Long> times = chatTimestamps.computeIfAbsent(uuid, k -> new ArrayList<>());
        times.removeIf(t -> now - t > 1000); // keep only last 1 second
        times.add(now);

        return times.size() > limit;
    }

    public boolean isMuted(Player player) {
        Long expiry = mutedPlayers.get(player.getUniqueId());
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            mutedPlayers.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void mutePlayer(Player player, long durationSeconds) {
        mutedPlayers.put(player.getUniqueId(), System.currentTimeMillis() + durationSeconds * 1000);
    }

    public int incrementChatViolation(UUID uuid) {
        int v = chatViolations.getOrDefault(uuid, 0) + 1;
        chatViolations.put(uuid, v);
        return v;
    }

    public int getChatViolations(UUID uuid) {
        return chatViolations.getOrDefault(uuid, 0);
    }

    public int getMuteDuration(UUID uuid) {
        int base = plugin.getSecurityConfig().getChatMuteDuration();
        int violations = getChatViolations(uuid);
        double multiplier = plugin.getSecurityConfig().getMuteEscalationMultiplier();
        return (int) (base * Math.pow(multiplier, Math.max(0, violations - 1)));
    }

    // ── Command Spam ──────────────────────────────────────────────────────────

    public boolean checkCommandSpam(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int limit = plugin.getSecurityConfig().getCommandsPerSecond();

        List<Long> times = commandTimestamps.computeIfAbsent(uuid, k -> new ArrayList<>());
        times.removeIf(t -> now - t > 1000);
        times.add(now);

        return times.size() > limit;
    }

    public int incrementCommandViolation(UUID uuid) {
        int v = commandViolations.getOrDefault(uuid, 0) + 1;
        commandViolations.put(uuid, v);
        return v;
    }

    public int getCommandViolations(UUID uuid) {
        return commandViolations.getOrDefault(uuid, 0);
    }

    // ── Movement ──────────────────────────────────────────────────────────────

    public int incrementAirTicks(UUID uuid) {
        int t = airTicks.getOrDefault(uuid, 0) + 1;
        airTicks.put(uuid, t);
        return t;
    }

    public void resetAirTicks(UUID uuid) {
        airTicks.put(uuid, 0);
    }

    public int getAirTicks(UUID uuid) {
        return airTicks.getOrDefault(uuid, 0);
    }

    public int incrementMovementViolation(UUID uuid) {
        int v = movementViolations.getOrDefault(uuid, 0) + 1;
        movementViolations.put(uuid, v);
        return v;
    }

    public int getMovementViolations(UUID uuid) {
        return movementViolations.getOrDefault(uuid, 0);
    }
}
