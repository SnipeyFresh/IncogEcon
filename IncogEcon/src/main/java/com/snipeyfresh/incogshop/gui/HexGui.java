package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.economy.WalletManager;
import com.snipeyfresh.incogshop.hex.HexManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Hex menus: MAIN, REFORGE, and ENCHANT.
 *
 * All clicks are cancelled and re-applied by hand; the upgraded item is tracked
 * on the Holder and never lives only in a slot, preventing duplication.
 */
public final class HexGui {
    public static final String MAIN    = "main";
    public static final String REFORGE = "reforge";
    public static final String ENCHANT = "enchant";

    /** Slot where the player drops their item on the MAIN screen. */
    public static final int ITEM_SLOT    = 22;
    /** Read-only item preview on REFORGE / ENCHANT screens. */
    public static final int PREVIEW_SLOT = 4;

    // MAIN screen action slots
    public static final int SLOT_REFORGE = 29;
    public static final int SLOT_ENCHANT = 33;
    public static final int SLOT_CLOSE   = 45;
    public static final int SLOT_TAKE    = 53;

    // Sub-screen navigation slots
    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_BACK     = 46;
    public static final int SLOT_PAGE     = 49;
    public static final int SLOT_NEXT     = 53;

    public static final int LIST_START    = 9;
    public static final int LIST_PER_PAGE = 36;

    /**
     * Tracks the screen being shown, the item being worked on, and which enchant
     * list was rendered so the listener can map a slot index back to an enchant
     * without rebuilding the list.
     */
    public static final class Holder implements InventoryHolder {
        private final String screen;
        private final int page;
        private ItemStack carried;
        private boolean moved;
        private List<Object> enchantList; // String = IncogRPG id, Enchantment = EE

        public Holder(String screen, int page, ItemStack carried) {
            this.screen = screen;
            this.page = page;
            this.carried = carried == null || carried.getType().isAir() ? null : carried;
        }

        @Override public Inventory getInventory() { return null; }
        public String screen() { return screen; }
        public int page() { return page; }
        public ItemStack carried() { return carried; }
        public void setCarried(ItemStack item) { carried = item == null || item.getType().isAir() ? null : item; }
        public boolean moved() { return moved; }
        public void setMoved(boolean v) { moved = v; }
        public List<Object> enchantList() { return enchantList; }
        public void setEnchantList(List<Object> list) { enchantList = list; }
    }

    private final IncogShopPlugin plugin;

    public HexGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    // ----------------------------------------------------------------- MAIN

    public void open(Player player, ItemStack carried) {
        HexManager hex = plugin.hex();
        ItemStack item = carried == null || carried.getType().isAir() ? null : carried;
        Inventory inv = Bukkit.createInventory(new Holder(MAIN, 0, item), 54, GuiTheme.title("&d&l", "The Hex"));
        GuiTheme.fill(inv, GuiTheme.TRIM);

        inv.setItem(4, GuiTheme.panel(Material.AMETHYST_CLUSTER, "&d&lTHE HEX", List.of(
                "&7Reforge and enchant your items using coins.",
                GuiTheme.stat("Balance", plugin.money(plugin.wallets().get(player.getUniqueId()))))));

        if (item == null) {
            inv.setItem(ITEM_SLOT, GuiTheme.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&f&lPLACE AN ITEM HERE",
                    List.of(GuiTheme.RULE,
                            "&7Click a weapon, tool, or armor piece",
                            "&7in your inventory to place it in the Hex.",
                            "",
                            "&8Your item is always returned when",
                            "&8this menu closes.")));
        } else {
            inv.setItem(ITEM_SLOT, item);
        }

        inv.setItem(SLOT_REFORGE, reforgeButton(player, item));
        inv.setItem(SLOT_ENCHANT, enchantButton(player, item));

        inv.setItem(SLOT_CLOSE, GuiTheme.close());
        inv.setItem(SLOT_TAKE, item == null
                ? GuiTheme.item(Material.GRAY_DYE, "&8Take Item Back", List.of(GuiTheme.RULE, "&8No item is in the Hex."))
                : GuiTheme.button(Material.HOPPER, "&e&lTAKE ITEM BACK",
                        List.of("&7Return the item to your inventory."), "Click to take it back"));

