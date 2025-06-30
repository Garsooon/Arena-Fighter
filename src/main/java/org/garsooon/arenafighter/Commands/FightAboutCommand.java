package org.garsooon.arenafighter.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

public class FightAboutCommand implements CommandExecutor {

    private final String version;
    private final String author;

    public FightAboutCommand(JavaPlugin plugin) {
        PluginDescriptionFile desc = plugin.getDescription();
        this.version = desc.getVersion();
        this.author = desc.getAuthors().isEmpty() ? "Garsooon" : desc.getAuthors().get(0);
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
