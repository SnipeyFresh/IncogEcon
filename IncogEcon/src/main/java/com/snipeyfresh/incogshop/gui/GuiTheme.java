package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared visual language for every IncogEcon menu.
 *
 * <p>Every screen builds its items through this class so titles, borders,
 * button hints, and stat lines look the same everywhere. Only characters that
 * render on both Java and Bedrock clients are used, and no helper here needs a
 * Java-only inventory gesture.</p>
 */
public final class GuiTheme {
    private GuiTheme() {}

    public static final String BRAND = "&8&lIncogEcon";
    public static final String ARROW = "&8» ";
    public static final String CHEVRON = "» ";
    public static final String DOT = "&8• ";
    public static final String RULE = "&8────────────────";
    public static final String STAR = "★";
    public static final String BLOCK = "■";

    public static final Material TRIM = Material.BLACK_STAINED_GLASS_PANE;
    public static final Material EMPTY = Material.GRAY_STAINED_GLASS_PANE;

    /** Builds a menu title such as "IncogEcon » Market". */
    public static String title(String accent, String section) {
        return Text.color(BRAND + " " + ARROW + (accent == null ? "&f" : accent) + section);
    }

    /** Builds a menu title with a trailing detail, such as "IncogEcon » Market · Ores". */
    public static String title(String accent, String section, String detail) {
        if (detail == null || detail.isBlank()) return title(accent, section);
        return Text.color(BRAND + " " + ARROW + (accent == null ? "&f" : accent) + section + " &8• &7" + detail);
    }

    /** Base item builder: colors the name and lore and hides vanilla attribute clutter. */
    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            meta.setLore(lore == null ? List.of() : lore.stream().map(Text::color).toList());
            meta.addItemFlags(ItemFlag.values());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** An information panel: a header rule above the body text. */
    public static ItemStack panel(Material material, String name, List<String> body) {
        List<String> lore = new ArrayList<>();
        lore.add(RULE);
        if (body != null) lore.addAll(body);
        return item(material, name, lore);
    }

    /** A clickable button: body text, then a highlighted call to action. */
    public static ItemStack button(Material material, String name, List<String> body, String action) {
        List<String> lore = new ArrayList<>();
        lore.add(RULE);
        if (body != null) lore.addAll(body);
        if (action != null && !action.isBlank()) {
            lore.add("");
            lore.add("&e" + CHEVRON + action);
        }
        return item(material, name, lore);
    }

    /** A button that is visible but cannot be used yet, with the reason shown. */
    public static ItemStack locked(Material material, String name, List<String> body, String reason) {
        List<String> lore = new ArrayList<>();
        lore.add(RULE);
        if (body != null) lore.addAll(body);
        lore.add("");
        lore.add("&c✖ " + (reason == null ? "Unavailable" : reason));
        return item(material, "&8" + stripColor(name), lore);
    }

    /** An on/off control rendered with a consistent colour and state line. */
    public static ItemStack toggle(boolean on, String name, List<String> body, String action) {
        List<String> lore = new ArrayList<>();
        lore.add(RULE);
        lore.add("&7State: " + (on ? "&aEnabled" : "&cDisabled"));
        if (body != null && !body.isEmpty()) {
            lore.add("");
            lore.addAll(body);
        }
        if (action != null && !action.isBlank()) {
            lore.add("");
            lore.add("&e" + CHEVRON + action);
        }
        return item(on ? Material.LIME_DYE : Material.GRAY_DYE, (on ? "&a" : "&c") + stripColor(name), lore);
    }

    /** A blank decorative pane. */
    public static ItemStack pane(Material material) {
        return item(material, " ", List.of());
    }

    public static ItemStack filler() { return pane(TRIM); }

    public static void fill(Inventory inv, Material material) {
        ItemStack pane = pane(material);
        for (int slot = 0; slot < inv.getSize(); slot++) inv.setItem(slot, pane);
    }

    public static void fillEmpty(Inventory inv, Material material) {
        ItemStack pane = pane(material);
        for (int slot = 0; slot < inv.getSize(); slot++) if (inv.getItem(slot) == null) inv.setItem(slot, pane);
    }

