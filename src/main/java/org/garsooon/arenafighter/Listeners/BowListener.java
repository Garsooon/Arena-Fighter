package org.garsooon.arenafighter.Listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.garsooon.arenafighter.Arena.ArenaFighter;


public class BowListener implements Listener {
    private final ArenaFighter plugin;

    public BowListener(ArenaFighter plugin) {
        this.plugin = plugin;
    }

    // some servers particularly RetroMC have some issues regarding bows that crashes the server.
    // This is a extreme but needed measure to stop crashes
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.isBowDisabled()
                && player.getItemInHand() != null
                && player.getItemInHand().getType() == Material.BOW
                && event.getAction().toString().contains("RIGHT"))
        {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Bows are currently disabled in Arena-Fighter!");
        }
    }
}