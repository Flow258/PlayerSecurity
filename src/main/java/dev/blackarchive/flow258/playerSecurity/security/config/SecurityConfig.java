package dev.blackarchive.flow258.playerSecurity.security.config;

import dev.blackarchive.flow258.playerSecurity.PlayerSecurityPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class SecurityConfig {

    private final PlayerSecurityPlugin plugin;
    private FileConfiguration cfg;

    public SecurityConfig(PlayerSecurityPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    // ── General ──────────────────────────────────────────────────────────────

    public String getPrefix() {
        return color(cfg.getString("SETTINGS.PREFIX", "<dark_gray>[<gradient:#FF7272:#FFC976>BlackArchive</gradient><dark_gray>] <gray>"));
    }

    public boolean isDebug() {
        return cfg.getBoolean("SETTINGS.DEBUG", false);
    }

    // ── VPN Detection ────────────────────────────────────────────────────────

    public boolean isVpnEnabled() {
        return cfg.getBoolean("VPN_DETECTION.ENABLED", true);
    }

    public String getVpnAction() {
        return cfg.getString("VPN_DETECTION.ACTION", "KICK").toUpperCase();
    }

    public int getVpnBanDuration() {
        return cfg.getInt("VPN_DETECTION.BAN_DURATION", 60);
    }

    public List<String> getIpWhitelist() {
        return cfg.getStringList("VPN_DETECTION.IP_WHITELIST");
    }

    public String getVpnKickMessage() {
        return color(cfg.getString("VPN_DETECTION.MESSAGES.KICK", ""));
    }

    public String getVpnAlertMessage() {
        return color(cfg.getString("VPN_DETECTION.MESSAGES.ALERT", ""));
    }

    // ── Join Security ────────────────────────────────────────────────────────

    public boolean isJoinSecurityEnabled() {
        return cfg.getBoolean("JOIN_SECURITY.ENABLED", true);
    }

    public int getMaxAccountsPerIp() {
        return cfg.getInt("JOIN_SECURITY.MAX_ACCOUNTS_PER_IP", 3);
    }

    public String getIpLimitAction() {
        return cfg.getString("JOIN_SECURITY.IP_LIMIT_ACTION", "KICK").toUpperCase();
    }

    public boolean isBotFilterEnabled() {
        return cfg.getBoolean("JOIN_SECURITY.BOT_NAME_FILTER.ENABLED", true);
    }

    public List<String> getBotNamePatterns() {
        return cfg.getStringList("JOIN_SECURITY.BOT_NAME_FILTER.PATTERNS");
    }

    public int getMinUsernameLength() {
        return cfg.getInt("JOIN_SECURITY.MIN_USERNAME_LENGTH", 3);
    }

    public String getIpLimitKickMessage() {
        return color(cfg.getString("JOIN_SECURITY.MESSAGES.IP_LIMIT_KICK", ""));
    }

    public String getBotKickMessage() {
        return color(cfg.getString("JOIN_SECURITY.MESSAGES.BOT_KICK", ""));
    }

    public String getIpLimitAlertMessage() {
        return color(cfg.getString("JOIN_SECURITY.MESSAGES.ALERT_IP_LIMIT", ""));
    }

    public String getBotAlertMessage() {
        return color(cfg.getString("JOIN_SECURITY.MESSAGES.ALERT_BOT", ""));
    }

    // ── Chat Security ─────────────────────────────────────────────────────────

    public boolean isChatSecurityEnabled() {
        return cfg.getBoolean("CHAT_SECURITY.ENABLED", true);
    }

    public boolean isChatSpamEnabled() {
        return cfg.getBoolean("CHAT_SECURITY.SPAM.ENABLED", true);
    }

    public int getChatMessagesPerSecond() {
        return cfg.getInt("CHAT_SECURITY.SPAM.MESSAGES_PER_SECOND", 3);
    }

    public int getChatMuteDuration() {
        return cfg.getInt("CHAT_SECURITY.SPAM.MUTE_DURATION", 30);
    }

    public double getMuteEscalationMultiplier() {
        return cfg.getDouble("CHAT_SECURITY.SPAM.MUTE_ESCALATION_MULTIPLIER", 2.0);
    }

    public int getChatMaxViolationsBeforeKick() {
        return cfg.getInt("CHAT_SECURITY.SPAM.MAX_VIOLATIONS_BEFORE_KICK", 5);
    }

    public String getChatSpamWarnMessage() {
        return color(cfg.getString("CHAT_SECURITY.SPAM.MESSAGES.WARN", ""));
    }

    public String getChatMutedMessage() {
        return color(cfg.getString("CHAT_SECURITY.SPAM.MESSAGES.MUTED", ""));
    }

    public String getChatSpamKickMessage() {
        return color(cfg.getString("CHAT_SECURITY.SPAM.MESSAGES.KICK", ""));
    }

    public String getChatSpamAlertMessage() {
        return color(cfg.getString("CHAT_SECURITY.SPAM.MESSAGES.ALERT", ""));
    }

    public boolean isAdvertisingBlockEnabled() {
        return cfg.getBoolean("CHAT_SECURITY.ADVERTISING.ENABLED", true);
    }

    public boolean isBlockIps() {
        return cfg.getBoolean("CHAT_SECURITY.ADVERTISING.BLOCK_IPS", true);
    }

    public boolean isBlockDomains() {
        return cfg.getBoolean("CHAT_SECURITY.ADVERTISING.BLOCK_DOMAINS", true);
    }

    public List<String> getAllowedDomains() {
        return cfg.getStringList("CHAT_SECURITY.ADVERTISING.ALLOWED_DOMAINS");
    }

    public String getAdvertisingBlockedMessage() {
        return color(cfg.getString("CHAT_SECURITY.ADVERTISING.MESSAGES.BLOCKED", ""));
    }

    public String getAdvertisingAlertMessage() {
        return color(cfg.getString("CHAT_SECURITY.ADVERTISING.MESSAGES.ALERT", ""));
    }

    public boolean isCapsFilterEnabled() {
        return cfg.getBoolean("CHAT_SECURITY.CAPS_FILTER.ENABLED", true);
    }

    public int getMaxCapsPercent() {
        return cfg.getInt("CHAT_SECURITY.CAPS_FILTER.MAX_CAPS_PERCENT", 70);
    }

    public int getCapsMinLength() {
        return cfg.getInt("CHAT_SECURITY.CAPS_FILTER.MIN_LENGTH", 6);
    }

    public String getCapsAction() {
        return cfg.getString("CHAT_SECURITY.CAPS_FILTER.ACTION", "CONVERT").toUpperCase();
    }

    public String getCapsWarnMessage() {
        return color(cfg.getString("CHAT_SECURITY.CAPS_FILTER.MESSAGES.WARN", ""));
    }

    // ── Command Security ──────────────────────────────────────────────────────

    public boolean isCommandSecurityEnabled() {
        return cfg.getBoolean("COMMAND_SECURITY.ENABLED", true);
    }

    public boolean isCommandSpamEnabled() {
        return cfg.getBoolean("COMMAND_SECURITY.SPAM.ENABLED", true);
    }

    public int getCommandsPerSecond() {
        return cfg.getInt("COMMAND_SECURITY.SPAM.COMMANDS_PER_SECOND", 5);
    }

    public int getCommandMaxViolationsBeforeKick() {
        return cfg.getInt("COMMAND_SECURITY.SPAM.MAX_VIOLATIONS_BEFORE_KICK", 3);
    }

    public String getCommandSpamWarnMessage() {
        return color(cfg.getString("COMMAND_SECURITY.SPAM.MESSAGES.WARN", ""));
    }

    public String getCommandSpamKickMessage() {
        return color(cfg.getString("COMMAND_SECURITY.SPAM.MESSAGES.KICK", ""));
    }

    public String getCommandSpamAlertMessage() {
        return color(cfg.getString("COMMAND_SECURITY.SPAM.MESSAGES.ALERT", ""));
    }

    public boolean isBlockedCommandsEnabled() {
        return cfg.getBoolean("COMMAND_SECURITY.BLOCKED_COMMANDS.ENABLED", true);
    }

    public List<String> getBlockedCommands() {
        return cfg.getStringList("COMMAND_SECURITY.BLOCKED_COMMANDS.LIST");
    }

    public String getBlockedCommandMessage() {
        return color(cfg.getString("COMMAND_SECURITY.BLOCKED_COMMANDS.MESSAGES.BLOCKED", ""));
    }

    public String getBlockedCommandAlertMessage() {
        return color(cfg.getString("COMMAND_SECURITY.BLOCKED_COMMANDS.MESSAGES.ALERT", ""));
    }

    // ── Movement Security ─────────────────────────────────────────────────────

    public boolean isMovementSecurityEnabled() {
        return cfg.getBoolean("MOVEMENT_SECURITY.ENABLED", true);
    }

    public boolean isFlyDetectionEnabled() {
        return cfg.getBoolean("MOVEMENT_SECURITY.FLY_DETECTION.ENABLED", true);
    }

    public int getMaxAirTicks() {
        return cfg.getInt("MOVEMENT_SECURITY.FLY_DETECTION.MAX_AIR_TICKS", 40);
    }

    public String getFlyAction() {
        return cfg.getString("MOVEMENT_SECURITY.FLY_DETECTION.ACTION", "WARN").toUpperCase();
    }

    public int getFlyMaxViolations() {
        return cfg.getInt("MOVEMENT_SECURITY.FLY_DETECTION.MAX_VIOLATIONS_BEFORE_KICK", 3);
    }

    public String getFlyWarnMessage() {
        return color(cfg.getString("MOVEMENT_SECURITY.FLY_DETECTION.MESSAGES.WARN", ""));
    }

    public String getFlyKickMessage() {
        return color(cfg.getString("MOVEMENT_SECURITY.FLY_DETECTION.MESSAGES.KICK", ""));
    }

    public String getFlyAlertMessage() {
        return color(cfg.getString("MOVEMENT_SECURITY.FLY_DETECTION.MESSAGES.ALERT", ""));
    }

    public boolean isSpeedDetectionEnabled() {
        return cfg.getBoolean("MOVEMENT_SECURITY.SPEED_DETECTION.ENABLED", true);
    }

    public double getMaxSpeed() {
        return cfg.getDouble("MOVEMENT_SECURITY.SPEED_DETECTION.MAX_SPEED", 0.7);
    }

    public int getSpeedMaxViolations() {
        return cfg.getInt("MOVEMENT_SECURITY.SPEED_DETECTION.MAX_VIOLATIONS_BEFORE_KICK", 5);
    }

    public String getSpeedWarnMessage() {
        return color(cfg.getString("MOVEMENT_SECURITY.SPEED_DETECTION.MESSAGES.WARN", ""));
    }

    public String getSpeedKickMessage() {
        return color(cfg.getString("MOVEMENT_SECURITY.SPEED_DETECTION.MESSAGES.KICK", ""));
    }

    public String getSpeedAlertMessage() {
        return color(cfg.getString("MOVEMENT_SECURITY.SPEED_DETECTION.MESSAGES.ALERT", ""));
    }

    // ── Violations ───────────────────────────────────────────────────────────

    public int getViolationAutoResetMinutes() {
        return cfg.getInt("VIOLATIONS.AUTO_RESET_AFTER_MINUTES", 30);
    }

    public boolean isPersistViolations() {
        return cfg.getBoolean("VIOLATIONS.PERSIST", true);
    }

    public List<Integer> getAlertThresholds() {
        return cfg.getIntegerList("VIOLATIONS.ALERT_THRESHOLDS");
    }

    public String getThresholdAlertMessage() {
        return color(cfg.getString("VIOLATIONS.MESSAGES.THRESHOLD_ALERT", ""));
    }

    // ── Alerts ───────────────────────────────────────────────────────────────

    public boolean isAlertsEnabled() {
        return cfg.getBoolean("ALERTS.ENABLED", true);
    }

    public boolean isLogToConsole() {
        return cfg.getBoolean("ALERTS.LOG_TO_CONSOLE", true);
    }

    public boolean isAlertSoundEnabled() {
        return cfg.getBoolean("ALERTS.SOUND.ENABLED", true);
    }

    public String getAlertSoundValue() {
        return cfg.getString("ALERTS.SOUND.VALUE", "BLOCK_NOTE_BLOCK_PLING");
    }

    public float getAlertSoundVolume() {
        return (float) cfg.getDouble("ALERTS.SOUND.VOLUME", 1.0);
    }

    public float getAlertSoundPitch() {
        return (float) cfg.getDouble("ALERTS.SOUND.PITCH", 1.5);
    }

    public boolean isAlertBossbarEnabled() {
        return cfg.getBoolean("ALERTS.BOSSBAR.ENABLED", true);
    }

    public int getAlertBossbarDuration() {
        return cfg.getInt("ALERTS.BOSSBAR.DURATION_TICKS", 100);
    }

    // ── Logging ───────────────────────────────────────────────────────────────

    public boolean isLoggingEnabled() {
        return cfg.getBoolean("LOGGING.ENABLED", true);
    }

    public String getLogFile() {
        return cfg.getString("LOGGING.FILE", "security-log.txt");
    }

    public boolean isLogJoins() { return cfg.getBoolean("LOGGING.LOG_JOINS", true); }
    public boolean isLogKicks() { return cfg.getBoolean("LOGGING.LOG_KICKS", true); }
    public boolean isLogBans() { return cfg.getBoolean("LOGGING.LOG_BANS", true); }
    public boolean isLogViolations() { return cfg.getBoolean("LOGGING.LOG_VIOLATIONS", true); }
    public boolean isLogChatFlags() { return cfg.getBoolean("LOGGING.LOG_CHAT_FLAGS", true); }
    public boolean isLogCommandFlags() { return cfg.getBoolean("LOGGING.LOG_COMMAND_FLAGS", true); }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Strips legacy & codes and returns the raw string.
     * Full MiniMessage parsing is done in MessageUtil.
     */
    private String color(String input) {
        return input == null ? "" : input;
    }
}
