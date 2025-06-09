package org.garsooon.arenafighter.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.garsooon.arenafighter.Fight.Fight;
import org.garsooon.arenafighter.Fight.FightManager;

public class PlayerDeathListener implements Listener {

    private final FightManager fightManager;

    public PlayerDeathListener(FightManager fightManager) {
        this.fightManager = fightManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player deadPlayer = (Player) event.getEntity();

        if (!fightManager.isInFight(deadPlayer)) {
            return;
        }

        Fight fight = fightManager.getFight(deadPlayer);
        if (fight == null) {
            return;
        }

        Player winner = fight.getOtherPlayer(deadPlayer);
        if (winner == null) {
            return;
        }

        fightManager.endFight(winner, deadPlayer);

        // Cant suppress death message in without a functioning playerdeathevent, deprecated
        // event.setDeathMessage(null);
    }
}
