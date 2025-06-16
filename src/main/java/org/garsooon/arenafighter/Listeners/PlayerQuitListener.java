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

        if (fightManager.isInFight(quitter)) {
            Fight fight = fightManager.getFight(quitter);
            if (fight != null) {
                Player winner = fight.getOtherPlayer(quitter);
                if (winner != null && winner.isOnline()) {
                    fightManager.getPlugin().getServer().broadcastMessage(
                            winner.getName() + " &ewins! &f" + quitter.getName() + " &echickened out and left."
                    );
                }
            }

            fightManager.cancelFight(quitter);
            fightManager.punishQuitter(quitter); // apply punishment
        }

        fightCommand.cancelChallengesInvolving(quitter.getName());
    }
}
