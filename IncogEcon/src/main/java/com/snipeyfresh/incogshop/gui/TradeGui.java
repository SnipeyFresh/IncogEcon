package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.trade.TradeManager;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public final class TradeGui {
    public static final int[] LEFT_ITEMS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int[] RIGHT_ITEMS = {14, 15, 16, 23, 24, 25, 32, 33, 34};
    public static final int LEFT_MONEY = 37;
    public static final int RIGHT_MONEY = 43;
    public static final int LEFT_CONFIRM = 46;
    public static final int CANCEL = 49;
    public static final int RIGHT_CONFIRM = 52;

    public record Holder(UUID sessionId) implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    private final IncogShopPlugin plugin;

    public TradeGui(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, TradeManager.TradeSession session) {
        Inventory inv = Bukkit.createInventory(new Holder(session.id()), 54, GuiTheme.title("&a&l", "Player Trade"));
        render(inv, session);
        player.openInventory(inv);
    }

    public void refresh(TradeManager.TradeSession session) {
        Player left = Bukkit.getPlayer(session.left());
        Player right = Bukkit.getPlayer(session.right());
        renderIfOpen(left, session);
        renderIfOpen(right, session);
    }

    private void renderIfOpen(Player player, TradeManager.TradeSession session) {
        if (player == null || !player.isOnline()) return;
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof Holder holder) || !holder.sessionId().equals(session.id())) return;
        render(top, session);
    }

    public void render(Inventory inv, TradeManager.TradeSession session) {
        GuiTheme.fill(inv, GuiTheme.TRIM);

        for (int slot : LEFT_ITEMS) inv.setItem(slot, null);
        for (int slot : RIGHT_ITEMS) inv.setItem(slot, null);

        ItemStack[] left = session.items(session.left());
        ItemStack[] right = session.items(session.right());
        for (int i = 0; i < LEFT_ITEMS.length; i++) if (left[i] != null) inv.setItem(LEFT_ITEMS[i], left[i]);
        for (int i = 0; i < RIGHT_ITEMS.length; i++) if (right[i] != null) inv.setItem(RIGHT_ITEMS[i], right[i]);

        // Centre column separates the two sides of the trade.
        for (int slot : new int[]{4, 13, 22, 31, 40}) {
            inv.setItem(slot, GuiTheme.pane(Material.GRAY_STAINED_GLASS_PANE));
        }

        inv.setItem(1, playerHead(session.left(), session.leftName(), "&a&l" + session.leftName(), List.of(
                GuiTheme.RULE,
                GuiTheme.stat("Items offered", session.itemCount(session.left())),
                GuiTheme.stat("Money offered", plugin.money(session.money(session.left()))),
                "&7Status: " + (session.confirmed(session.left()) ? "&a✔ Confirmed" : "&e✖ Not confirmed")
        )));
        inv.setItem(7, playerHead(session.right(), session.rightName(), "&a&l" + session.rightName(), List.of(
                GuiTheme.RULE,
                GuiTheme.stat("Items offered", session.itemCount(session.right())),
                GuiTheme.stat("Money offered", plugin.money(session.money(session.right()))),
                "&7Status: " + (session.confirmed(session.right()) ? "&a✔ Confirmed" : "&e✖ Not confirmed")
        )));

        inv.setItem(LEFT_MONEY, GuiTheme.button(Material.GOLD_INGOT, "&6" + session.leftName() + "'s Money Offer",
                List.of("&f" + plugin.money(session.money(session.left())), "&8Use 0 to clear the offer."),
                "Click, then type an exact amount in chat"));
        inv.setItem(RIGHT_MONEY, GuiTheme.button(Material.GOLD_INGOT, "&6" + session.rightName() + "'s Money Offer",
                List.of("&f" + plugin.money(session.money(session.right())), "&8Use 0 to clear the offer."),
                "Click, then type an exact amount in chat"));

        inv.setItem(LEFT_CONFIRM, confirmButton(session.leftName(), session.confirmed(session.left())));
        inv.setItem(RIGHT_CONFIRM, confirmButton(session.rightName(), session.confirmed(session.right())));
        inv.setItem(CANCEL, GuiTheme.button(Material.BARRIER, "&c&lCANCEL TRADE", List.of("&7Returns all offered items."), "Click to cancel"));

        inv.setItem(4, GuiTheme.panel(Material.EMERALD, "&a&lSECURE PLAYER TRADE", List.of(
                "&7Tap/click an item in your inventory",
                "&7to add the entire stack to your side.",
                "&7Click your offered item to take it back.",
                "", "&cAny offer change resets both confirmations."
        )));
    }

    private ItemStack confirmButton(String name, boolean confirmed) {
        return confirmed
                ? GuiTheme.panel(Material.LIME_CONCRETE, "&a&l" + name + " CONFIRMED",
                        List.of("&aReady to trade.", "&7Waiting for both players."))
                : GuiTheme.button(Material.YELLOW_CONCRETE, "&e&l" + name + " CONFIRM",
                        List.of("&7Review both sides carefully."), "Click to confirm");
    }

    private ItemStack playerHead(UUID uuid, String fallbackName, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
            skull.setOwningPlayer(owner);
            skull.setDisplayName(Text.color(displayName));
            skull.setLore(lore.stream().map(Text::color).toList());
            head.setItemMeta(skull);
            return head;
        }
        meta.setDisplayName(Text.color(fallbackName));
        meta.setLore(lore.stream().map(Text::color).toList());
        head.setItemMeta(meta);
        return head;
    }

    public static int offerIndex(UUID viewer, TradeManager.TradeSession session, int rawSlot) {
        int[] own = session.isLeft(viewer) ? LEFT_ITEMS : RIGHT_ITEMS;
        for (int i = 0; i < own.length; i++) if (own[i] == rawSlot) return i;
        return -1;
    }

    public static boolean isOtherOfferSlot(UUID viewer, TradeManager.TradeSession session, int rawSlot) {
        int[] other = session.isLeft(viewer) ? RIGHT_ITEMS : LEFT_ITEMS;
        for (int slot : other) if (slot == rawSlot) return true;
        return false;
    }
}
