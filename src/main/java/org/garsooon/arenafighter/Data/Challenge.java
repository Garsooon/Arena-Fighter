package org.garsooon.arenafighter.Data;

public class Challenge {
    private final String challengerName;
    private final long timestamp;
    private final double wagerAmount;

    public Challenge(String challengerName, long timestamp, double wagerAmount) {
        this.challengerName = challengerName;
        this.timestamp = timestamp;
        this.wagerAmount = wagerAmount;
    }

    // Old contructor
//    public Challenge(String challengerName, long timestamp) {
//        this(challengerName, timestamp, 0.0);
//    }

    public String getChallengerName() {
        return challengerName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getWagerAmount() {
        return wagerAmount;
    }
}
