package dev.blackarchive.flow258.playerSecurity.security.commands;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import dev.blackarchive.flow258.playerSecurity.security.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class SecurityCommand implements CommandExecutor, TabCompleter {

    private final PlayerSecurityPlugin plugin;

    public SecurityCommand(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getSecurityConfig().getPrefix();
        var msg = plugin.getMessagesConfig();

        if (!sender.hasPermission("playersecurity.admin")) {
            sender.sendMessage(MessageUtil.parse(msg.get("MESSAGES.NO_PERMISSION")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── reload ────────────────────────────────────────────────────────
            case "reload" -> {
                plugin.reloadConfig();
                plugin.getSecurityConfig().reload();
                plugin.getMessagesConfig().reload();
                plugin.getRaidProtection().loadConfig();
                plugin.getVpnChecker().clearCache();
                sender.sendMessage(MessageUtil.parse(prefix + msg.get("MESSAGES.RELOAD_SUCCESS")));
            }

            // ── status ────────────────────────────────────────────────────────
            case "status" -> {
                var cfg = plugin.getSecurityConfig();
                String on  = msg.get("MESSAGES.STAFF.STATUS_ENABLED");
                String off = msg.get("MESSAGES.STAFF.STATUS_DISABLED");
                sender.sendMessage(MessageUtil.parse(msg.get("MESSAGES.STAFF.STATUS_HEADER")));
                sendStatus(sender, "VPN Detection",      cfg.isVpnEnabled(), on, off);
                sendStatus(sender, "Join Security",      cfg.isJoinSecurityEnabled(), on, off);
                sendStatus(sender, "Raid Protection",    plugin.getRaidProtection().isLockdownActive()
                        ? false : cfg.isJoinSecurityEnabled(), on, off);
                sendStatus(sender, "Chat Security",      cfg.isChatSecurityEnabled(), on, off);
                sendStatus(sender, "Command Security",   cfg.isCommandSecurityEnabled(), on, off);
                sendStatus(sender, "Movement Security",  cfg.isMovementSecurityEnabled(), on, off);
                sendStatus(sender, "Logging",            cfg.isLoggingEnabled(), on, off);
                sender.sendMessage(MessageUtil.parse(
                    "  <dark_gray>» <white>Lockdown: " +
                    (plugin.getRaidProtection().isLockdownActive() ? "<red>ACTIVE" : "<green>Inactive")));
                sender.sendMessage(MessageUtil.parse(
                    "  <dark_gray>» <white>Join rate (last window): <white>" +
                    plugin.getRaidProtection().getJoinCount() + " <gray>joins"));
            }

            // ── violations ────────────────────────────────────────────────────
            case "violations" -> {
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.parse(prefix + "<red>Usage: /" + label + " violations <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.parse(prefix +
                        msg.get("MESSAGES.PLAYER_NOT_FOUND", Map.of("player", args[1]))));
                    return true;
                }
                int v = plugin.getViolationManager().getViolations(target.getUniqueId());
                sender.sendMessage(MessageUtil.parse(prefix +
                    msg.get("MESSAGES.VIOLATIONS.CHECK",
                        Map.of("player", target.getName(), "violations", String.valueOf(v)))));
            }

            // ── clearviolations ───────────────────────────────────────────────
            case "clearviolations" -> {
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.parse(prefix + "<red>Usage: /" + label + " clearviolations <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.parse(prefix +
                        msg.get("MESSAGES.PLAYER_NOT_FOUND", Map.of("player", args[1]))));
                    return true;
                }
                plugin.getViolationManager().clearViolations(target.getUniqueId());
                sender.sendMessage(MessageUtil.parse(prefix +
                    msg.get("MESSAGES.VIOLATIONS.CLEARED", Map.of("player", target.getName()))));
                target.sendMessage(MessageUtil.parse(prefix +
                    msg.get("MESSAGES.VIOLATIONS.CLEARED_NOTIFY")));
            }

            // ── unban ─────────────────────────────────────────────────────────
            case "unban" -> {
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.parse(prefix + "<red>Usage: /" + label + " unban <player>"));
                    return true;
                }
                String targetName = args[1];
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(targetName);
                sender.sendMessage(MessageUtil.parse(prefix +
                    msg.get("MESSAGES.STAFF.UNBAN_SUCCESS", Map.of("player", targetName))));
                plugin.getLogManager().log("[UNBAN] " + targetName + " pardoned by " + sender.getName());
            }

            // ── lockdown ──────────────────────────────────────────────────────
            case "lockdown" -> {
                if (args.length < 2) {
                    boolean active = plugin.getRaidProtection().isLockdownActive();
                    sender.sendMessage(MessageUtil.parse(prefix + "Lockdown is currently: " +
                        (active ? "<red>ACTIVE" : "<green>Inactive")));
                    sender.sendMessage(MessageUtil.parse(prefix + "Usage: /" + label + " lockdown <on|off>"));
                    return true;
                }
                boolean enable = args[1].equalsIgnoreCase("on");
                plugin.getRaidProtection().setLockdown(enable);
                sender.sendMessage(MessageUtil.parse(prefix +
                    "Lockdown " + (enable ? "<red>activated." : "<green>deactivated.")));
                plugin.getLogManager().log("[LOCKDOWN] " + sender.getName() + " set lockdown to " + enable);
            }

            // ── lookup ────────────────────────────────────────────────────────
            case "lookup" -> {
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.parse(prefix + "<red>Usage: /" + label + " lookup <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.parse(prefix +
                        msg.get("MESSAGES.PLAYER_NOT_FOUND", Map.of("player", args[1]))));
                    return true;
                }
                var ipData = plugin.getIpDataManager();
                var vm     = plugin.getViolationManager();
                UUID uuid  = target.getUniqueId();
                String ip  = plugin.getSecurityManager().getIp(target);

                sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬ <gradient:#FF7272:#FFC976>Security Lookup</gradient> <dark_gray>▬▬▬"));
                sender.sendMessage(MessageUtil.parse("  <white>Player: <gray>" + target.getName()));
                sender.sendMessage(MessageUtil.parse("  <white>UUID: <dark_gray>" + uuid));
                sender.sendMessage(MessageUtil.parse("  <white>Current IP: <gray>" + (ip != null ? ip : "Unknown")));
                sender.sendMessage(MessageUtil.parse("  <white>Known IPs <gray>(" + ipData.getIpsForUuid(uuid).size() + ")<gray>: <white>" +
                    String.join("<gray>, <white>", ipData.getIpsForUuid(uuid))));
                int accounts = ip != null ? ipData.getAccountCount(ip) : 0;
                sender.sendMessage(MessageUtil.parse("  <white>Accounts on current IP: <gray>" + accounts));
                sender.sendMessage(MessageUtil.parse("  <white>Total Violations: " +
                    (vm.getViolations(uuid) > 0 ? "<red>" : "<green>") + vm.getViolations(uuid)));
                sender.sendMessage(MessageUtil.parse("  <white>Online: <green>Yes"));
                sender.sendMessage(MessageUtil.parse("<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            }

            default -> sendHelp(sender, label);
        }
        return true;
    }

    private void sendStatus(CommandSender sender, String module, boolean enabled, String on, String off) {
        sender.sendMessage(MessageUtil.parse(
            plugin.getMessagesConfig().get("MESSAGES.STAFF.STATUS_MODULE",
                Map.of("module", module, "status", enabled ? on : off))));
    }

    private void sendHelp(CommandSender sender, String label) {
        var msg = plugin.getMessagesConfig();
        sender.sendMessage(MessageUtil.parse(msg.get("MESSAGES.STAFF.HELP_HEADER")));
        sender.sendMessage(MessageUtil.parse(msg.get("MESSAGES.STAFF.HELP_TITLE")));
        for (String key : List.of("HELP_RELOAD","HELP_STATUS","HELP_VIOLATIONS",
                                  "HELP_CLEAR","HELP_UNBAN","HELP_LOCKDOWN","HELP_LOOKUP")) {
            sender.sendMessage(MessageUtil.parse(
                msg.get("MESSAGES.STAFF." + key, Map.of("label", label))));
        }
        sender.sendMessage(MessageUtil.parse(msg.get("MESSAGES.STAFF.HELP_FOOTER")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help","reload","status","violations","clearviolations","unban","lockdown","lookup");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "violations", "clearviolations", "unban", "lookup" ->
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                case "lockdown" -> List.of("on", "off");
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }
}
