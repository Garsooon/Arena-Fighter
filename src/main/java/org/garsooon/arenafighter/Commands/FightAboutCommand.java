package org.garsooon.arenafighter.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

//Information needs to be updated manually in ArenaFighter executor
//TODO find a way to automatically pull version from pom.xml
public class FightAboutCommand implements CommandExecutor {

    private final String version;
    private final String author;

    public FightAboutCommand(String version, String author) {
        this.version = version;
        this.author = author;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        player.sendMessage(ChatColor.GOLD + "=== ArenaFighter Plugin ===");
        player.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + version);
        player.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + author);

        return true;
    }
}
