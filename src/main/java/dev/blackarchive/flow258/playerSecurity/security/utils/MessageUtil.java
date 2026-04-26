package dev.blackarchive.flow258.playerSecurity.security.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Parse a MiniMessage string into a Component.
     */
    public static Component parse(String message) {
        return MM.deserialize(message);
    }

    /**
     * Parse a MiniMessage string with placeholder replacements.
     * e.g. replacements: Map.of("player", "Steve", "violations", "3")
     */
    public static Component parse(String message, Map<String, String> replacements) {
        TagResolver.Builder resolver = TagResolver.builder();
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            resolver.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
        }
        return MM.deserialize(message, resolver.build());
    }

    /**
     * Send a MiniMessage string to a player.
     */
    public static void send(Player player, String message) {
        player.sendMessage(parse(message));
    }

    /**
     * Send a MiniMessage string with placeholders to a player.
     */
    public static void send(Player player, String message, Map<String, String> replacements) {
        player.sendMessage(parse(message, replacements));
    }

    /**
     * Apply simple {key} style replacements to a raw string before parsing.
     */
    public static String replace(String message, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}
