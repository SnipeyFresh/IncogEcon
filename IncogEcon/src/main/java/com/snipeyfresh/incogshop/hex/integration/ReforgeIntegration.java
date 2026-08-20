package com.snipeyfresh.incogshop.hex.integration;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/** A plugin that provides reforges the Hex can apply on the player's behalf. */
public interface ReforgeIntegration extends HexIntegration {
    /** Reforge ids that can be applied to this item. */
    List<String> options(ItemStack item);

    /** Reforge currently on the item, or null. */
    String current(ItemStack item);

    /** Applies a reforge. Returns false when the plugin refused. */
    boolean apply(ItemStack item, String reforgeId);

    default String display(String reforgeId) { return reforgeId; }
}
