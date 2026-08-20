package com.snipeyfresh.incogshop.hex;

import org.bukkit.Material;

/** Item rarity ladder used by the Recombobulator upgrade. */
public enum HexRarity {
    COMMON("&fCommon"),
    UNCOMMON("&aUncommon"),
    RARE("&9Rare"),
    EPIC("&5Epic"),
    LEGENDARY("&6Legendary"),
    MYTHIC("&dMythic"),
    DIVINE("&bDivine");

    private final String display;
    HexRarity(String display) { this.display = display; }
    public String display() { return display; }

    public HexRarity bump(int steps) {
        int index = Math.min(values().length - 1, Math.max(0, ordinal() + Math.max(0, steps)));
        return values()[index];
    }

    /** Best-effort starting rarity for a vanilla material. */
    public static HexRarity baseFor(Material material) {
        if (material == null) return COMMON;
        String name = material.name();
        if (name.startsWith("NETHERITE_")) return EPIC;
        if (name.equals("ELYTRA") || name.equals("TRIDENT") || name.equals("TOTEM_OF_UNDYING")) return EPIC;
        if (name.startsWith("DIAMOND_")) return RARE;
        if (name.startsWith("IRON_") || name.startsWith("GOLDEN_") || name.startsWith("CHAINMAIL_")) return UNCOMMON;
        if (name.equals("BOW") || name.equals("CROSSBOW") || name.equals("SHIELD")) return UNCOMMON;
        return COMMON;
    }
}
