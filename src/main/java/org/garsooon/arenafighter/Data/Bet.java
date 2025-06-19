package org.garsooon.arenafighter.Data;

public class Bet {
    private final String spectator;
    private final String fighter;
    private final double amount;

    //TODO pretty up code for betting into a betmanager, currently a mess of data in the fight class
    public Bet(String spectator, String fighter, double amount) {
        this.spectator = spectator;
        this.fighter = fighter;
        this.amount = amount;
    }

    public String getSpectator() {
        return spectator;
    }

    public String getFighter() {
        return fighter;
    }

    public double getAmount() {
        return amount;
    }
}
