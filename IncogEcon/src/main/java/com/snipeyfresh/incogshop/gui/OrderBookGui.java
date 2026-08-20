package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.order.MarketOrder;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class OrderBookGui {
    private final IncogShopPlugin plugin;
    public OrderBookGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    public record BookHolder(Material material) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record MyOrdersHolder(int page) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record GlobalOrdersHolder(String filter, int page) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }

    public void open(Player player, Material material) {
        if (material == null || !plugin.market().isTradable(material)) {
            player.sendMessage(plugin.prefix() + "§cThat item is not currently available in the Bazaar.");
            plugin.gui().openCategories(player, false);
            return;
        }

        Inventory inv = Bukkit.createInventory(new BookHolder(material), 54,
                GuiTheme.title("&6&l", "Bazaar", Text.prettyEnum(material.name())));
        fill(inv);

        long stock = plugin.market().entry(material).stock();
        boolean infiniteStock = plugin.market().isInfiniteStockEnabled();
        boolean canBuy = plugin.market().isBuyAllowed(material);
        boolean canSell = plugin.market().isSellAllowed(material);
        double instantBuy = plugin.market().buyUnitPrice(material);
        double instantSell = plugin.market().sellUnitPrice(material);
        double bestBuy = plugin.orders().bestBuy(material);
        double bestSell = plugin.orders().bestSell(material);
        int eligibleOwned = plugin.market().countEligible(player, material);
        int stackSize = Math.max(1, material.getMaxStackSize());

        ItemStack center = new ItemStack(material);
        ItemMeta meta = center.getItemMeta();
        meta.setDisplayName(Text.color("&f&l" + Text.prettyEnum(material.name())));
        List<String> lore = new ArrayList<>();
        lore.add(Text.color(GuiTheme.RULE));
        lore.add(Text.color(GuiTheme.stat("Category", plugin.market().categoryOf(material).display())));
        lore.add(Text.color(GuiTheme.stat("Section", plugin.market().subcategoryOf(material).display())));
        lore.add(Text.color(GuiTheme.stat("Server Stock", infiniteStock ? "Unlimited" : String.valueOf(stock))));
        if (!infiniteStock) {
            double reference = Math.max(1.0, plugin.getConfig().getDouble("market.target-stock", 512) * 2.0);
            lore.add(Text.color(GuiTheme.bar(stock / reference, 12, "&a", "&8") + " &8supply"));
        }
        lore.add("");
        lore.add(Text.color("&aInstant Buy: " + (canBuy ? "&f" + plugin.money(instantBuy) + " &7each" : "&8Unavailable")));
        lore.add(Text.color("&cInstant Sell: " + (canSell ? "&f" + plugin.money(instantSell) + " &7each" : "&8Unavailable")));
        lore.add(Text.color(GuiTheme.stat("Your eligible items", eligibleOwned)));
        lore.add("");
        lore.add(Text.color("&7Best Buy Order: " + (bestBuy > 0 ? "&a" + plugin.money(bestBuy) : "&8None")));
        lore.add(Text.color("&7Best Sell Order: " + (bestSell > 0 ? "&c" + plugin.money(bestSell) : "&8None")));
        lore.add("");
        lore.add(Text.color("&8Tip: click another item in your inventory"));
        lore.add(Text.color("&8to jump directly to its Bazaar page."));
        meta.setLore(lore);
        center.setItemMeta(meta);
        inv.setItem(4, center);

        List<MarketOrder> buys = plugin.orders().buyOrders(material);
        List<MarketOrder> sells = plugin.orders().sellOrders(material);
        int[] buySlots = {10,11,12,13,14,15,16};
        int[] sellSlots = {28,29,30,31,32,33,34};
        for (int i = 0; i < Math.min(buySlots.length, buys.size()); i++) {
            inv.setItem(buySlots[i], orderIcon(buys.get(i), true));
        }
        for (int i = 0; i < Math.min(sellSlots.length, sells.size()); i++) {
            inv.setItem(sellSlots[i], orderIcon(sells.get(i), false));
        }

        inv.setItem(9, GuiTheme.panel(Material.LIME_STAINED_GLASS_PANE, "&a&lTOP BUY ORDERS",
                List.of("&7Highest player bid first.", "&8Older orders win price ties.")));
        inv.setItem(27, GuiTheme.panel(Material.RED_STAINED_GLASS_PANE, "&c&lTOP SELL ORDERS",
                List.of("&7Lowest player ask first.", "&8Older orders win price ties.")));

        String buyBlocked = "This item cannot be bought from server stock right now.";
        String sellBlocked = "This item cannot be sold to the server market right now.";

        inv.setItem(20, canBuy
                ? GuiTheme.button(Material.EMERALD_BLOCK, "&a&lBUY 1",
                        List.of("&7Buy exactly one item from server stock.",
                                GuiTheme.stat("Price", plugin.money(instantBuy))), "Click to buy 1")
                : GuiTheme.locked(Material.BARRIER, "BUY 1", List.of(), buyBlocked));

        inv.setItem(21, canBuy
                ? GuiTheme.button(Material.OAK_SIGN, "&a&lCUSTOM BUY AMOUNT",
                        List.of("&7Choose an exact amount to buy",
                                "&7from the server market.", "",
                                GuiTheme.stat("Price each", plugin.money(instantBuy)),
                                GuiTheme.stat("Available stock", infiniteStock ? "Unlimited" : String.valueOf(stock)),
                                "&8Examples: 5, 128, 2k"), "Click, then type the amount in chat")
                : GuiTheme.locked(Material.BARRIER, "CUSTOM BUY AMOUNT", List.of(), buyBlocked));

        inv.setItem(22, canBuy
                ? GuiTheme.button(Material.EMERALD, "&aBuy " + stackSize,
                        List.of("&7Buy up to one full material stack.",
                                GuiTheme.stat("Amount", stackSize),
                                GuiTheme.stat("Price each", plugin.money(instantBuy))), "Click to buy a stack")
                : GuiTheme.locked(Material.BARRIER, "BUY STACK", List.of(), buyBlocked));

        inv.setItem(24, canSell
                ? GuiTheme.button(Material.REDSTONE_BLOCK, "&c&lSELL 1",
                        List.of("&7Sell exactly one eligible item",
                                "&7to the server market.",
                                GuiTheme.stat("Price", plugin.money(instantSell)),
                                "&8Transaction fee is deducted from payout."), "Click to sell 1")
                : GuiTheme.locked(Material.BARRIER, "SELL 1", List.of(), sellBlocked));

        inv.setItem(25, canSell
                ? GuiTheme.button(Material.REDSTONE, "&cSell up to " + stackSize,
                        List.of("&7Sell up to one full material stack.",
                                GuiTheme.stat("Maximum amount", stackSize),
                                GuiTheme.stat("Eligible in inventory", eligibleOwned),
                                GuiTheme.stat("Price each", plugin.money(instantSell))), "Click to sell a stack")
                : GuiTheme.locked(Material.BARRIER, "SELL STACK", List.of(), sellBlocked));

        inv.setItem(45, GuiTheme.back("market categories"));
        inv.setItem(46, GuiTheme.button(Material.WRITABLE_BOOK, "&b&lALL MARKET ORDERS",
                List.of("&7Browse active Buy and Sell Orders", "&7for every Bazaar item."), "Click to browse"));
        inv.setItem(47, GuiTheme.button(Material.EMERALD, "&a&lCREATE BUY ORDER",
                List.of("&7Escrow money now and wait", "&7for matching Sell Orders."), "Click to enter amount and price"));
        inv.setItem(49, GuiTheme.panel(Material.GOLD_INGOT,
                "&6&lBALANCE &8• &f" + plugin.money(plugin.wallets().get(player.getUniqueId())),
                List.of(GuiTheme.stat("Economy", plugin.wallets().providerName()))));
        inv.setItem(51, GuiTheme.button(Material.REDSTONE, "&c&lCREATE SELL ORDER",
                List.of("&7Escrow eligible items now and wait", "&7for matching Buy Orders."), "Click to enter amount and price"));
        inv.setItem(52, GuiTheme.button(Material.BOOK, "&b&lMY ORDERS",
                List.of(GuiTheme.stat("Active orders", plugin.orders().owned(player.getUniqueId()).size())), "Click to manage"));
        inv.setItem(53, GuiTheme.button(Material.ENDER_CHEST, "&a&lCLAIM FILLED ITEMS",
                List.of(GuiTheme.stat("Claimable items", plugin.orders().claimCount(player.getUniqueId()))), "Click to claim"));
        player.openInventory(inv);
    }

    public void openGlobal(Player player, String requestedFilter, int requestedPage) {
        String normalizedFilter = requestedFilter == null ? "ALL" : requestedFilter.toUpperCase(java.util.Locale.ROOT);
        if (!normalizedFilter.equals("BUY") && !normalizedFilter.equals("SELL")) normalizedFilter = "ALL";
        final String filter = normalizedFilter;

        List<MarketOrder> all = plugin.orders().all().stream()
                .filter(o -> filter.equals("ALL") || o.type().name().equals(filter))
                .sorted(java.util.Comparator
                        .comparing((MarketOrder o) -> o.type() == MarketOrder.Type.BUY ? 0 : 1)
                        .thenComparing(MarketOrder::material)
                        .thenComparingLong(MarketOrder::createdAt))
                .toList();

        int perPage = 45;
        int pages = Math.max(1, (int)Math.ceil(all.size() / (double)perPage));
        int page = Math.max(0, Math.min(pages - 1, requestedPage));

        Inventory inv = Bukkit.createInventory(new GlobalOrdersHolder(filter, page), 54,
                GuiTheme.title("&b&l", "All Market Orders"));
        fill(inv);

        int start = page * perPage;
        for (int slot = 0; slot < perPage && start + slot < all.size(); slot++) {
            MarketOrder order = all.get(start + slot);
            ItemStack icon = orderIcon(order, order.type() == MarketOrder.Type.BUY);
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add("");
            lore.add(Text.color("&e" + GuiTheme.CHEVRON + "Click to open this item's Bazaar page"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
        }

        if (page > 0) inv.setItem(45, GuiTheme.previousPage(page, true));
        inv.setItem(46, GuiTheme.button(Material.BOOK, (filter.equals("ALL") ? "&a&lALL ORDERS ✔" : "&7All Orders"),
                List.of("&7Show Buy and Sell Orders."), "Click to filter"));
        inv.setItem(47, GuiTheme.button(Material.LIME_CONCRETE, filter.equals("BUY") ? "&a&lBUY ORDERS ✔" : "&aBuy Orders",
                List.of("&7Show only active Buy Orders."), "Click to filter"));
        inv.setItem(48, GuiTheme.button(Material.RED_CONCRETE, filter.equals("SELL") ? "&c&lSELL ORDERS ✔" : "&cSell Orders",
                List.of("&7Show only active Sell Orders."), "Click to filter"));
        inv.setItem(49, GuiTheme.button(Material.CHEST, "&eBack to Market",
                List.of("&7Return to market categories."), "Click to go back"));
        inv.setItem(50, GuiTheme.pageIndicator(page + 1, pages, "Matching active orders", all.size()));
        inv.setItem(51, GuiTheme.button(Material.WRITABLE_BOOK, "&b&lMY ORDERS",
                List.of(GuiTheme.stat("Your active orders", plugin.orders().owned(player.getUniqueId()).size())), "Click to manage"));
        inv.setItem(52, GuiTheme.button(Material.ENDER_CHEST, "&a&lCLAIM FILLED ITEMS",
                List.of(GuiTheme.stat("Waiting", plugin.orders().claimCount(player.getUniqueId()))), "Click to claim"));
        if (page + 1 < pages) inv.setItem(53, GuiTheme.nextPage(page + 2, true));

        player.openInventory(inv);
    }

    public MarketOrder globalOrderAt(String filter, int page, int slot) {
        if (slot < 0 || slot >= 45) return null;
        String f = filter == null ? "ALL" : filter.toUpperCase(java.util.Locale.ROOT);
        List<MarketOrder> all = plugin.orders().all().stream()
                .filter(o -> f.equals("ALL") || o.type().name().equals(f))
                .sorted(java.util.Comparator
                        .comparing((MarketOrder o) -> o.type() == MarketOrder.Type.BUY ? 0 : 1)
                        .thenComparing(MarketOrder::material)
                        .thenComparingLong(MarketOrder::createdAt))
                .toList();
        int index = page * 45 + slot;
        return index >= 0 && index < all.size() ? all.get(index) : null;
    }

    public void openMy(Player player, int requestedPage) {
        List<MarketOrder> own = plugin.orders().owned(player.getUniqueId());
        int pages = Math.max(1, (int)Math.ceil(own.size() / 45.0));
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        Inventory inv = Bukkit.createInventory(new MyOrdersHolder(page), 54, GuiTheme.title("&b&l", "My Orders"));
        fill(inv);
        int start = page * 45;
        for (int slot = 0; slot < 45 && start + slot < own.size(); slot++) {
            MarketOrder order = own.get(start + slot);
            ItemStack icon = orderIcon(order, order.type() == MarketOrder.Type.BUY);
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add(""); lore.add(Text.color("&c" + GuiTheme.CHEVRON + "Click to cancel and return escrow"));
            meta.setLore(lore); icon.setItemMeta(meta); inv.setItem(slot, icon);
        }
        if (page > 0) inv.setItem(45, GuiTheme.previousPage(page, true));
        inv.setItem(49, GuiTheme.panel(Material.BOOK, "&b&lMY MARKET ORDERS",
                List.of(GuiTheme.stat("Active", own.size()), GuiTheme.stat("Page", (page + 1) + "/" + pages))));
        inv.setItem(50, GuiTheme.button(Material.ENDER_CHEST, "&a&lCLAIM ITEMS",
                List.of(GuiTheme.stat("Waiting", plugin.orders().claimCount(player.getUniqueId()))), "Click to claim"));
        inv.setItem(48, GuiTheme.button(Material.CHEST, "&eBack to Market", List.of("&7Return to market categories."), "Click to go back"));
        if (page + 1 < pages) inv.setItem(53, GuiTheme.nextPage(page + 2, true));
        player.openInventory(inv);
    }

    public MarketOrder orderAt(Player player, int page, int slot) {
        if (slot < 0 || slot >= 45) return null;
        List<MarketOrder> own = plugin.orders().owned(player.getUniqueId());
        int index = page * 45 + slot;
        return index >= 0 && index < own.size() ? own.get(index) : null;
    }

    private ItemStack orderIcon(MarketOrder order, boolean buy) {
        Material iconMaterial = buy ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        ItemStack stack = new ItemStack(iconMaterial);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Text.color((buy ? "&aBuy " : "&cSell ") + Text.prettyEnum(order.material().name())));
        double progress = order.originalAmount() <= 0 ? 0
                : (order.originalAmount() - order.remaining()) / (double) order.originalAmount();
        meta.setLore(List.of(
                Text.color(GuiTheme.RULE),
                Text.color(GuiTheme.stat("Price each", plugin.money(order.unitPrice()))),
                Text.color(GuiTheme.stat("Remaining", order.remaining() + "&7/&f" + order.originalAmount())),
                Text.color(GuiTheme.bar(progress, 10, buy ? "&a" : "&c", "&8") + " &8filled"),
                Text.color(GuiTheme.stat("Owner", order.ownerName())),
                Text.color("&7ID: &8" + order.id().toString().substring(0, 8))
        ));
        stack.setItemMeta(meta);
        return stack;
    }

    private void fill(Inventory inv) {
        GuiTheme.fill(inv, GuiTheme.TRIM);
    }
}
