package org.garsooon.arenafighter.Fight;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.garsooon.arenafighter.Arena.Arena;
import org.garsooon.arenafighter.Arena.ArenaFighter;
import org.garsooon.arenafighter.Arena.ArenaManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FightManager {

    private final ArenaFighter plugin;
    private final ArenaManager arenaManager;
    private final Map<UUID, Fight> activeFights;
    private final Map<UUID, Location> originalLocations;
    private final Map<UUID, UUID> pendingChallenges;
    private final Map<UUID, Location> spectatorOriginalLocations;

    public FightManager(ArenaFighter plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.activeFights = new HashMap<>();
        this.originalLocations = new HashMap<>();
        this.pendingChallenges = new HashMap<>();
        this.spectatorOriginalLocations = new HashMap<>();
    }

    public boolean startFight(Player player1, Player player2) {
        if (isInFight(player1) || isInFight(player2)) {
            return false;
        }

        Arena arena = arenaManager.getAvailableArena();
        if (arena == null) {
            return false;
        }

        originalLocations.put(player1.getUniqueId(), player1.getLocation().clone());
        originalLocations.put(player2.getUniqueId(), player2.getLocation().clone());

        Fight fight = new Fight(player1, player2, arena);
        activeFights.put(player1.getUniqueId(), fight);
        activeFights.put(player2.getUniqueId(), fight);

        arenaManager.occupyArena(arena);

        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        healAndFeedPlayer(player1);
        healAndFeedPlayer(player2);

        String message = ChatColor.YELLOW + "Fight started! " +
                ChatColor.WHITE + player1.getName() +
                ChatColor.YELLOW + " vs " +
                ChatColor.WHITE + player2.getName() +
                ChatColor.YELLOW + " in arena " +
                ChatColor.GREEN + arena.getName();

        plugin.getServer().broadcastMessage(message);

        return true;
    }

    public void endFight(Player winner, Player loser) {
        Fight fight = activeFights.get(winner.getUniqueId());
        if (fight == null) return;

        activeFights.remove(winner.getUniqueId());
        activeFights.remove(loser.getUniqueId());

        arenaManager.releaseArena(fight.getArena());

        Location winnerOriginal = originalLocations.remove(winner.getUniqueId());
        Location loserOriginal = originalLocations.remove(loser.getUniqueId());

        if (winnerOriginal != null) {
            winner.teleport(winnerOriginal);
            healAndFeedPlayer(winner);
        }

        if (loserOriginal != null) {
            loser.teleport(loserOriginal);
            healAndFeedPlayer(loser);
        }

        stopAllSpectators();

        String message = ChatColor.GOLD + winner.getName() +
                ChatColor.YELLOW + " has defeated " +
                ChatColor.RED + loser.getName() +
                ChatColor.YELLOW + " in arena " +
                ChatColor.GREEN + fight.getArena().getName() + "!";

        plugin.getServer().broadcastMessage(message);
    }

    public void cancelFight(Player player) {
        Fight fight = activeFights.get(player.getUniqueId());
        if (fight == null) return;

        Player otherPlayer = fight.getOtherPlayer(player);

        activeFights.remove(player.getUniqueId());
        activeFights.remove(otherPlayer.getUniqueId());

        arenaManager.releaseArena(fight.getArena());

        Location playerOriginal = originalLocations.remove(player.getUniqueId());
        Location otherOriginal = originalLocations.remove(otherPlayer.getUniqueId());

        if (playerOriginal != null) {
            player.teleport(playerOriginal);
            healAndFeedPlayer(player);
        }

        if (otherOriginal != null) {
            otherPlayer.teleport(otherOriginal);
            healAndFeedPlayer(otherPlayer);
        }

        stopAllSpectators();

        String message = ChatColor.RED + "Fight cancelled!";
        player.sendMessage(message);
        otherPlayer.sendMessage(message);
    }

    public boolean isInFight(Player player) {
        return activeFights.containsKey(player.getUniqueId());
    }

    public Fight getFight(Player player) {
        return activeFights.get(player.getUniqueId());
    }

    private void healAndFeedPlayer(Player player) {
        player.setHealth(20);
    }

    public void cleanup() {
        for (Fight fight : activeFights.values()) {
            Player player1 = fight.getPlayer1();
            Player player2 = fight.getPlayer2();

            Location loc1 = originalLocations.get(player1.getUniqueId());
            Location loc2 = originalLocations.get(player2.getUniqueId());

            if (loc1 != null && player1.isOnline()) {
                player1.teleport(loc1);
            }

            if (loc2 != null && player2.isOnline()) {
                player2.teleport(loc2);
            }

            arenaManager.releaseArena(fight.getArena());
        }

        activeFights.clear();
        originalLocations.clear();
        stopAllSpectators();
    }

    public boolean hasPendingChallenge(Player player) {
        UUID uuid = player.getUniqueId();
        return pendingChallenges.containsKey(uuid) || pendingChallenges.containsValue(uuid);
    }

    public boolean sendChallenge(Player challenger, Player target) {
        UUID challengerId = challenger.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (hasPendingChallenge(challenger) || hasPendingChallenge(target)) {
            return false;
        }

        pendingChallenges.put(challengerId, targetId);
        return true;
    }

    public boolean acceptChallenge(Player target) {
        UUID targetId = target.getUniqueId();
        UUID challengerId = null;

        for (Map.Entry<UUID, UUID> entry : pendingChallenges.entrySet()) {
            if (entry.getValue().equals(targetId)) {
                challengerId = entry.getKey();
                break;
            }
        }

        if (challengerId == null) return false;

        Player challenger = plugin.getServer().getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            pendingChallenges.remove(challengerId);
            return false;
        }

        pendingChallenges.remove(challengerId);

        // Start 30 second countdown before fight starts
        //TODO look into how I set up scheduler, it looks like fightcommand takes priority this may be redundant
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // Send 15 seconds left message to both players
            if (challenger.isOnline() && target.isOnline()) {
                challenger.sendMessage(ChatColor.YELLOW + "Fight starts in 15 seconds...");
                target.sendMessage(ChatColor.YELLOW + "Fight starts in 15 seconds...");
            }
        }, 20L * 15);

        // Schedule the actual fight start after 30 seconds
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (challenger.isOnline() && target.isOnline()) {
                startFight(challenger, target);
            }
        }, 20L * 30);

        return true;
    }

    public void cancelChallenge(Player player) {
        UUID uuid = player.getUniqueId();
        pendingChallenges.entrySet().removeIf(entry ->
                entry.getKey().equals(uuid) || entry.getValue().equals(uuid));
    }

    // Spectator Methods
    public boolean startSpectating(Player player) {
        UUID uuid = player.getUniqueId();
        if (spectatorOriginalLocations.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already spectating.");
            return false;
        }

        Fight fight = null;
        for (Fight f : activeFights.values()) {
            if (!f.getPlayer1().getUniqueId().equals(uuid) && !f.getPlayer2().getUniqueId().equals(uuid)) {
                fight = f;
                break;
            }
        }

        if (fight == null) {
            player.sendMessage(ChatColor.RED + "No ongoing fight to spectate.");
            return false;
        }

        Arena arena = fight.getArena();
        Location specSpawn = arena.getSpectatorSpawn();

        if (specSpawn == null) {
            player.sendMessage(ChatColor.RED + "No spectator spawn set for this arena.");
            return false;
        }

        spectatorOriginalLocations.put(uuid, player.getLocation().clone());
        player.teleport(specSpawn);
        player.sendMessage(ChatColor.YELLOW + "You are now spectating the fight between " +
                fight.getPlayer1().getName() + " and " + fight.getPlayer2().getName() + ".");
        return true;
    }

    //TODO Clean up old code with no references anymore or use in game -spectate command, fightcommand and fight manager
    //Start spectating a specific arena by name
    public boolean startSpectating(Player player, String arenaName) {
        UUID uuid = player.getUniqueId();

        if (spectatorOriginalLocations.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already spectating.");
            return false;
        }

        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' does not exist.");
            return false;
        }

        Location specSpawn = arena.getSpectatorSpawn();
        if (specSpawn == null) {
            player.sendMessage(ChatColor.RED + "Spectator spawn is not set for arena '" + arenaName + "'.");
            return false;
        }

        spectatorOriginalLocations.put(uuid, player.getLocation().clone());
        player.teleport(specSpawn);
        player.sendMessage(ChatColor.YELLOW + "You are now spectating arena: " + ChatColor.AQUA + arenaName);
        player.sendMessage(ChatColor.YELLOW + "Use /spectate to return to your original location.");
        return true;
    }

    public boolean stopSpectating(Player player) {
        UUID uuid = player.getUniqueId();
        Location original = spectatorOriginalLocations.remove(uuid);

        if (original != null) {
            player.teleport(original);
            player.sendMessage(ChatColor.YELLOW + "Returned from spectating.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "You are not spectating.");
        return false;
    }

    public void stopAllSpectators() {
        for (UUID uuid : new HashMap<>(spectatorOriginalLocations).keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                stopSpectating(player);
            }
        }
    }

    public boolean isSpectating(Player player) {
        return spectatorOriginalLocations.containsKey(player.getUniqueId());
    }
}