    /** Border of dark panes with accent panes in the corners of the top and bottom rows. */
    public static void frame(Inventory inv, Material accent) {
        ItemStack trim = pane(TRIM);
        ItemStack highlight = pane(accent == null ? TRIM : accent);
        int rows = inv.getSize() / 9;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) inv.setItem(slot, trim);
        }
        if (inv.getSize() >= 27) {
            inv.setItem(1, highlight); inv.setItem(7, highlight);
            inv.setItem(inv.getSize() - 8, highlight); inv.setItem(inv.getSize() - 2, highlight);
        }
    }

    /** Fills only the bottom navigation row, leaving content rows untouched. */
    public static void bottomBar(Inventory inv, Material accent) {
        ItemStack trim = pane(TRIM);
        ItemStack highlight = pane(accent == null ? TRIM : accent);
        int start = inv.getSize() - 9;
        for (int slot = start; slot < inv.getSize(); slot++) inv.setItem(slot, trim);
        inv.setItem(start + 1, highlight);
        inv.setItem(start + 7, highlight);
    }

    public static ItemStack back(String destination) {
        return button(Material.ARROW, "&eBack", List.of("&7Return to " + (destination == null ? "the previous menu" : destination) + "."), "Click to go back");
    }

    public static ItemStack close() {
        return button(Material.BARRIER, "&cClose", List.of("&7Close this menu."), "Click to close");
    }

    public static ItemStack previousPage(int page, boolean enabled) {
        return enabled
                ? button(Material.ARROW, "&ePrevious Page", List.of("&7Go to page &f" + page + "&7."), "Click to turn back")
                : item(Material.GRAY_DYE, "&8Previous Page", List.of(RULE, "&8Already on the first page."));
    }

    public static ItemStack nextPage(int page, boolean enabled) {
        return enabled
                ? button(Material.ARROW, "&eNext Page", List.of("&7Go to page &f" + page + "&7."), "Click to turn forward")
                : item(Material.GRAY_DYE, "&8Next Page", List.of(RULE, "&8Already on the last page."));
    }

    public static ItemStack pageIndicator(int page, int pages, String countLabel, int count) {
        return panel(Material.PAPER, "&fPage &e" + page + "&7/&e" + pages,
                List.of("&7" + countLabel + ": &f" + count));
    }

    /** "&7Label: &fValue" so every stat line lines up across menus. */
    public static String stat(String label, Object value) {
        return "&7" + label + ": &f" + value;
    }

    /** A progress bar such as "&a###&8###" used for stock, levels, and upgrade caps. */
    public static String bar(double ratio, int segments, String filledColor, String emptyColor) {
        int width = Math.max(1, segments);
        double clamped = Double.isFinite(ratio) ? Math.max(0.0, Math.min(1.0, ratio)) : 0.0;
        int filled = (int) Math.round(clamped * width);
        StringBuilder out = new StringBuilder(filledColor == null ? "&a" : filledColor);
        for (int i = 0; i < filled; i++) out.append(BLOCK);
        out.append(emptyColor == null ? "&8" : emptyColor);
        for (int i = filled; i < width; i++) out.append(BLOCK);
        return out.toString();
    }

    /** Star display used by Hex upgrades: filled stars in gold, remaining stars dark. */
    public static String stars(int filled, int max) {
        StringBuilder out = new StringBuilder("&6");
        int safeMax = Math.max(0, max);
        int safeFilled = Math.max(0, Math.min(safeMax, filled));
        for (int i = 0; i < safeFilled; i++) out.append(STAR);
        if (safeFilled < safeMax) {
            out.append("&8");
            for (int i = safeFilled; i < safeMax; i++) out.append(STAR);
        }
        return out.toString();
    }

    /** Wraps a long sentence into lore-sized grey lines. */
    public static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > Math.max(12, width)) {
                out.add("&7" + line);
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) out.add("&7" + line);
        return out;
    }

    private static String stripColor(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)[&§][0-9a-fk-or]", "");
    }
}
