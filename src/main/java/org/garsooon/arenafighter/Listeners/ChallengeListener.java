package org.garsooon.arenafighter.Listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.garsooon.arenafighter.Commands.FightCommand;

public class ChallengeListener implements Listener {

    private final FightCommand fightCommand;

    public ChallengeListener(FightCommand fightCommand) {
        this.fightCommand = fightCommand;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();
        fightCommand.cancelChallengesInvolving(playerName);
    }
}
