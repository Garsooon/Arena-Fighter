package org.garsooon.arenafighter.Arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ArenaManager {

    private final ArenaFighter plugin;
    private final Map<String, Arena> arenas;
    private final List<Arena> availableArenas;
    private final Map<Player, Arena> spectators;
    private final Logger logger;
    private final File configFile;

    public ArenaManager(ArenaFighter plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.availableArenas = new ArrayList<>();
        this.spectators = new HashMap<>();
        this.logger = Logger.getLogger("ArenaFighter");
        this.configFile = new File(plugin.getDataFolder(), "arenas.properties");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    public void loadArenas() {
        if (!configFile.exists()) {
            logger.warning("No arenas configured! Please set up arenas using commands.");
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        } catch (IOException e) {
            logger.warning("Failed to load arenas config: " + e.getMessage());
            return;
        }

        Map<String, Boolean> arenaNames = new HashMap<>();
        for (Object key : props.keySet()) {
            String keyStr = (String) key;
            if (keyStr.contains(".")) {
                String arenaName = keyStr.substring(0, keyStr.indexOf('.'));
                arenaNames.put(arenaName, true);
            }
        }

        for (String arenaName : arenaNames.keySet()) {
            try {
                String worldName = props.getProperty(arenaName + ".world");
                World world = plugin.getServer().getWorld(worldName);

                if (world == null) {
                    logger.warning("World '" + worldName + "' not found for arena '" + arenaName + "'");
                    continue;
                }

                Location spawn1 = new Location(
                        world,
                        Double.parseDouble(props.getProperty(arenaName + ".spawn1.x", "0")),
                        Double.parseDouble(props.getProperty(arenaName + ".spawn1.y", "64")),
                        Double.parseDouble(props.getProperty(arenaName + ".spawn1.z", "0")),
                        Float.parseFloat(props.getProperty(arenaName + ".spawn1.yaw", "0")),
                        Float.parseFloat(props.getProperty(arenaName + ".spawn1.pitch", "0"))
                );

                Location spawn2 = new Location(
                        world,
                        Double.parseDouble(props.getProperty(arenaName + ".spawn2.x", "0")),
                        Double.parseDouble(props.getProperty(arenaName + ".spawn2.y", "64")),
                        Double.parseDouble(props.getProperty(arenaName + ".spawn2.z", "0")),
                        Float.parseFloat(props.getProperty(arenaName + ".spawn2.yaw", "0")),
                        Float.parseFloat(props.getProperty(arenaName + ".spawn2.pitch", "0"))
                );

                Location spectatorSpawn = new Location(
                        world,
                        Double.parseDouble(props.getProperty(arenaName + ".spectator.x", "0")),
                        Double.parseDouble(props.getProperty(arenaName + ".spectator.y", "64")),
                        Double.parseDouble(props.getProperty(arenaName + ".spectator.z", "0")),
                        Float.parseFloat(props.getProperty(arenaName + ".spectator.yaw", "0")),
                        Float.parseFloat(props.getProperty(arenaName + ".spectator.pitch", "0"))
                );

                Arena arenaObj = new Arena(arenaName, spawn1, spawn2);
                arenaObj.setSpectatorSpawn(spectatorSpawn);
                arenas.put(arenaName, arenaObj);
                availableArenas.add(arenaObj);

                logger.info("Loaded arena: " + arenaName);

            } catch (Exception e) {
                logger.warning("Failed to load arena '" + arenaName + "': " + e.getMessage());
            }
        }
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Arena getAvailableArena() {
        if (availableArenas.isEmpty()) {
            return null;
        }
        return availableArenas.get(0);
    }

    public void occupyArena(Arena arena) {
        availableArenas.remove(arena);
        arena.setOccupied(true);
    }

    public void releaseArena(Arena arena) {
        arena.setOccupied(false);
        if (!availableArenas.contains(arena)) {
            availableArenas.add(arena);
        }
    }

    public void createArena(String name, Location spawn1, Location spawn2, Location spectatorSpawn) {
        Arena arena = new Arena(name, spawn1, spawn2);
        arena.setSpectatorSpawn(spectatorSpawn);
        arenas.put(name, arena);
        availableArenas.add(arena);
        saveArenaToConfig(name, spawn1, spawn2, spectatorSpawn);
    }

    public void removeArena(String name) {
        Arena arena = arenas.remove(name);
        if (arena != null) {
            availableArenas.remove(arena);
            removeArenaFromConfig(name);
        }
    }

    private void saveArenaToConfig(String name, Location spawn1, Location spawn2, Location spectatorSpawn) {
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                logger.warning("Failed to load existing config: " + e.getMessage());
            }
        }

        props.setProperty(name + ".world", spawn1.getWorld().getName());
        props.setProperty(name + ".spawn1.x", String.valueOf(spawn1.getX()));
        props.setProperty(name + ".spawn1.y", String.valueOf(spawn1.getY()));
        props.setProperty(name + ".spawn1.z", String.valueOf(spawn1.getZ()));
        props.setProperty(name + ".spawn1.yaw", String.valueOf(spawn1.getYaw()));
        props.setProperty(name + ".spawn1.pitch", String.valueOf(spawn1.getPitch()));

        props.setProperty(name + ".spawn2.x", String.valueOf(spawn2.getX()));
        props.setProperty(name + ".spawn2.y", String.valueOf(spawn2.getY()));
        props.setProperty(name + ".spawn2.z", String.valueOf(spawn2.getZ()));
        props.setProperty(name + ".spawn2.yaw", String.valueOf(spawn2.getYaw()));
        props.setProperty(name + ".spawn2.pitch", String.valueOf(spawn2.getPitch()));

        // Save spectator spawn
        props.setProperty(name + ".spectator.x", String.valueOf(spectatorSpawn.getX()));
        props.setProperty(name + ".spectator.y", String.valueOf(spectatorSpawn.getY()));
        props.setProperty(name + ".spectator.z", String.valueOf(spectatorSpawn.getZ()));
        props.setProperty(name + ".spectator.yaw", String.valueOf(spectatorSpawn.getYaw()));
        props.setProperty(name + ".spectator.pitch", String.valueOf(spectatorSpawn.getPitch()));

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Arena Fighter Arenas Configuration");
        } catch (IOException e) {
            logger.warning("Failed to save arena config: " + e.getMessage());
        }
    }

    private void removeArenaFromConfig(String name) {
        Properties props = new Properties();

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                logger.warning("Failed to load existing config: " + e.getMessage());
                return;
            }
        }

        List<String> keysToRemove = new ArrayList<>();
        for (Object key : props.keySet()) {
            String keyStr = (String) key;
            if (keyStr.startsWith(name + ".")) {
                keysToRemove.add(keyStr);
            }
        }

        for (String key : keysToRemove) {
            props.remove(key);
        }

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Arena Fighter Arenas Configuration");
        } catch (IOException e) {
            logger.warning("Failed to save arena config: " + e.getMessage());
        }
    }

    public List<String> getArenaNames() {
        return new ArrayList<>(arenas.keySet());
    }

    // Spectator Methods

    public void addSpectator(Player player, Arena arena) {
        spectators.put(player, arena);
        arena.addSpectator(player);
    }

    public void removeSpectator(Player player) {
        Arena arena = spectators.remove(player);
        if (arena != null) {
            arena.removeSpectator(player);
        }
    }

    public Arena getSpectatingArena(Player player) {
        return spectators.get(player);
    }

    public boolean isSpectating(Player player) {
        return spectators.containsKey(player);
    }
}
