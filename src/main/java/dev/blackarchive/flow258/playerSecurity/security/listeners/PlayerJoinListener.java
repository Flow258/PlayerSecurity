package dev.blackarchive.flow258.playerSecurity.security.listeners;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import dev.blackarchive.flow258.playerSecurity.security.config.MessagesConfig;
import dev.blackarchive.flow258.playerSecurity.security.config.SecurityConfig;
import dev.blackarchive.flow258.playerSecurity.security.managers.*;
import dev.blackarchive.flow258.playerSecurity.security.managers.SecurityManager;
import dev.blackarchive.flow258.playerSecurity.security.utils.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.regex.Pattern;

public class PlayerJoinListener implements Listener {

    private final PlayerSecurityPlugin plugin;
    private final SecurityConfig       cfg;
    private final MessagesConfig       msg;
    private final SecurityManager sm;
    private final AlertManager         am;
    private final IPDataManager        ipData;
    private final VpnChecker           vpnChecker;
    private final RaidProtectionManager raidProtection;

    public PlayerJoinListener(PlayerSecurityPlugin plugin) {
        this.plugin         = plugin;
        this.cfg            = plugin.getSecurityConfig();
        this.msg            = plugin.getMessagesConfig();
        this.sm             = plugin.getSecurityManager();
        this.am             = plugin.getAlertManager();
        this.ipData         = plugin.getIpDataManager();
        this.vpnChecker     = plugin.getVpnChecker();
        this.raidProtection = plugin.getRaidProtection();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        String ip   = event.getAddress().getHostAddress();
        String name = event.getPlayer().getName();
        java.util.UUID uuid = event.getPlayer().getUniqueId();

        if (event.getPlayer().hasPermission("playersecurity.bypass")) {
            ipData.record(uuid, name, ip);
            return;
        }

        // ── 1. Raid / flood protection ────────────────────────────────────────
        if (raidProtection.checkJoin()) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                MessageUtil.parse(msg.get("MESSAGES.RAID.KICK"))
            );
            plugin.getLogManager().log("[RAID-BLOCK] " + name + " (" + ip + ") blocked — lockdown active.");
            return;
        }

        // ── 2. Username length ────────────────────────────────────────────────
        if (cfg.isJoinSecurityEnabled() && name.length() < cfg.getMinUsernameLength()) {
            event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                MessageUtil.parse(msg.get("MESSAGES.JOIN.USERNAME_TOO_SHORT"))
            );
            am.broadcastAlert(msg.get("MESSAGES.JOIN.ALERT_BOT",
                Map.of("player", name, "ip", ip)));
            plugin.getLogManager().log("[JOIN-BLOCK] " + name + " (" + ip + ") — username too short.");
            return;
        }

        // ── 3. Bot name filter ────────────────────────────────────────────────
        if (cfg.isJoinSecurityEnabled() && cfg.isBotFilterEnabled()) {
            for (String pattern : cfg.getBotNamePatterns()) {
                if (Pattern.matches(pattern, name)) {
                    event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        MessageUtil.parse(msg.get("MESSAGES.JOIN.BOT_KICK"))
                    );
                    am.broadcastAlert(msg.get("MESSAGES.JOIN.ALERT_BOT",
                        Map.of("player", name, "ip", ip)));
                    plugin.getLogManager().log("[JOIN-BLOCK] " + name + " (" + ip + ") — bot pattern: " + pattern);
                    return;
                }
            }
        }

        // ── 4. IP account limit (uses persisted ipdata) ───────────────────────
        if (cfg.isJoinSecurityEnabled()) {
            // Record this join so the count is current
            ipData.record(uuid, name, ip);
            int count = ipData.getAccountCount(ip);
            int max   = cfg.getMaxAccountsPerIp();

            if (count > max) {
                String action = cfg.getIpLimitAction();
                if (action.equals("BAN")) {
                    event.getPlayer().banPlayer("Too many accounts from your IP.");
                    plugin.getLogManager().log("[BAN] " + name + " (" + ip + ") — IP limit.");
                }
                event.disallow(
                    PlayerLoginEvent.Result.KICK_OTHER,
                    MessageUtil.parse(msg.get("MESSAGES.JOIN.IP_LIMIT_KICK",
                        Map.of("max", String.valueOf(max))))
                );
                am.broadcastAlert(msg.get("MESSAGES.JOIN.ALERT_IP_LIMIT",
                    Map.of("player", name, "ip", ip,
                           "count", String.valueOf(count),
                           "max",   String.valueOf(max))));
                plugin.getLogManager().log("[JOIN-BLOCK] " + name + " (" + ip + ") — IP limit (" + count + "/" + max + ").");
                return;
            }
        } else {
            ipData.record(uuid, name, ip);
        }

        // ── 5. VPN / Proxy detection (async — player joins, then gets kicked if VPN) ──
        if (cfg.isVpnEnabled() && !event.getPlayer().hasPermission("playersecurity.bypass.vpn")) {
            // We allow them in first, then kick async to avoid blocking the login thread
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!event.getPlayer().isOnline()) return;
                vpnChecker.checkAsync(ip, uuid, isVpn -> {
                    if (isVpn) {
                        String action = cfg.getVpnAction();
                        if (action.equals("BAN")) {
                            event.getPlayer().banPlayer("VPN/Proxy connection detected.");
                            plugin.getLogManager().log("[BAN] " + name + " (" + ip + ") — VPN detected.");
                        }
                        event.getPlayer().kick(MessageUtil.parse(msg.get("MESSAGES.VPN.KICK")));
                        am.broadcastAlert(msg.get("MESSAGES.VPN.ALERT",
                            Map.of("player", name, "ip", ip)));
                        plugin.getLogManager().log("[VPN-BLOCK] " + name + " (" + ip + ") — VPN/Proxy detected.");
                    }
                });
            }, 10L); // 0.5s delay so player is fully online
        }

        if (cfg.isLoggingEnabled() && cfg.isLogJoins()) {
            plugin.getLogManager().log("[JOIN] " + name + " (" + ip + ")");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        sm.registerJoin(event.getPlayer());
    }
}
