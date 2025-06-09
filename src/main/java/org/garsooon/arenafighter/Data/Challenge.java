package org.garsooon.arenafighter.Data;

public class Challenge {
    private final String challengerName;
    private final long timestamp;

    public Challenge(String challengerName, long timestamp) {
        this.challengerName = challengerName;
        this.timestamp = timestamp;
    }

    public String getChallengerName() {
        return challengerName;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
