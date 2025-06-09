package org.garsooon.arenafighter.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.garsooon.arenafighter.Commands.FightCommand;
import org.garsooon.arenafighter.Fight.FightManager;

public class PlayerQuitListener implements Listener {

    private final FightManager fightManager;
    private final FightCommand fightCommand;

    public PlayerQuitListener(FightManager fightManager, FightCommand fightCommand) {
        this.fightManager = fightManager;
        this.fightCommand = fightCommand;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Cancel any ongoing fight
        if (fightManager.isInFight(player)) {
            fightManager.cancelFight(player);
        }

        // Cancel any pending challenges involving the player
        fightCommand.cancelChallengesInvolving(player.getName());
    }
}