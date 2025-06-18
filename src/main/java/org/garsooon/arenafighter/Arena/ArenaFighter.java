package org.garsooon.arenafighter.Arena;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.garsooon.arenafighter.Commands.FightAboutCommand;
import org.garsooon.arenafighter.Commands.ArenaCommand;
import org.garsooon.arenafighter.Commands.FightCommand;
import org.garsooon.arenafighter.Commands.SpectateCommand;
import org.garsooon.arenafighter.Fight.FightManager;
import org.garsooon.arenafighter.Listeners.PlayerDeathListener;
import org.garsooon.arenafighter.Listeners.PlayerQuitListener;
import org.garsooon.arenafighter.Economy.Method;
import org.garsooon.arenafighter.Economy.Methods;

import static org.bukkit.Bukkit.getLogger;

public class ArenaFighter extends JavaPlugin {

    private ArenaManager arenaManager;
    private FightManager fightManager;

    @Override
    public void onEnable() {
        // Initialize managers
        this.arenaManager = new ArenaManager(this);
        this.fightManager = new FightManager(this, arenaManager);

        // Create shared FightCommand instance
        FightCommand fightCommand = new FightCommand(this, fightManager);

        // Register commands
        getCommand("fight").setExecutor(fightCommand);
        getCommand("arena").setExecutor(new ArenaCommand(arenaManager));
        getCommand("spectate").setExecutor(new SpectateCommand(fightManager));
        this.getCommand("fightabout").setExecutor(new FightAboutCommand("1.0.4", "Garsooon"));

        // Register event listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerDeathListener(fightManager), this);
        pm.registerEvents(new PlayerQuitListener(fightManager, fightCommand), this);

        // Load configuration
        createDefaultConfig();
        arenaManager.loadArenas();

        //Eco loading and test. TODO Rework after wagers are added BEFORE pushing to main
        boolean economyLoaded = Methods.setMethod(getServer().getPluginManager());

        if (!economyLoaded) {
            getLogger().warning("[AF Eco] No economy plugin loaded!");
        } else {
            Method method = Methods.getMethod();
            getLogger().info("[AF Eco] Method loaded: " + method.getName() + " v" + method.getVersion());
        }

        getLogger().info("ArenaFighter plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up any ongoing fights
        if (fightManager != null) {
            fightManager.cleanup();
        }

        getLogger().info("ArenaFighter plugin has been disabled!");
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public FightManager getFightManager() {
        return fightManager;
    }

    private void createDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        java.io.File configFile = new java.io.File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();

                // Write default configuration
                java.io.FileWriter writer = new java.io.FileWriter(configFile);
                writer.write("# ArenaFighter Configuration\n");
                writer.write("# This file contains the configuration for arena locations\n\n");
                writer.write("# Example arena configuration\n");
                writer.write("arenas:\n");
                writer.write("  example_arena:\n");
                writer.write("    world: world\n");
                writer.write("    spawn1:\n");
                writer.write("      x: 100.5\n");
                writer.write("      y: 64.0\n");
                writer.write("      z: 200.5\n");
                writer.write("      yaw: 0.0\n");
                writer.write("      pitch: 0.0\n");
                writer.write("    spawn2:\n");
                writer.write("      x: 120.5\n");
                writer.write("      y: 64.0\n");
                writer.write("      z: 200.5\n");
                writer.write("      yaw: 180.0\n");
                writer.write("      pitch: 0.0\n");
                writer.write("    spectatorSpawn:\n");
                writer.write("      x: 110.5\n");
                writer.write("      y: 64.0\n");
                writer.write("      z: 200.5\n");
                writer.write("      yaw: 180.0\n");
                writer.write("      pitch: 0.0\n");
                writer.close();

                getLogger().info("Created default config.yml");
            } catch (java.io.IOException e) {
                getLogger().warning("Could not create default config: " + e.getMessage());
            }
        }

        getLogger().info("Configuration file ready for loading");
    }

    //TODO remove before release, rework loader.
    private void testEconomyLoad() {
        Method method = Methods.getMethod();

        if (method == null) {
            getLogger().warning("[AF Eco] No economy plugin loaded!");
            return;
        }

        getLogger().info("[AF Eco] Method loaded: " + method.getName() + " v" + method.getVersion());
    }
}
