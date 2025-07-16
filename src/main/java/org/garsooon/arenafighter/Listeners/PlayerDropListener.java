package org.garsooon.arenafighter.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.entity.Player;
import org.garsooon.arenafighter.Fight.FightManager;

public class PlayerDropListener implements Listener {

    private final FightManager fightManager;

    public PlayerDropListener(FightManager fightManager) {
        this.fightManager = fightManager;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (fightManager.isInFight(player) || fightManager.isInPostFightCooldown(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou can't drop items during a fight!");
        }
    }
}
