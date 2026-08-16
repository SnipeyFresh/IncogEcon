package com.snipeyfresh.incogshop.listener;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.shop.PlayerShop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class ShopProtectionListener implements Listener {
    private final IncogShopPlugin plugin;
    public ShopProtectionListener(IncogShopPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        // Sell Wand clicks are handled by SellWandListener at HIGHEST priority.
        // Do not open the customer/player-shop GUI first and cancel the wand event.
        if (plugin.sellWands().isSellWand(event.getItem())) return;
        PlayerShop shop = plugin.shops().at(event.getClickedBlock());
        if (shop == null) return;

        Player player = event.getPlayer();
        boolean canManage = shop.owner().equals(player.getUniqueId())
                || player.hasPermission("incogshop.playershop.bypass");

        // Sneak-right-click is Minecraft's "do not interact with the block"
        // gesture, so relying on vanilla here meant owners could never open
        // the backing chest/barrel. Open it explicitly instead.
        if (canManage && player.isSneaking()) {
            event.setCancelled(true);
            if (!plugin.shops().openStockInventory(player, shop)) {
                player.sendMessage(plugin.prefix() + "§cThe physical shop container is missing or unavailable.");
            } else {
                player.sendMessage(plugin.prefix() + "§7Opened physical stock for §f"
                        + shop.item().getType().name() + "§7. Only matching items count as sellable stock.");
            }
            return;
        }

        event.setCancelled(true);
        plugin.gui().openPlayerShop(player, shop);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        PlayerShop exact = plugin.shops().at(event.getBlock());
        if (exact != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.prefix()
                    + "§cThis is a registered player shop. Use §f/pshop remove §cwhile looking at it before breaking the container.");
            return;
        }

        PlayerShop protecting = plugin.shops().protecting(event.getBlock().getLocation(), event.getPlayer());
        if (protecting == null) return;
        event.setCancelled(true);
        protectionMessage(event.getPlayer(), protecting);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        PlayerShop protecting = plugin.shops().protecting(event.getBlockPlaced().getLocation(), event.getPlayer());
        if (protecting == null) return;
        event.setCancelled(true);
        protectionMessage(event.getPlayer(), protecting);
    }

    @EventHandler
    public void onExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.shops().protectedLocation(block.getLocation()));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.shops().protectedLocation(block.getLocation()));
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (plugin.shops().protectedLocation(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        for (Block block : event.getBlocks()) {
            if (plugin.shops().protectedLocation(block.getLocation())
                    || plugin.shops().protectedLocation(block.getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (plugin.shops().protectedLocation(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        for (Block block : event.getBlocks()) {
            if (plugin.shops().protectedLocation(block.getLocation())
                    || plugin.shops().protectedLocation(block.getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        var source = event.getSource().getLocation();
        var destination = event.getDestination().getLocation();
        if ((source != null && plugin.shops().at(source) != null)
                || (destination != null && plugin.shops().at(destination) != null)) {
            event.setCancelled(true);
        }
    }

    private void protectionMessage(Player player, PlayerShop shop) {
        int radius = Math.max(0, plugin.getConfig().getInt("player-shops.protection-radius", 10));
        player.sendMessage(plugin.prefix() + "§cThis block is protected by §f" + shop.ownerName()
                + "§c's player shop §7(" + radius + " block radius)§c.");
    }
}
