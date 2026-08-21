package com.snipeyfresh.incogshop.listener;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.gui.HexGui;
import com.snipeyfresh.incogshop.hex.HexResult;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Drives every Hex menu (MAIN, REFORGE, ENCHANT).
 *
 * All clicks are cancelled and applied by hand; the item being worked on lives
 * on the Holder so it can never be duplicated or lost through an unusual click.
 * When any Hex menu closes without handing the item to another Hex screen the
 * item is returned to the player automatically.
 */
public final class HexGuiListener implements Listener {
    private final IncogShopPlugin plugin;

    public HexGuiListener(IncogShopPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof HexGui.Holder holder)) return;
        event.setCancelled(true);

        int raw = event.getRawSlot();
        if (raw < 0) return;
        if (raw >= top.getSize()) {
            if (HexGui.MAIN.equals(holder.screen())) insert(player, holder, event);
            return;
        }

        switch (holder.screen()) {
            case HexGui.MAIN    -> onMainClick(player, holder, raw);
            case HexGui.REFORGE -> onReforgeClick(player, holder, raw);
            case HexGui.ENCHANT -> onEnchantClick(player, holder, raw);
            default -> player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof HexGui.Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof HexGui.Holder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        if (holder.moved()) return;
        ItemStack carried = holder.carried();
        if (carried == null) return;
        holder.setCarried(null);
        int stashed = plugin.stash().deliverOrStash(player, carried);
        player.sendMessage(plugin.prefix() + (stashed > 0
                ? "§dYour Hex item did not fit and was moved to /stash."
                : "§7Your Hex item was returned to your inventory."));
    }

    // ------------------------------------------------------------------ MAIN

    private void onMainClick(Player player, HexGui.Holder holder, int slot) {
        ItemStack carried = holder.carried();
        switch (slot) {
            case HexGui.SLOT_CLOSE -> player.closeInventory();
            case HexGui.ITEM_SLOT, HexGui.SLOT_TAKE -> takeBack(player, holder);
            case HexGui.SLOT_REFORGE -> {
                if (require(player, carried))
                    move(player, holder, () -> plugin.hexGui().openReforge(player, carried, 0));
            }
            case HexGui.SLOT_ENCHANT -> {
                if (require(player, carried))
                    move(player, holder, () -> plugin.hexGui().openEnchant(player, carried, 0));
            }
            default -> { }
        }
    }

    /** Player clicked an item in their own inventory while on the MAIN screen. */
    private void insert(Player player, HexGui.Holder holder, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (holder.carried() != null) {
            player.sendMessage(plugin.prefix() + "§eThe Hex already holds an item. Take it back first.");
            return;
        }
        if (clicked.getAmount() > 1) {
            player.sendMessage(plugin.prefix() + "§cThe Hex upgrades one item at a time, not a stack.");
            return;
        }
        HexResult blocked = plugin.hex().guard(clicked);
        if (blocked != null) {
            player.sendMessage(plugin.prefix() + "§c" + blocked.message());
            return;
        }
        ItemStack moved = clicked.clone();
        event.setCurrentItem(null);
        player.updateInventory();
        holder.setMoved(true);
        plugin.hexGui().open(player, moved);
    }

    private void takeBack(Player player, HexGui.Holder holder) {
        ItemStack carried = holder.carried();
        if (carried == null) return;
        holder.setCarried(null);
        int stashed = plugin.stash().deliverOrStash(player, carried);
        player.sendMessage(plugin.prefix() + (stashed > 0
                ? "§dThat item did not fit and was moved to /stash."
                : "§aItem returned to your inventory."));
        holder.setMoved(true);
        plugin.hexGui().open(player, null);
    }

    private boolean require(Player player, ItemStack carried) {
        if (carried != null) return true;
        player.sendMessage(plugin.prefix() + "§ePlace an item in the Hex slot first.");
        return false;
    }

    private void move(Player player, HexGui.Holder holder, Runnable opener) {
        holder.setMoved(true);
        opener.run();
    }

    // --------------------------------------------------------------- REFORGE

    private void onReforgeClick(Player player, HexGui.Holder holder, int slot) {
        ItemStack carried = holder.carried();

        if (slot == HexGui.SLOT_BACK) {
            move(player, holder, () -> plugin.hexGui().open(player, carried));
            return;
        }
        if (slot == HexGui.SLOT_PREVIOUS) {
            if (holder.page() > 0)
                move(player, holder, () -> plugin.hexGui().openReforge(player, carried, holder.page() - 1));
            else
                move(player, holder, () -> plugin.hexGui().open(player, carried));
            return;
        }
        if (slot == HexGui.SLOT_NEXT) {
            List<String> options = carried == null ? List.of() : plugin.hex().reforgeOptions(carried);
            int pages = Math.max(1, (options.size() + HexGui.LIST_PER_PAGE - 1) / HexGui.LIST_PER_PAGE);
            if (holder.page() + 1 < pages)
                move(player, holder, () -> plugin.hexGui().openReforge(player, carried, holder.page() + 1));
            else
                player.closeInventory();
            return;
        }
        if (slot < HexGui.LIST_START || slot >= HexGui.LIST_START + HexGui.LIST_PER_PAGE) return;
        if (!require(player, carried)) return;

        List<String> options = plugin.hex().reforgeOptions(carried);
        int index = holder.page() * HexGui.LIST_PER_PAGE + (slot - HexGui.LIST_START);
        if (index < 0 || index >= options.size()) return;

        HexResult result = plugin.hex().reforge(player, carried, options.get(index));
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        move(player, holder, () -> plugin.hexGui().openReforge(player, carried, holder.page()));
    }

    // --------------------------------------------------------------- ENCHANT

    private void onEnchantClick(Player player, HexGui.Holder holder, int slot) {
        ItemStack carried = holder.carried();

        if (slot == HexGui.SLOT_BACK) {
            move(player, holder, () -> plugin.hexGui().open(player, carried));
            return;
        }
        if (slot == HexGui.SLOT_PREVIOUS) {
            if (holder.page() > 0)
                move(player, holder, () -> plugin.hexGui().openEnchant(player, carried, holder.page() - 1));
            else
                move(player, holder, () -> plugin.hexGui().open(player, carried));
            return;
        }
        if (slot == HexGui.SLOT_NEXT) {
            List<Object> all = holder.enchantList() != null ? holder.enchantList() : List.of();
            int pages = Math.max(1, (all.size() + HexGui.LIST_PER_PAGE - 1) / HexGui.LIST_PER_PAGE);
            if (holder.page() + 1 < pages)
                move(player, holder, () -> plugin.hexGui().openEnchant(player, carried, holder.page() + 1));
            else
                player.closeInventory();
            return;
        }
        if (slot < HexGui.LIST_START || slot >= HexGui.LIST_START + HexGui.LIST_PER_PAGE) return;
        if (!require(player, carried)) return;

        List<Object> all = holder.enchantList() != null ? holder.enchantList() : List.of();
        int index = holder.page() * HexGui.LIST_PER_PAGE + (slot - HexGui.LIST_START);
        if (index < 0 || index >= all.size()) return;

        Object entry = all.get(index);
        HexResult result;
        if (entry instanceof String id) {
            result = plugin.hex().applyIncogRpgEnchant(player, carried, id);
        } else if (entry instanceof Enchantment ench) {
            result = plugin.hex().applyEeEnchant(player, carried, ench);
        } else {
            return;
        }
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        move(player, holder, () -> plugin.hexGui().openEnchant(player, carried, holder.page()));
    }
}
