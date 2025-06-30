package org.garsooon.arenafighter.Economy;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import java.util.HashSet;
import java.util.Set;

/**
 * The <code>Methods</code> class initializes and manages economy Method implementations.
 * This version has been tailored for the ArenaFighter plugin with support for Essentials Economy.
 *
 * Originally created by Nijikokun. Modified by Garsooon.
 */
public class Methods {
    private static String version = null;
    private static boolean self = false;
    private static Method Method = null;
    public static String preferred = "";
    private static final Set<Method> Methods = new HashSet<>();
    private static final Set<String> Dependencies = new HashSet<>();
    private static final Set<Method> Attachables = new HashSet<>();

    static { _init(); }

    /**
     * Initializes supported economy methods.
     * The higher up the list the higher the priority if ran in conjunction with other economy plugins.
     * Add any additional economy providers here.
     * Make sure to uncomment any methods being added as well.
     */
    private static void _init() {
        addMethod("Essentials", new org.garsooon.arenafighter.Economy.methods.EE17());
        addMethod("ZCore", new org.garsooon.arenafighter.Economy.methods.ZCoreEco());
        Dependencies.add("MultiCurrency"); // Optional/future support
    }

    public static void setVersion(String v) {
        version = v;
    }

    public static void reset() {
        version = null;
        self = false;
        Method = null;
        preferred = "";
        Attachables.clear();
    }

    public static String getVersion() {
        return version;
    }

    public static Set<String> getDependencies() {
        return Dependencies;
    }

    public static Method createMethod(Plugin plugin) {
        for (Method method : Methods) {
            if (method.isCompatible(plugin)) {
                method.setPlugin(plugin);
                return method;
            }
        }
        return null;
    }

    private static void addMethod(String name, Method method) {
        Dependencies.add(name);
        Methods.add(method);
    }

    public static boolean hasMethod() {
        return Method != null;
    }

    /**
     * Scans the server's PluginManager for compatible economy plugins.
     * @param manager Bukkit PluginManager
     * @return true if an economy method was successfully attached
     */
    public static boolean setMethod(PluginManager manager) {
        if (hasMethod()) return true;
        if (self) { self = false; return false; }

        int count = 0;
        boolean match = false;

        for (String name : Dependencies) {
            if (hasMethod()) break;
            Plugin plugin = manager.getPlugin(name);
            if (plugin == null) continue;

            Method current = createMethod(plugin);
            if (current == null) continue;

            if (preferred.isEmpty()) {
                Method = current;
            } else {
                Attachables.add(current);
            }
        }

        if (!preferred.isEmpty()) {
            do {
                if (hasMethod()) {
                    match = true;
                } else {
                    for (Method attached : Attachables) {
                        if (attached == null) continue;
                        if (hasMethod()) {
                            match = true;
                            break;
                        }

                        if (preferred.isEmpty()) {
                            Method = attached;
                        }

                        if (count == 0) {
                            if (preferred.equalsIgnoreCase(attached.getName())) {
                                Method = attached;
                            }
                        } else {
                            Method = attached;
                        }
                    }
                    count++;
                }
            } while (!match);
        }

        return hasMethod();
    }

    /**
     * Returns the active economy method.
     * @return Method implementation or null
     */
    public static Method getMethod() {
        return Method;
    }

    /**
     * Checks if the economy plugin was disabled or removed.
     * @param method Plugin
     * @return true if the method was unset or incompatible
     */
    public static boolean checkDisabled(Plugin method) {
        if (!hasMethod()) return true;
        if (Method.isCompatible(method)) Method = null;
        return (Method == null);
    }
}
