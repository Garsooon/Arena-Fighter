package org.garsooon.arenafighter.Arena;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.garsooon.arenafighter.Commands.FightAboutCommand;
import org.garsooon.arenafighter.Commands.ArenaCommand;
import org.garsooon.arenafighter.Commands.FightCommand;
import org.garsooon.arenafighter.Commands.SpectateCommand;
import org.garsooon.arenafighter.Commands.SpectateBetCommand;
import org.garsooon.arenafighter.Fight.FightManager;
import org.garsooon.arenafighter.Listeners.*;
import org.garsooon.arenafighter.Economy.Method;
import org.garsooon.arenafighter.Economy.Methods;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class ArenaFighter extends JavaPlugin {

    private ArenaManager arenaManager;
    private FightManager fightManager;
    @SuppressWarnings("FieldCanBeLocal")
    private Method economy;
    @SuppressWarnings("FieldMayBeFinal")
    private Set<String> blockedCommands = new HashSet<>();

    private boolean arenaEnabled = true;
    private Map<String, Object> configData;

    @Override
    public void onEnable() {
        // Create default config if missing
        createDefaultConfig();

        loadArenaEnabledFlag();

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
        getCommand("arena").setExecutor(new ArenaCommand(arenaManager, this));
        getCommand("spectate").setExecutor(new SpectateCommand(fightManager));
        getCommand("bet").setExecutor(new SpectateBetCommand(fightManager));
        this.getCommand("fightabout").setExecutor(new FightAboutCommand(this));

        // Register event listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerDeathListener(fightManager), this);
        pm.registerEvents(new PlayerQuitListener(fightManager, fightCommand), this);
        pm.registerEvents(new PlayerDropListener(fightManager), this);
        pm.registerEvents(new PlayerCommandListener(this, fightManager), this);
        pm.registerEvents(new BowListener(this), this);

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

    public boolean isArenaEnabled() {
        return arenaEnabled;
    }

    public void setArenaEnabled(boolean enabled) {
        this.arenaEnabled = enabled;
        if (configData == null) configData = new LinkedHashMap<>();
        Map<String, Object> arenaSection;
        if (configData.containsKey("arena") && configData.get("arena") instanceof Map) {
            arenaSection = (Map<String, Object>) configData.get("arena");
        } else {
            arenaSection = new LinkedHashMap<>();
            configData.put("arena", arenaSection);
        }
        arenaSection.put("enabled", enabled);
        saveConfigFile();
    }

    private void loadArenaEnabledFlag() {
        File configFile = new File(getDataFolder(), "config.yml");
        Yaml yaml = new Yaml();
        configData = null;

        try (InputStream input = new FileInputStream(configFile)) {
            configData = (Map<String, Object>) yaml.load(input);
        } catch (Exception e) {
            configData = new LinkedHashMap<>();
        }

        if (configData == null) configData = new LinkedHashMap<>();

        boolean needSave = false;

        Map<String, Object> arenaSection;
        if (configData.containsKey("arena") && configData.get("arena") instanceof Map) {
            arenaSection = (Map<String, Object>) configData.get("arena");
        } else {
            arenaSection = new LinkedHashMap<>();
            configData.put("arena", arenaSection);
            needSave = true;
        }

        if (!arenaSection.containsKey("enabled")) {
            arenaSection.put("enabled", true);
            this.arenaEnabled = true;
            needSave = true;
        } else {
            Object value = arenaSection.get("enabled");
            this.arenaEnabled = (value instanceof Boolean) ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
        }

        if (needSave) saveConfigFile();
    }

    public boolean isBowDisabled() {
        if (configData == null) return false;
        Object value = configData.get("disable-bows");
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return false;
    }

    //I should pr a config api that isnt for poseidons own config xd
    private void saveConfigFile() {
        File configFile = new File(getDataFolder(), "config.yml");
        org.yaml.snakeyaml.DumperOptions options = new org.yaml.snakeyaml.DumperOptions();
        options.setDefaultFlowStyle(org.yaml.snakeyaml.DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(configData, writer);
        } catch (Exception e) {
            getLogger().warning("Failed to save config.yml: " + e.getMessage());
        }
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
                writer.write("# Arena system enabled/disabled\n");
                writer.write("arena:\n");
                writer.write("  enabled: true\n");
                writer.write("# Enable / Disable bow use (may stop crashes for certain servers)\n");
                writer.write("disable-bows: false\n");
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

    @SuppressWarnings({"IOStreamConstructor", "VulnerableCodeUsages"})
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
