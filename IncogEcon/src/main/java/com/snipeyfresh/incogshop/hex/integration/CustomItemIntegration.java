package com.snipeyfresh.incogshop.hex.integration;

import org.bukkit.inventory.ItemStack;

/** A plugin that issues custom items the Hex should recognise before touching. */
public interface CustomItemIntegration extends HexIntegration {
    /** Custom item id from this plugin, or null when the item is not one of its own. */
    String itemId(ItemStack item);
}