        player.openInventory(inv);
    }

    private ItemStack reforgeButton(Player player, ItemStack item) {
        HexManager hex = plugin.hex();
        if (!hex.reforgeEnabled()) {
            return GuiTheme.locked(Material.ANVIL, "REFORGE",
                    List.of("&8Requires IncogRPG to be installed."), "Not available");
        }
        if (item == null) {
            return GuiTheme.locked(Material.ANVIL, "REFORGE",
                    List.of("&7Place an item in the Hex first."), "No item in the Hex");
        }
        String current = hex.currentReforge(item);
        List<String> lore = new ArrayList<>();
        lore.add(GuiTheme.stat("Current", current == null ? "None" : hex.reforgeDisplayName(current)));
        lore.add(GuiTheme.stat("Available", hex.reforgeOptions(item).size()));
        lore.add("&7Coin cost applies per reforge.");
        return GuiTheme.button(Material.ANVIL, "&b&lREFORGE", lore, "Click to choose a reforge");
    }

    private ItemStack enchantButton(Player player, ItemStack item) {
        HexManager hex = plugin.hex();
        if (!hex.enchantEnabled()) {
            return GuiTheme.locked(Material.ENCHANTED_BOOK, "ENCHANT", List.of(), "Disabled");
        }
        if (item == null) {
            return GuiTheme.locked(Material.ENCHANTED_BOOK, "ENCHANT",
                    List.of("&7Place an item in the Hex first."), "No item in the Hex");
        }
        int rpgCount = hex.integrations().incogRpg().compatibleEnchants(item).size();
        int eeCount  = hex.integrations().excellentEnchants().compatibleEnchants(item).size();
        List<String> lore = new ArrayList<>();
        if (rpgCount > 0) lore.add(GuiTheme.stat("IncogRPG enchants", rpgCount));
        if (eeCount  > 0) lore.add(GuiTheme.stat("ExcellentEnchants", eeCount));
        if (rpgCount == 0 && eeCount == 0) lore.add("&7No compatible enchants found for this item.");
        lore.add("");
        lore.add(GuiTheme.stat("Cost per level", plugin.money(hex.enchantCostPerLevel())));
        return GuiTheme.button(Material.ENCHANTED_BOOK, "&5&lENCHANT", lore, "Click to apply enchants");
    }

    // --------------------------------------------------------------- REFORGE

    public void openReforge(Player player, ItemStack carried, int requestedPage) {
        List<String> options = carried == null ? List.of() : plugin.hex().reforgeOptions(carried);
        int pages = Math.max(1, (options.size() + LIST_PER_PAGE - 1) / LIST_PER_PAGE);
        int page  = Math.max(0, Math.min(pages - 1, requestedPage));

        Inventory inv = Bukkit.createInventory(new Holder(REFORGE, page, carried), 54,
                GuiTheme.title("&b&l", "The Hex", "Reforge"));
        GuiTheme.fill(inv, GuiTheme.TRIM);
        preview(inv, carried);

        String current = carried == null ? null : plugin.hex().currentReforge(carried);
        double balance = plugin.wallets().get(player.getUniqueId());
        int start = page * LIST_PER_PAGE;
        for (int i = 0; i < LIST_PER_PAGE && start + i < options.size(); i++) {
            String id = options.get(start + i);
            boolean applied = id.equalsIgnoreCase(current);
            double cost = plugin.hex().reforgeCost(carried, id);
            List<String> lore = new ArrayList<>();
            String rarity = plugin.hex().integrations().incogRpg().reforgeRarity(id);
            if (!rarity.isBlank()) lore.add(GuiTheme.stat("Rarity", rarity));
            if (applied) lore.add("&a✔ Currently applied");
            lore.add("");
            if (cost > 0) {
                boolean affordable = balance + 1e-9 >= cost;
                lore.add("&7Cost: " + (affordable ? "&a" : "&c") + plugin.money(cost));
            } else {
                lore.add("&aFree");
            }
            inv.setItem(LIST_START + i, GuiTheme.button(
                    applied ? Material.LIME_DYE : Material.ANVIL,
                    plugin.hex().reforgeDisplayName(id), lore,
                    applied ? "Click to reroll" : "Click to apply"));
        }

        inv.setItem(SLOT_PREVIOUS, page > 0 ? GuiTheme.previousPage(page, true) : GuiTheme.back("the Hex"));
        inv.setItem(SLOT_BACK,     GuiTheme.back("the Hex"));
        inv.setItem(SLOT_PAGE,     GuiTheme.panel(Material.ANVIL, "&b&lREFORGE STATION", List.of(
                GuiTheme.stat("Current", current == null ? "None" : plugin.hex().reforgeDisplayName(current)),
                GuiTheme.stat("Options", options.size()),
                GuiTheme.stat("Page",    (page + 1) + "/" + pages))));
        inv.setItem(52, GuiTheme.pageIndicator(page + 1, pages, "Reforges", options.size()));
        inv.setItem(SLOT_NEXT, page + 1 < pages ? GuiTheme.nextPage(page + 2, true) : GuiTheme.close());

        player.openInventory(inv);
    }

    // --------------------------------------------------------------- ENCHANT

    public void openEnchant(Player player, ItemStack carried, int requestedPage) {
        List<Object> all = buildEnchantList(carried);
        int pages = Math.max(1, (all.size() + LIST_PER_PAGE - 1) / LIST_PER_PAGE);
        int page  = Math.max(0, Math.min(pages - 1, requestedPage));

        Holder holder = new Holder(ENCHANT, page, carried);
        holder.setEnchantList(all);
        Inventory inv = Bukkit.createInventory(holder, 54, GuiTheme.title("&5&l", "The Hex", "Enchant"));
        GuiTheme.fill(inv, GuiTheme.TRIM);
        preview(inv, carried);

        Map<String, Integer> rpgLevels = carried == null
                ? Map.of() : plugin.hex().integrations().incogRpg().currentEnchants(carried);
        double balance = plugin.wallets().get(player.getUniqueId());

        int start = page * LIST_PER_PAGE;
        for (int i = 0; i < LIST_PER_PAGE && start + i < all.size(); i++) {
            inv.setItem(LIST_START + i, enchantEntryButton(player, carried, all.get(start + i), rpgLevels, balance));
        }

        inv.setItem(SLOT_PREVIOUS, page > 0 ? GuiTheme.previousPage(page, true) : GuiTheme.back("the Hex"));
        inv.setItem(SLOT_BACK,     GuiTheme.back("the Hex"));
        inv.setItem(SLOT_PAGE,     GuiTheme.panel(Material.ENCHANTED_BOOK, "&5&lENCHANT", List.of(
                GuiTheme.stat("Total enchants", all.size()),
                GuiTheme.stat("Cost per level",  plugin.money(plugin.hex().enchantCostPerLevel())),
                GuiTheme.stat("Page",            (page + 1) + "/" + pages))));
        inv.setItem(52, GuiTheme.pageIndicator(page + 1, pages, "Enchants", all.size()));
        inv.setItem(SLOT_NEXT, page + 1 < pages ? GuiTheme.nextPage(page + 2, true) : GuiTheme.close());

        player.openInventory(inv);
    }

    private List<Object> buildEnchantList(ItemStack item) {
        List<Object> out = new ArrayList<>();
        if (item == null) return out;
        plugin.hex().integrations().incogRpg().compatibleEnchants(item).forEach(out::add);
        plugin.hex().integrations().excellentEnchants().compatibleEnchants(item).forEach(out::add);
        return out;
    }

    private ItemStack enchantEntryButton(Player player, ItemStack item, Object entry,
                                         Map<String, Integer> rpgLevels, double balance) {
        HexManager hex = plugin.hex();

        if (entry instanceof String id) {
            int current = rpgLevels.getOrDefault(id, 0);
            int max     = hex.integrations().incogRpg().enchantMaxLevel(id);
            String name   = hex.integrations().incogRpg().enchantDisplayName(id);
            String rarity = hex.integrations().incogRpg().enchantRarity(id);
            List<String> desc = hex.integrations().incogRpg().enchantDescription(id);

            List<String> lore = new ArrayList<>();
            if (!rarity.isBlank()) lore.add(GuiTheme.stat("Rarity", rarity));
            lore.addAll(desc);
            lore.add(GuiTheme.bar(max <= 0 ? 1.0 : current / (double) max, 10, "&5", "&8"));
            lore.add(GuiTheme.stat("Level", current + "/" + max));

            if (current >= max) {
                lore.add("&a✔ Maxed");
                return GuiTheme.locked(Material.PURPLE_DYE, "&5" + name, lore, "Already at max level");
            }
            int next = current + 1;
            double cost = WalletManager.round(hex.enchantCostPerLevel() * next);
            lore.add("");
            if (cost > 0) {
                lore.add("&7Cost (level " + next + "): " + (balance + 1e-9 >= cost ? "&a" : "&c") + plugin.money(cost));
            } else {
                lore.add("&aFree");
            }
            return GuiTheme.button(Material.BOOK, "&5" + name, lore, "Click to apply level " + next);

        } else if (entry instanceof Enchantment ench) {
            int current = item == null ? 0 : item.getEnchantmentLevel(ench);
            int max     = ench.getMaxLevel();
            String name = hex.integrations().excellentEnchants().displayName(ench);

            List<String> lore = new ArrayList<>();
            lore.add(GuiTheme.bar(max <= 0 ? 1.0 : current / (double) max, 10, "&5", "&8"));
            lore.add(GuiTheme.stat("Level", current + "/" + max));
            lore.add("&8ExcellentEnchants");

            if (current >= max) {
                lore.add("&a✔ Maxed");
                return GuiTheme.locked(Material.PURPLE_DYE, "&5" + name, lore, "Already at max level");
            }
            int next = current + 1;
            double cost = WalletManager.round(hex.enchantCostPerLevel() * next);
            lore.add("");
            if (cost > 0) {
                lore.add("&7Cost (level " + next + "): " + (balance + 1e-9 >= cost ? "&a" : "&c") + plugin.money(cost));
            } else {
                lore.add("&aFree");
            }
            return GuiTheme.button(Material.ENCHANTED_BOOK, "&5" + name, lore, "Click to apply level " + next);
        }

        return GuiTheme.panel(Material.BARRIER, "&cUnknown", List.of());
    }

    // ---------------------------------------------------------------- helpers

    private void preview(Inventory inv, ItemStack carried) {
        if (carried == null || carried.getType().isAir()) {
            inv.setItem(PREVIEW_SLOT, GuiTheme.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&8No item in the Hex",
                    List.of(GuiTheme.RULE, "&8Go back and place one in the Hex slot.")));
            return;
        }
        inv.setItem(PREVIEW_SLOT, carried);
    }
}
