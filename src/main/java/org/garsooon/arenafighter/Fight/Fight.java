package org.garsooon.arenafighter.Fight;

import org.bukkit.entity.Player;
import org.garsooon.arenafighter.Arena.Arena;

public class Fight {

    private final Player player1;
    private final Player player2;
    private final Arena arena;
    private final long startTime;
    private final double wager; // Added wager field

    public Fight(Player player1, Player player2, Arena arena) {
        this(player1, player2, arena, 0.0); // default wager = 0.0
    }

    public Fight(Player player1, Player player2, Arena arena, double wager) {
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
        this.startTime = System.currentTimeMillis();
        this.wager = wager;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Arena getArena() {
        return arena;
    }

    public long getStartTime() {
        return startTime;
    }

    public double getWager() {
        return wager;
    }

    public Player getOtherPlayer(Player player) {
        if (player.equals(player1)) {
            return player2;
        } else if (player.equals(player2)) {
            return player1;
        }
        return null;
    }

    public boolean containsPlayer(Player player) {
        return player.equals(player1) || player.equals(player2);
    }

    @Override
    public String toString() {
        return "Fight{" + player1.getName() + " vs " + player2.getName() +
                " in " + arena.getName() + " with wager " + wager + "}";
    }
}
