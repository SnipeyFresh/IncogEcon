package com.snipeyfresh.incogshop.hex.integration;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Base contract for every third-party plugin the Hex knows how to work with. */
public interface HexIntegration {
    /** Stable lower-case id used in config under {@code hex.integrations}. */
    String id();

    /** Plugin name as it appears in {@code plugins/}. */
    String pluginName();

    /** True when the plugin is installed and its API shape was recognised. */
    boolean available();

    /** Extra lore lines describing what this plugin knows about the item. */
    default List<String> describe(ItemStack item) { return List.of(); }

    /** True when the item belongs to this plugin. */
    default boolean claims(ItemStack item) { return false; }
}
