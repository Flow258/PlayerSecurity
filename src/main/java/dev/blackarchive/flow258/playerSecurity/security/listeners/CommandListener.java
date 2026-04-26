package dev.blackarchive.flow258.playerSecurity.security.listeners;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import dev.blackarchive.flow258.playerSecurity.security.config.SecurityConfig;
import dev.blackarchive.flow258.playerSecurity.security.managers.AlertManager;
import dev.blackarchive.flow258.playerSecurity.security.managers.SecurityManager;
import dev.blackarchive.flow258.playerSecurity.security.managers.ViolationManager;
import dev.blackarchive.flow258.playerSecurity.security.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class CommandListener implements Listener {

    private final PlayerSecurityPlugin plugin;
    private final SecurityConfig cfg;
    private final SecurityManager sm;
    private final AlertManager am;
    private final ViolationManager vm;

    public CommandListener(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getSecurityConfig();
        this.sm     = plugin.getSecurityManager();
        this.am     = plugin.getAlertManager();
        this.vm     = plugin.getViolationManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!cfg.isCommandSecurityEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("playersecurity.bypass")) return;

        String fullCommand = event.getMessage(); // includes the leading /
        String command = fullCommand.substring(1).split(" ")[0].toLowerCase();

        // ── Blocked commands ──────────────────────────────────────────────────
        if (cfg.isBlockedCommandsEnabled() && !player.isOp()) {
            if (cfg.getBlockedCommands().stream().anyMatch(c -> c.equalsIgnoreCase(command))) {
                event.setCancelled(true);
                MessageUtil.send(player, cfg.getBlockedCommandMessage());
                vm.addViolation(uuid);

                String alert = cfg.getBlockedCommandAlertMessage()
                        .replace("{player}", player.getName())
                        .replace("{command}", command);
                am.broadcastAlert(alert);

                if (cfg.isLogCommandFlags())
                    plugin.getLogManager().log("[CMD-BLOCKED] " + player.getName() + " tried: /" + command);
                return;
            }
        }

        // ── Command spam ──────────────────────────────────────────────────────
        if (cfg.isCommandSpamEnabled() && !player.hasPermission("playersecurity.bypass.spam")) {
            if (sm.checkCommandSpam(player)) {
                event.setCancelled(true);
                int violations = sm.incrementCommandViolation(uuid);
                vm.addViolation(uuid);

                if (violations >= cfg.getCommandMaxViolationsBeforeKick()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.kick(MessageUtil.parse(cfg.getCommandSpamKickMessage()))
                    );
                    if (cfg.isLogKicks()) plugin.getLogManager().log("[KICK] " + player.getName() + " — command spam.");
                    return;
                }

                MessageUtil.send(player, cfg.getCommandSpamWarnMessage());

                String alert = cfg.getCommandSpamAlertMessage()
                        .replace("{player}", player.getName());
                am.broadcastAlert(alert);

                if (cfg.isLogCommandFlags())
                    plugin.getLogManager().log("[CMD-SPAM] " + player.getName() + " — violations: " + violations);
            }
        }
    }
}
