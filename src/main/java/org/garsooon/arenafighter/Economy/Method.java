package org.garsooon.arenafighter.Economy;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Interface to be implemented by a payment method.
 *
 * Original author: Nijikokun <nijikokun@shortmail.com> (@nijikokun)
 * Modified by: garsooon for ArenaFighter wager integration
 */
public interface Method {
    public Object getPlugin();
    public String getName();
    public String getVersion();
    public int fractionalDigits();
    public String format(double amount);
    public boolean hasBanks();
    public boolean hasBank(String bank, World world);
    public boolean hasAccount(String name, World world);
    public boolean hasBankAccount(String bank, String name, World world);
    public MethodAccount getAccount(String name, World world);
    public MethodBankAccount getBankAccount(String bank, String name, World world);
    public boolean isCompatible(Plugin plugin);
    public void setPlugin(Plugin plugin);

    /**
     * 🚀 NEW: Convenience method to get player balance
     */
    default double getBalance(String name, World world) {
        MethodAccount acc = getAccount(name, world);
        return acc != null ? acc.balance(world) : 0.0;
    }

    /**
     * 🚀 NEW: Check if player has enough funds
     */
    default boolean hasEnough(String name, double amount, World world) {
        MethodAccount acc = getAccount(name, world);
        return acc != null && acc.hasEnough(amount, world);
    }

    /**
     * 🚀 NEW: Withdraw funds from a player's account
     */
    default boolean withdrawPlayer(String name, double amount, World world) {
        MethodAccount acc = getAccount(name, world);
        return acc != null && acc.subtract(amount, world);
    }

    /**
     * 🚀 NEW: Deposit funds into a player's account
     */
    default boolean depositPlayer(String name, double amount, World world) {
        MethodAccount acc = getAccount(name, world);
        return acc != null && acc.add(amount, world);
    }

    /**
     * Contains Calculator and Balance functions for Accounts.
     */
    public interface MethodAccount {
        public double balance(World world);
        public boolean set(double amount, World world);
        public boolean add(double amount, World world);
        public boolean subtract(double amount, World world);
        public boolean multiply(double amount, World world);
        public boolean divide(double amount, World world);
        public boolean hasEnough(double amount, World world);
        public boolean hasOver(double amount, World world);
        public boolean hasUnder(double amount, World world);
        public boolean isNegative(World world);
        public boolean remove();

        @Override
        public String toString();
    }

    /**
     * Contains Calculator and Balance functions for Bank Accounts.
     */
    public interface MethodBankAccount {
        public double balance();
        public String getBankName();
        public int getBankId();
        public boolean set(double amount);
        public boolean add(double amount);
        public boolean subtract(double amount);
        public boolean multiply(double amount);
        public boolean divide(double amount);
        public boolean hasEnough(double amount);
        public boolean hasOver(double amount);
        public boolean hasUnder(double amount);
        public boolean isNegative();
        public boolean remove();

        @Override
        public String toString();
    }
}
