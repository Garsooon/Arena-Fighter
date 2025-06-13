package org.garsooon.arenafighter.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.garsooon.arenafighter.Commands.FightCommand;
import org.garsooon.arenafighter.Fight.Fight;
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
        Player quitter = event.getPlayer();

        // Cancel any ongoing fight
        if (fightManager.isInFight(quitter)) {
            // Get the fight the quitting player is in
            Fight fight = fightManager.getFight(quitter);
            if (fight != null) {
                // Get the other player as the winner
                //TODO hook into eco wagers to give the wager to winner & spectators that bet on winner
                Player winner = fight.getOtherPlayer(quitter);

                // Broadcast Quitter message
                if (winner != null && winner.isOnline()) {
                    fightManager.getPlugin().getServer().broadcastMessage(
                            winner.getName() + " &ewins! &f" + quitter.getName() + " &echickened out and left."
                    );
                }
            }

            fightManager.cancelFight(quitter);
        }

        // Cancel any pending challenges involving the player
        fightCommand.cancelChallengesInvolving(quitter.getName());
    }

}