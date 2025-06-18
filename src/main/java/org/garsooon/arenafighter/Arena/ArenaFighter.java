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
    private Method economy;

    @Override
    public void onEnable() {
        // Initialize ArenaManager
        this.arenaManager = new ArenaManager(this);

        // Load economy before FightManager, Prevents NULL pointer exceptions
        boolean economyLoaded = Methods.setMethod(getServer().getPluginManager());

        if (!economyLoaded) {
            getLogger().warning("[ArenaFighter Eco] No economy plugin loaded, Wagers Disabled!");
            this.economy = null; // Handle null in FightManager if needed
        } else {
            this.economy = Methods.getMethod();
            getLogger().info("[ArenaFighter Eco] Method loaded: " + economy.getName() + " v" + economy.getVersion());
        }

        // Create FightManager
        this.fightManager = new FightManager(this, arenaManager, economy);

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

        getLogger().info("[ArenaFighter] ArenaFighter plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Clean up any ongoing fights
        if (fightManager != null) {
            fightManager.cleanup();
        }

        getLogger().info("[ArenaFighter] ArenaFighter plugin has been disabled!");
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

        getLogger().info("Configuration file ready for loading");
    }
}
