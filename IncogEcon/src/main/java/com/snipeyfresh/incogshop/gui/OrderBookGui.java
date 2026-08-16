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
                Text.color("&8Bazaar &7• &f" + Text.prettyEnum(material.name())));
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
        lore.add(Text.color("&8────────────────"));
        lore.add(Text.color("&7Category: &f" + plugin.market().categoryOf(material).display()));
        lore.add(Text.color("&7Section: &f" + plugin.market().subcategoryOf(material).display()));
        lore.add(Text.color("&7Server Stock: &f" + (infiniteStock ? "Unlimited" : stock)));
        lore.add("");
        lore.add(Text.color("&aInstant Buy: " + (canBuy ? "&f" + plugin.money(instantBuy) + " &7each" : "&8Unavailable")));
        lore.add(Text.color("&cInstant Sell: " + (canSell ? "&f" + plugin.money(instantSell) + " &7each" : "&8Unavailable")));
        lore.add(Text.color("&7Your eligible items: &f" + eligibleOwned));
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

        inv.setItem(9, ShopGui.named(Material.LIME_STAINED_GLASS_PANE, "&aTop Buy Orders",
                List.of("&7Highest player bid first.", "&7Older orders win price ties.")));
        inv.setItem(27, ShopGui.named(Material.RED_STAINED_GLASS_PANE, "&cTop Sell Orders",
                List.of("&7Lowest player ask first.", "&7Older orders win price ties.")));

        inv.setItem(20, ShopGui.named(canBuy ? Material.EMERALD_BLOCK : Material.BARRIER,
                canBuy ? "&a&lBUY 1" : "&c&lBUY UNAVAILABLE",
                canBuy
                        ? List.of(
                                "&7Buy exactly one item from server stock.",
                                "&7Price: &f" + plugin.money(instantBuy),
                                "",
                                "&eClick to buy 1",
                                "&8Works the same on Java and Bedrock.")
                        : List.of("&7This item cannot currently be", "&7bought directly from server stock.")));

        inv.setItem(21, ShopGui.named(canBuy ? Material.OAK_SIGN : Material.BARRIER,
                canBuy ? "&aCustom Buy Amount" : "&cCustom Buy Unavailable",
                canBuy
                        ? List.of(
                                "&7Choose an exact amount to buy",
                                "&7from the server market.",
                                "",
                                "&7Price each: &f" + plugin.money(instantBuy),
                                "&7Available stock: &f" + (infiniteStock ? "Unlimited" : stock),
                                "",
                                "&eClick, then type the amount in chat",
                                "&8Examples: 5, 128, 2k")
                        : List.of("&7This item cannot currently be", "&7bought directly from server stock.")));

        inv.setItem(22, ShopGui.named(canBuy ? Material.EMERALD : Material.BARRIER,
                canBuy ? "&aBuy " + stackSize : "&cBuy Stack Unavailable",
                canBuy
                        ? List.of(
                                "&7Buy up to one full material stack.",
                                "&7Amount: &f" + stackSize,
                                "&7Price each: &f" + plugin.money(instantBuy),
                                "",
                                "&eClick to buy")
                        : List.of("&7This item cannot currently be", "&7bought directly from server stock.")));

        inv.setItem(24, ShopGui.named(canSell ? Material.REDSTONE_BLOCK : Material.BARRIER,
                canSell ? "&c&lSELL 1" : "&c&lSELL UNAVAILABLE",
                canSell
                        ? List.of(
                                "&7Sell exactly one eligible item",
                                "&7to the server market.",
                                "&7Price: &f" + plugin.money(instantSell),
                                "",
                                "&eClick to sell 1",
                                "&8Transaction fee is deducted from payout.")
                        : List.of("&7This item cannot currently", "&7be sold to the server market.")));

        inv.setItem(25, ShopGui.named(canSell ? Material.REDSTONE : Material.BARRIER,
                canSell ? "&cSell up to " + stackSize : "&cSell Stack Unavailable",
                canSell
                        ? List.of(
                                "&7Sell up to one full material stack.",
                                "&7Maximum amount: &f" + stackSize,
                                "&7Eligible in inventory: &f" + eligibleOwned,
                                "&7Price each: &f" + plugin.money(instantSell),
                                "",
                                "&eClick to sell")
                        : List.of("&7This item cannot currently", "&7be sold to the server market.")));

        inv.setItem(45, ShopGui.named(Material.ARROW, "&eBack to Bazaar",
                List.of("&7Return to market categories.")));
        inv.setItem(46, ShopGui.named(Material.WRITABLE_BOOK, "&bAll Market Orders",
                List.of("&7Browse active Buy and Sell Orders", "&7for every Bazaar item.", "", "&eClick to browse")));
        inv.setItem(47, ShopGui.named(Material.EMERALD, "&aCreate Buy Order",
                List.of("&7Escrow money now and wait", "&7for matching Sell Orders.", "", "&eClick to enter amount + price")));
        inv.setItem(49, ShopGui.named(Material.GOLD_INGOT,
                "&6Balance: &f" + plugin.money(plugin.wallets().get(player.getUniqueId())),
                List.of("&7Economy: &f" + plugin.wallets().providerName())));
        inv.setItem(51, ShopGui.named(Material.REDSTONE, "&cCreate Sell Order",
                List.of("&7Escrow eligible items now and wait", "&7for matching Buy Orders.", "", "&eClick to enter amount + price")));
        inv.setItem(52, ShopGui.named(Material.BOOK, "&bMy Orders",
                List.of("&7Active orders: &f" + plugin.orders().owned(player.getUniqueId()).size(), "", "&eClick to manage")));
        inv.setItem(53, ShopGui.named(Material.ENDER_CHEST, "&aClaim Filled Items",
                List.of("&7Claimable items: &f" + plugin.orders().claimCount(player.getUniqueId()), "", "&eClick to claim")));
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
                Text.color("&8IncogEcon &7• &fAll Market Orders"));
        fill(inv);

        int start = page * perPage;
        for (int slot = 0; slot < perPage && start + slot < all.size(); slot++) {
            MarketOrder order = all.get(start + slot);
            ItemStack icon = orderIcon(order, order.type() == MarketOrder.Type.BUY);
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add("");
            lore.add(Text.color("&eClick to open this item's Bazaar page"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot, icon);
        }

        if (page > 0) inv.setItem(45, ShopGui.named(Material.ARROW, "&ePrevious Page", List.of()));
        inv.setItem(46, ShopGui.named(Material.BOOK, filter.equals("ALL") ? "&aAll Orders" : "&7All Orders",
                List.of("&7Show Buy and Sell Orders.", "", "&eClick to filter")));
        inv.setItem(47, ShopGui.named(Material.LIME_CONCRETE, filter.equals("BUY") ? "&aBuy Orders ✓" : "&aBuy Orders",
                List.of("&7Show only active Buy Orders.", "", "&eClick to filter")));
        inv.setItem(48, ShopGui.named(Material.RED_CONCRETE, filter.equals("SELL") ? "&cSell Orders ✓" : "&cSell Orders",
                List.of("&7Show only active Sell Orders.", "", "&eClick to filter")));
        inv.setItem(49, ShopGui.named(Material.CHEST, "&eBack to Market",
                List.of("&7Return to market categories.")));
        inv.setItem(50, ShopGui.named(Material.PAPER, "&fPage &e" + (page + 1) + "&7/&e" + pages,
                List.of("&7Matching active orders: &f" + all.size())));
        inv.setItem(51, ShopGui.named(Material.WRITABLE_BOOK, "&bMy Orders",
                List.of("&7Your active orders: &f" + plugin.orders().owned(player.getUniqueId()).size(), "", "&eClick to manage")));
        inv.setItem(52, ShopGui.named(Material.ENDER_CHEST, "&aClaim Filled Items",
                List.of("&7Waiting: &f" + plugin.orders().claimCount(player.getUniqueId()), "", "&eClick to claim")));
        if (page + 1 < pages) inv.setItem(53, ShopGui.named(Material.ARROW, "&eNext Page", List.of()));

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
        Inventory inv = Bukkit.createInventory(new MyOrdersHolder(page), 54, Text.color("&8IncogEcon &7• &fMy Orders"));
        fill(inv);
        int start = page * 45;
        for (int slot = 0; slot < 45 && start + slot < own.size(); slot++) {
            MarketOrder order = own.get(start + slot);
            ItemStack icon = orderIcon(order, order.type() == MarketOrder.Type.BUY);
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
            lore.add(""); lore.add(Text.color("&cClick to cancel and return escrow"));
            meta.setLore(lore); icon.setItemMeta(meta); inv.setItem(slot, icon);
        }
        if (page > 0) inv.setItem(45, ShopGui.named(Material.ARROW, "&ePrevious Page", List.of()));
        inv.setItem(49, ShopGui.named(Material.BOOK, "&bMy Market Orders", List.of("&7Active: &f" + own.size(), "&7Page: &f" + (page + 1) + "/" + pages)));
        inv.setItem(50, ShopGui.named(Material.ENDER_CHEST, "&aClaim Items", List.of("&7Waiting: &f" + plugin.orders().claimCount(player.getUniqueId()))));
        inv.setItem(48, ShopGui.named(Material.CHEST, "&eBack to Market", List.of()));
        if (page + 1 < pages) inv.setItem(53, ShopGui.named(Material.ARROW, "&eNext Page", List.of()));
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
        meta.setLore(List.of(
                Text.color("&7Price each: &f" + plugin.money(order.unitPrice())),
                Text.color("&7Remaining: &f" + order.remaining() + "&7/&f" + order.originalAmount()),
                Text.color("&7Owner: &f" + order.ownerName()),
                Text.color("&7ID: &8" + order.id().toString().substring(0, 8))
        ));
        stack.setItemMeta(meta);
        return stack;
    }

    private void fill(Inventory inv) {
        ItemStack filler = ShopGui.named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }
}
