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

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bet <fighter> <amount>");
            return true;
        }

        String fighterName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
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

        //FUTURE USE
        if (fight.hasStarted()) {
            player.sendMessage(ChatColor.RED + "You cannot bet, the fight has already started.");
            return true;
        }

        if (!economy.hasEnough(player.getName(), amount, player.getWorld())) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to bet that amount.");
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

        player.sendMessage(ChatColor.GREEN + "You placed a bet of " + amount + " on " + fighterName + ".");
        return true;
    }
}
