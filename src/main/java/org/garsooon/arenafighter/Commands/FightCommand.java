package org.garsooon.arenafighter.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.garsooon.arenafighter.Data.Challenge;
import org.garsooon.arenafighter.Fight.FightManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FightCommand implements CommandExecutor {

    private final Plugin plugin;
    private final FightManager fightManager;
    private final Map<String, Challenge> pendingChallenges = new HashMap<>();
    private final Map<String, Integer> timeoutTasks = new HashMap<>();
    private final long TIMEOUT_TICKS = 20 * 30; // 30 seconds

    public FightCommand(Plugin plugin, FightManager fightManager) {
        this.plugin = plugin;
        this.fightManager = fightManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "challenge":
                return handleChallenge(player, args);
            case "accept":
                return handleAccept(player, args);
            case "cancel":
                return handleCancel(player);
            case "help":
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleChallenge(Player challenger, String[] args) {
        if (args.length < 2) {
            challenger.sendMessage(ChatColor.RED + "Usage: /fight challenge <player>");
            return true;
        }

        if (fightManager.isInFight(challenger)) {
            challenger.sendMessage(ChatColor.RED + "You're already in a fight!");
            return true;
        }

        Player target = challenger.getServer().getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            challenger.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        if (target.equals(challenger)) {
            challenger.sendMessage(ChatColor.RED + "You can't challenge yourself.");
            return true;
        }

        if (fightManager.isInFight(target)) {
            challenger.sendMessage(ChatColor.RED + target.getName() + " is already in a fight.");
            return true;
        }

        if (hasPendingChallenge(challenger.getName()) || hasPendingChallenge(target.getName())) {
            challenger.sendMessage(ChatColor.RED + "Either you or " + target.getName() + " already has a pending challenge.");
            return true;
        }

        // Store challenge for target
        pendingChallenges.put(target.getName(), new Challenge(challenger.getName(), System.currentTimeMillis()));
        challenger.sendMessage(ChatColor.YELLOW + "Challenge sent to " + ChatColor.WHITE + target.getName());
        target.sendMessage(ChatColor.YELLOW + "You've been challenged by " + ChatColor.WHITE + challenger.getName() + ChatColor.YELLOW + "!");
        target.sendMessage(ChatColor.GREEN + "Use " + ChatColor.WHITE + "/fight accept " + challenger.getName() + ChatColor.GREEN + " to accept.");

        // Schedule timeout to remove challenge if no response after 30 seconds
        int taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                plugin,
                () -> {
                    Challenge challenge = pendingChallenges.get(target.getName());
                    if (challenge != null && challenge.getChallengerName().equals(challenger.getName())) {
                        pendingChallenges.remove(target.getName());
                        timeoutTasks.remove(target.getName());
                        challenger.sendMessage(ChatColor.RED + target.getName() + " didn't respond to your duel request.");
                        target.sendMessage(ChatColor.RED + "You didn't respond to " + challenger.getName() + "'s duel request.");
                    }
                },
                TIMEOUT_TICKS
        );
        timeoutTasks.put(target.getName(), taskId);

        return true;
    }

    private boolean hasPendingChallenge(String playerName) {
        if (pendingChallenges.containsKey(playerName)) {
            return true;
        }
        for (Challenge challenge : pendingChallenges.values()) {
            if (challenge.getChallengerName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleAccept(Player accepter, String[] args) {
        if (args.length < 2) {
            accepter.sendMessage(ChatColor.RED + "Usage: /fight accept <player>");
            return true;
        }

        String challengerName = args[1];
        Player challenger = accepter.getServer().getPlayer(challengerName);
        Challenge challenge = pendingChallenges.get(accepter.getName());

        if (challenger == null || !challenger.isOnline()) {
            accepter.sendMessage(ChatColor.RED + "Player not found: " + challengerName);
            return true;
        }

        if (challenge == null || !challenge.getChallengerName().equalsIgnoreCase(challengerName)) {
            accepter.sendMessage(ChatColor.RED + "No pending challenge from " + challengerName);
            return true;
        }

        pendingChallenges.remove(accepter.getName());
        cancelTimeout(accepter.getName());

        if (fightManager.isInFight(challenger) || fightManager.isInFight(accepter)) {
            accepter.sendMessage(ChatColor.RED + "Either you or " + challengerName + " is already in a fight.");
            return true;
        }

        // Broadcast to server on accept
        Bukkit.broadcastMessage(ChatColor.GOLD + accepter.getName() + ChatColor.YELLOW + " has accepted " +
                ChatColor.GOLD + challenger.getName() + ChatColor.YELLOW + "'s duel request! The fight will begin in 30 seconds...");

        // Schedule private message to players at 15 seconds into wait
        int fifteenSecondsTicks = 20 * 15;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            challenger.sendMessage(ChatColor.YELLOW + "Fight starting in 15 seconds...");
            accepter.sendMessage(ChatColor.YELLOW + "Fight starting in 15 seconds...");
        }, fifteenSecondsTicks);

        // Schedule fight start at 30 seconds
        int thirtySecondsTicks = 20 * 30;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            boolean success = fightManager.startFight(challenger, accepter);
            if (!success) {
                accepter.sendMessage(ChatColor.RED + "Could not start fight. No available arenas.");
                challenger.sendMessage(ChatColor.RED + "Could not start fight. No available arenas.");
            }
        }, thirtySecondsTicks);

        return true;
    }

    private boolean handleCancel(Player player) {
        if (!fightManager.isInFight(player)) {
            player.sendMessage(ChatColor.RED + "You're not in a fight.");
            return true;
        }

        fightManager.cancelFight(player);
        player.sendMessage(ChatColor.YELLOW + "Fight cancelled.");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Arena Fighter Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/fight challenge <player>" + ChatColor.WHITE + " - Challenge a player");
        player.sendMessage(ChatColor.YELLOW + "/fight accept <player>" + ChatColor.WHITE + " - Accept a challenge");
        player.sendMessage(ChatColor.YELLOW + "/fight cancel" + ChatColor.WHITE + " - Cancel current fight");
        player.sendMessage(ChatColor.YELLOW + "/fight help" + ChatColor.WHITE + " - Show help");
        player.sendMessage(ChatColor.YELLOW + "/spectate <arena>" + ChatColor.WHITE + " - Start spectating");
    }

    private void cancelTimeout(String name) {
        Integer taskId = timeoutTasks.remove(name);
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    public void cancelChallengesInvolving(String playerName) {
        Iterator<Map.Entry<String, Challenge>> iterator = pendingChallenges.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Challenge> entry = iterator.next();
            String receiver = entry.getKey();
            String challenger = entry.getValue().getChallengerName();

            if (receiver.equalsIgnoreCase(playerName)) {
                Player challengerPlayer = plugin.getServer().getPlayer(challenger);
                if (challengerPlayer != null && challengerPlayer.isOnline()) {
                    challengerPlayer.sendMessage(ChatColor.RED + "Your challenge to " + receiver + " was cancelled because they left the server.");
                }
                cancelTimeout(receiver);
                iterator.remove();
            } else if (challenger.equalsIgnoreCase(playerName)) {
                Player receiverPlayer = plugin.getServer().getPlayer(receiver);
                if (receiverPlayer != null && receiverPlayer.isOnline()) {
                    receiverPlayer.sendMessage(ChatColor.RED + "Challenge from " + challenger + " was cancelled because they left the server.");
                }
                cancelTimeout(receiver);
                iterator.remove();
            }
        }
    }
}
