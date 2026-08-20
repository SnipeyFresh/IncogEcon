package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class SellGui {
    public static final int SELL_SLOTS = 45;
    public static final int CONFIRM_SLOT = 49;
    public static final int CANCEL_SLOT = 45;
    private final IncogShopPlugin plugin;

    public SellGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    public static final class Holder implements InventoryHolder {
        private boolean processed;
        private boolean cancelled;
        @Override public Inventory getInventory() { return null; }
        public boolean processed() { return processed; }
        public boolean cancelled() { return cancelled; }
        public void setProcessed(boolean value) { processed = value; }
        public void setCancelled(boolean value) { cancelled = value; }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new Holder(), 54, GuiTheme.title("&a&l", "Sell Items"));
        GuiTheme.bottomBar(inv, Material.LIME_STAINED_GLASS_PANE);
        inv.setItem(CANCEL_SLOT, GuiTheme.button(Material.BARRIER, "&c&lCANCEL", List.of("&7Return every item without selling."), "Click to cancel"));
        inv.setItem(47, GuiTheme.panel(Material.BOOK, "&e&lHOW THIS WORKS", List.of(
                "&7Place eligible market items in the",
                "&7top five rows.", "",
                "&aClosing the menu sells everything.",
                "&aSell Eligible Items does the same.", "",
                "&8Ineligible items are returned or stashed.")));
        inv.setItem(CONFIRM_SLOT, GuiTheme.button(Material.EMERALD_BLOCK, "&a&lSELL ELIGIBLE ITEMS", List.of(
                "&7The server adds sold items to stock",
                "&7and pays your balance."), "Click to sell now"));
        inv.setItem(51, GuiTheme.panel(Material.GOLD_INGOT, "&6&lBALANCE &8• &f" + plugin.money(plugin.wallets().get(player.getUniqueId())),
                List.of(GuiTheme.stat("Economy", plugin.wallets().providerName()))));
        player.openInventory(inv);
    }
}
