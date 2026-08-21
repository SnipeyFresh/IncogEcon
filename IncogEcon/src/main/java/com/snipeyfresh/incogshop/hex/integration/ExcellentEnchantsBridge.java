package com.snipeyfresh.incogshop.hex.integration;

import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Reflection bridge to ExcellentEnchants.
 *
 * ExcellentEnchants registers its custom enchants into Bukkit's enchantment
 * registry as proper {@link Enchantment} objects, so item-level operations
 * (add / check level) work through the standard Bukkit API. Reflection is used
 * only to enumerate all EE enchants and to read their display names.
 */
public final class ExcellentEnchantsBridge {
    private final List<Enchantment> enchants = new ArrayList<>();
    private final Map<String, String> displayNames = new LinkedHashMap<>();
    private Method getDisplayName; // (int level) -> String, on the EE enchant object

    public boolean init() {
        enchants.clear();
        displayNames.clear();
        getDisplayName = null;

        Plugin ee = Bukkit.getPluginManager().getPlugin("ExcellentEnchants");
        if (ee == null || !ee.isEnabled()) return false;

        // Attempt to get all EE enchants through the plugin's own manager
        Object manager = null;
        for (String name : new String[]{"getEnchantManager", "getManager", "enchants"}) {
            manager = Reflect.call(Reflect.method(ee.getClass(), new String[]{name}), ee);
            if (manager != null) break;
        }

        if (manager != null) {
            Object values = null;
            for (String name : new String[]{"getValues", "getEnchants", "values", "getAll"}) {
                values = Reflect.call(Reflect.method(manager.getClass(), new String[]{name}), manager);
                if (values instanceof Collection<?>) break;
            }
            if (values instanceof Collection<?> col) {
                for (Object obj : col) {
                    if (obj instanceof Enchantment ench) {
                        enchants.add(ench);
                        if (getDisplayName == null) {
                            getDisplayName = Reflect.looseMethod(obj.getClass(),
                                    new String[]{"getDisplayName", "getFormattedName", "formatName"}, 1);
                        }
                    }
                }
            }
        }

        // Fallback: scan the Bukkit registry for any enchants under EE's namespace
        if (enchants.isEmpty()) {
            try {
                for (Enchantment e : Registry.ENCHANTMENT) {
                    String ns = e.getKey().getNamespace();
                    if (ns.contains("excellent") || ns.equalsIgnoreCase("ee")) enchants.add(e);
                }
            } catch (Throwable ignored) {}
        }

        // Populate display names cache
        for (Enchantment e : enchants) {
            String name = null;
            if (getDisplayName != null) {
                Object r = Reflect.call(getDisplayName, e, 1);
                if (r instanceof String s && !s.isBlank()) name = s;
            }
            if (name == null) name = prettyKey(e.getKey().getKey());
            displayNames.put(e.getKey().toString(), name);
        }

        return !enchants.isEmpty();
    }

    public boolean available() { return !enchants.isEmpty(); }

    /** All EE enchants compatible with the given item. */
    public List<Enchantment> compatibleEnchants(ItemStack item) {
        if (item == null) return List.of();
        List<Enchantment> out = new ArrayList<>();
        for (Enchantment e : enchants) {
            try { if (e.canEnchantItem(item)) out.add(e); } catch (Throwable ignored) {}
        }
        return out;
    }

    /** Display name for an EE enchant (no level suffix). */
    public String displayName(Enchantment ench) {
        return displayNames.getOrDefault(ench.getKey().toString(), prettyKey(ench.getKey().getKey()));
    }

    private static String prettyKey(String key) {
        String[] parts = key.replace('-', '_').split("_");
        StringJoiner j = new StringJoiner(" ");
        for (String p : parts) if (!p.isEmpty()) j.add(Character.toUpperCase(p.charAt(0)) + p.substring(1));
        return j.toString();
    }
}
