package org.garsooon.arenafighter.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Utility class to try and stop the feature bloat in FightManager
public class PlayerDataManager {
    private static final Map<UUID, String> uuidToUsername = new HashMap<>();
    private static final Map<String, UUID> usernameToUUID = new HashMap<>();

    public static void setPlayer(UUID uuid, String username) {
        if (uuid == null || username == null) return;

        uuidToUsername.put(uuid, username);
        usernameToUUID.put(username.toLowerCase(), uuid);
    }

    public static String getUsername(UUID uuid) {
        return uuidToUsername.get(uuid);
    }

    public static UUID getUUID(String username) {
        return usernameToUUID.get(username.toLowerCase());
    }

    public static boolean hasPlayer(UUID uuid) {
        return uuidToUsername.containsKey(uuid);
    }

    public static boolean hasUsername(String name) {
        return usernameToUUID.containsKey(name.toLowerCase());
    }

    public static Map<UUID, String> getAllUUIDToUsername() {
        return new HashMap<>(uuidToUsername);
    }

    public static void clear() {
        uuidToUsername.clear();
        usernameToUUID.clear();
    }
}
