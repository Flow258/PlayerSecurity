package dev.blackarchive.flow258.playerSecurity.security.listeners;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import dev.blackarchive.flow258.playerSecurity.security.config.SecurityConfig;
import dev.blackarchive.flow258.playerSecurity.security.managers.AlertManager;
import dev.blackarchive.flow258.playerSecurity.security.managers.SecurityManager;
import dev.blackarchive.flow258.playerSecurity.security.managers.ViolationManager;
import dev.blackarchive.flow258.playerSecurity.security.utils.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    // Matches IPv4 addresses in chat
    private static final Pattern IP_PATTERN =
            Pattern.compile("\\b((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b");

    // Matches common domain patterns
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("\\b([a-zA-Z0-9-]+\\.)(com|net|org|dev|gg|io|xyz|me|co|tv|live|club)\\b");

    private final PlayerSecurityPlugin plugin;
    private final SecurityConfig cfg;
    private final SecurityManager sm;
    private final AlertManager am;
    private final ViolationManager vm;

    public ChatListener(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getSecurityConfig();
        this.sm     = plugin.getSecurityManager();
        this.am     = plugin.getAlertManager();
        this.vm     = plugin.getViolationManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!cfg.isChatSecurityEnabled()) return;
        if (player.hasPermission("playersecurity.bypass")) return;

        String message = event.getMessage();

        // ── Mute check ────────────────────────────────────────────────────────
        if (sm.isMuted(player)) {
            event.setCancelled(true);
            MessageUtil.send(player, cfg.getChatMutedMessage()
                    .replace("{duration}", "..."));
            return;
        }

        // ── Spam check ────────────────────────────────────────────────────────
        if (cfg.isChatSpamEnabled() && !player.hasPermission("playersecurity.bypass.spam")) {
            if (sm.checkChatSpam(player)) {
                event.setCancelled(true);
                int violations = sm.incrementChatViolation(uuid);
                int remaining  = cfg.getChatMaxViolationsBeforeKick() - violations;
                vm.addViolation(uuid);

                if (violations >= cfg.getChatMaxViolationsBeforeKick()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.kick(MessageUtil.parse(cfg.getChatSpamKickMessage()))
                    );
                    if (cfg.isLogKicks()) plugin.getLogManager().log("[KICK] " + player.getName() + " — chat spam.");
                    return;
                }

                String warn = cfg.getChatSpamWarnMessage().replace("{remaining}", String.valueOf(Math.max(0, remaining)));
                MessageUtil.send(player, warn);

                // Mute player
                int muteDuration = sm.getMuteDuration(uuid);
                sm.mutePlayer(player, muteDuration);
                MessageUtil.send(player, cfg.getChatMutedMessage().replace("{duration}", String.valueOf(muteDuration)));

                String alert = cfg.getChatSpamAlertMessage()
                        .replace("{player}", player.getName())
                        .replace("{violations}", String.valueOf(violations));
                am.broadcastAlert(alert);

                if (cfg.isLogChatFlags()) plugin.getLogManager().log("[CHAT-SPAM] " + player.getName() + " — violations: " + violations);
                return;
            }
        }

        // ── Advertising check ─────────────────────────────────────────────────
        if (cfg.isAdvertisingBlockEnabled() && !player.hasPermission("playersecurity.bypass.spam")) {
            boolean blocked = false;

            if (cfg.isBlockIps() && IP_PATTERN.matcher(message).find()) {
                blocked = true;
            }

            if (!blocked && cfg.isBlockDomains()) {
                if (DOMAIN_PATTERN.matcher(message).find()) {
                    // Check against whitelist
                    boolean whitelisted = cfg.getAllowedDomains().stream()
                            .anyMatch(d -> message.toLowerCase().contains(d.toLowerCase()));
                    if (!whitelisted) blocked = true;
                }
            }

            if (blocked) {
                event.setCancelled(true);
                MessageUtil.send(player, cfg.getAdvertisingBlockedMessage());
                vm.addViolation(uuid);

                String alert = cfg.getAdvertisingAlertMessage()
                        .replace("{player}", player.getName())
                        .replace("{message}", message);
                am.broadcastAlert(alert);

                if (cfg.isLogChatFlags()) plugin.getLogManager().log("[CHAT-ADVERT] " + player.getName() + ": " + message);
                return;
            }
        }

        // ── Caps filter ───────────────────────────────────────────────────────
        if (cfg.isCapsFilterEnabled() && message.length() >= cfg.getCapsMinLength()) {
            long capsCount = message.chars().filter(Character::isUpperCase).count();
            long letterCount = message.chars().filter(Character::isLetter).count();

            if (letterCount > 0) {
                int capsPercent = (int) ((capsCount * 100) / letterCount);
                if (capsPercent > cfg.getMaxCapsPercent()) {
                    String action = cfg.getCapsAction();
                    if (action.equals("CONVERT")) {
                        event.setMessage(message.toLowerCase());
                        MessageUtil.send(player, cfg.getCapsWarnMessage());
                    } else if (action.equals("BLOCK")) {
                        event.setCancelled(true);
                        MessageUtil.send(player, cfg.getCapsWarnMessage());
                    }
                }
            }
        }
    }
}
