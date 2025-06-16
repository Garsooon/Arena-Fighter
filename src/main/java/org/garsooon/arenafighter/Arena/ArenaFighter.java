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
        //TODO (Lowest Priority) automatically pull version info from pom.xml or plugin.yml
        this.getCommand("fightabout").setExecutor(new FightAboutCommand("1.0.3", "Garsooon"));

        // Register event listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerDeathListener(fightManager), this);
        pm.registerEvents(new PlayerQuitListener(fightManager, fightCommand), this);

        // Load configuration
        createDefaultConfig();
        arenaManager.loadArenas();

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
                writer.write("# To configure your arenas, run /arena create <arena_name>\n");
                writer.write("# then you can modify it in arenas.properties in this folder\n");
                writer.write("      punishment:\n");
                writer.write("        duration-minute: 5\n");
                writer.close();

                getLogger().info("Created default config.yml");
            } catch (java.io.IOException e) {
                getLogger().warning("Could not create default config: " + e.getMessage());
            }
        }

        // For Beta 1.7.3, we don't have reloadConfig() method like in modern
        // The ArenaManager will handle loading the config file directly
        getLogger().info("Configuration file ready for loading");
    }
}
