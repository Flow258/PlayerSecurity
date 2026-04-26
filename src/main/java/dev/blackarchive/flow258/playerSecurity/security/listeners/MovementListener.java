package dev.blackarchive.flow258.playerSecurity.security.listeners;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import dev.blackarchive.flow258.playerSecurity.security.config.SecurityConfig;
import dev.blackarchive.flow258.playerSecurity.security.managers.AlertManager;
import dev.blackarchive.flow258.playerSecurity.security.managers.SecurityManager;
import dev.blackarchive.flow258.playerSecurity.security.managers.ViolationManager;
import dev.blackarchive.flow258.playerSecurity.security.utils.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class MovementListener implements Listener {

    private final PlayerSecurityPlugin plugin;
    private final SecurityConfig cfg;
    private final SecurityManager sm;
    private final AlertManager am;
    private final ViolationManager vm;

    public MovementListener(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getSecurityConfig();
        this.sm     = plugin.getSecurityManager();
        this.am     = plugin.getAlertManager();
        this.vm     = plugin.getViolationManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!cfg.isMovementSecurityEnabled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Skip creative/spectator/flying (allowed) players and permission bypass
        if (player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.isFlying()
                || player.getAllowFlight()
                || player.hasPermission("playersecurity.bypass")
                || player.hasPermission("playersecurity.bypass.movement")) {
            sm.resetAirTicks(uuid);
            return;
        }

        // Only check if the player actually moved (not just looked around)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // ── Speed detection ───────────────────────────────────────────────────
        if (cfg.isSpeedDetectionEnabled()) {
            double dx = event.getTo().getX() - event.getFrom().getX();
            double dz = event.getTo().getZ() - event.getFrom().getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);

            if (speed > cfg.getMaxSpeed()) {
                int violations = sm.incrementMovementViolation(uuid);
                vm.addViolation(uuid);

                String speedStr = String.format("%.3f", speed);
                String alert = cfg.getSpeedAlertMessage()
                        .replace("{player}", player.getName())
                        .replace("{speed}", speedStr);
                am.broadcastAlert(alert);

                if (violations >= cfg.getSpeedMaxViolations()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.kick(MessageUtil.parse(cfg.getSpeedKickMessage()))
                    );
                    plugin.getLogManager().log("[KICK] " + player.getName() + " — speed hack (speed=" + speedStr + ").");
                    return;
                }

                MessageUtil.send(player, cfg.getSpeedWarnMessage());
                plugin.getLogManager().log("[SPEED] " + player.getName() + " — speed=" + speedStr + " violations=" + violations);
            }
        }

        // ── Fly detection ─────────────────────────────────────────────────────
        if (cfg.isFlyDetectionEnabled()) {
            boolean isOnGround = player.isOnGround();

            if (!isOnGround) {
                int ticks = sm.incrementAirTicks(uuid);

                if (ticks > cfg.getMaxAirTicks()) {
                    int violations = sm.incrementMovementViolation(uuid);
                    vm.addViolation(uuid);

                    String alert = cfg.getFlyAlertMessage()
                            .replace("{player}", player.getName())
                            .replace("{ticks}", String.valueOf(ticks));
                    am.broadcastAlert(alert);

                    // Reset ticks to avoid spam-alerting every tick
                    sm.resetAirTicks(uuid);

                    String action = cfg.getFlyAction();

                    if (action.equals("KICK") || violations >= cfg.getFlyMaxViolations()) {
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                player.kick(MessageUtil.parse(cfg.getFlyKickMessage()))
                        );
                        plugin.getLogManager().log("[KICK] " + player.getName() + " — fly hack (ticks=" + ticks + ").");
                        return;
                    }

                    if (action.equals("WARN")) {
                        MessageUtil.send(player, cfg.getFlyWarnMessage());
                    }

                    plugin.getLogManager().log("[FLY] " + player.getName() + " — air ticks=" + ticks + " violations=" + violations);
                }
            } else {
                sm.resetAirTicks(uuid);
            }
        }
    }
}
