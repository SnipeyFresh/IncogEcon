package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.auction.AuctionListing;
import com.snipeyfresh.incogshop.auction.AuctionManager;
import com.snipeyfresh.incogshop.economy.WalletManager;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AuctionGui {
    private final IncogShopPlugin plugin;
    public AuctionGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    public record Holder(int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public record MyHolder(UUID owner, int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public record DetailHolder(UUID listingId, int returnPage, boolean fromMyListings) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public record ClaimsHolder(UUID owner, int page) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
    public record CreateHolder(AuctionListing.Mode mode, double price, int hours) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public void open(Player player, int page) {
        List<AuctionListing> listings = plugin.auctions().activeListings();
        int perPage = 45;
        int pages = Math.max(1, (listings.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        Inventory inv = Bukkit.createInventory(new Holder(page), 54, GuiTheme.title("&6&l", "Auction House"));
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < listings.size(); i++) inv.setItem(i, icon(listings.get(start + i)));
        GuiTheme.bottomBar(inv, Material.ORANGE_STAINED_GLASS_PANE);
        if (page > 0) inv.setItem(45, GuiTheme.previousPage(page, true));
        inv.setItem(46, GuiTheme.button(Material.ANVIL, "&a&lCREATE LISTING", List.of("&7List the item in your main hand", "&7through an easy setup menu."), "Click to create"));
        inv.setItem(47, GuiTheme.button(Material.CHEST, "&6&lMY AUCTIONS", List.of("&7View and manage your active", "&7Auction House listings."), "Click to open"));
        inv.setItem(49, GuiTheme.panel(Material.GOLD_INGOT, "&6&lBALANCE &8• &f" + plugin.money(plugin.wallets().get(player.getUniqueId())),
                List.of(GuiTheme.stat("Economy", plugin.wallets().providerName()),
                        GuiTheme.stat("Live listings", listings.size()))));
        if (player.hasPermission("incogshop.auction.admin")) {
            boolean permanent = plugin.auctions().permanentMode(player);
            inv.setItem(50, GuiTheme.toggle(permanent, "ADMIN PERMANENT LISTINGS",
                    permanent ? List.of("&7New Auction House listings you create", "&7never expire until sold or cancelled.")
                              : List.of("&7Admin-only setting.", "&7When enabled, new listings never expire."),
                    permanent ? "Click to disable" : "Click to enable"));
        }
        inv.setItem(51, GuiTheme.button(Material.ENDER_CHEST, "&a&lCLAIMS &8• &f" + plugin.auctions().claimCount(player.getUniqueId()),
                List.of("&7Won or returned items wait here."), "Click to open claims"));
        if (page < pages - 1) inv.setItem(53, GuiTheme.nextPage(page + 2, true));
        player.openInventory(inv);
    }

    public void openCreate(Player player) {
        openCreate(player, AuctionListing.Mode.AUCTION, 0, plugin.getConfig().getInt("auction-house.default-duration-hours", 24));
    }

    public void openCreate(Player player, AuctionListing.Mode mode, double price, int hours) {
        int maxHours = Math.max(1, plugin.getConfig().getInt("auction-house.maximum-duration-hours", 168));
        hours = Math.max(1, Math.min(maxHours, hours));

        Inventory inv = Bukkit.createInventory(new CreateHolder(mode, price, hours), 45,
                GuiTheme.title("&a&l", "Create Listing"));
        GuiTheme.fill(inv, GuiTheme.TRIM);

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            inv.setItem(13, GuiTheme.button(Material.BARRIER, "&c&lNO ITEM SELECTED",
                    List.of("&7Put the item or stack you want to list", "&7in your main hand, then refresh."), "Click to refresh"));
        } else {
            ItemStack preview = held.clone();
            ItemMeta meta = preview.getItemMeta();
            List<String> lore = meta.hasLore() && meta.getLore()!=null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(Text.color(GuiTheme.RULE));
            lore.add(Text.color(GuiTheme.stat("Listing stack", held.getAmount()+"x "+Text.prettyEnum(held.getType().name()))));
            lore.add(Text.color("&7This exact held stack is removed only"));
            lore.add(Text.color("&7after the listing is successfully created."));
            meta.setLore(lore);
            preview.setItemMeta(meta);
            inv.setItem(13, preview);
        }

        inv.setItem(20, GuiTheme.button(mode == AuctionListing.Mode.AUCTION ? Material.GOLD_INGOT : Material.EMERALD,
                mode == AuctionListing.Mode.AUCTION ? "&6&lMODE &8• &fAuction" : "&a&lMODE &8• &fBuy It Now",
                mode == AuctionListing.Mode.AUCTION
                        ? List.of("&7Players compete by bidding.")
                        : List.of("&7First player to pay the price wins."),
                mode == AuctionListing.Mode.AUCTION ? "Click to switch to Buy It Now" : "Click to switch to Auction"));

        inv.setItem(22, GuiTheme.button(Material.PAPER, price > 0 ? "&b&lPRICE &8• &f"+plugin.money(price) : "&b&lSET PRICE",
                List.of(price > 0 ? GuiTheme.stat("Price / start bid", plugin.money(price)) : "&7No price selected yet.",
                        "&8Supports 10k, 2.5m and 1b."),
                "Click, then type a value in chat"));

        boolean permanent = player.hasPermission("incogshop.auction.admin") && plugin.auctions().permanentMode(player);
        inv.setItem(24, GuiTheme.panel(permanent ? Material.CLOCK : Material.REPEATER,
                permanent ? "&d&lDURATION &8• &fNever expires" : "&e&lDURATION &8• &f"+hours+"h",
                permanent
                        ? List.of("&7Your admin permanent-listing mode", "&7is currently enabled.", "", "&dThis listing will not expire.")
                        : List.of("&7Use the four buttons below to", "&7change the listing duration.",
                                  GuiTheme.bar(hours / (double) maxHours, 10, "&e", "&8"),
                                  GuiTheme.stat("Maximum", maxHours + " hours"))));
        if (!permanent) {
            inv.setItem(29, GuiTheme.button(Material.RED_CONCRETE, "&c-24 Hours", List.of("&7Shorten the listing."), "Click to subtract 24 hours"));
            inv.setItem(30, GuiTheme.button(Material.RED_STAINED_GLASS_PANE, "&c-1 Hour", List.of("&7Shorten the listing."), "Click to subtract 1 hour"));
            inv.setItem(32, GuiTheme.button(Material.LIME_STAINED_GLASS_PANE, "&a+1 Hour", List.of("&7Extend the listing."), "Click to add 1 hour"));
            inv.setItem(33, GuiTheme.button(Material.LIME_CONCRETE, "&a+24 Hours", List.of("&7Extend the listing."), "Click to add 24 hours"));
        }

        double fee=Math.max(0,plugin.getConfig().getDouble("auction-house.listing-fee",25));
        List<String> confirmLore=new ArrayList<>();
        confirmLore.add(GuiTheme.stat("Mode", mode==AuctionListing.Mode.AUCTION?"Auction":"Buy It Now"));
        confirmLore.add(GuiTheme.stat("Price", price>0?plugin.money(price):"Not set"));
        confirmLore.add(GuiTheme.stat("Duration", permanent?"Never":hours+"h"));
        confirmLore.add(GuiTheme.stat("Listing fee", plugin.money(fee)));
        confirmLore.add(GuiTheme.stat("Balance", plugin.money(plugin.wallets().get(player.getUniqueId()))));
        boolean ready = price>0 && held!=null && !held.getType().isAir();
        inv.setItem(31, ready
                ? GuiTheme.button(Material.LIME_CONCRETE, "&a&lCREATE LISTING", confirmLore, "Click to create listing")
                : GuiTheme.locked(Material.RED_CONCRETE, "&cCANNOT CREATE YET", confirmLore, "Select an item and a price first"));

        inv.setItem(36, GuiTheme.back("the Auction House"));
        inv.setItem(40, GuiTheme.button(Material.SUNFLOWER, "&eRefresh Held Item", List.of("&7Refresh the preview from your main hand."), "Click to refresh"));
        player.openInventory(inv);
    }

    public void openMy(Player player, int page) {
        List<AuctionListing> listings = plugin.auctions().activeListings().stream()
                .filter(l -> l.seller().equals(player.getUniqueId()))
                .toList();
        int perPage = 45;
        int pages = Math.max(1, (listings.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        Inventory inv = Bukkit.createInventory(new MyHolder(player.getUniqueId(), page), 54, GuiTheme.title("&6&l", "My Auctions"));
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < listings.size(); i++) {
            AuctionListing listing = listings.get(start + i);
            ItemStack item = icon(listing);
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(Text.color(listing.mode() == AuctionListing.Mode.AUCTION && listing.hasBid()
                    ? "&cThis auction has a bid and cannot be cancelled."
                    : "&c" + GuiTheme.CHEVRON + "Click to manage or cancel"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        GuiTheme.bottomBar(inv, Material.ORANGE_STAINED_GLASS_PANE);
        if (page > 0) inv.setItem(45, GuiTheme.previousPage(page, true));
        inv.setItem(49, GuiTheme.button(Material.BARRIER, "&cBack to Auction House", List.of("&7Return to all listings."), "Click to go back"));
        inv.setItem(51, GuiTheme.button(Material.ENDER_CHEST, "&a&lCLAIMS &8• &f" + plugin.auctions().claimCount(player.getUniqueId()),
                List.of("&7Cancelled and won items wait here."), "Click to open claims"));
        if (page < pages - 1) inv.setItem(53, GuiTheme.nextPage(page + 2, true));
        player.openInventory(inv);
    }

    public void openClaims(Player player, int requestedPage) {
        List<ItemStack> claims = plugin.auctions().claimItems(player.getUniqueId());
        int pages = Math.max(1, (claims.size() + 44) / 45);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        Inventory inv = Bukkit.createInventory(new ClaimsHolder(player.getUniqueId(), page), 54,
                GuiTheme.title("&a&l", "Auction Claims"));
        int start = page * 45;
        for (int i=0;i<45 && start+i<claims.size();i++) {
            ItemStack item = claims.get(start+i).clone();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.hasLore() && meta.getLore()!=null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(""); lore.add(Text.color(GuiTheme.RULE));
            lore.add(Text.color("&e" + GuiTheme.CHEVRON + "Click to claim"));
            meta.setLore(lore); item.setItemMeta(meta); inv.setItem(i,item);
        }
        GuiTheme.bottomBar(inv, Material.LIME_STAINED_GLASS_PANE);
        if (page>0) inv.setItem(45, GuiTheme.previousPage(page, true));
        inv.setItem(47, GuiTheme.button(Material.CHEST,"&a&lCLAIM ALL",List.of("&7Claim all Auction House items.", "&8Overflow goes safely to /stash."),"Click to claim"));
        inv.setItem(49, GuiTheme.button(Material.BARRIER,"&cBack to Auction House",List.of("&7Return to all listings."),"Click to go back"));
        inv.setItem(51, GuiTheme.panel(Material.ENDER_CHEST,"&a&lCLAIMS &8• &f"+claims.size(),
                List.of(GuiTheme.stat("Page", (page+1)+"/"+pages),"&7Click individual items or Claim All.")));
        if (page<pages-1) inv.setItem(53, GuiTheme.nextPage(page+2, true));
        player.openInventory(inv);
    }

    public void openDetail(Player player, AuctionListing listing, int returnPage) {
        openDetail(player, listing, returnPage, false);
    }

    public void openDetail(Player player, AuctionListing listing, int returnPage, boolean fromMyListings) {
        if (listing == null) {
            if (fromMyListings) openMy(player, returnPage); else open(player, returnPage);
            return;
        }
        Inventory inv = Bukkit.createInventory(new DetailHolder(listing.id(), returnPage, fromMyListings), 45, GuiTheme.title("&6&l", "Listing Details"));
        GuiTheme.fill(inv, GuiTheme.TRIM);
        inv.setItem(13, icon(listing));
        inv.setItem(40, GuiTheme.back(fromMyListings ? "your listings" : "the listing browser"));
        boolean owner = listing.seller().equals(player.getUniqueId());
        if (owner) {
            boolean canCancel = listing.mode() != AuctionListing.Mode.AUCTION || !listing.hasBid();
            inv.setItem(36, canCancel
                    ? GuiTheme.button(Material.BARRIER, "&c&lCANCEL LISTING",
                            List.of("&7The item will be moved to", "&7your Auction House claims."), "Click to cancel this listing")
                    : GuiTheme.locked(Material.RED_STAINED_GLASS_PANE, "CANCELLATION LOCKED",
                            List.of("&7This auction already has a bid."), "Auctions with bids cannot be cancelled"));
        }

        if (listing.mode() == AuctionListing.Mode.BUY_NOW) {
            inv.setItem(31, GuiTheme.button(Material.EMERALD_BLOCK, "&a&lBUY IT NOW", List.of(
                    GuiTheme.stat("Price", plugin.money(listing.buyNowPrice())),
                    GuiTheme.stat("Your balance", plugin.money(plugin.wallets().get(player.getUniqueId())))
            ), "Click to purchase"));
        } else {
            double minimum = minimumBid(listing);
            inv.setItem(28, GuiTheme.button(Material.GOLD_NUGGET, "&eMinimum Bid",
                    List.of(GuiTheme.stat("Bid", plugin.money(minimum))), "Click to place bid"));
            inv.setItem(30, GuiTheme.button(Material.GOLD_INGOT, "&6Bid +10%",
                    List.of(GuiTheme.stat("Bid", plugin.money(WalletManager.round(minimum * 1.10)))), "Click to place bid"));
            inv.setItem(32, GuiTheme.button(Material.GOLD_BLOCK, "&6Bid +25%",
                    List.of(GuiTheme.stat("Bid", plugin.money(WalletManager.round(minimum * 1.25)))), "Click to place bid"));
            inv.setItem(34, GuiTheme.button(Material.OAK_SIGN, "&bCustom Bid",
                    List.of("&7Enter any bid at or above", "&f" + plugin.money(minimum)), "Click, then type amount in chat"));
        }
        player.openInventory(inv);
    }

    public double minimumBid(AuctionListing listing) {
        if (!listing.hasBid()) return listing.startPrice();
        double increment = Math.max(plugin.getConfig().getDouble("auction-house.minimum-bid-increment", 1),
                listing.highBid() * Math.max(0, plugin.getConfig().getDouble("auction-house.minimum-bid-increment-percent", 5)) / 100.0);
        return WalletManager.round(listing.highBid() + increment);
    }

    private ItemStack icon(AuctionListing listing) {
        ItemStack stack = listing.item();
        ItemMeta meta = stack.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.isEmpty()) lore.add("");
        lore.add(Text.color(GuiTheme.RULE));
        lore.add(Text.color(GuiTheme.stat("Seller", listing.sellerName())));
        lore.add(Text.color(GuiTheme.stat("ID", AuctionManager.shortId(listing.id()))));
        if (listing.mode() == AuctionListing.Mode.BUY_NOW) {
            lore.add(Text.color("&7Mode: &aBuy It Now"));
            lore.add(Text.color(GuiTheme.stat("Price", plugin.money(listing.buyNowPrice()))));
            lore.add("");
            lore.add(Text.color("&e" + GuiTheme.CHEVRON + "Click to view or purchase"));
        } else {
            lore.add(Text.color("&7Mode: &6Auction"));
            lore.add(Text.color(listing.hasBid() ? GuiTheme.stat("Current bid", plugin.money(listing.highBid())) : GuiTheme.stat("Starting bid", plugin.money(listing.startPrice()))));
            lore.add(Text.color(listing.hasBid() ? GuiTheme.stat("High bidder", listing.highBidderName()) : "&7No bids yet"));
            lore.add(Text.color(GuiTheme.stat("Minimum next bid", plugin.money(minimumBid(listing)))));
            lore.add("");
            lore.add(Text.color("&e" + GuiTheme.CHEVRON + "Click to open the bidding menu"));
        }
        lore.add(Text.color(listing.neverExpires() ? "&7Expires: &dNever" : GuiTheme.stat("Ends in", remaining(listing.expiresAt()))));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String remaining(long end) {
        long sec = Math.max(0, Duration.ofMillis(end - System.currentTimeMillis()).toSeconds());
        long h = sec / 3600; long m = (sec % 3600) / 60; long s = sec % 60;
        return h > 0 ? h + "h " + m + "m" : m > 0 ? m + "m " + s + "s" : s + "s";
    }
}
