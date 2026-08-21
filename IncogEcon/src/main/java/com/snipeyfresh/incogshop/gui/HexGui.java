package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.hex.EssenceType;
import com.snipeyfresh.incogshop.hex.HexCost;
import com.snipeyfresh.incogshop.hex.HexManager;
import com.snipeyfresh.incogshop.hex.HexState;
import com.snipeyfresh.incogshop.hex.integration.ArmorUpgradeIntegration;
import com.snipeyfresh.incogshop.hex.integration.HexIntegration;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Hex menus.
 *
 * <p>Every screen carries the item being worked on, and the item is always
 * returned to the player when the menu closes. All controls are plain left
 * clicks so Java and Bedrock players use the Hex the same way.</p>
 */
public final class HexGui {
    public static final String MAIN = "main";
    public static final String POUCH = "pouch";
    public static final String SHOP = "shop";
    public static final String REFORGE = "reforge";

    /** Interactive item slot on the main screen. */
    public static final int ITEM_SLOT = 22;
    /** Read-only item preview on every other screen. */
    public static final int PREVIEW_SLOT = 4;

    public static final int SLOT_TIER = 10;
    public static final int SLOT_STARS = 11;
    public static final int SLOT_POTATO = 12;
    public static final int SLOT_GEMS = 14;
    public static final int SLOT_RECOMBOBULATOR = 15;
    public static final int SLOT_ENCHANTS = 16;
    public static final int SLOT_SUMMARY = 20;
    public static final int SLOT_COMPAT = 24;
    public static final int SLOT_ARMOR_TIER = 29;
    public static final int SLOT_REFORGE = 31;
    public static final int SLOT_ARMOR_ADVANCE = 33;
    public static final int SLOT_GUIDE = 40;
    public static final int SLOT_CLOSE = 45;
    public static final int SLOT_POUCH = 47;
    public static final int SLOT_SHOP = 49;
    public static final int SLOT_TAKE = 53;

    public static final int SLOT_BACK = 45;
    public static final int SLOT_SUB_CLOSE = 53;
    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_PAGE = 49;
    public static final int SLOT_NEXT = 53;
    public static final int REFORGE_START = 9;
    public static final int REFORGE_PER_PAGE = 36;

    /**
     * Tracks which Hex screen an inventory is, the item it is working on, and
     * whether that item was handed to another screen rather than left behind.
     *
     * <p>The holder, not the inventory slot, is the authoritative reference to
     * the carried item. Every click in a Hex menu is cancelled and applied by
     * hand, so the item can never be duplicated by an unusual click type.</p>
     */
    public static final class Holder implements InventoryHolder {
        private final String screen;
        private final int page;
        private ItemStack carried;
        private boolean moved;

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
        public void setMoved(boolean value) { moved = value; }
    }

    private final IncogShopPlugin plugin;

    public HexGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    // ------------------------------------------------------------ main menu

    public void open(Player player, ItemStack carried) {
        HexManager hex = plugin.hex();
        ItemStack item = carried == null || carried.getType().isAir() ? null : carried;
        Inventory inv = Bukkit.createInventory(new Holder(MAIN, 0, item), 54, GuiTheme.title("&d&l", "The Hex"));
        GuiTheme.fill(inv, GuiTheme.TRIM);

        inv.setItem(ITEM_SLOT, item);
        if (item == null) {
            inv.setItem(ITEM_SLOT, GuiTheme.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&f&lPLACE AN ITEM HERE",
                    List.of(GuiTheme.RULE,
                            "&7Click a weapon, tool, or armor piece",
                            "&7in your inventory to move it in.",
                            "",
                            "&8Your item is always returned when",
                            "&8this menu closes.")));
        }

        inv.setItem(4, GuiTheme.panel(Material.AMETHYST_CLUSTER, "&d&lTHE HEX", List.of(
                "&7Spend essence and coins to push",
                "&7one item toward its maximum.",
                GuiTheme.stat("Essence banked", hex.totalEssence(player.getUniqueId())),
                GuiTheme.stat("Balance", plugin.money(plugin.wallets().get(player.getUniqueId()))))));

        inv.setItem(SLOT_TIER, upgradeButton(player, item, HexManager.TIER, Material.NETHER_STAR));
        inv.setItem(SLOT_STARS, upgradeButton(player, item, HexManager.STARS, Material.NETHER_STAR));
        inv.setItem(SLOT_POTATO, upgradeButton(player, item, HexManager.POTATO, Material.BAKED_POTATO));
        inv.setItem(SLOT_GEMS, upgradeButton(player, item, HexManager.GEMSTONES, Material.AMETHYST_SHARD));
        inv.setItem(SLOT_RECOMBOBULATOR, upgradeButton(player, item, HexManager.RECOMBOBULATOR, Material.END_CRYSTAL));
        inv.setItem(SLOT_ENCHANTS, upgradeButton(player, item, HexManager.ENCHANTMENTS, Material.ENCHANTED_BOOK));

