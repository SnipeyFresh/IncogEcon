package com.snipeyfresh.incogshop.hex;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * A reforge provided by IncogEcon itself.
 *
 * <p>These are only used when no supported reforge plugin is installed. When
 * one is present the Hex hands reforging over to that plugin instead so both
 * systems never fight over the same item.</p>
 */
public record HexReforge(String id, String display, double attackDamage, double armor,
                         double armorToughness, double health, double speed) {

    public static HexReforge from(String id, ConfigurationSection section) {
        String key = id.toUpperCase(Locale.ROOT);
        if (section == null) return new HexReforge(key, "&f" + id, 0, 0, 0, 0, 0);
        return new HexReforge(
                key,
                section.getString("display", "&f" + id),
                section.getDouble("attack-damage", 0),
                section.getDouble("armor", 0),
                section.getDouble("armor-toughness", 0),
                section.getDouble("health", 0),
                section.getDouble("speed", 0));
    }

    /** Short "+3 Damage, +1 Armor" style summary for menus and lore. */
    public String summary() {
        StringBuilder out = new StringBuilder();
        appendStat(out, attackDamage, "Damage");
        appendStat(out, armor, "Armor");
        appendStat(out, armorToughness, "Toughness");
        appendStat(out, health, "Health");
        appendStat(out, speed, "Speed");
        return out.isEmpty() ? "No stat changes" : out.toString();
    }

    private static void appendStat(StringBuilder out, double value, String label) {
        if (value == 0) return;
        if (!out.isEmpty()) out.append(", ");
        out.append(value > 0 ? "+" : "").append(trim(value)).append(' ').append(label);
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
