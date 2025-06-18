package org.garsooon.arenafighter.Economy.methods;

import org.garsooon.arenafighter.Economy.Method;
import com.earth2me.essentials.api.Economy;
import com.johnymuffin.beta.fundamentals.api.EconomyAPI;
import com.johnymuffin.beta.fundamentals.api.FundamentalsAPI;
import com.projectposeidon.api.PoseidonUUID;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

import static com.johnymuffin.beta.fundamentals.util.Utils.getUUIDFromUsername;

/**
 * Essentials 17 Implementation of Method
 *
 * @author Nijikokun <nijikokun@shortmail.com> (@nijikokun)
 * @author Snowleo
 * @author Acrobot
 * @author KHobbits
 * @copyright (c) 2011
 * @license AOL license <http://aol.nexua.org>
 */
public class Fundamentals implements Method {
    private Fundamentals fundamentals;

    public Fundamentals getPlugin() {
        return this.fundamentals;
    }

    public String getName() {
        return "Fundamentals";
    }

    public String getVersion() {
        return "1.0.6";
    }

    public int fractionalDigits() {
        return -1;
    }

    public String format(double amount) {
        return amount + " Currency";
    }

    public boolean hasBanks() {
        return false;
    }

    public boolean hasBank(String bank, World world) {
        return false;
    }

    public boolean hasAccount(String name, World world) {
        return Economy.playerExists(name);
    }

    public boolean hasBankAccount(String bank, String name, World world) {
        return false;
    }

    public MethodAccount getAccount(String name, World world) {
        if (!hasAccount(name, world)) return null;
        return new EEcoAccount(name);
    }

    public MethodBankAccount getBankAccount(String bank, String name, World world) {
        return null;
    }

    public boolean isCompatible(Plugin plugin) {
        try {
            Class.forName("com.johnymuffin.beta.fundamentals.api.EconomyAPI");
        } catch (Exception e) {
            return false;
        }

        return plugin.getDescription().getName().equalsIgnoreCase("fundamentals") && plugin instanceof Fundamentals;
    }

    public void setPlugin(Plugin plugin) {
        fundamentals = (Fundamentals) plugin;
    }

    public static class EEcoAccount implements MethodAccount {
        private String name;
        private UUID uuid;

        public EEcoAccount(String name) {
            this.name = name;
            this.uuid = getUUIDFromUsername(this.name);
            if (this.uuid == null) {
                this.uuid = PoseidonUUID.getPlayerGracefulUUID(this.name);
            }
        }

        public double balance(World world) {
            Double balance = 0.0;

            try {
                EconomyAPI.BalanceWrapper balanceWrapper = FundamentalsAPI.getEconomy().getBalance(uuid, world.getName());
                if (balanceWrapper.getEconomyResult() == EconomyAPI.EconomyResult.successful) {
                    balance = balanceWrapper.getBalance();
                } else {
                    System.out.println("[REGISTER] Failed to grab balance from Fundamentals for " + uuid + ": " + balanceWrapper.getEconomyResult().toString());
                }
            } catch (Exception ex) {
                System.out.println("[REGISTER] Exception occurred with Fundamentals Economy: " + ex.getMessage());
            }

            return balance;
        }

        public boolean set(double amount, World world) {

            try {
                EconomyAPI.EconomyResult balanceWrapper = FundamentalsAPI.getEconomy().setBalance(uuid, amount, world.getName());
                if (balanceWrapper == EconomyAPI.EconomyResult.successful) {
                    return true;
                } else {
                    System.out.println("[REGISTER] Failed to set balance from Fundamentals for " + uuid + ": " + balanceWrapper.toString());
                    return false;
                }
            } catch (Exception ex) {
                System.out.println("[REGISTER] Exception occurred with Fundamentals Economy: " + ex.getMessage());
                return false;
            }


//            return false;
        }

        public boolean add(double amount, World world) {
            return set(balance(world) + amount, world);
        }

        public boolean subtract(double amount, World world) {
            return set(balance(world) - amount, world);
        }

        public boolean multiply(double amount, World world) {
            return set(balance(world)*amount, world);
        }

        public boolean divide(double amount, World world) {
            return set(balance(world)/amount, world);
        }

        public boolean hasEnough(double amount, World world) {
            return amount <= balance(world);
        }

        public boolean hasOver(double amount, World world) {
            return amount <= balance(world);
        }

        public boolean hasUnder(double amount, World world) {
            return amount > balance(world);
        }

        public boolean isNegative(World world) {
            return false; //Negative balances should never exist in Fundamentals
        }

        public boolean remove() {
            return false;
        }
    }
}