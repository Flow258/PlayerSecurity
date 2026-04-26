package dev.blackarchive.flow258.playerSecurity;

import dev.blackarchive.flow258.playerSecurity.security.commands.SecurityCommand;
import dev.blackarchive.flow258.playerSecurity.security.config.MessagesConfig;
import dev.blackarchive.flow258.playerSecurity.security.config.SecurityConfig;
import dev.blackarchive.flow258.playerSecurity.security.listeners.*;
import dev.blackarchive.flow258.playerSecurity.security.managers.*;
import dev.blackarchive.flow258.playerSecurity.security.managers.SecurityManager;
import dev.blackarchive.flow258.playerSecurity.security.utils.LogManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerSecurityPlugin extends JavaPlugin {

    private static PlayerSecurityPlugin instance;

    private SecurityConfig            securityConfig;
    private MessagesConfig            messagesConfig;
    private SecurityManager securityManager;
    private ViolationManager          violationManager;
    private AlertManager              alertManager;
    private LogManager                logManager;
    private IPDataManager             ipDataManager;
    private VpnChecker                vpnChecker;
    private RaidProtectionManager     raidProtectionManager;

    @Override
    public void onEnable() {
        instance = this;
        printBanner();

        saveDefaultConfig();

        securityConfig        = new SecurityConfig(this);
        messagesConfig        = new MessagesConfig(this);

        logManager            = new LogManager(this);
        alertManager          = new AlertManager(this);
        violationManager      = new ViolationManager(this);
        ipDataManager         = new IPDataManager(this);
        vpnChecker            = new VpnChecker(this);
        raidProtectionManager = new RaidProtectionManager(this);
        securityManager       = new SecurityManager(this);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);

        SecurityCommand cmd = new SecurityCommand(this);
        getCommand("security").setExecutor(cmd);
        getCommand("security").setTabCompleter(cmd);

        violationManager.startAutoResetTask();

        getLogger().info("PlayerSecurity enabled! Protecting BlackArchive.");
    }

    @Override
    public void onDisable() {
        if (violationManager != null) violationManager.saveViolations();
        if (ipDataManager    != null) ipDataManager.save();
        if (logManager       != null) logManager.close();
        getLogger().info("PlayerSecurity disabled. Stay safe!");
    }

    public static PlayerSecurityPlugin getInstance()           { return instance; }
    public SecurityConfig         getSecurityConfig()          { return securityConfig; }
    public MessagesConfig         getMessagesConfig()          { return messagesConfig; }
    public SecurityManager        getSecurityManager()         { return securityManager; }
    public ViolationManager       getViolationManager()        { return violationManager; }
    public AlertManager           getAlertManager()            { return alertManager; }
    public LogManager             getLogManager()              { return logManager; }
    public IPDataManager          getIpDataManager()           { return ipDataManager; }
    public VpnChecker             getVpnChecker()              { return vpnChecker; }
    public RaidProtectionManager  getRaidProtection()          { return raidProtectionManager; }

    private void printBanner() {
        getLogger().info("  ____  _        _    ___ _____ ");
        getLogger().info(" |  _ \\| |      / \\  |_ _|_   _|");
        getLogger().info(" | |_) | |     / _ \\  | |  | |  ");
        getLogger().info(" |  __/| |___ / ___ \\ | |  | |  ");
        getLogger().info(" |_|   |_____/_/   \\_\\___| |_|  ");
        getLogger().info("  SECURITY & DEFENSE PROTOCOL");
        getLogger().info("  By BlackArchive | v" + getDescription().getVersion());
        getLogger().info("-------------------------------------");
    }
}
