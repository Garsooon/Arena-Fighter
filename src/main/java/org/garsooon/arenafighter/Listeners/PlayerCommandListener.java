package org.garsooon.arenafighter.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.Listener;
import org.garsooon.arenafighter.Arena.ArenaFighter;
import org.garsooon.arenafighter.Fight.FightManager;


public class PlayerCommandListener implements Listener {

    private final ArenaFighter plugin;
    private final FightManager fightManager;

    public PlayerCommandListener(ArenaFighter plugin, FightManager fightManager) {
        this.plugin = plugin;
        this.fightManager = fightManager;
    }

    @org.bukkit.event.EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        if (fightManager.isInFight(player)) {
            for (String blocked : plugin.getBlockedCommands()) {
                if (message.startsWith(blocked.toLowerCase())) {
                    player.sendMessage(ChatColor.RED + "You cannot use this command while fighting!");
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
