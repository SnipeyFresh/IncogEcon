package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.custom.CustomCategoryManager.CustomCategory;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LayoutEditorGui {
    public record Holder(String screen, String selectedKey) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final IncogShopPlugin plugin;
    public LayoutEditorGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    public void open(Player player, String screen, String selected) {
        String normalized = screen == null ? "menu" : screen.toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "categories" -> openCategories(player, selected);
            case "subcategories" -> openSubcategories(player, selected);
            case "items" -> openItems(player, selected);
            default -> openMenu(player);
        }
    }

    public void open(Player player, String selected) { open(player, "menu", selected); }

    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new Holder("menu", null), 45,
                GuiTheme.title("&5&l", "Layout Designer"));
        frame(inv);
        inv.setItem(4, GuiTheme.panel(Material.ARMOR_STAND, "&5&lGUI LAYOUT DESIGNER", List.of(
                "&7Choose a screen to customize.",
                "&7Select a control, then click its new slot.")));
        inv.setItem(20, GuiTheme.button(Material.CHEST, "&6&lCATEGORY SCREEN", List.of("&7Move category and navigation buttons."), "Click to edit"));
        inv.setItem(22, GuiTheme.button(Material.BOOKSHELF, "&e&lSUBCATEGORY SCREEN", List.of("&7Move subcategory and navigation buttons."), "Click to edit"));
        inv.setItem(24, GuiTheme.button(Material.ITEM_FRAME, "&b&lITEM BROWSER", List.of("&7Move item positions and browser controls."), "Click to edit"));
        inv.setItem(40, GuiTheme.back("Admin Studio"));
        player.openInventory(inv);
    }

    private void openCategories(Player p, String selected) {
        Inventory inv = base("categories", selected, GuiTheme.title("&5&l", "Layout", "Categories"));
        put(inv, "search", Material.COMPASS, "&bSearch", 46, selected);
        put(inv, "orders", Material.NETHER_STAR, "&dOrders / Admin Studio", 48, selected);
        put(inv, "balance", Material.GOLD_INGOT, "&6Balance / Infinite Stock", 50, selected);
        put(inv, "help", Material.BOOK, "&eGuide / Layout", 52, selected);

        int total = MarketCategory.values().length + plugin.customCategories().all().size();
        int[] defaults = ShopGui.centeredSlots(total);
        int i = 0;
        for (MarketCategory cat : MarketCategory.values()) {
            put(inv, "builtin:" + cat.name(), cat.icon(), "&6" + cat.display(), defaults[i++], selected);
        }
        for (CustomCategory cat : plugin.customCategories().all()) {
            put(inv, "custom:" + cat.id(), cat.icon(), "&d" + cat.display(), defaults[i++], selected);
        }
        finish(inv, "categories");
        p.openInventory(inv);
    }

    private void openSubcategories(Player p, String selected) {
        Inventory inv = base("subcategories", selected, GuiTheme.title("&5&l", "Layout", "Subcategories"));
        int[] defaults = ShopGui.centeredSlots(21);
        for (int i = 0; i < 21; i++) put(inv, "sub:" + i, Material.BOOKSHELF, "&eSubcategory Slot " + (i + 1), defaults[i], selected);
        put(inv, "back", Material.ARROW, "&eBack", 45, selected);
        put(inv, "all", Material.CHEST, "&6All Category Items", 47, selected);
        put(inv, "search", Material.COMPASS, "&bSearch Category", 49, selected);
        put(inv, "status", Material.GOLD_INGOT, "&6Balance / Admin Status", 51, selected);
        finish(inv, "subcategories");
        p.openInventory(inv);
    }

    private void openItems(Player p, String selected) {
        Inventory inv = base("items", selected, GuiTheme.title("&5&l", "Layout", "Item Browser"));
        for (int i = 0; i < 36; i++) put(inv, "item:" + i, Material.ITEM_FRAME, "&bItem Position " + (i + 1), i, selected);
        put(inv, "previous", Material.ARROW, "&ePrevious Page", 45, selected);
        put(inv, "back", Material.CHEST, "&6Back to Categories / Subcategories", 46, selected);
        put(inv, "search", Material.COMPASS, "&bSearch", 47, selected);
        put(inv, "clear", Material.BARRIER, "&cClear Search", 48, selected);
        put(inv, "status", Material.GOLD_INGOT, "&6Balance / Admin", 49, selected);
        put(inv, "orders", Material.WRITABLE_BOOK, "&bOrders / Infinite Stock", 50, selected);
        put(inv, "section", Material.BOOK, "&6Current Section", 51, selected);
        put(inv, "page", Material.PAPER, "&fPage Indicator", 52, selected);
        put(inv, "next", Material.ARROW, "&eNext Page", 53, selected);
        finish(inv, "items");
        p.openInventory(inv);
    }

    private Inventory base(String screen, String selected, String title) {
        Inventory inv = Bukkit.createInventory(new Holder(screen, selected), 54, title);
        ItemStack filler = GuiTheme.item(Material.GRAY_STAINED_GLASS_PANE, "&8Available Slot",
                List.of(GuiTheme.RULE, "&7Select a control, then click here."));
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);
        return inv;
    }

    private void finish(Inventory inv, String screen) {
        inv.setItem(43, GuiTheme.button(Material.RECOVERY_COMPASS, "&c&lRESET THIS LAYOUT", List.of(
                "&7Restore the centered default layout."), "Click to reset"));
        inv.setItem(44, GuiTheme.button(Material.NETHER_STAR, "&d&lCHANGE SCREEN",
                List.of("&7Return to screen selection.", "&8Changes save automatically."), "Click to switch screens"));
    }

    private void put(Inventory inv, String key, Material mat, String name, int fallback, String selected) {
        String screen = ((Holder) inv.getHolder()).screen();
        int slot = plugin.layouts().slot(screen, key, fallback);
        if (slot == 43 || slot == 44) return;
        List<String> lore = new ArrayList<>();
        lore.add(GuiTheme.RULE);
        lore.add(GuiTheme.stat("Control", key));
        lore.add(GuiTheme.stat("Slot", slot));
        lore.add("");
        lore.add(key.equals(selected) ? "&a✔ Selected — click a destination slot" : "&e" + GuiTheme.CHEVRON + "Click to select");
        inv.setItem(slot, GuiTheme.item(mat, name, lore));
    }

    private void frame(Inventory inv) {
        GuiTheme.frame(inv, Material.PURPLE_STAINED_GLASS_PANE);
    }
}
