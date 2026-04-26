package dev.blackarchive.flow258.playerSecurity.security.managers;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Async VPN / proxy detection.
 *
 * Uses proxycheck.io (free, no key required for basic checks).
 * Optional: set your API key in config for higher rate limits.
 *
 * Response format (JSON):
 * {
 *   "status": "ok",
 *   "1.2.3.4": {
 *     "proxy": "yes" | "no",
 *     "type": "VPN" | "TOR" | ...
 *   }
 * }
 */
public class VpnChecker {

    private static final String API_BASE = "https://proxycheck.io/v2/";
    // Cache IPs that have already passed or failed so we don't re-check
    private final Set<String> whitelistedIps = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> flaggedIps      = Collections.synchronizedSet(new HashSet<>());

    private final PlayerSecurityPlugin plugin;

    public VpnChecker(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        // Pre-load config whitelist
        whitelistedIps.addAll(plugin.getSecurityConfig().getIpWhitelist());
    }

    /**
     * Async check. Calls onResult with true if VPN/proxy detected, false if clean.
     */
    public void checkAsync(String ip, UUID uuid, Consumer<Boolean> onResult) {
        // Instant cache hits
        if (whitelistedIps.contains(ip)) {
            onResult.accept(false);
            return;
        }
        if (flaggedIps.contains(ip)) {
            onResult.accept(true);
            return;
        }

        CompletableFuture.supplyAsync(() -> isProxy(ip))
                .thenAccept(isProxy -> {
                    // Cache result
                    if (isProxy) {
                        flaggedIps.add(ip);
                    } else {
                        whitelistedIps.add(ip);
                    }
                    // Run callback back on the main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> onResult.accept(isProxy));
                })
                .exceptionally(ex -> {
                    if (plugin.getSecurityConfig().isDebug()) {
                        plugin.getLogger().warning("[VPN] Check failed for " + ip + ": " + ex.getMessage());
                    }
                    // On error, allow the player through (fail open)
                    plugin.getServer().getScheduler().runTask(plugin, () -> onResult.accept(false));
                    return null;
                });
    }

    private boolean isProxy(String ip) {
        try {
            String apiKey = plugin.getConfig().getString("VPN_DETECTION.API_KEY", "");
            String urlStr = API_BASE + ip + "?vpn=1&short=1";
            if (!apiKey.isBlank()) {
                urlStr += "&key=" + apiKey;
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "PlayerSecurity/1.0 (BlackArchive)");

            int code = conn.getResponseCode();
            if (code != 200) return false;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();

            // Simple JSON parse — avoids needing a JSON library dependency
            // proxycheck.io short response looks like: {"status":"ok","1.2.3.4":{"proxy":"yes"}}
            if (json.contains("\"proxy\":\"yes\"")) {
                return true;
            }

            return false;

        } catch (Exception e) {
            if (plugin.getSecurityConfig().isDebug()) {
                plugin.getLogger().warning("[VPN] API error: " + e.getMessage());
            }
            return false; // fail open
        }
    }

    /**
     * Manually whitelist an IP (e.g. after a staff override).
     */
    public void whitelistIp(String ip) {
        flaggedIps.remove(ip);
        whitelistedIps.add(ip);
    }

    /**
     * Clear all caches (e.g. on reload).
     */
    public void clearCache() {
        whitelistedIps.clear();
        flaggedIps.clear();
        whitelistedIps.addAll(plugin.getSecurityConfig().getIpWhitelist());
    }
}
