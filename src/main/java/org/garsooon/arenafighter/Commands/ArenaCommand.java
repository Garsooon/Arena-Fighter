package org.garsooon.arenafighter.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.garsooon.arenafighter.Arena.Arena;
import org.garsooon.arenafighter.Arena.ArenaManager;

import java.util.List;

public class ArenaCommand implements CommandExecutor {

    private final ArenaManager arenaManager;

    public ArenaCommand(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                return handleCreate(player, args);
            case "remove":
                return handleRemove(player, args);
            case "list":
                return handleList(player);
            case "tp":
                return handleTeleport(player, args);
            case "help":
                sendHelpMessage(player);
                return true;
            default:
                sendHelpMessage(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("arenafighter.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to create arenas!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena create <name>");
            player.sendMessage(ChatColor.YELLOW + "Stand at the first spawn point and run this command.");
            player.sendMessage(ChatColor.YELLOW + "Then stand at the second spawn point and run /arena create <name> again.");
            return true;
        }

        String arenaName = args[1];
        Location currentLoc = player.getLocation();

        Location spawn1 = currentLoc.clone();
        Location spawn2 = currentLoc.clone().add(10, 0, 0); // 10 blocks away on X axis

        // Define spectator spawn, e.g., 5 blocks east of spawn1
        Location spectatorSpawn = currentLoc.clone().add(5, 0, 0);

        arenaManager.createArena(arenaName, spawn1, spawn2, spectatorSpawn);

        player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' created successfully!");
        player.sendMessage(ChatColor.YELLOW + "Spawn 1: " + locationToString(spawn1));
        player.sendMessage(ChatColor.YELLOW + "Spawn 2: " + locationToString(spawn2));
        player.sendMessage(ChatColor.YELLOW + "Spectator Spawn: " + locationToString(spectatorSpawn));
        player.sendMessage(ChatColor.GRAY + "Note: Spawn 2 was automatically set 10 blocks east of Spawn 1.");
        player.sendMessage(ChatColor.GRAY + "Spectator spawn set 5 blocks east of Spawn 1.");
        player.sendMessage(ChatColor.GRAY + "You can manually edit the config file to adjust spawn locations.");

        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (!player.hasPermission("arenafighter.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to remove arenas!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena remove <name>");
            return true;
        }

        String arenaName = args[1];
        Arena arena = arenaManager.getArena(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' does not exist!");
            return true;
        }

        if (arena.isOccupied()) {
            player.sendMessage(ChatColor.RED + "Cannot remove arena '" + arenaName + "' - it's currently in use!");
            return true;
        }

        arenaManager.removeArena(arenaName);
        player.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' removed successfully!");

        return true;
    }

    private boolean handleList(Player player) {
        List<String> arenaNames = arenaManager.getArenaNames();

        if (arenaNames.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No arenas configured.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Available Arenas ===");
        for (String name : arenaNames) {
            Arena arena = arenaManager.getArena(name);
            String status = arena.isOccupied() ? ChatColor.RED + "[OCCUPIED]" : ChatColor.GREEN + "[AVAILABLE]";
            player.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + name + " " + status);
        }

        return true;
    }

    // Teleport handler for fighters
    private boolean handleTeleport(Player player, String[] args) {
        if (!player.hasPermission("arenafighter.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to teleport to arenas!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /arena tp <name> [spawn1|spawn2]");
            return true;
        }

        String arenaName = args[1];
        Arena arena = arenaManager.getArena(arenaName);

        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' does not exist!");
            return true;
        }

        Location teleportLocation;
        if (args.length >= 3 && args[2].equalsIgnoreCase("spawn2")) {
            teleportLocation = arena.getSpawn2();
            player.sendMessage(ChatColor.GREEN + "Teleported to " + arenaName + " spawn 2!");
        } else {
            teleportLocation = arena.getSpawn1();
            player.sendMessage(ChatColor.GREEN + "Teleported to " + arenaName + " spawn 1!");
        }

        // Actually teleport the player
        player.teleport(teleportLocation);

        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Arena Fighter Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/arena create <name>" + ChatColor.WHITE + " - Create a new arena");
        player.sendMessage(ChatColor.YELLOW + "/arena remove <name>" + ChatColor.WHITE + " - Remove an arena");
        player.sendMessage(ChatColor.YELLOW + "/arena list" + ChatColor.WHITE + " - List all arenas");
        player.sendMessage(ChatColor.YELLOW + "/arena tp <name> [spawn1|spawn2]" + ChatColor.WHITE + " - Teleport to an arena");
        player.sendMessage(ChatColor.YELLOW + "/arena help" + ChatColor.WHITE + " - Show this help message");
    }

    private String locationToString(Location location) {
        return String.format("World: %s, X: %.1f, Y: %.1f, Z: %.1f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }
}
