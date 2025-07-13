package org.garsooon.arenafighter.Fight;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityPlayer;
import com.legacyminecraft.poseidon.util.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.garsooon.arenafighter.Arena.Arena;
import org.garsooon.arenafighter.Arena.ArenaFighter;
import org.garsooon.arenafighter.Arena.ArenaManager;
import org.garsooon.arenafighter.Data.Bet;
import org.garsooon.arenafighter.Data.Challenge;
import org.garsooon.arenafighter.Data.PlayerDataManager;
import org.garsooon.arenafighter.Economy.Method;
import org.garsooon.arenafighter.Economy.Methods;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class FightManager {

    private final ArenaFighter plugin;
    private final ArenaManager arenaManager;
    private final Map<UUID, Fight> activeFights;
    private final Map<UUID, Location> originalLocations;
    private final Map<UUID, ItemStack[]> originalInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> originalArmor = new HashMap<>();
    private final HashMap<UUID, Integer> postFightCooldowns = new HashMap<>();
    private final Map<UUID, FightChallenge> pendingChallenges;
    private final Map<UUID, Location> spectatorOriginalLocations;
    private final Map<UUID, Long> punishments = new HashMap<>();
    private final long punishmentDurationMillis;
    private final Method economy;
    private final File statsFile;
    private Map<String, Object> stats; // Key: UUID.toString()
    private final PlayerDataManager playerDataManager = new PlayerDataManager();

    public FightManager(ArenaFighter plugin, ArenaManager arenaManager, Method economy) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.economy = economy;
        this.activeFights = new HashMap<>();
        this.originalLocations = new HashMap<>();
        this.pendingChallenges = new HashMap<>();
        this.spectatorOriginalLocations = new HashMap<>();
        this.punishmentDurationMillis = loadPunishmentDuration(plugin.getDataFolder());
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        loadStats();
    }

    private long loadPunishmentDuration(File dataFolder) {
        File configFile = new File(dataFolder, "config.yml");
        long defaultMinutes = 5;

        if (!configFile.exists()) return defaultMinutes * 60 * 1000;

        try (FileInputStream input = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(input);

            if (data instanceof Map) {
                Map<?, ?> root = (Map<?, ?>) data;
                Object punishmentObj = root.get("punishment");

                if (punishmentObj instanceof Map) {
                    Map<?, ?> punishmentMap = (Map<?, ?>) punishmentObj;
                    Object minutesObj = punishmentMap.get("duration-minutes");

                    if (minutesObj instanceof Number) {
                        return ((Number) minutesObj).longValue() * 60 * 1000;
                    }
                }
            }
        } catch (Exception e) {
            plugin.getServer().getLogger().warning("Failed to load punishment duration: " + e.getMessage());
        }

        return defaultMinutes * 60 * 1000;
    }

    public void punishQuitter(Player quitter) {
        long expireAt = System.currentTimeMillis() + punishmentDurationMillis;
        punishments.put(quitter.getUniqueId(), expireAt);
    }

    public long getRemainingPunishment(Player player) {
        Long expireTime = punishments.get(player.getUniqueId());
        if (expireTime == null) return 0;
        return Math.max(0, expireTime - System.currentTimeMillis());
    }

    public boolean isPunished(Player player) {
        UUID uuid = player.getUniqueId();
        Long expire = punishments.get(uuid);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            punishments.remove(uuid);
            return false;
        }
        return true;
    }

    public long getPunishmentDurationMillis() {
        return punishmentDurationMillis;
    }

    public int getPunishmentDurationMinutes() {
        return (int) (punishmentDurationMillis / 1000 / 60);
    }

    public ArenaFighter getPlugin() {
        return plugin;
    }

    // Helpers for ECO function via Method interface
    private boolean tryWithdraw(Player player, double amount) {
        if (amount <= 0) return true;
        boolean success = economy.withdrawPlayer(player.getName(), amount, player.getWorld());
        if (!success) {
            player.sendMessage(ChatColor.RED + "You do not have enough money to wager " + amount);
        }
        return success;
    }

    public void deposit(Player player, double amount) {
        if (amount <= 0) return;
        economy.depositPlayer(player.getName(), amount, player.getWorld());
    }

    public boolean hasSufficientFunds(Player player, double amount) {
        return economy != null && economy.hasEnough(player.getName(), amount, player.getWorld());
    }

    public Fight getFightByArena(String arenaName) {
        for (Fight fight : activeFights.values()) {
            if (fight.getArena().getName().equalsIgnoreCase(arenaName)) {
                return fight;
            }
        }
        return null;
    }

    public Collection<Fight> getActiveFights() {
        return activeFights.values();
    }

    private void ejectPlayer(Player player) {
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }

        CraftPlayer cp = (CraftPlayer) player;
        EntityHuman eh = cp.getHandle();
        if (eh.sleeping) {
            eh.a(true, true, true);
        }
    }

    public boolean startFight(Player player1, Player player2, double wager) {
        double truncatedWager = Bet.roundDownTwoDecimals(wager);

        if (isInFight(player1) || isInFight(player2)) return false;

        // Final balance check before fight begins
        if (wager > 0) {
            if (!hasSufficientFunds(player1, wager)) {
                player1.sendMessage(ChatColor.RED + "You no longer have enough money to enter this fight.");
                player2.sendMessage(ChatColor.RED + player1.getName() + " no longer has enough money. Fight canceled.");
                return false;
            }
            if (!hasSufficientFunds(player2, wager)) {
                player2.sendMessage(ChatColor.RED + "You no longer have enough money to enter this fight.");
                player1.sendMessage(ChatColor.RED + player2.getName() + " no longer has enough money. Fight canceled.");
                return false;
            }
        }

        if (wager > 0) {
            if (!tryWithdraw(player1, wager)) return false;
            if (!tryWithdraw(player2, wager)) {
                // Refund player1 if player2 can't pay, edge case
                deposit(player1, wager);
                return false;
            }
        }

        Arena arena = arenaManager.getAvailableArena();
        if (arena == null) {
            // Refund wagers if no arena available
            if (wager > 0) {
                deposit(player1, wager);
                deposit(player2, wager);
            }
            return false;
        }

        originalLocations.put(player1.getUniqueId(), player1.getLocation().clone());
        originalLocations.put(player2.getUniqueId(), player2.getLocation().clone());

        Fight fight = new Fight(player1, player2, arena, wager);
        activeFights.put(player1.getUniqueId(), fight);
        activeFights.put(player2.getUniqueId(), fight);

        arenaManager.occupyArena(arena);

        forceCloseInventory(player1);
        forceCloseInventory(player2);

        // TODO call inFight when scheduling to be able to cancel on PlayerQuitListener
        // Check if both players are still online before starting the fight
        if (!player1.isOnline() || !player2.isOnline()) {
            // Refund wagers if any
            if (wager > 0) {
                deposit(player1, wager);
                deposit(player2, wager);
            }

            activeFights.remove(player1.getUniqueId());
            activeFights.remove(player2.getUniqueId());

            arenaManager.releaseArena(arena);

            plugin.getServer().broadcastMessage(ChatColor.YELLOW + "Fight between "
                    + ChatColor.GOLD + player1.getName() + ChatColor.YELLOW + " and "
                    + ChatColor.GOLD + player2.getName() + ChatColor.YELLOW + " was canceled because a player left.");

            return false;
        }

        ejectPlayer(player1);
        ejectPlayer(player2);

        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        healAndFeedPlayer(player1);
        healAndFeedPlayer(player2);

        String message = ChatColor.YELLOW + "Fight started! " +
                ChatColor.WHITE + player1.getName() +
                ChatColor.YELLOW + " vs " +
                ChatColor.WHITE + player2.getName() +
                ChatColor.YELLOW + " in arena " +
                ChatColor.GREEN + arena.getName();

        if (wager > 0) {
            message += ChatColor.YELLOW + " with a wager of " + ChatColor.GOLD + truncatedWager;
        }

        fight.clearBets();

        plugin.getServer().broadcastMessage(message);
        return true;
    }

    public void endFight(Player winner, Player loser) {
        Fight fight = activeFights.get(winner.getUniqueId());
        if (fight == null) return;

        activeFights.remove(winner.getUniqueId());
        activeFights.remove(loser.getUniqueId());

        arenaManager.releaseArena(fight.getArena());

        startPostFightCooldown(winner);
        startPostFightCooldown(loser);

//        forceCloseInventory(winner);
//        forceCloseInventory(loser);

        UUID loserId = loser.getUniqueId();
        ItemStack[] inventory = loser.getInventory().getContents().clone();
        ItemStack[] armor = loser.getInventory().getArmorContents().clone();

        ItemStack cursor = getItemOnCursor(loser);
        if (cursor != null && cursor.getTypeId() != 0) {
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i] == null || inventory[i].getTypeId() == 0) {
                    inventory[i] = cursor;
                    break;
                }
            }
            setItemOnCursor(loser, new ItemStack(0));
        }

        originalInventories.put(loserId, inventory);
        originalArmor.put(loserId, armor);

        Location winnerOriginal = originalLocations.remove(winner.getUniqueId());
        Location loserOriginal = originalLocations.remove(loser.getUniqueId());

        if (winnerOriginal != null) {
            winner.teleport(winnerOriginal);
            healAndFeedPlayer(winner);
        }

        if (loserOriginal != null) {
            loser.teleport(loserOriginal);
            restoreOriginalInventoryAndArmor(loser);
            healAndFeedPlayer(loser);
        }

        stopAllSpectators();

        double wager = fight.getWager();

        if (wager > 0) {
            double truncatedWager = Bet.roundDownTwoDecimals(wager);
            deposit(winner, wager * 2);
            winner.sendMessage(ChatColor.GREEN + "You have won " + (wager * 2) + " from the wager!");
        }

        fight.resolveBets(winner.getName());

        incrementStat(winner.getUniqueId(), "wins", winner.getName());
        incrementStat(loser.getUniqueId(), "losses", loser.getName());

        String message = ChatColor.GOLD + winner.getName() +
                ChatColor.YELLOW + " has defeated " +
                ChatColor.RED + loser.getName() +
                ChatColor.YELLOW + " in arena " +
                ChatColor.GREEN + fight.getArena().getName();

        if (wager > 0) {
            double truncatedWager = Bet.roundDownTwoDecimals(wager * 2);
            message += ChatColor.YELLOW + " and won a wager of " + ChatColor.GOLD + truncatedWager;
        }

        message += ChatColor.YELLOW + "!";

        plugin.getServer().broadcastMessage(message);
    }

    // Boy I sure do love not having itemStackCursor in poseidon :clueless:
    // This function is getting more and more bloated as I have to do alot of hacky methods to prevent duping item stack
    // fill items in reverse so if a player edits stack values but still keeps one of an item in the saved slot # it won't revert under stacks to the original value
    // This will print to the console if the inventory doesn't match the original inventory
    // Check for total stack across exceptions then if total amount doesn't = saved amount add back to either missing stack or existing
    // Items may be "Ate" if the item is held in the cursor during transition to arena, forceCloseInventory should mitigate this, but I really can't do much without itemStackCursor
    //TODO rewrite ALL of this if and when Inventory API gets merged for poseidon
    private void restoreOriginalInventoryAndArmor(final Player player) {
        final UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                final ItemStack[] savedInventory = originalInventories.remove(uuid);
                final ItemStack[] savedArmor = originalArmor.remove(uuid);

                if (savedInventory != null) {
                    player.getInventory().clear();

                    // Fill inventory backwards to reduce stacking dupes
                    for (int i = savedInventory.length - 1; i >= 0; i--) {
                        ItemStack item = savedInventory[i];
                        player.getInventory().setItem(i, item != null ? item.clone() : null);
                    }
                }

                if (savedArmor != null) {
                    player.getInventory().setArmorContents(savedArmor);
                }

                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        player.updateInventory();

                        // Shouldn't run now that inventories are saved on fight end.
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                boolean inventoryMismatch = !deepInventoryMatch(player.getInventory().getContents(), savedInventory);
                                boolean armorMismatch = !deepInventoryMatch(player.getInventory().getArmorContents(), savedArmor);

                                if (inventoryMismatch || armorMismatch) {
                                    player.sendMessage(ChatColor.RED + "Your inventory failed to restore properly. Retrying...");

                                    //Debug
                                    if (savedInventory != null) {
                                        ItemStack[] current = player.getInventory().getContents();
                                        for (int i = 0; i < savedInventory.length; i++) {
                                            if (!deepItemEquals(savedInventory[i], current[i])) {
                                                plugin.getServer().getLogger().warning("[ArenaFighter] Inventory slot mismatch at " + i + " for player " + player.getName());
                                                plugin.getServer().getLogger().warning("Expected: " + itemToString(savedInventory[i]));
                                                plugin.getServer().getLogger().warning("Found: " + itemToString(current[i]));
                                            }
                                        }
                                    }

                                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                                        @Override
                                        public void run() {
                                            player.getInventory().clear();
                                            for (int i = savedInventory.length - 1; i >= 0; i--) {
                                                ItemStack item = savedInventory[i];
                                                player.getInventory().setItem(i, item != null ? item.clone() : null);
                                            }

                                            if (savedArmor != null) {
                                                player.getInventory().setArmorContents(savedArmor);
                                            }

                                            player.updateInventory();

                                            // Final check for total item quantity mismatch
                                            Map<String, Integer> expected = countItemQuantities(savedInventory);
                                            Map<String, Integer> actual = countItemQuantities(player.getInventory().getContents());

                                            for (Map.Entry<String, Integer> entry : expected.entrySet()) {
                                                String key = entry.getKey();
                                                int expectedAmount = entry.getValue();
                                                int actualAmount = actual.getOrDefault(key, 0);

                                                if (actualAmount < expectedAmount) {
                                                    int missing = expectedAmount - actualAmount;
                                                    String[] split = key.split(":");
                                                    int typeId = Integer.parseInt(split[0]);
                                                    short damage = Short.parseShort(split[1]);

                                                    ItemStack stack = new ItemStack(typeId, missing, damage);
                                                    player.getInventory().addItem(stack);

                                                    // Debug log -defunct
                                                    plugin.getServer().getLogger().warning("[ArenaFighter] Mismatch recovery for " + player.getName() +
                                                            ": added back " + missing + " of ItemStack{typeId=" + typeId + ", damage=" + damage + "} " +
                                                            "(expected=" + expectedAmount + ", actual=" + actualAmount + ")");
                                                }
                                            }


                                            player.updateInventory();
                                        }
                                    }, 2L);
                                }
                            }
                        }, 2L);
                    }
                }, 2L);
            }
        }, 3L);
    }

    // Inventory helpers start

    public ItemStack getItemOnCursor(Player player) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        net.minecraft.server.ItemStack nms = ep.inventory.j();
        return (nms != null) ? new CraftItemStack(nms) : null;
    }

    public void setItemOnCursor(Player player, ItemStack item) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();

        if (item == null || item.getTypeId() == 0) {
            ep.inventory.b((net.minecraft.server.ItemStack) null);
        } else {
            net.minecraft.server.ItemStack nmsStack =
                    new net.minecraft.server.ItemStack(item.getTypeId(), item.getAmount(), item.getDurability());
            ep.inventory.b(nmsStack);
        }

        ep.z();
    }

    private Map<String, Integer> countItemQuantities(ItemStack[] contents) {
        Map<String, Integer> countMap = new HashMap<>();
        if (contents == null) return countMap;

        for (ItemStack item : contents) {
            if (item == null || item.getTypeId() == 0) continue;
            String key = item.getTypeId() + ":" + item.getDurability();
            countMap.put(key, countMap.getOrDefault(key, 0) + item.getAmount());
        }
        return countMap;
    }


    private String itemToString(ItemStack item) {
        if (item == null) return "null";
        return "ItemStack{" + item.getType() + " x " + item.getAmount() + "}";
    }

    private boolean deepInventoryMatch(ItemStack[] current, ItemStack[] original) {
        if (current == null || original == null || current.length != original.length) return false;

        for (int i = 0; i < current.length; i++) {
            ItemStack a = current[i];
            ItemStack b = original[i];

            if (!deepItemEquals(a, b)) return false;
        }

        return true;
    }

    private boolean deepItemEquals(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;

        if (a.getTypeId() != b.getTypeId()) return false;
        if (a.getAmount() != b.getAmount()) return false;
        if (a.getDurability() != b.getDurability()) return false;

        return true;
    }

    private boolean isInventoryMatch(ItemStack[] a, ItemStack[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            ItemStack itemA = a[i];
            ItemStack itemB = b[i];
            if (!Objects.equals(itemA, itemB)) return false;
        }
        return true;
    }

    public void startPostFightCooldown(Player player) {
        postFightCooldowns.put(player.getUniqueId(), 100); // 5 second cool down
        scheduleCooldownTask(player);
    }

    private void scheduleCooldownTask(final Player player) {
        final UUID uuid = player.getUniqueId();

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                Integer remaining = postFightCooldowns.get(uuid);
                if (remaining == null) {
                    return;
                }

                if (remaining <= 1) {
                    postFightCooldowns.remove(uuid);
                } else {
                    postFightCooldowns.put(uuid, remaining - 1);
                }
            }
        }, 1L, 1L);
    }

    public boolean isInPostFightCooldown(Player player) {
        return postFightCooldowns.containsKey(player.getUniqueId());
    }

    private void forceCloseInventory(Player player) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();

        // Set player's active container to inventory
        ep.activeContainer = ep.defaultContainer;

        // Reset windowId for activeContainer which I set previously
        ep.activeContainer.windowId = 0;

        // Send packet to client to close window
        ep.netServerHandler.sendPacket(new net.minecraft.server.Packet101CloseWindow(0));
    }

    // inventory helpers end

    public void cancelFight(Player player) {
        Fight fight = activeFights.get(player.getUniqueId());
        if (fight == null) return;

        Player otherPlayer = fight.getOtherPlayer(player);

        activeFights.remove(player.getUniqueId());
        activeFights.remove(otherPlayer.getUniqueId());

        arenaManager.releaseArena(fight.getArena());

        Location playerOriginal = originalLocations.remove(player.getUniqueId());
        Location otherOriginal = originalLocations.remove(otherPlayer.getUniqueId());

        if (playerOriginal != null) {
            player.teleport(playerOriginal);
            healAndFeedPlayer(player);
        }

        if (otherOriginal != null) {
            otherPlayer.teleport(otherOriginal);
            healAndFeedPlayer(otherPlayer);
        }

        stopAllSpectators();

        String message = ChatColor.RED + "Fight cancelled!";
        player.sendMessage(message);
        otherPlayer.sendMessage(message);
    }

    public boolean isInFight(Player player) {
        return activeFights.containsKey(player.getUniqueId());
    }

    public Fight getFight(Player player) {
        return activeFights.get(player.getUniqueId());
    }

    private void healAndFeedPlayer(Player player) {
        player.setHealth(20);
    }

    public void cleanup() {
        for (Fight fight : activeFights.values()) {
            Player player1 = fight.getPlayer1();
            Player player2 = fight.getPlayer2();

            Location loc1 = originalLocations.get(player1.getUniqueId());
            Location loc2 = originalLocations.get(player2.getUniqueId());

            if (loc1 != null && player1.isOnline()) {
                player1.teleport(loc1);
            }

            if (loc2 != null && player2.isOnline()) {
                player2.teleport(loc2);
            }

            arenaManager.releaseArena(fight.getArena());
        }

        activeFights.clear();
        originalLocations.clear();
        stopAllSpectators();
    }

    public boolean hasPendingChallenge(Player player) {
        UUID uuid = player.getUniqueId();
        for (FightChallenge challenge : pendingChallenges.values()) {
            if (challenge.getChallengerId().equals(uuid) || challenge.getTargetId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean sendChallenge(Player challenger, Player target, double wager) {
        if (hasPendingChallenge(challenger) || hasPendingChallenge(target)) {
            return false;
        }

        FightChallenge challenge = new FightChallenge(challenger.getUniqueId(), target.getUniqueId(), wager);
        pendingChallenges.put(challenger.getUniqueId(), challenge);
        return true;
    }

    public boolean acceptChallenge(Player target) {
        UUID targetId = target.getUniqueId();
        FightChallenge foundChallenge = null;

        for (FightChallenge challenge : pendingChallenges.values()) {
            if (challenge.getTargetId().equals(targetId)) {
                foundChallenge = challenge;
                break;
            }
        }

        if (foundChallenge == null) return false;

        UUID challengerId = foundChallenge.getChallengerId();
        double wager = foundChallenge.getWager();

        Player challenger = plugin.getServer().getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            pendingChallenges.values().remove(foundChallenge);
            return false;
        }

        pendingChallenges.values().remove(foundChallenge);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (challenger.isOnline() && target.isOnline()) {
                challenger.sendMessage(ChatColor.YELLOW + "Fight starts in 15 seconds...");
                target.sendMessage(ChatColor.YELLOW + "Fight starts in 15 seconds...");
            }
        }, 20L * 15);

        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (challenger.isOnline() && target.isOnline()) {
                startFight(challenger, target, wager);
            }
        }, 20L * 30);

        return true;
    }

    public void cancelChallenge(Player player) {
        UUID uuid = player.getUniqueId();
        pendingChallenges.entrySet().removeIf(entry ->
                entry.getKey().equals(uuid) || entry.getValue().getTargetId().equals(uuid));
    }

    public boolean startSpectating(Player player) {
        UUID uuid = player.getUniqueId();
        if (spectatorOriginalLocations.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already spectating.");
            return false;
        }

        Fight fight = null;
        for (Fight f : activeFights.values()) {
            if (!f.getPlayer1().getUniqueId().equals(uuid) && !f.getPlayer2().getUniqueId().equals(uuid)) {
                fight = f;
                break;
            }
        }

        if (fight == null) {
            player.sendMessage(ChatColor.RED + "No ongoing fight to spectate.");
            return false;
        }

        Arena arena = fight.getArena();
        Location specSpawn = arena.getSpectatorSpawn();

        if (specSpawn == null) {
            player.sendMessage(ChatColor.RED + "No spectator spawn set for this arena.");
            return false;
        }

        spectatorOriginalLocations.put(uuid, player.getLocation().clone());
        player.teleport(specSpawn);
        player.sendMessage(ChatColor.YELLOW + "You are now spectating the fight between " +
                fight.getPlayer1().getName() + " and " + fight.getPlayer2().getName() + ".");
        return true;
    }

    public boolean startSpectating(Player player, String arenaName) {
        UUID uuid = player.getUniqueId();

        if (spectatorOriginalLocations.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "You are already spectating.");
            return false;
        }

        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' does not exist.");
            return false;
        }

        Location specSpawn = arena.getSpectatorSpawn();
        if (specSpawn == null) {
            player.sendMessage(ChatColor.RED + "Spectator spawn is not set for arena '" + arenaName + "'.");
            return false;
        }

        spectatorOriginalLocations.put(uuid, player.getLocation().clone());
        player.teleport(specSpawn);
        player.sendMessage(ChatColor.YELLOW + "You are now spectating arena: " + ChatColor.AQUA + arenaName);
        player.sendMessage(ChatColor.YELLOW + "Use /spectate to return to your original location.");
        return true;
    }

    public boolean stopSpectating(Player player) {
        UUID uuid = player.getUniqueId();
        Location original = spectatorOriginalLocations.remove(uuid);

        if (original != null) {
            player.teleport(original);
            player.sendMessage(ChatColor.YELLOW + "Returned from spectating.");
            return true;
        }

        player.sendMessage(ChatColor.RED + "You are not spectating.");
        return false;
    }

    public void stopAllSpectators() {
        for (UUID uuid : new HashMap<>(spectatorOriginalLocations).keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                stopSpectating(player);
            }
        }
    }

    public boolean isSpectating(Player player) {
        return spectatorOriginalLocations.containsKey(player.getUniqueId());
    }

    //ECO Wager per Challenge data start
    private static class FightChallenge {
        private final UUID challengerId;
        private final UUID targetId;
        private final double wager;

        public FightChallenge(UUID challengerId, UUID targetId, double wager) {
            this.challengerId = challengerId;
            this.targetId = targetId;
            this.wager = wager;
        }

        public UUID getChallengerId() {
            return challengerId;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public double getWager() {
            return wager;
        }
    }
    //ECO Wager per Challenge data end

    //Stat and leaderboard functions start
    public static int getInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void loadStats() {
        stats = new HashMap<>();
        if (!statsFile.exists()) return;

        try (FileInputStream fis = new FileInputStream(statsFile)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(fis);
            if (data instanceof Map) {
                stats = (Map<String, Object>) data;
            }
        } catch (Exception e) {
            plugin.getServer().getLogger().warning("Failed to load stats.yml: " + e.getMessage());
        }
    }

    private void saveStats() {
        try (FileWriter writer = new FileWriter(statsFile)) {
            Yaml yaml = new Yaml();
            yaml.dump(stats, writer);
        } catch (Exception e) {
            plugin.getServer().getLogger().warning("Failed to save stats.yml: " + e.getMessage());
        }
    }

    //I LOVE SUPPRESS WARNINGS I LOVE SUPPRESS WARNINGS I LOVE SUPPRESS WARNINGS, GOD I LOVE SUPPRESS WARNINGS
    @SuppressWarnings("unchecked")
    public void incrementStat(UUID uuid, String key, String username) {
        String uuidKey = uuid.toString();

        Map<String, Object> playerStats = (Map<String, Object>) stats.get(uuidKey);
        if (playerStats == null) {
            playerStats = new HashMap<>();
            stats.put(uuidKey, playerStats);
        }

        playerStats.put("username", username);

        PlayerDataManager.setPlayer(uuid, username);

        int current = 0;
        Object val = playerStats.get(key);
        if (val instanceof Number) {
            current = ((Number) val).intValue();
        }

        playerStats.put(key, current + 1);
        saveStats();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getTopPlayersByWins() {
        Map<String, Integer> winsMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            String uuidStr = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Map<String, Object> playerStats = (Map<String, Object>) value;
                Object winsObj = playerStats.get("wins");

                if (winsObj instanceof Number) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);

                        String name = PlayerDataManager.getUsername(uuid);
                        if (name == null) {
                            name = uuidStr; // fallback raw UUID
                        }

                        winsMap.put(name, ((Number) winsObj).intValue());
                    } catch (IllegalArgumentException ignored) {

                    }
                }
            }
        }

        return winsMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    @SuppressWarnings("unchecked")
    public List<String> getPlayerStats(UUID uuid) {
        Object raw = stats.get(uuid.toString());

        if (!(raw instanceof Map)) {
            return Collections.singletonList(ChatColor.RED + "No stats found for " + uuid + ".");
        }

        // Checked type, safe to cast. In other words I LOVE SUPPRESS WARNING!!!11!
        Map<String, Object> playerStats = (Map<String, Object>) raw;

        int wins = getInt(playerStats.get("wins"));
        int losses = getInt(playerStats.get("losses"));

        // Resolve username from PlayerDataManager or fallback to a UUID string
        String displayName = PlayerDataManager.getUsername(uuid);
        if (displayName == null) {
            displayName = uuid.toString();
        }

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "=== Stats for " + ChatColor.AQUA + displayName + ChatColor.GOLD + " ===");
        lines.add(ChatColor.YELLOW + "Wins: " + ChatColor.GREEN + wins);
        lines.add(ChatColor.YELLOW + "Losses: " + ChatColor.RED + losses);

        return lines;
    }

    // For /fight stats <name>
    @SuppressWarnings("unchecked")
    public List<String> getPlayerStatsByName(String name) {
        UUID uuid;
        try {
            uuid = UUIDFetcher.getUUIDOf(name);
        } catch (Exception e) {
            return Collections.singletonList(ChatColor.RED + "Could not resolve player: " + name);
        }

        if (uuid == null) {
            return Collections.singletonList(ChatColor.RED + "Could not resolve player: " + name);
        }

        Object raw = stats.get(uuid.toString());
        if (!(raw instanceof Map)) {
            return Collections.singletonList(ChatColor.RED + "No stats found for " + name + ".");
        }

        Map<String, Object> playerStats = (Map<String, Object>) raw;
        int wins = getInt(playerStats.get("wins"));
        int losses = getInt(playerStats.get("losses"));

        // Resolve username using PlayerDataManager or fallback to input name
        String displayName = PlayerDataManager.getUsername(uuid);
        if (displayName == null) {
            displayName = name;
        }

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "=== Stats for " + ChatColor.AQUA + displayName + ChatColor.GOLD + " ===");
        lines.add(ChatColor.YELLOW + "Wins: " + ChatColor.GREEN + wins);
        lines.add(ChatColor.YELLOW + "Losses: " + ChatColor.RED + losses);

        return lines;
    }
    // Stat and leaderboard functions end

}
