package com.snipeyfresh.incogshop.sellwand;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.market.MarketManager;
import com.snipeyfresh.incogshop.shop.PlayerShop;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class SellWandManager {
    public record SellResult(boolean success, String message, int itemsSold, int stacksSold, double payout) {}

    private final IncogShopPlugin plugin;
    private final NamespacedKey wandKey;

    public SellWandManager(IncogShopPlugin plugin) {
        this.plugin = plugin;
        this.wandKey = new NamespacedKey(plugin, "sell_wand");
    }

    public ItemStack createWand() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("sell-wands.material", "BLAZE_ROD"));
        if (material == null || material.isAir() || !material.isItem()) material = Material.BLAZE_ROD;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Text.color(plugin.getConfig().getString("sell-wands.name", "&6&lSell Wand")));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("sell-wands.lore")) lore.add(Text.color(line));
        if (lore.isEmpty()) {
            lore.add(Text.color("&7Use/interact with a storage container"));
            lore.add(Text.color("&7to sell all eligible market items."));
            lore.add("");
            lore.add(Text.color("&eUnlimited Uses"));
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSellWand(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        Byte tagged = item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.BYTE);
        return tagged != null && tagged == (byte) 1;
    }

    public SellResult sellContainer(Player player, Block block) {
        if (!plugin.getConfig().getBoolean("sell-wands.enabled", true)) {
            return new SellResult(false, "Sell Wands are disabled.", 0, 0, 0.0);
        }
        if (player == null || block == null || !(block.getState() instanceof Container container)) {
            return new SellResult(false, "That block is not a supported storage container.", 0, 0, 0.0);
        }

        PlayerShop exactShop = plugin.shops().at(block);
        if (exactShop != null
                && !exactShop.owner().equals(player.getUniqueId())
                && !player.hasPermission("incogshop.playershop.bypass")) {
            return new SellResult(false, "You cannot use a Sell Wand on another player's shop stock.", 0, 0, 0.0);
        }

        PlayerShop protecting = plugin.shops().protecting(block.getLocation(), player);
        if (protecting != null) {
            return new SellResult(false, "That container is protected by " + protecting.ownerName() + "'s player shop.", 0, 0, 0.0);
        }

        Inventory inventory = container.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        int itemsSold = 0;
        int stacksSold = 0;
        double payout = 0.0;

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType().isAir() || isSellWand(stack)) continue;
            if (!plugin.market().isEligibleStack(stack)) continue;

            ItemStack offered = stack.clone();
            MarketManager.TradeResult result = plugin.market().sellEscrowStack(player, offered);
            if (!result.success() || result.amount() <= 0) continue;

            int remaining = stack.getAmount() - result.amount();
            if (remaining <= 0) contents[i] = null;
            else stack.setAmount(remaining);

            itemsSold += result.amount();
            stacksSold++;
            payout += result.total();
        }

        if (itemsSold <= 0) {
            return new SellResult(false,
                    "No eligible items could be sold. Unsellable/custom items and materials at maximum market stock were left untouched.",
                    0, 0, 0.0);
        }

        inventory.setStorageContents(contents);
        payout = com.snipeyfresh.incogshop.economy.WalletManager.round(payout);
        plugin.market().audit("SELL_WAND", player.getUniqueId(), player.getName(),
                block.getType().name(), itemsSold, payout,
                "world=" + block.getWorld().getName() + ";x=" + block.getX() + ";y=" + block.getY() + ";z=" + block.getZ() + ";stacks=" + stacksSold);

        return new SellResult(true,
                "Sold " + itemsSold + " item(s) from the container for " + plugin.money(payout) + ".",
                itemsSold, stacksSold, payout);
    }
}
