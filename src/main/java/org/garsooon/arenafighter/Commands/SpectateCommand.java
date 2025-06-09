package org.garsooon.arenafighter.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.garsooon.arenafighter.Fight.FightManager;

public class SpectateCommand implements CommandExecutor {

    private final FightManager fightManager;

    public SpectateCommand(FightManager fightManager) {
        this.fightManager = fightManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("spectate")) {
            boolean started = fightManager.startSpectating(player);
            if (started) {
                player.sendMessage(ChatColor.YELLOW + "You are now spectating the arena.");
            } else {
                player.sendMessage(ChatColor.RED + "Spectator spawn not set or error occurred.");
            }
            return true;
        } else if (label.equalsIgnoreCase("stopspectate")) {
            boolean stopped = fightManager.stopSpectating(player);
            if (stopped) {
                player.sendMessage(ChatColor.YELLOW + "You stopped spectating and returned to your original location.");
            } else {
                player.sendMessage(ChatColor.RED + "You are not currently spectating.");
            }
            return true;
        }
        return false;
    }
}
