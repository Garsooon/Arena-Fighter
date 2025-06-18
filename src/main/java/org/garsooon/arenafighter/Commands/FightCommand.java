package org.garsooon.arenafighter.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.garsooon.arenafighter.Data.Challenge;
import org.garsooon.arenafighter.Economy.Method;
import org.garsooon.arenafighter.Economy.Methods;
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
            challenger.sendMessage(ChatColor.RED + "Usage: /fight challenge <player> [wagerAmount]");
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

        if (fightManager.isPunished(target)) {
            challenger.sendMessage(ChatColor.RED + "This player has left a match recently and is not");
            challenger.sendMessage(ChatColor.RED + "allowed to duel until their punishment is over.");
            return true;
        }

        long remainingMillis = fightManager.getRemainingPunishment(challenger);
        if (remainingMillis > 0) {
            long minutes = (remainingMillis / 1000) / 60;
            long seconds = (remainingMillis / 1000) % 60;
            challenger.sendMessage(ChatColor.RED + "You are temporarily blocked from fighting due to leaving a");
            challenger.sendMessage(ChatColor.RED + "match for " + ChatColor.YELLOW + minutes + "m " + seconds + "s" + ChatColor.RED + ".");
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

        // Parse optional wager
        double wagerAmount = 0.0;
        if (args.length >= 3) {
            try {
                wagerAmount = Double.parseDouble(args[2]);
                if (wagerAmount < 0) {
                    challenger.sendMessage(ChatColor.RED + "Wager amount cannot be negative.");
                    return true;
                }
            } catch (NumberFormatException e) {
                challenger.sendMessage(ChatColor.RED + "Invalid wager amount.");
                return true;
            }

            Method economy = Methods.getMethod();
            if (economy == null) {
                challenger.sendMessage(ChatColor.RED + "Economy plugin not found. Wagers are disabled.");
                return true;
            }

            // Failsafe for if challenge send fund check fails
            if (!economy.hasEnough(challenger.getName(), wagerAmount, challenger.getWorld())) {
                challenger.sendMessage(ChatColor.RED + "You do not have enough funds to make this wager.");
                return true;
            }

            if (!economy.hasEnough(target.getName(), wagerAmount, target.getWorld())) {
                challenger.sendMessage(ChatColor.RED + target.getName() + " does not have enough funds to accept this wager.");
                return true;
            }
        }

        // Store challenge for target
        Challenge challenge = new Challenge(challenger.getName(), System.currentTimeMillis(), wagerAmount);
        pendingChallenges.put(target.getName(), challenge);

        challenger.sendMessage(ChatColor.YELLOW + "Challenge sent to " + ChatColor.WHITE + target.getName());
        if (wagerAmount > 0) {
            target.sendMessage(ChatColor.YELLOW + "You've been challenged by " + ChatColor.WHITE + challenger.getName() +
                    ChatColor.YELLOW + " with a wager of " + ChatColor.GREEN + wagerAmount + ChatColor.YELLOW + "!");
        } else {
            target.sendMessage(ChatColor.YELLOW + "You've been challenged by " + ChatColor.WHITE + challenger.getName() + ChatColor.YELLOW + "!");
        }
        target.sendMessage(ChatColor.GREEN + "Use " + ChatColor.WHITE + "/fight accept " + challenger.getName() + ChatColor.GREEN + " to accept.");

        int taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(
                plugin,
                () -> {
                    Challenge active = pendingChallenges.get(target.getName());
                    if (active != null && active.getChallengerName().equals(challenger.getName())) {
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
            accepter.sendMessage(ChatColor.RED + "Usage: /fight accept <player> <wager>");
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

        double wager = challenge.getWagerAmount();

        // Balance check for wagers
        if (wager > 0) {
            if (!fightManager.hasSufficientFunds(challenger, wager)) {
                accepter.sendMessage(ChatColor.RED + "The challenger doesn't have enough funds to cover the wager.");
                return true;
            }
            if (!fightManager.hasSufficientFunds(accepter, wager)) {
                accepter.sendMessage(ChatColor.RED + "You don't have enough funds to accept this wager.");
                return true;
            }
        }

        // Broadcast fight accept with wager message when there is a wager
        String line1 = ChatColor.GOLD + accepter.getName() + ChatColor.YELLOW + " has accepted " +
                ChatColor.GOLD + challenger.getName() + ChatColor.YELLOW + "'s duel request";
        String line2 = (wager > 0 ? ChatColor.YELLOW + " with a wager of " + ChatColor.GREEN + wager : "") + ChatColor.YELLOW + "!";
        String line3 = ChatColor.YELLOW + "The fight will begin in 30 seconds...";

        Bukkit.broadcastMessage(line1);
        Bukkit.broadcastMessage(line2);
        Bukkit.broadcastMessage(line3);

        int fifteenSecondsTicks = 20 * 15;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            challenger.sendMessage(ChatColor.YELLOW + "Fight starting in 15 seconds...");
            accepter.sendMessage(ChatColor.YELLOW + "Fight starting in 15 seconds...");
        }, fifteenSecondsTicks);

        int thirtySecondsTicks = 20 * 30;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            boolean success = fightManager.startFight(challenger, accepter, wager);
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
        player.sendMessage(ChatColor.YELLOW + "/fight challenge <player> <wager>" + ChatColor.WHITE + " - Challenge a player");
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
