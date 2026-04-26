package dev.blackarchive.flow258.playerSecurity.security.listeners;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final PlayerSecurityPlugin plugin;

    public PlayerQuitListener(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getSecurityManager().registerQuit(event.getPlayer());
        plugin.getViolationManager().recordActivity(event.getPlayer().getUniqueId());
    }
}