        inv.setItem(SLOT_SUMMARY, summary(item));
        inv.setItem(SLOT_COMPAT, compatibility(item));
        inv.setItem(SLOT_REFORGE, reforgeButton(player, item));

        ArmorUpgradeIntegration armor = item == null ? null : hex.armorProvider(item);
        inv.setItem(SLOT_ARMOR_TIER, armorTierButton(player, item, armor));
        inv.setItem(SLOT_ARMOR_ADVANCE, armorAdvanceButton(player, item, armor));

        inv.setItem(SLOT_GUIDE, GuiTheme.panel(Material.BOOK, "&e&lHOW THE HEX WORKS", List.of(
                "&71. Click an item in your inventory",
                "&7   to place it in the Hex.",
                "&72. Click an upgrade to pay and apply it.",
                "&73. Take the item back or close the menu.",
                "",
                "&8Upgrades stay on the item permanently,",
                "&8even if another plugin redraws its lore.")));

        inv.setItem(SLOT_CLOSE, GuiTheme.close());
        inv.setItem(SLOT_POUCH, GuiTheme.button(Material.ENDER_CHEST, "&d&lESSENCE POUCH",
                List.of(GuiTheme.stat("Total essence", hex.totalEssence(player.getUniqueId()))), "Click to view balances"));
        inv.setItem(SLOT_SHOP, hex.buyingAllowed()
                ? GuiTheme.button(Material.GOLD_INGOT, "&6&lBUY ESSENCE",
                        List.of("&7Trade coins for essence.",
                                GuiTheme.stat("Balance", plugin.money(plugin.wallets().get(player.getUniqueId())))), "Click to open the essence shop")
                : GuiTheme.locked(Material.GOLD_INGOT, "BUY ESSENCE", List.of("&7Essence must be earned here."), "Buying is disabled"));
        inv.setItem(SLOT_TAKE, item == null
                ? GuiTheme.item(Material.GRAY_DYE, "&8Take Item Back", List.of(GuiTheme.RULE, "&8No item is in the Hex."))
                : GuiTheme.button(Material.HOPPER, "&e&lTAKE ITEM BACK",
                        List.of("&7Return the item to your inventory."), "Click to take it back"));

