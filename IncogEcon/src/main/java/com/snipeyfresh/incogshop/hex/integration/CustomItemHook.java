package com.snipeyfresh.incogshop.hex.integration;

import com.snipeyfresh.incogshop.gui.GuiTheme;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Generic identification hook for custom-item plugins.
 *
 * <p>These plugins do not upgrade items, but the Hex still needs to know when
 * an item belongs to one of them. That drives the "protect unknown custom
 * items" safety switch and lets the menus show where an item came from.</p>
 */
public final class CustomItemHook implements CustomItemIntegration {
    private final String id;
    private final String pluginName;
    private final String label;
    private final Method lookup;
    private final Method idAccessor;

    private CustomItemHook(String id, String pluginName, String label, Method lookup, Method idAccessor) {
        this.id = id;
        this.pluginName = pluginName;
        this.label = label;
        this.lookup = lookup;
        this.idAccessor = idAccessor;
    }

    public static CustomItemHook itemsAdder() {
        Class<?> stack = Reflect.findClass("dev.lone.itemsadder.api.CustomStack");
        return new CustomItemHook("itemsadder", "ItemsAdder", "ItemsAdder item",
                Reflect.looseMethod(stack, new String[]{"byItemStack"}, 1),
                Reflect.looseMethod(stack, new String[]{"getNamespacedID", "getId"}, 0));
    }

    public static CustomItemHook oraxen() {
        Class<?> items = Reflect.findClass("io.th0rgal.oraxen.api.OraxenItems");
        return new CustomItemHook("oraxen", "Oraxen", "Oraxen item",
                Reflect.looseMethod(items, new String[]{"getIdByItem"}, 1), null);
    }

    public static CustomItemHook nexo() {
        Class<?> items = Reflect.findClass("com.nexomc.nexo.api.NexoItems");
        return new CustomItemHook("nexo", "Nexo", "Nexo item",
                Reflect.looseMethod(items, new String[]{"idFromItem"}, 1), null);
    }

    public static CustomItemHook mmoItems() {
        Class<?> mmo = Reflect.findClass("net.Indyuce.mmoitems.MMOItems");
        return new CustomItemHook("mmoitems", "MMOItems", "MMOItems item",
                Reflect.looseMethod(mmo, new String[]{"getID"}, 1), null);
    }

    public static CustomItemHook ecoItems() {
        Class<?> utils = Reflect.findClass(
                "com.willfp.ecoitems.items.ItemUtils",
                "com.willfp.ecoitems.util.ItemUtils");
        return new CustomItemHook("ecoitems", "EcoItems", "EcoItems item",
                Reflect.looseMethod(utils, new String[]{"getItem", "getEcoItem"}, 1),
                null);
    }

    @Override public String id() { return id; }
    @Override public String pluginName() { return pluginName; }

    @Override
    public boolean available() {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null && lookup != null;
    }

    @Override
    public String itemId(ItemStack item) {
        if (!available() || item == null || item.getType().isAir()) return null;
        Object result = Reflect.invokeWithItem(lookup, item);
        if (result == null) return null;
        if (result instanceof String text) return text.isBlank() ? null : text;
        if (idAccessor != null) {
            Object name = Reflect.call(idAccessor, result);
            if (name instanceof String text && !text.isBlank()) return text;
        }
        return Reflect.nameOf(result);
    }

    @Override
    public boolean claims(ItemStack item) {
        return itemId(item) != null;
    }

    @Override
    public List<String> describe(ItemStack item) {
        String itemId = itemId(item);
        return itemId == null ? List.of() : List.of(GuiTheme.stat(label, itemId));
    }
}
