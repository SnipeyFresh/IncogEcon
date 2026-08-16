package com.snipeyfresh.incogshop.sellwand;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class SellWandListener implements Listener {
    private final IncogShopPlugin plugin;

    public SellWandListener(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!plugin.sellWands().isSellWand(event.getItem())) return;

        event.setCancelled(true);
        var player = event.getPlayer();
        if (!player.hasPermission("incogshop.sellwand.use")) {
            player.sendMessage(plugin.prefix() + "§cYou do not have permission to use Sell Wands.");
            return;
        }
        if (!(event.getClickedBlock().getState() instanceof Container)) {
            player.sendMessage(plugin.prefix() + "§cSell Wands only work on storage containers.");
            return;
        }

        SellWandManager.SellResult result = plugin.sellWands().sellContainer(player, event.getClickedBlock());
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        if (result.success()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.35f);
        }
    }
}