        player.openInventory(inv);
    }

    // -------------------------------------------------------- essence pouch

    public void openPouch(Player player, ItemStack carried) {
        Inventory inv = Bukkit.createInventory(new Holder(POUCH, 0, carried), 54, GuiTheme.title("&d&l", "The Hex", "Essence Pouch"));
        GuiTheme.fill(inv, GuiTheme.TRIM);
        preview(inv, carried);

        Map<String, Long> balances = plugin.hex().essence(player.getUniqueId());
        List<EssenceType> types = new ArrayList<>(plugin.hex().types());
        int[] slots = ShopGui.centeredSlots(types.size());
        for (int i = 0; i < types.size() && i < slots.length; i++) {
            EssenceType type = types.get(i);
            long held = balances.getOrDefault(type.id(), 0L);
            inv.setItem(slots[i], GuiTheme.panel(type.icon(), type.display() + " &8• &f" + held, List.of(
                    GuiTheme.stat("Banked", held),
                    type.purchasable() ? GuiTheme.stat("Shop price", plugin.money(type.buyPrice()) + " each") : "&8Cannot be bought",
                    "&8Id: " + type.id())));
        }

        inv.setItem(SLOT_BACK, GuiTheme.back("the Hex"));
        inv.setItem(SLOT_PAGE, GuiTheme.panel(Material.ENDER_CHEST, "&d&lESSENCE POUCH",
                List.of(GuiTheme.stat("Total", plugin.hex().totalEssence(player.getUniqueId())),
                        "&7Essence drops from mobs and can be",
                        "&7bought with coins when enabled.")));
        inv.setItem(SLOT_SUB_CLOSE, GuiTheme.close());
        player.openInventory(inv);
    }

    // --------------------------------------------------------- essence shop

    public void openShop(Player player, ItemStack carried) {
        Inventory inv = Bukkit.createInventory(new Holder(SHOP, 0, carried), 54, GuiTheme.title("&6&l", "The Hex", "Essence Shop"));
        GuiTheme.fill(inv, GuiTheme.TRIM);
        preview(inv, carried);

        List<EssenceType> types = new ArrayList<>(plugin.hex().types());
        int[] slots = ShopGui.centeredSlots(types.size());
        for (int i = 0; i < types.size() && i < slots.length; i++) {
            EssenceType type = types.get(i);
            long held = plugin.hex().essence(player.getUniqueId(), type.id());
            inv.setItem(slots[i], type.purchasable()
                    ? GuiTheme.button(type.icon(), type.display(), List.of(
                            GuiTheme.stat("Price", plugin.money(type.buyPrice()) + " each"),
                            GuiTheme.stat("Banked", held)), "Click, then type an amount in chat")
                    : GuiTheme.locked(type.icon(), type.display(), List.of(GuiTheme.stat("Banked", held)),
                            "This essence cannot be bought"));
        }

        inv.setItem(SLOT_BACK, GuiTheme.back("the Hex"));
        inv.setItem(SLOT_PAGE, GuiTheme.panel(Material.GOLD_INGOT,
                "&6&lBALANCE &8• &f" + plugin.money(plugin.wallets().get(player.getUniqueId())),
                List.of(GuiTheme.stat("Economy", plugin.wallets().providerName()))));
        inv.setItem(SLOT_SUB_CLOSE, GuiTheme.close());
        player.openInventory(inv);
    }

    // ------------------------------------------------------------- reforges

    public void openReforge(Player player, ItemStack carried, int requestedPage) {
        List<String> options = carried == null ? List.of() : plugin.hex().reforgeOptions(carried);
        int pages = Math.max(1, (options.size() + REFORGE_PER_PAGE - 1) / REFORGE_PER_PAGE);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));

        Inventory inv = Bukkit.createInventory(new Holder(REFORGE, page, carried), 54, GuiTheme.title("&b&l", "The Hex", "Reforge"));
        GuiTheme.fill(inv, GuiTheme.TRIM);
        preview(inv, carried);

        HexCost cost = plugin.hex().reforgeCost();
        String current = carried == null ? null : plugin.hex().currentReforge(carried);
        int start = page * REFORGE_PER_PAGE;
        for (int i = 0; i < REFORGE_PER_PAGE && start + i < options.size(); i++) {
            String option = options.get(start + i);
            boolean applied = option.equalsIgnoreCase(current);
            List<String> lore = new ArrayList<>();
            var nativeReforge = plugin.hex().items().reforge(option);
            if (nativeReforge != null) lore.add(GuiTheme.stat("Stats", nativeReforge.summary()));
            lore.addAll(costLines(player, cost));
            if (applied) lore.add("&a✔ Currently applied");
            inv.setItem(REFORGE_START + i, GuiTheme.button(applied ? Material.LIME_DYE : Material.ANVIL,
                    plugin.hex().reforgeDisplay(carried, option), lore, applied ? "Click to reroll" : "Click to reforge"));
        }

        inv.setItem(SLOT_PREVIOUS, page > 0 ? GuiTheme.previousPage(page, true) : GuiTheme.back("the Hex"));
        inv.setItem(46, GuiTheme.back("the Hex"));
        inv.setItem(SLOT_PAGE, GuiTheme.panel(Material.ANVIL, "&b&lREFORGE STATION", List.of(
                GuiTheme.stat("Current", current == null ? "None" : plugin.hex().reforgeDisplay(carried, current)),
                GuiTheme.stat("Options", options.size()),
                GuiTheme.stat("Page", (page + 1) + "/" + pages),
                provider(carried))));
        inv.setItem(52, GuiTheme.pageIndicator(page + 1, pages, "Reforges", options.size()));
        inv.setItem(SLOT_NEXT, page + 1 < pages ? GuiTheme.nextPage(page + 2, true) : GuiTheme.close());
        player.openInventory(inv);
    }

    private String provider(ItemStack item) {
        var reforgePlugin = item == null ? null : plugin.hex().integrations().reforgeProviderFor(item);
        return reforgePlugin == null
                ? "&8Provided by IncogEcon"
                : "&8Provided by " + reforgePlugin.pluginName();
    }

    // -------------------------------------------------------------- helpers

    private void preview(Inventory inv, ItemStack carried) {
        if (carried == null || carried.getType().isAir()) {
            inv.setItem(PREVIEW_SLOT, GuiTheme.item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&8No item in the Hex",
                    List.of(GuiTheme.RULE, "&8Go back and place one in the Hex slot.")));
            return;
        }
        inv.setItem(PREVIEW_SLOT, carried);
    }

    private ItemStack summary(ItemStack item) {
        if (item == null) {
            return GuiTheme.panel(Material.PAPER, "&f&lITEM SUMMARY", List.of("&8Place an item in the Hex to see", "&8its current upgrade state."));
        }
        HexManager hex = plugin.hex();
        HexState state = hex.items().read(item);
        List<String> lore = new ArrayList<>();
        lore.add(GuiTheme.stat("Item", Text.color(hex.items().displayName(item)) + "§r"));
        lore.add(GuiTheme.stat("Rarity", Text.color(hex.items().rarity(item, state).display()) + "§r"));
        lore.add(GuiTheme.stat("Hex Tier", state.tier() + "/" + hex.maxLevel(HexManager.TIER)));
        lore.add(GuiTheme.stat("Stars", GuiTheme.stars(state.stars(), hex.maxLevel(HexManager.STARS))));
        lore.add(GuiTheme.stat("Hot Potato", state.potato() + "/" + hex.maxLevel(HexManager.POTATO)));
        lore.add(GuiTheme.stat("Gemstone Slots", state.gemSlots() + "/" + hex.maxLevel(HexManager.GEMSTONES)));
        lore.add(GuiTheme.stat("Reforge", Text.color(hex.reforgeDisplay(item, hex.currentReforge(item))) + "§r"));
        return GuiTheme.panel(Material.PAPER, "&f&lITEM SUMMARY", lore);
    }

    private ItemStack compatibility(ItemStack item) {
        List<String> lore = new ArrayList<>();
        List<HexIntegration> active = plugin.hex().integrations().active();
        if (active.isEmpty()) {
            lore.add("&7No supported upgrade plugins were found.");
            lore.add("&8IncogEcon handles every Hex upgrade itself.");
        } else {
            lore.add("&7Working with:");
            for (HexIntegration integration : active) lore.add("&8• &f" + integration.pluginName());
        }
        if (item != null) {
            List<String> described = plugin.hex().integrations().describe(item);
            if (!described.isEmpty()) {
                lore.add("");
                lore.add("&7This item:");
                lore.addAll(described);
            }
        }
        return GuiTheme.panel(Material.COMPARATOR, "&b&lPLUGIN COMPATIBILITY", lore);
    }

    private ItemStack upgradeButton(Player player, ItemStack item, String upgrade, Material icon) {
        HexManager hex = plugin.hex();
        String name = "&d&l" + hex.label(upgrade).toUpperCase(java.util.Locale.ROOT);
        if (!hex.upgradeEnabled(upgrade)) {
            return GuiTheme.locked(icon, name, List.of(), "Disabled on this server");
        }
        if (item == null) {
            return GuiTheme.locked(icon, name, List.of("&7Place an item in the Hex first."), "No item in the Hex");
        }

        if (HexManager.ENCHANTMENTS.equals(upgrade)) {
            int above = Math.max(0, plugin.getConfig().getInt("hex.upgrades.enchantments.max-level-above-vanilla", 2));
            List<String> lore = new ArrayList<>();
            lore.add("&7Raises every enchantment on the item");
            lore.add("&7by one level, up to &f+" + above + " &7over its");
            lore.add("&7normal maximum.");
            lore.addAll(costLines(player, hex.cost(HexManager.ENCHANTMENTS, 1)));
            lore.add("&8Cost is multiplied by the number of");
            lore.add("&8enchantments actually upgraded.");
            return GuiTheme.button(icon, name, lore, "Click to strengthen enchantments");
        }

        HexState state = hex.items().read(item);
        int current = hex.level(state, upgrade);
        int max = hex.maxLevel(upgrade);
        List<String> lore = new ArrayList<>();
        lore.add(GuiTheme.stat("Progress", current + "/" + max));
        lore.add(GuiTheme.bar(max <= 0 ? 1 : current / (double) max, 10, "&d", "&8"));
        lore.addAll(describeUpgrade(upgrade));
        if (current >= max) {
            return GuiTheme.locked(icon, name, lore, "Already maxed on this item");
        }
        lore.addAll(costLines(player, hex.cost(upgrade, current + 1)));
        return GuiTheme.button(icon, name, lore, "Click to upgrade to " + (current + 1));
    }

    private List<String> describeUpgrade(String upgrade) {
        return switch (upgrade) {
            case HexManager.TIER -> List.of("&8Raises the item's core Hex stats.");
            case HexManager.STARS -> List.of("&8Master Stars add damage and defence.");
            case HexManager.POTATO -> List.of("&8Hot Potato points add small flat stats.");
            case HexManager.GEMSTONES -> List.of("&8Each slot adds a permanent stat bonus.");
            case HexManager.RECOMBOBULATOR -> List.of("&8Bumps the item up one rarity.");
            default -> List.of();
        };
    }

    private ItemStack reforgeButton(Player player, ItemStack item) {
        HexManager hex = plugin.hex();
        if (!hex.reforgeEnabled()) {
            return GuiTheme.locked(Material.ANVIL, "REFORGE", List.of(), "Disabled on this server");
        }
        if (item == null) {
            return GuiTheme.locked(Material.ANVIL, "REFORGE", List.of("&7Place an item in the Hex first."), "No item in the Hex");
        }
        List<String> lore = new ArrayList<>();
        lore.add(GuiTheme.stat("Current", Text.color(hex.reforgeDisplay(item, hex.currentReforge(item))) + "§r"));
        lore.add(GuiTheme.stat("Available", hex.reforgeOptions(item).size()));
        lore.add(provider(item));
        lore.addAll(costLines(player, hex.reforgeCost()));
        return GuiTheme.button(Material.ANVIL, "&b&lREFORGE", lore, "Click to choose a reforge");
    }

    private ItemStack armorTierButton(Player player, ItemStack item, ArmorUpgradeIntegration armor) {
        if (armor == null) {
            return GuiTheme.locked(Material.NETHERITE_CHESTPLATE, "ARMOR TIER UPGRADE",
                    List.of("&7Works with armor from supported",
                            "&7plugins such as &fEcoArmor&7."),
                    item == null ? "No item in the Hex" : "No supported armor plugin owns this item");
        }
        String current = armor.currentTier(item);
        String next = armor.nextTier(item);
        List<String> lore = new ArrayList<>();
        lore.add(GuiTheme.stat("Plugin", armor.pluginName()));
        lore.add(GuiTheme.stat("Current tier", current == null ? "Default" : current));
        lore.add(GuiTheme.stat("Next tier", next == null ? "Maxed" : next));
        if (next == null) {
            return GuiTheme.locked(Material.NETHERITE_CHESTPLATE, "ARMOR TIER UPGRADE", lore, "Already at the highest tier");
        }
        lore.addAll(costLines(player, plugin.hex().armorTierCost(armor)));
        return GuiTheme.button(Material.NETHERITE_CHESTPLATE, "&6&lARMOR TIER UPGRADE", lore,
                "Click to apply the " + armor.pluginName() + " tier");
    }

    private ItemStack armorAdvanceButton(Player player, ItemStack item, ArmorUpgradeIntegration armor) {
        if (armor == null || item == null || !armor.supportsAdvancement(item)) {
            return GuiTheme.locked(Material.NETHER_STAR, "ARMOR ADVANCEMENT",
                    List.of("&7Applies the owning plugin's advanced",
                            "&7upgrade, such as EcoArmor's Advanced set."),
                    item == null ? "No item in the Hex" : "Not available for this item");
        }
        List<String> lore = new ArrayList<>();
        lore.add(GuiTheme.stat("Plugin", armor.pluginName()));
        lore.add("&7Advanced: " + (armor.advanced(item) ? "&aYes" : "&8No"));
        if (armor.advanced(item)) {
            return GuiTheme.locked(Material.NETHER_STAR, "ARMOR ADVANCEMENT", lore, "This piece is already advanced");
        }
        lore.addAll(costLines(player, plugin.hex().armorAdvancementCost(armor)));
        return GuiTheme.button(Material.NETHER_STAR, "&d&lARMOR ADVANCEMENT", lore, "Click to advance this piece");
    }

    /** Cost lines coloured green when the player can pay and red when they cannot. */
    private List<String> costLines(Player player, HexCost cost) {
        List<String> lore = new ArrayList<>();
        if (cost == null || cost.free()) {
            lore.add("");
            lore.add("&aFree");
            return lore;
        }
        lore.add("");
        lore.add("&7Cost:");
        if (cost.coins() > 0) {
            boolean affordable = plugin.wallets().get(player.getUniqueId()) + 1e-9 >= cost.coins();
            lore.add("&8• " + (affordable ? "&a" : "&c") + plugin.money(cost.coins()));
        }
        for (Map.Entry<String, Long> entry : cost.essence().entrySet()) {
            EssenceType type = plugin.hex().type(entry.getKey());
            long held = plugin.hex().essence(player.getUniqueId(), entry.getKey());
            boolean affordable = held >= entry.getValue();
            lore.add("&8• " + (affordable ? "&a" : "&c") + entry.getValue() + "x "
                    + (type == null ? entry.getKey() : type.display()) + " &8(" + held + " banked)");
        }
        return lore;
    }
}
