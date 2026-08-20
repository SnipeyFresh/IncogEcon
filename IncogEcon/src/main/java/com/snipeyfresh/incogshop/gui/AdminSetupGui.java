package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.custom.CustomCategoryManager.CustomCategory;
import com.snipeyfresh.incogshop.custom.CustomCategoryManager.CustomSubcategory;
import com.snipeyfresh.incogshop.market.MarketMode;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AdminSetupGui {
    private static final int[] GRID_28 = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };

    public record Holder(String screen, String categoryId, int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public record MoveCategoryHolder(Material material) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public record MoveCustomSubHolder(Material material, String categoryId) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public record ConfirmDeleteHolder(String type, String categoryId, String subcategoryId, int returnPage) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final IncogShopPlugin plugin;
    public AdminSetupGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new Holder("menu", "", 0), 45,
                GuiTheme.title("&d&l", "Admin Studio"));
        frame(inv, Material.PURPLE_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(4, GuiTheme.panel(Material.NETHER_STAR, "&d&lMARKET CONTROL CENTER", List.of(
                "&7Everything you need to organize",
                "&7and maintain the server market.",
                GuiTheme.stat("Market materials", plugin.market().tradableMaterials().size()))));

        inv.setItem(11, GuiTheme.button(Material.BOOKSHELF, "&d&lCATEGORIES & SECTIONS", List.of(
                "&7Create, browse, assign, and remove",
                "&7custom categories and subcategories."), "Click to manage"));
        inv.setItem(13, GuiTheme.button(Material.HOPPER, "&b&lORGANIZE MARKET ITEMS", List.of(
                "&7Browse every market material and",
                "&7move it to a built-in or custom section."), "Click to organize"));
        ItemStack held = player.getInventory().getItemInMainHand();
        Material heldMat = held == null || held.getType().isAir() ? Material.ITEM_FRAME : held.getType();
        inv.setItem(15, GuiTheme.button(heldMat, "&a&lADD HELD ITEM", List.of(
                GuiTheme.stat("Held", held == null || held.getType().isAir() ? "Nothing" : Text.prettyEnum(held.getType().name())),
                "&7Adds or enables it with the default",
                "&7price and &aBuy & Sell &7mode."), "Click to add"));

        inv.setItem(29, GuiTheme.button(Material.ARMOR_STAND, "&5&lGUI LAYOUT DESIGNER", List.of(
                "&7Reposition categories, subcategories,",
                "&7item slots, and navigation controls."), "Click to customize"));
        inv.setItem(31, GuiTheme.button(Material.COMMAND_BLOCK, "&6&lOPEN ADMIN MARKET", List.of(
                "&7Fine-tune stock, prices, modes,",
                "&7and individual market items."), "Click to open"));
        boolean infinite = plugin.market().isInfiniteStockEnabled();
        inv.setItem(33, GuiTheme.toggle(infinite, "INFINITE STOCK",
                List.of("&7Global server-stock behavior."),
                infinite ? "Click to disable" : "Click to enable"));

        inv.setItem(40, GuiTheme.close());
        player.openInventory(inv);
    }

    public void openCategories(Player player, int requestedPage) {
        List<CustomCategory> categories = new ArrayList<>(plugin.customCategories().all());
        int perPage = GRID_28.length;
        int pages = Math.max(1, (categories.size() + perPage - 1) / perPage);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        Inventory inv = Bukkit.createInventory(new Holder("categories", "", page), 54,
                GuiTheme.title("&d&l", "Admin Studio", "Categories"));
        frame(inv, Material.PURPLE_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(Material.BOOKSHELF, "&d&lCUSTOM CATEGORIES", List.of(
                "&7Left-click: &fManage subcategories",
                "&7Shift-click: &fAssign held item",
                "&7Right-click: &cRemove category")));

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < categories.size(); i++) {
            CustomCategory c = categories.get(start + i);
            inv.setItem(GRID_28[i], GuiTheme.panel(c.icon(), "&d" + c.display(), List.of(
                    GuiTheme.stat("ID", c.id()),
                    GuiTheme.stat("Subcategories", plugin.customCategories().subcategories(c.id()).size()),
                    GuiTheme.stat("Items", plugin.customCategories().materials(c.id()).size()),
                    "", "&eLeft-click &7Manage sections",
                    "&bShift-click &7Assign held item",
                    "&cRight-click &7Delete")));
        }

        inv.setItem(45, GuiTheme.back("Admin Studio"));
        if (page > 0) inv.setItem(47, GuiTheme.previousPage(page, true));
        inv.setItem(49, GuiTheme.button(Material.LIME_CONCRETE, "&a&l+ CREATE CATEGORY", List.of(
                "&7Hold an item to use as the icon.",
                "&7You will name it on a sign."), "Click to create"));
        inv.setItem(51, GuiTheme.pageIndicator(page + 1, pages, "Custom categories", categories.size()));
        if (page < pages - 1) inv.setItem(53, GuiTheme.nextPage(page + 2, true));
        player.openInventory(inv);
    }

    public void openSubcategories(Player player, String categoryId, int requestedPage) {
        CustomCategory category = plugin.customCategories().get(categoryId);
        if (category == null) { openCategories(player, 0); return; }
        List<CustomSubcategory> subs = plugin.customCategories().subcategories(category.id());
        int perPage = GRID_28.length;
        int pages = Math.max(1, (subs.size() + perPage - 1) / perPage);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        Inventory inv = Bukkit.createInventory(new Holder("subcategories", category.id(), page), 54,
                GuiTheme.title("&d&l", "Admin Studio", category.display()));
        frame(inv, Material.MAGENTA_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(category.icon(), "&d&l" + category.display(), List.of(
                "&7Left-click a section to assign held item.",
                "&7Right-click a section to remove it.")));

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < subs.size(); i++) {
            CustomSubcategory sub = subs.get(start + i);
            inv.setItem(GRID_28[i], GuiTheme.panel(sub.icon(), "&e" + sub.display(), List.of(
                    GuiTheme.stat("ID", sub.id()),
                    GuiTheme.stat("Items", plugin.customCategories().materials(category.id(), sub.id()).size()),
                    "", "&eLeft-click &7Assign held item",
                    "&cRight-click &7Delete")));
        }

        inv.setItem(45, GuiTheme.back("the category list"));
        if (page > 0) inv.setItem(47, GuiTheme.previousPage(page, true));
        inv.setItem(49, GuiTheme.button(Material.LIME_CONCRETE, "&a&l+ CREATE SUBCATEGORY", List.of(
                "&7Hold an item to use as the icon.",
                "&7You will name it on a sign."), "Click to create"));
        inv.setItem(51, GuiTheme.pageIndicator(page + 1, pages, "Sections", subs.size()));
        if (page < pages - 1) inv.setItem(53, GuiTheme.nextPage(page + 2, true));
        player.openInventory(inv);
    }

    public void openItems(Player player, int requestedPage) {
        List<Material> materials = plugin.market().tradableMaterials();
        int perPage = GRID_28.length;
        int pages = Math.max(1, (materials.size() + perPage - 1) / perPage);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        Inventory inv = Bukkit.createInventory(new Holder("items", "", page), 54,
                GuiTheme.title("&b&l", "Admin Studio", "Item Organizer"));
        frame(inv, Material.CYAN_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(Material.HOPPER, "&b&lITEM ORGANIZER", List.of(
                "&7Left-click: &fMove item",
                "&7Right-click: &fCycle market mode",
                "&7Middle-click: &fToggle auto restock",
                "&7Current location is shown on each item.")));

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < materials.size(); i++) {
            Material material = materials.get(start + i);
            String custom = plugin.customCategories().assigned(material);
            String customSub = plugin.customCategories().assignedSubcategory(material);
            String location;
            if (custom != null) {
                CustomCategory cat = plugin.customCategories().get(custom);
                CustomSubcategory sub = customSub == null ? null : plugin.customCategories().subcategory(custom, customSub);
                location = cat == null ? "Custom" : cat.display() + (sub == null ? "" : " → " + sub.display());
            } else {
                location = plugin.market().categoryOf(material).display() + " → " + plugin.market().subcategoryOf(material).display();
            }
            MarketMode mode = plugin.market().marketMode(material);
            String modeText = mode == MarketMode.BUY_SELL ? "&aBuy & Sell" : mode == MarketMode.SELL_ONLY ? "&eSell Only" : "&cDisabled";
            boolean autoRestock = plugin.market().isAutoRestockEnabled(material);
            inv.setItem(GRID_28[i], GuiTheme.panel(material, "&f" + Text.prettyEnum(material.name()), List.of(
                    GuiTheme.stat("Location", location),
                    "&7Mode: " + modeText,
                    GuiTheme.stat("Stock", plugin.market().entry(material).stock()),
                    "&7Auto Restock: " + (autoRestock ? "&aON" : "&cOFF"),
                    "", "&eLeft-click &7Move item",
                    "&bRight-click &7Cycle mode",
                    "&dMiddle-click &7Toggle auto restock")));
        }
        inv.setItem(45, GuiTheme.back("Admin Studio"));
        if (page > 0) inv.setItem(47, GuiTheme.previousPage(page, true));
        inv.setItem(49, GuiTheme.button(Material.ITEM_FRAME, "&a&lADD HELD ITEM", List.of(
                "&7Add or enable the material in your hand."), "Click to add"));
        inv.setItem(51, GuiTheme.pageIndicator(page + 1, pages, "Market materials", materials.size()));
        if (page < pages - 1) inv.setItem(53, GuiTheme.nextPage(page + 2, true));
        player.openInventory(inv);
    }

    public void openMoveCategories(Player player, Material material) {
        Inventory inv = Bukkit.createInventory(new MoveCategoryHolder(material), 54,
                GuiTheme.title("&b&l", "Move Item", Text.prettyEnum(material.name())));
        frame(inv, Material.BLUE_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(material, "&b&lCHOOSE DESTINATION", List.of(
                "&7Pick a built-in or custom category.",
                "&7Subcategories are chosen next when needed.")));

        List<MarketCategory> built = java.util.Arrays.stream(MarketCategory.values()).filter(c -> c != MarketCategory.ALL).toList();
        int[] builtSlots = ShopGui.centeredSlots(built.size());
        for (int i = 0; i < built.size() && i < builtSlots.length; i++) {
            MarketCategory c = built.get(i);
            inv.setItem(builtSlots[i], GuiTheme.button(c.icon(), "&6" + c.display(), List.of("&7Built-in category"), "Click to choose section"));
        }

        List<CustomCategory> custom = new ArrayList<>(plugin.customCategories().all());
        int[] customSlots = ShopGui.centeredSlots(custom.size());
        for (int i = 0; i < custom.size() && i < customSlots.length; i++) {
            int shifted = customSlots[i] + 18;
            if (shifted >= 45) break;
            CustomCategory c = custom.get(i);
            inv.setItem(shifted, GuiTheme.button(c.icon(), "&d" + c.display(), List.of("&7Custom category"), "Click to move here"));
        }

        inv.setItem(45, GuiTheme.back("the item organizer"));
        inv.setItem(49, GuiTheme.button(Material.RECOVERY_COMPASS, "&bReset to Automatic", List.of(
                "&7Clears custom and built-in overrides."), "Click to reset"));
        player.openInventory(inv);
    }

    public void openMoveCustomSubcategories(Player player, Material material, String categoryId) {
        CustomCategory category = plugin.customCategories().get(categoryId);
        if (category == null) { openMoveCategories(player, material); return; }
        List<CustomSubcategory> subs = plugin.customCategories().subcategories(category.id());
        Inventory inv = Bukkit.createInventory(new MoveCustomSubHolder(material, category.id()), 54,
                GuiTheme.title("&d&l", "Move Item", category.display()));
        frame(inv, Material.MAGENTA_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(material, "&d&lCHOOSE SUBCATEGORY", List.of(GuiTheme.stat("Destination", category.display()))));
        int[] slots = ShopGui.centeredSlots(subs.size());
        for (int i = 0; i < subs.size() && i < slots.length; i++) {
            CustomSubcategory sub = subs.get(i);
            inv.setItem(slots[i], GuiTheme.button(sub.icon(), "&e" + sub.display(), List.of("&7Custom section"), "Click to move item here"));
        }
        inv.setItem(39, GuiTheme.button(category.icon(), "&dPlace in Category Root", List.of("&7Do not use a subcategory."), "Click to move"));
        inv.setItem(45, GuiTheme.back("the category list"));
        player.openInventory(inv);
    }

    public void openDeleteCategory(Player player, String categoryId, int returnPage) {
        CustomCategory category = plugin.customCategories().get(categoryId);
        if (category == null) { openCategories(player, returnPage); return; }
        Inventory inv = Bukkit.createInventory(new ConfirmDeleteHolder("category", category.id(), "", returnPage), 27,
                GuiTheme.title("&c&l", "Confirm Removal", "Category"));
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(category.icon(), "&c&lREMOVE " + category.display().toUpperCase(java.util.Locale.ROOT) + "?", List.of(
                "&7Its items are NOT deleted.",
                "&7They return to automatic built-in placement.",
                "&7Its custom subcategories are removed too.")));
        inv.setItem(11, GuiTheme.button(Material.LIME_CONCRETE, "&aKeep Category", List.of("&7Nothing is removed."), "Click to cancel"));
        inv.setItem(15, GuiTheme.button(Material.RED_CONCRETE, "&c&lREMOVE CATEGORY", List.of("&cThis cannot be undone."), "Click to confirm"));
        player.openInventory(inv);
    }

    public void openDeleteSubcategory(Player player, String categoryId, String subcategoryId, int returnPage) {
        CustomSubcategory sub = plugin.customCategories().subcategory(categoryId, subcategoryId);
        if (sub == null) { openSubcategories(player, categoryId, returnPage); return; }
        Inventory inv = Bukkit.createInventory(new ConfirmDeleteHolder("subcategory", categoryId, sub.id(), returnPage), 27,
                GuiTheme.title("&c&l", "Confirm Removal", "Subcategory"));
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(sub.icon(), "&c&lREMOVE " + sub.display().toUpperCase(java.util.Locale.ROOT) + "?", List.of(
                "&7Items remain in the parent custom category.",
                "&7Only this subcategory is removed.")));
        inv.setItem(11, GuiTheme.button(Material.LIME_CONCRETE, "&aKeep Subcategory", List.of("&7Nothing is removed."), "Click to cancel"));
        inv.setItem(15, GuiTheme.button(Material.RED_CONCRETE, "&c&lREMOVE SUBCATEGORY", List.of("&cThis cannot be undone."), "Click to confirm"));
        player.openInventory(inv);
    }

    private void frame(Inventory inv, Material accent, Material trim) {
        GuiTheme.frame(inv, accent);
    }

    private void fill(Inventory inv, Material material) {
        GuiTheme.fill(inv, material);
    }
}
