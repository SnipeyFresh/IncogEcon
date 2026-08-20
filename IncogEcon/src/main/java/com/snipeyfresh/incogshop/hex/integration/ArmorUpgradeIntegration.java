package com.snipeyfresh.incogshop.hex.integration;

import org.bukkit.inventory.ItemStack;

/** A plugin that owns its own armor upgrade ladder, such as EcoArmor tiers. */
public interface ArmorUpgradeIntegration extends HexIntegration {
    /** True when this plugin can upgrade the item. */
    boolean handles(ItemStack item);

    /** Current tier name, or null when the item has none. */
    String currentTier(ItemStack item);

    /** Next tier name, or null when the item is already at the top tier. */
    String nextTier(ItemStack item);

    /** Advances the item one tier. Returns false when nothing changed. */
    boolean upgradeTier(ItemStack item);

    /** True when the plugin also has a one-off "advanced" upgrade for this item. */
    default boolean supportsAdvancement(ItemStack item) { return false; }

    default boolean advanced(ItemStack item) { return false; }

    /** Applies the advanced upgrade. Returns false when nothing changed. */
    default boolean advance(ItemStack item) { return false; }
}
