package org.garsooon.arenafighter.Fight;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.garsooon.arenafighter.Arena.Arena;
import org.garsooon.arenafighter.Arena.ArenaFighter;
import org.garsooon.arenafighter.Arena.ArenaManager;
import org.garsooon.arenafighter.Data.Challenge;
import org.garsooon.arenafighter.Economy.Method;
import org.garsooon.arenafighter.Economy.Methods;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class FightManager {

    private final ArenaFighter plugin;
    private final ArenaManager arenaManager;
    private final Map<UUID, Fight> activeFights;
    private final Map<UUID, Location> originalLocations;
    private final Map<UUID, FightChallenge> pendingChallenges;
    private final Map<UUID, Location> spectatorOriginalLocations;
    private final Map<UUID, Long> punishments = new HashMap<>();
    private final long punishmentDurationMillis;
    private final Method economy;

    public FightManager(ArenaFighter plugin, ArenaManager arenaManager, Method economy) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.economy = economy;
        this.activeFights = new HashMap<>();
        this.originalLocations = new HashMap<>();
        this.pendingChallenges = new HashMap<>();
        this.spectatorOriginalLocations = new HashMap<>();
        this.punishmentDurationMillis = loadPunishmentDuration(plugin.getDataFolder());
    }

    private long loadPunishmentDuration(File dataFolder) {
        File configFile = new File(dataFolder, "config.yml");
        long defaultMinutes = 5;

        if (!configFile.exists()) return defaultMinutes * 60 * 1000;

        try (FileInputStream input = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(input);

            if (data instanceof Map) {
                Map<?, ?> root = (Map<?, ?>) data;
                Object punishmentObj = root.get("punishment");

                if (punishmentObj instanceof Map) {
                    Map<?, ?> punishmentMap = (Map<?, ?>) punishmentObj;
                    Object minutesObj = punishmentMap.get("duration-minutes");

                    if (minutesObj instanceof Number) {
                        return ((Number) minutesObj).longValue() * 60 * 1000;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getServer().getLogger().warning("Failed to load punishment duration: " + e.getMessage());
        }

        return defaultMinutes * 60 * 1000;
    }

    public void punishQuitter(Player quitter) {
        long expireAt = System.currentTimeMillis() + punishmentDurationMillis;
        punishments.put(quitter.getUniqueId(), expireAt);
    }

    public long getRemainingPunishment(Player player) {
        Long expireTime = punishments.get(player.getUniqueId());
        if (expireTime == null) return 0;
        return Math.max(0, expireTime - System.currentTimeMillis());
    }

    public boolean isPunished(Player player) {
        UUID uuid = player.getUniqueId();
        Long expire = punishments.get(uuid);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            punishments.remove(uuid);
            return false;
        }
        return true;
    }

    public long getPunishmentDurationMillis() {
        return punishmentDurationMillis;
    }

    public int getPunishmentDurationMinutes() {
        return (int) (punishmentDurationMillis / 1000 / 60);
    }

    public ArenaFighter getPlugin() {
        return plugin;
    }

    // Helpers for ECO function via Method interface
    private boolean tryWithdraw(Player player, double amount) {
        if (amount <= 0) return true;
        boolean success = economy.withdrawPlayer(player.getName(), amount, player.getWorld());
        if (!success) {
            player.sendMessage(ChatColor.RED + "You do not have enough money to wager " + amount);
        }
        return success;
    }

    public void deposit(Player player, double amount) {
        if (amount <= 0) return;
        economy.depositPlayer(player.getName(), amount, player.getWorld());
    }

    public boolean hasSufficientFunds(Player player, double amount) {
        return economy != null && economy.hasEnough(player.getName(), amount, player.getWorld());
    }

    public Fight getFightByArena(String arenaName) {
        for (Fight fight : activeFights.values()) {
            if (fight.getArena().getName().equalsIgnoreCase(arenaName)) {
                return fight;
            }
        }
        return null;
    }

    public Collection<Fight> getActiveFights() {
        return activeFights.values();
    }

    public boolean startFight(Player player1, Player player2, double wager) {
        if (isInFight(player1) || isInFight(player2)) return false;

        // Final balance check before fight begins
        if (wager > 0) {
            if (!hasSufficientFunds(player1, wager)) {
                player1.sendMessage(ChatColor.RED + "You no longer have enough money to enter this fight.");
                player2.sendMessage(ChatColor.RED + player1.getName() + " no longer has enough money. Fight canceled.");
                return false;
            }
            if (!hasSufficientFunds(player2, wager)) {
                player2.sendMessage(ChatColor.RED + "You no longer have enough money to enter this fight.");
                player1.sendMessage(ChatColor.RED + player2.getName() + " no longer has enough money. Fight canceled.");
                return false;
            }
        }

        if (wager > 0) {
            if (!tryWithdraw(player1, wager)) return false;
            if (!tryWithdraw(player2, wager)) {
                // Refund player1 if player2 can't pay, edge case
                deposit(player1, wager);
                return false;
            }
        }

        Arena arena = arenaManager.getAvailableArena();
        if (arena == null) {
            // Refund wagers if no arena available
            if (wager > 0) {
                deposit(player1, wager);
                deposit(player2, wager);
            }
            return false;
        }

        originalLocations.put(player1.getUniqueId(), player1.getLocation().clone());
        originalLocations.put(player2.getUniqueId(), player2.getLocation().clone());

        Fight fight = new Fight(player1, player2, arena, wager);
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

        if (wager > 0) {
            message += ChatColor.YELLOW + " with a wager of " + ChatColor.GOLD + wager;
        }

        fight.clearBets();

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

        double wager = fight.getWager();

        if (wager > 0) {
            deposit(winner, wager * 2);
            winner.sendMessage(ChatColor.GREEN + "You have won " + (wager * 2) + " from the wager!");
        }

        fight.resolveBets(winner.getName());

        String message = ChatColor.GOLD + winner.getName() +
                ChatColor.YELLOW + " has defeated " +
                ChatColor.RED + loser.getName() +
                ChatColor.YELLOW + " in arena " +
                ChatColor.GREEN + fight.getArena().getName();

        if (wager > 0) {
            message += ChatColor.YELLOW + " and won a wager of " + ChatColor.GOLD + (wager * 2);
        }

        message += ChatColor.YELLOW + "!";

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
        for (FightChallenge challenge : pendingChallenges.values()) {
            if (challenge.getChallengerId().equals(uuid) || challenge.getTargetId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean sendChallenge(Player challenger, Player target, double wager) {
        if (hasPendingChallenge(challenger) || hasPendingChallenge(target)) {
            return false;
        }

        FightChallenge challenge = new FightChallenge(challenger.getUniqueId(), target.getUniqueId(), wager);
        pendingChallenges.put(challenger.getUniqueId(), challenge);
        return true;
    }

    public boolean acceptChallenge(Player target) {
        UUID targetId = target.getUniqueId();
        FightChallenge foundChallenge = null;

        for (FightChallenge challenge : pendingChallenges.values()) {
            if (challenge.getTargetId().equals(targetId)) {
                foundChallenge = challenge;
                break;
            }
        }

        if (foundChallenge == null) return false;

        UUID challengerId = foundChallenge.getChallengerId();
        double wager = foundChallenge.getWager();

        Player challenger = plugin.getServer().getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            pendingChallenges.values().remove(foundChallenge);
            return false;
        }

        pendingChallenges.values().remove(foundChallenge);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (challenger.isOnline() && target.isOnline()) {
                challenger.sendMessage(ChatColor.YELLOW + "Fight starts in 15 seconds...");
                target.sendMessage(ChatColor.YELLOW + "Fight starts in 15 seconds...");
            }
        }, 20L * 15);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (challenger.isOnline() && target.isOnline()) {
                startFight(challenger, target, wager);
            }
        }, 20L * 30);

        return true;
    }

    public void cancelChallenge(Player player) {
        UUID uuid = player.getUniqueId();
        pendingChallenges.entrySet().removeIf(entry ->
                entry.getKey().equals(uuid) || entry.getValue().getTargetId().equals(uuid));
    }

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

    //ECO Wager per Challenge data
    private static class FightChallenge {
        private final UUID challengerId;
        private final UUID targetId;
        private final double wager;

        public FightChallenge(UUID challengerId, UUID targetId, double wager) {
            this.challengerId = challengerId;
            this.targetId = targetId;
            this.wager = wager;
        }

        public UUID getChallengerId() {
            return challengerId;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public double getWager() {
            return wager;
        }
    }
}
