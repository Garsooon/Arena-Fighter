package org.garsooon.arenafighter.Arena;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Arena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private boolean occupied;
    private final List<Player> spectators;

    public Arena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.occupied = false;
        this.spectators = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Location getSpawn1() {
        return spawn1.clone();
    }

    public Location getSpawn2() {
        return spawn2.clone();
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    // --- Spectator Methods ---

    public void addSpectator(Player player) {
        if (!spectators.contains(player)) {
            spectators.add(player);
        }
    }

    private Location spectatorSpawn;

    public void setSpectatorSpawn(Location location) {
        this.spectatorSpawn = location;
    }

    public void setSpawn1(Location location) {this.spawn1 = location;}

    public void setSpawn2(Location location) {this.spawn2 = location;}

    public Location getSpectatorSpawn() {
        return spectatorSpawn != null ? spectatorSpawn.clone() : null;
    }

    public void removeSpectator(Player player) {
        spectators.remove(player);
    }

    public List<Player> getSpectators() {
        return Collections.unmodifiableList(spectators);
    }

    public boolean isSpectating(Player player) {
        return spectators.contains(player);
    }

    @Override
    public String toString() {
        return "Arena{name='" + name + "', occupied=" + occupied + ", spectators=" + spectators.size() + "}";
    }
}
