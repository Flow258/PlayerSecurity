package dev.blackarchive.flow258.playerSecurity.security.managers;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import dev.blackarchive.flow258.playerSecurity.security.utils.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class AlertManager {

    private final PlayerSecurityPlugin plugin;

    public AlertManager(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Broadcast a security alert to all players with playersecurity.alerts permission.
     */
    public void broadcastAlert(String miniMessage) {
        if (!plugin.getSecurityConfig().isAlertsEnabled()) return;

        Component component = MessageUtil.parse(
                plugin.getSecurityConfig().getPrefix() + miniMessage
        );

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("playersecurity.alerts")) {
                player.sendMessage(component);
                playAlertSound(player);
            }
        }

        if (plugin.getSecurityConfig().isLogToConsole()) {
            plugin.getLogger().warning("[ALERT] " + net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component));
        }

        if (plugin.getSecurityConfig().isAlertBossbarEnabled()) {
            showAlertBossbar(miniMessage);
        }
    }

    private void playAlertSound(Player player) {
        if (!plugin.getSecurityConfig().isAlertSoundEnabled()) return;
        try {
            Sound sound = Sound.valueOf(plugin.getSecurityConfig().getAlertSoundValue());
            player.playSound(
                    player.getLocation(),
                    sound,
                    plugin.getSecurityConfig().getAlertSoundVolume(),
                    plugin.getSecurityConfig().getAlertSoundPitch()
            );
        } catch (IllegalArgumentException e) {
            if (plugin.getSecurityConfig().isDebug()) {
                plugin.getLogger().warning("[Debug] Invalid alert sound: " + plugin.getSecurityConfig().getAlertSoundValue());
            }
        }
    }

    private void showAlertBossbar(String miniMessage) {
        int duration = plugin.getSecurityConfig().getAlertBossbarDuration();
        Component title = MessageUtil.parse(miniMessage);

        BossBar bossBar = BossBar.bossBar(
                title,
                1.0f,
                BossBar.Color.RED,
                BossBar.Overlay.PROGRESS
        );

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("playersecurity.alerts")) {
                player.showBossBar(bossBar);
            }
        }

        // Remove bossbar after duration ticks
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission("playersecurity.alerts")) {
                    player.hideBossBar(bossBar);
                }
            }
        }, duration);
    }
}
