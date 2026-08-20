package com.snipeyfresh.incogshop.hex.integration;

import com.snipeyfresh.incogshop.gui.GuiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Support for Auxilor's Reforges plugin.
 *
 * <p>When it is installed the Hex reforge station lists that plugin's reforges
 * and applies them through its own API, so stat calculation, lore, and any
 * reforge stones keep working exactly as that plugin defines them. IncogEcon's
 * built-in reforges are only used when no reforge plugin is present.</p>
 */
public final class ReforgesIntegration implements ReforgeIntegration {
    private final Class<?> reforgesClass;
    private final Class<?> utilsClass;
    private final Method valuesMethod;
    private final Method getReforge;
    private final Method setReforge;
    // Resolved from the first reforge object seen, then reused: scanning every
    // reforge's methods on each menu open is needlessly expensive.
    private Method targetsMethod;
    private Method targetItemsMethod;

    public ReforgesIntegration() {
        this.reforgesClass = Reflect.findClass("com.willfp.reforges.reforges.Reforges");
        this.utilsClass = Reflect.findClass(
                "com.willfp.reforges.util.ReforgeUtils",
                "com.willfp.reforges.reforges.util.ReforgeUtils");
        this.valuesMethod = Reflect.looseMethod(reforgesClass, new String[]{"values"}, 0);
        this.getReforge = Reflect.looseMethod(utilsClass, new String[]{"getReforge"}, 1);
        this.setReforge = Reflect.looseMethod(utilsClass, new String[]{"setReforge"}, 2);
    }

    @Override public String id() { return "reforges"; }
    @Override public String pluginName() { return "Reforges"; }

    @Override
    public boolean available() {
        return Bukkit.getPluginManager().getPlugin("Reforges") != null
                && reforgesClass != null && utilsClass != null && valuesMethod != null && setReforge != null;
    }

    @Override
    public List<String> options(ItemStack item) {
        if (!available() || item == null) return List.of();
        List<String> ids = new ArrayList<>();
        for (Object reforge : reforges()) {
            if (!targets(reforge, item)) continue;
            String name = Reflect.nameOf(reforge);
            if (name != null && !ids.contains(name)) ids.add(name);
        }
        return ids;
    }

    @Override
    public String current(ItemStack item) {
        if (!available() || getReforge == null) return null;
        Object reforge = Reflect.invokeWithItem(getReforge, item);
        return reforge == null ? null : Reflect.nameOf(reforge);
    }

    @Override
    public boolean apply(ItemStack item, String reforgeId) {
        if (!available() || reforgeId == null) return false;
        Object target = byId(reforgeId);
        if (target == null) return false;
        Reflect.invokeWithItem(setReforge, item, target);
        String applied = current(item);
        // Older builds have no readable getter; treat a silent apply as success.
        return applied == null || applied.equalsIgnoreCase(reforgeId);
    }

    @Override
    public boolean claims(ItemStack item) {
        return current(item) != null;
    }

    @Override
    public List<String> describe(ItemStack item) {
        String current = current(item);
        return current == null ? List.of() : List.of(GuiTheme.stat("Reforge (Reforges)", current));
    }

    private Object byId(String reforgeId) {
        for (Object reforge : reforges()) {
            String name = Reflect.nameOf(reforge);
            if (name != null && name.equalsIgnoreCase(reforgeId)) return reforge;
        }
        return null;
    }

    private List<Object> reforges() {
        Object values = Reflect.call(valuesMethod, null);
        if (values instanceof Collection<?> collection) return new ArrayList<>(collection);
        if (values instanceof Object[] array) return new ArrayList<>(List.of(array));
        return List.of();
    }

    /**
     * Asks the reforge which materials it applies to. When that shape is not
     * recognisable the reforge is offered anyway and the plugin itself decides.
     */
    private boolean targets(Object reforge, ItemStack item) {
        if (targetsMethod == null) targetsMethod = Reflect.looseMethod(reforge.getClass(), new String[]{"getTargets"}, 0);
        Object targets = Reflect.call(targetsMethod, reforge);
        if (!(targets instanceof Collection<?> collection) || collection.isEmpty()) return true;
        Material material = item.getType();
        for (Object target : collection) {
            if (targetItemsMethod == null) {
                targetItemsMethod = Reflect.looseMethod(target.getClass(), new String[]{"getItems", "getMaterials"}, 0);
            }
            Object items = Reflect.call(targetItemsMethod, target);
            if (!(items instanceof Collection<?> materials)) return true;
            for (Object candidate : materials) {
                if (candidate == material) return true;
                if (candidate != null && candidate.toString().equalsIgnoreCase(material.name())) return true;
            }
        }
        return false;
    }

    @Override
    public String display(String reforgeId) {
        if (reforgeId == null || reforgeId.isBlank()) return "None";
        String lower = reforgeId.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
