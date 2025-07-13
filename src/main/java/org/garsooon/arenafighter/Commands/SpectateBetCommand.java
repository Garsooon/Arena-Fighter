package org.garsooon.arenafighter.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.garsooon.arenafighter.Fight.FightManager;
import org.garsooon.arenafighter.Fight.Fight;
import org.garsooon.arenafighter.Economy.Methods;
import org.garsooon.arenafighter.Economy.Method;
import org.garsooon.arenafighter.Data.Bet;

public class SpectateBetCommand implements CommandExecutor {

    private final FightManager fightManager;
    private final Method economy;

    public SpectateBetCommand(FightManager fightManager) {
        this.fightManager = fightManager;
        this.economy = Methods.getMethod();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (economy == null) {
            player.sendMessage(ChatColor.RED + "Economy plugin not detected. Betting is disabled.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bet <fighter> <amount>");
            return true;
        }

        String fighterName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
            amount = Bet.roundDownTwoDecimals(amount);
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "Bet amount must be greater than 0.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount.");
            return true;
        }

        Fight fight = null;
        for (Fight f : fightManager.getActiveFights()) {
            if (f.isFighter(fighterName)) {
                fight = f;
                break;
            }
        }

        if (fight == null) {
            player.sendMessage(ChatColor.RED + "There is no active fight with that fighter.");
            return true;
        }

        if (fight.hasStarted()) {
            player.sendMessage(ChatColor.RED + "You cannot bet, the fight has already started.");
            return true;
        }

        if (!economy.hasEnough(player.getName(), amount, player.getWorld())) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to bet that amount.");
            return true;
        }

        if (fight.containsPlayer(player)) {
            player.sendMessage(ChatColor.RED + "You cannot bet on a fight you are participating in.");
            return true;
        }

        if (!fight.placeBet(player.getName(), fighterName, amount)) {
            player.sendMessage(ChatColor.RED + "You have already placed a bet.");
            return true;
        }

        if (!economy.withdrawPlayer(player.getName(), amount, player.getWorld())) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw funds.");
            return true;
        }
        String msg = ChatColor.AQUA + player.getName() +
                ChatColor.YELLOW + " placed a bet of " +
                ChatColor.GOLD + amount +
                ChatColor.YELLOW + " on " +
                ChatColor.GREEN + fighterName + ChatColor.YELLOW + "!";
        player.getServer().broadcastMessage(msg);

        return true;
    }
}
