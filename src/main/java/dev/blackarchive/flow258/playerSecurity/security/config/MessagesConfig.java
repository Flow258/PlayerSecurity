package dev.blackarchive.flow258.playerSecurity.security.config;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessagesConfig {

    private final PlayerSecurityPlugin plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessagesConfig(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        load();
    }

    public void load() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Merge any missing keys from the bundled default
        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8)
            );
            messages.setDefaults(def);
        }
    }

    public void reload() {
        load();
    }

    /**
     * Get a message string from messages.yml.
     * Falls back to the key name if missing.
     */
    public String get(String path) {
        String value = messages.getString(path);
        if (value == null) {
            plugin.getLogger().warning("[Messages] Missing key: " + path);
            return "<red>[Missing message: " + path + "]";
        }
        return value;
    }

    /**
     * Get with inline {key} replacements applied.
     */
    public String get(String path, java.util.Map<String, String> replacements) {
        String msg = get(path);
        for (java.util.Map.Entry<String, String> entry : replacements.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }
}
