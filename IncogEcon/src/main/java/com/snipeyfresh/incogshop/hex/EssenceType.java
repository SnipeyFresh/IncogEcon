package com.snipeyfresh.incogshop.hex;

import org.bukkit.Material;

/**
 * One configurable Hex currency, such as Wither Essence.
 *
 * @param id         upper-case identifier used in config and commands
 * @param display    coloured display name
 * @param icon       GUI icon material
 * @param buyPrice   coin price of a single unit, or 0 when it cannot be bought
 */
public record EssenceType(String id, String display, Material icon, double buyPrice) {
    public boolean purchasable() { return buyPrice > 0; }
}
