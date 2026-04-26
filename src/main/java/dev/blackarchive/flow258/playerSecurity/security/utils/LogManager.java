package dev.blackarchive.flow258.playerSecurity.security.utils;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private final PlayerSecurityPlugin plugin;
    private BufferedWriter writer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LogManager(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        if (!plugin.getSecurityConfig().isLoggingEnabled()) return;

        File logFile = new File(plugin.getDataFolder(), plugin.getSecurityConfig().getLogFile());
        try {
            if (!logFile.exists()) {
                plugin.getDataFolder().mkdirs();
                logFile.createNewFile();
            }
            writer = new BufferedWriter(new FileWriter(logFile, true));
            log("=== PlayerSecurity started ===");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not open log file: " + e.getMessage());
        }
    }

    public void log(String message) {
        if (writer == null) return;
        try {
            writer.write("[" + dateFormat.format(new Date()) + "] " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to log: " + e.getMessage());
        }
    }

    public void close() {
        if (writer == null) return;
        try {
            log("=== PlayerSecurity stopped ===");
            writer.close();
        } catch (IOException ignored) {}
    }
}
