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

        if (fightManager.isSpectating(player)) {
            // If already spectating, stop and return
            boolean stopped = fightManager.stopSpectating(player);
            if (stopped) {
                player.sendMessage(ChatColor.YELLOW + "You have stopped spectating and returned to your original location.");
            } else {
                player.sendMessage(ChatColor.RED + "An error occurred while stopping spectating.");
            }
            return true;
        }

        // Start spectating: requires arena name for multi-arena support
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /spectate <arena>");
            return true;
        }

        String arenaName = args[0];
        boolean started = fightManager.startSpectating(player, arenaName);
        if (!started) {
            player.sendMessage(ChatColor.RED + "Arena not found or spectator spawn not set.");
        }

        return true;
    }
}
