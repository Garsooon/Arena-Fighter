package org.garsooon.arenafighter.Fight;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.garsooon.arenafighter.Arena.Arena;
import org.garsooon.arenafighter.Data.Bet;
import org.garsooon.arenafighter.Economy.Methods;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Fight {

    private final Player player1;
    private final Player player2;
    private final Arena arena;
    private final long startTime;
    private final double wager;
    private final Map<String, Bet> bets = new HashMap<String, Bet>();
    private boolean started = false;
//    private boolean countingDown = false;

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

    // Return true if the name matches a fighter
    public boolean isFighter(String name) {
        return player1.getName().equalsIgnoreCase(name) || player2.getName().equalsIgnoreCase(name);
    }

    // Place a spectator bet; returns false if already placed
    public boolean placeBet(String spectator, String fighter, double amount) {
        if (bets.containsKey(spectator)) return false;
        bets.put(spectator, new Bet(spectator, fighter, amount));
        return true;
    }

    public boolean hasStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

//    public boolean isCountingDown() {
//        return countingDown;
//    }
//
//    public void setCountingDown(boolean countingDown) {
//        this.countingDown = countingDown;
//    }

    public void resolveBets(String winner) {
        double winnerPool = 0;
        double loserPool = 0;

        for (Bet bet : bets.values()) {
            if (bet.getFighter().equalsIgnoreCase(winner)) {
                winnerPool += bet.getAmount();
            } else {
                loserPool += bet.getAmount();
            }
        }

        if (winnerPool == 0) {
            // No one bet on the winner
            for (Bet bet : bets.values()) {
                Player p = Bukkit.getPlayer(bet.getSpectator());
                if (p != null) {
                    p.sendMessage(ChatColor.RED + "No one won the bets. Better luck next time!");
                }
            }
            return;
        }

        for (Bet bet : bets.values()) {
            Player p = Bukkit.getPlayer(bet.getSpectator());
            if (p == null) continue;

            if (bet.getFighter().equalsIgnoreCase(winner)) {
                double base = bet.getAmount(); // original bet
                double share = (bet.getAmount() / winnerPool) * loserPool;
                double payout = base + share;

                Methods.getMethod().depositPlayer(bet.getSpectator(), payout, p.getWorld());
                p.sendMessage(ChatColor.GOLD + "You won your bet on " + winner + "! You receive " + payout);
            } else {
                p.sendMessage(ChatColor.RED + "You lost your bet on " + bet.getFighter() + ".");
            }
        }
    }

    // Clear all stored spectator bets/Reset bet status
    public void clearBets() {
        bets.clear();
    }

    @Override
    public String toString() {
        return "Fight{" + player1.getName() + " vs " + player2.getName() +
                " in " + arena.getName() + " with wager " + wager + "}";
    }
}
