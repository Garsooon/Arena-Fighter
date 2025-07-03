package org.garsooon.arenafighter.Arena;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.garsooon.arenafighter.Commands.FightAboutCommand;
import org.garsooon.arenafighter.Commands.ArenaCommand;
import org.garsooon.arenafighter.Commands.FightCommand;
import org.garsooon.arenafighter.Commands.SpectateCommand;
import org.garsooon.arenafighter.Commands.SpectateBetCommand;
import org.garsooon.arenafighter.Fight.FightManager;
import org.garsooon.arenafighter.Listeners.PlayerCommandListener;
import org.garsooon.arenafighter.Listeners.PlayerDeathListener;
import org.garsooon.arenafighter.Listeners.PlayerDropListener;
import org.garsooon.arenafighter.Listeners.PlayerQuitListener;
import org.garsooon.arenafighter.Economy.Method;
import org.garsooon.arenafighter.Economy.Methods;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.bukkit.Bukkit.getLogger;

public class ArenaFighter extends JavaPlugin {

    private ArenaManager arenaManager;
    private FightManager fightManager;
    private Method economy;
    private Set<String> blockedCommands = new HashSet<>();

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
        getCommand("bet").setExecutor(new SpectateBetCommand(fightManager));
        this.getCommand("fightabout").setExecutor(new FightAboutCommand(this));

        // Register event listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerDeathListener(fightManager), this);
        pm.registerEvents(new PlayerQuitListener(fightManager, fightCommand), this);
        pm.registerEvents(new PlayerDropListener(fightManager), this);
        pm.registerEvents(new PlayerCommandListener(this, fightManager), this);

        // Create default config if missing
        createDefaultConfig();

        // Load blocked commands list
        loadBlockedCommands();

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

    public Set<String> getBlockedCommands() {
        return blockedCommands;
    }

    private void createDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();

                // Write default configuration
                java.io.FileWriter writer = new java.io.FileWriter(configFile);
                writer.write("# ArenaFighter Configuration\n");
                writer.write("# To configure your arenas, run /arena create <arena_name>\n");
                writer.write("# then you can modify it in arenas.properties in this folder\n");
                writer.write("\n");
                writer.write("punishment:\n");
                writer.write("  duration-minute: 5\n");
                writer.write("\n");
                writer.write("# Command blocking for players during a fight\n");
                writer.write("blocked-commands:\n");
                writer.write("  - \"/spectate\"\n");
                writer.write("  - \"/spawn\"\n");
                writer.write("  - \"/home\"\n");
                writer.write("  - \"/homes\"\n");
                writer.write("  - \"/sethome\"\n");
                writer.write("  - \"/warp\"\n");
                writer.write("  - \"/tp\"\n");
                writer.write("  - \"/tpa\"\n");
                writer.write("  - \"/tpaccept\"\n");
                writer.write("  - \"/tphere\"\n");
                writer.write("  - \"/heal\"\n");
                writer.close();

                getLogger().info("Created default config.yml");
            } catch (Exception e) {
                getLogger().warning("Could not create default config: " + e.getMessage());
            }
        }

        getLogger().info("Configuration file ready for loading");
    }

    private void loadBlockedCommands() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                getLogger().warning("Config file does not exist!");
                return;
            }

            Yaml yaml = new Yaml();
            try (InputStream input = new FileInputStream(configFile)) {
                Object loaded = yaml.load(input);
                if (!(loaded instanceof Map)) {
                    getLogger().warning("Invalid config.yml format");
                    return;
                }

                Map<?, ?> map = (Map<?, ?>) loaded;
                Object blocked = map.get("blocked-commands");
                if (blocked instanceof List<?>) {
                    for (Object cmd : (List<?>) blocked) {
                        if (cmd instanceof String) {
                            blockedCommands.add(((String) cmd).toLowerCase());
                        }
                    }
                } else {
                    getLogger().warning("blocked-commands missing or invalid");
                }
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load config.yml: " + e.getMessage());
        }
    }
}
