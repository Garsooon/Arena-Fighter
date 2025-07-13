package org.garsooon.arenafighter.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public static double roundDownTwoDecimals(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(2, RoundingMode.DOWN); // truncate to 2 decimal places, no rounding up
        return bd.doubleValue();
    }
}
