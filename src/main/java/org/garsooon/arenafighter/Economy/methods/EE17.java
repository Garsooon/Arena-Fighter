package org.garsooon.arenafighter.Economy.methods;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.garsooon.arenafighter.Economy.Method;

import java.math.BigDecimal;

public class EE17 implements Method {
    private Essentials essentials;

    public Essentials getPlugin() {
        return this.essentials;
    }

    public String getName() {
        return "Essentials";
    }

    public String getVersion() {
        return "2.2";
    }

    public int fractionalDigits() {
        return -1;
    }

    public String format(double amount) {
        return "$" + String.format("%.2f", amount);
    }

    public boolean hasBanks() {
        return false;
    }

    public boolean hasBank(String bank, World world) {
        return false;
    }

    public boolean hasAccount(String name, World world) {
        return essentials.getUserMap().getUser(name) != null;
    }

    public boolean hasBankAccount(String bank, String name, World world) {
        return false;
    }

    public MethodAccount getAccount(String name, World world) {
        if (!hasAccount(name, world)) return null;
        return new EEcoAccount(name, essentials);
    }

    public MethodBankAccount getBankAccount(String bank, String name, World world) {
        return null;
    }

    public boolean isCompatible(Plugin plugin) {
        return plugin instanceof Essentials;
    }

    public void setPlugin(Plugin plugin) {
        this.essentials = (Essentials) plugin;
    }

    public static class EEcoAccount implements MethodAccount {
        private final String name;
        private final Essentials essentials;

        public EEcoAccount(String name, Essentials essentials) {
            this.name = name;
            this.essentials = essentials;
        }

        private User getUser() {
            return essentials.getUserMap().getUser(name);
        }

        public double balance(World world) {
            User user = getUser();
            return user != null ? user.getMoney() : 0.0;
        }

        public boolean set(double amount, World world) {
            User user = getUser();
            if (user == null) return false;

            user.setMoney(amount);
            return true;
        }

        public boolean add(double amount, World world) {
            User user = getUser();
            if (user == null) return false;

            user.giveMoney(amount);
            return true;
        }

        public boolean subtract(double amount, World world) {
            User user = getUser();
            if (user == null) return false;

            user.takeMoney(amount);
            return true;
        }


        public boolean multiply(double amount, World world) {
            double current = balance(world);
            return set(current * amount, world);
        }

        public boolean divide(double amount, World world) {
            if (amount == 0) return false;
            double current = balance(world);
            return set(current / amount, world);
        }

        public boolean hasEnough(double amount, World world) {
            return balance(world) >= amount;
        }

        public boolean hasOver(double amount, World world) {
            return balance(world) > amount;
        }

        public boolean hasUnder(double amount, World world) {
            return balance(world) < amount;
        }

        public boolean isNegative(World world) {
            return balance(world) < 0;
        }

        public boolean remove() {
            return false;
        }
    }
}
