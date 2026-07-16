package com.opsbodyguard;

import com.opsbodyguard.commands.AllGuardsCommand;
import com.opsbodyguard.commands.BuyGuardCommand;
import com.opsbodyguard.commands.CancelGuardCommand;
import com.opsbodyguard.commands.GuardControlCommand;
import com.opsbodyguard.commands.HelpCommand;
import com.opsbodyguard.commands.ReloadCommand;
import com.opsbodyguard.listeners.GuardListener;
import com.opsbodyguard.listeners.GUIListener;
import com.opsbodyguard.listeners.ChatListener;
import com.opsbodyguard.manager.GuardManager;
import com.opsbodyguard.manager.EconomyManager;
import com.opsbodyguard.gui.GUIManager;
import com.opsbodyguard.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class OPsBodyGuard extends JavaPlugin {

    private static OPsBodyGuard instance;
    private GuardManager guardManager;
    private EconomyManager economyManager;
    private GUIManager guiManager;
    private Economy economy;
    private long lastDayTime = -1; // Track last MC day time

    @Override
    public void onEnable() {
        instance = this;

        // Save default config and guide
        saveDefaultConfig();
        saveGuideFile();

        // Setup Vault Economy
        if (!setupEconomy()) {
            MessageUtil.logError("Vault not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        guardManager = new GuardManager(this);
        economyManager = new EconomyManager(this, economy);
        guiManager = new GUIManager(this);

        // Load saved guards
        guardManager.loadGuards();

        // Register commands
        getCommand("guardbuy").setExecutor(new BuyGuardCommand(this));
        getCommand("guardcontrol").setExecutor(new GuardControlCommand(this));
        getCommand("guardall").setExecutor(new AllGuardsCommand(this));
        getCommand("guardcancel").setExecutor(new CancelGuardCommand(this));
        getCommand("guardhelp").setExecutor(new HelpCommand(this));
        getCommand("guardreload").setExecutor(new ReloadCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new GuardListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Start MC day tracking task
        startMCDayTrackingTask();

        // Start guard behavior task
        startGuardBehaviorTask();

        MessageUtil.logInfo("Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all guards
        if (guardManager != null) {
            guardManager.saveGuards();
        }

        MessageUtil.logError("Plugin has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Track Minecraft day cycles and process payments/expiry when a new day starts.
     * A Minecraft day is 24000 ticks. Day starts at time 0.
     * This also detects when night is skipped (time jumps from night to morning).
     */
    private void startMCDayTrackingTask() {
        // Check every 20 ticks (1 second) for day changes
        getServer().getScheduler().runTaskTimer(this, () -> {
            World world = Bukkit.getWorlds().get(0); // Use main world
            long currentTime = world.getTime();
            long currentFullTime = world.getFullTime();
            long currentDay = currentFullTime / 24000L;

            // Initialize on first run
            if (lastDayTime == -1) {
                lastDayTime = currentDay;
                return;
            }

            // Check if a new day has started (including skipped nights)
            if (currentDay > lastDayTime) {
                // New MC day started - process payments
                guardManager.processMCDayPayments();
                lastDayTime = currentDay;
            }
        }, 20L, 20L); // Check every second
    }

    private void startGuardBehaviorTask() {
        // Run every 10 ticks for smooth behavior
        getServer().getScheduler().runTaskTimer(this, () -> {
            guardManager.updateGuardBehaviors();
        }, 20L, 10L);
    }

    public static OPsBodyGuard getInstance() {
        return instance;
    }

    public GuardManager getGuardManager() {
        return guardManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * Save the GUIDE.txt file as read-only so users can view but not modify it.
     * Always overwrites to ensure latest version.
     */
    private void saveGuideFile() {
        try {
            File guideFile = new File(getDataFolder(), "GUIDE.txt");

            // Make writable if exists (so we can overwrite)
            if (guideFile.exists()) {
                guideFile.setWritable(true);
            }

            // Copy from resources
            try (InputStream in = getResource("GUIDE.txt")) {
                if (in != null) {
                    Files.copy(in, guideFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    // Set read-only
                    guideFile.setReadOnly();
                }
            }
        } catch (Exception e) {
            MessageUtil.logWarning("Could not save GUIDE.txt: " + e.getMessage());
        }
    }
}
