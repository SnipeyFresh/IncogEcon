package com.snipeyfresh.incogshop.hex;

import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility methods for inspecting items handed to the Hex.
 */
public final class HexItems {

    public enum Kind { WEAPON, ARMOR, TOOL, OTHER }

    private HexItems() {}

    public static Kind kind(ItemStack item) {
        if (item == null) return Kind.OTHER;
        String name = item.getType().name();
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("TRIDENT")
                || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("MACE")) return Kind.WEAPON;
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS") || name.equals("ELYTRA") || name.equals("TURTLE_HELMET")) return Kind.ARMOR;
        if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.equals("SHEARS") || name.equals("FISHING_ROD")) return Kind.TOOL;
        return Kind.OTHER;
    }

    /** Only single, non-stacked weapons, armor, and tools can be upgraded. */
    public static boolean supported(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (item.getAmount() > 1) return false;
        return kind(item) != Kind.OTHER;
    }

    public static String displayName(ItemStack item) {
        if (item == null) return "Nothing";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()
                && meta.getDisplayName() != null && !meta.getDisplayName().isBlank()) {
            return meta.getDisplayName();
        }
        return "&f" + Text.prettyEnum(item.getType().name());
    }
}
