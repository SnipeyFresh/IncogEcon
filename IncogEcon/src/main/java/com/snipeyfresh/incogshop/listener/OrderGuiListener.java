package com.snipeyfresh.incogshop.listener;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.gui.OrderBookGui;
import com.snipeyfresh.incogshop.order.MarketOrder;
import com.snipeyfresh.incogshop.util.Money;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrderGuiListener implements Listener {
    private record Prompt(Material material, MarketOrder.Type type) {}
    private record InstantBuyInput(Material material) {}
    private final IncogShopPlugin plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();
    private final Map<UUID, InstantBuyInput> instantBuyInputs = new ConcurrentHashMap<>();

    public OrderGuiListener(IncogShopPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (handleInventoryBazaarJump(event, top, player)) return;
        if (top.getHolder() instanceof OrderBookGui.BookHolder holder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= top.getSize()) return;

            if (slot == 20) {
                instantTrade(player, holder.material(), true, false);
                return;
            }
            if (slot == 21) {
                beginInstantBuyAmount(player, holder.material());
                return;
            }
            if (slot == 22) {
                instantTrade(player, holder.material(), true, true);
                return;
            }
            if (slot == 24) {
                instantTrade(player, holder.material(), false, false);
                return;
            }
            if (slot == 25) {
                instantTrade(player, holder.material(), false, true);
                return;
            }
            if (slot == 45) { plugin.gui().openCategories(player, false); return; }
            if (slot == 46) { plugin.orderGui().openGlobal(player, "ALL", 0); return; }
            if (slot == 47) { begin(player, holder.material(), MarketOrder.Type.BUY); return; }
            if (slot == 51) { begin(player, holder.material(), MarketOrder.Type.SELL); return; }
            if (slot == 52) { plugin.orderGui().openMy(player, 0); return; }
            if (slot == 53) {
                int claimed = plugin.orders().claim(player);
                player.sendMessage(plugin.prefix() + (claimed > 0 ? "§aClaimed §f" + claimed + " §amarket-order item(s)." : "§eNo claimable items, or your inventory is full."));
                plugin.orderGui().open(player, holder.material());
            }
            return;
        }

        if (top.getHolder() instanceof OrderBookGui.GlobalOrdersHolder holder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < 45) {
                MarketOrder order = plugin.orderGui().globalOrderAt(holder.filter(), holder.page(), slot);
                if (order != null) plugin.orderGui().open(player, order.material());
                return;
            }
            if (slot == 45 && holder.page() > 0) plugin.orderGui().openGlobal(player, holder.filter(), holder.page() - 1);
            else if (slot == 46) plugin.orderGui().openGlobal(player, "ALL", 0);
            else if (slot == 47) plugin.orderGui().openGlobal(player, "BUY", 0);
            else if (slot == 48) plugin.orderGui().openGlobal(player, "SELL", 0);
            else if (slot == 49) plugin.gui().openCategories(player, false);
            else if (slot == 51) plugin.orderGui().openMy(player, 0);
            else if (slot == 52) {
                int claimed = plugin.orders().claim(player);
                player.sendMessage(plugin.prefix() + (claimed > 0 ? "§aClaimed §f" + claimed + " §amarket-order item(s)." : "§eNo claimable items, or your inventory is full."));
                plugin.orderGui().openGlobal(player, holder.filter(), holder.page());
            } else if (slot == 53) plugin.orderGui().openGlobal(player, holder.filter(), holder.page() + 1);
            return;
        }

        if (top.getHolder() instanceof OrderBookGui.MyOrdersHolder holder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < 45) {
                MarketOrder order = plugin.orderGui().orderAt(player, holder.page(), slot);
                if (order != null) {
                    var result = plugin.orders().cancel(player, order);
                    player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
                    plugin.orderGui().openMy(player, holder.page());
                }
                return;
            }
            if (slot == 45 && holder.page() > 0) plugin.orderGui().openMy(player, holder.page() - 1);
            else if (slot == 48) plugin.gui().openCategories(player, false);
            else if (slot == 50) {
                int claimed = plugin.orders().claim(player);
                player.sendMessage(plugin.prefix() + (claimed > 0 ? "§aClaimed §f" + claimed + " §amarket-order item(s)." : "§eNo claimable items, or your inventory is full."));
                plugin.orderGui().openMy(player, holder.page());
            } else if (slot == 53) plugin.orderGui().openMy(player, holder.page() + 1);
        }
    }

    private boolean handleInventoryBazaarJump(InventoryClickEvent event, Inventory top, Player player) {
        Object holder = top.getHolder();
        if (!(holder instanceof OrderBookGui.BookHolder)
                && !(holder instanceof OrderBookGui.GlobalOrdersHolder)
                && !(holder instanceof OrderBookGui.MyOrdersHolder)) return false;

        int rawSlot = event.getRawSlot();
        if (rawSlot < top.getSize()) return false;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return true;
        if (plugin.sellWands() != null && plugin.sellWands().isSellWand(clicked)) return true;

        Material material = clicked.getType();
        if (plugin.market().isTradable(material)) {
            plugin.orderGui().open(player, material);
        }
        return true;
    }

    private void beginInstantBuyAmount(Player player, Material material) {
        if (!plugin.market().isBuyAllowed(material)) {
            player.sendMessage(plugin.prefix() + "§cThat item cannot currently be bought instantly.");
            plugin.orderGui().open(player, material);
            return;
        }

        prompts.remove(player.getUniqueId());
        instantBuyInputs.put(player.getUniqueId(), new InstantBuyInput(material));
        player.closeInventory();
        player.sendMessage(plugin.prefix() + "§eType the exact amount you want to instant buy in chat. §7Examples: §f5§7, §f128§7, §f2k§7. Type §fcancel §7to stop.");
    }

    private void handleInstantBuy(Player player, InstantBuyInput pending, String input) {
        if (!player.isOnline()) return;
        if (input.isBlank() || input.equalsIgnoreCase("cancel")) {
            plugin.orderGui().open(player, pending.material());
            return;
        }

        Integer amount = parseInstantBuyAmount(input);
        if (amount == null || amount <= 0) {
            player.sendMessage(plugin.prefix() + "§cInvalid amount. Enter a positive whole number. Shorthand such as §f2k §cis supported.");
            plugin.orderGui().open(player, pending.material());
            return;
        }

        var result = plugin.market().buyExact(player, pending.material(), amount);
        player.sendMessage(plugin.prefix()
                + (result.success() ? "§a" : "§c") + result.message()
                + (result.total() > 0 ? " §7(" + plugin.money(result.total()) + ")" : ""));
        plugin.orderGui().open(player, pending.material());
    }

    private Integer parseInstantBuyAmount(String input) {
        double parsed = Money.parsePositive(input == null ? "" : input.trim());
        if (parsed <= 0 || !Double.isFinite(parsed) || parsed > Integer.MAX_VALUE) return null;
        long rounded = Math.round(parsed);
        if (Math.abs(parsed - rounded) > 0.000001d || rounded <= 0 || rounded > Integer.MAX_VALUE) return null;
        return (int) rounded;
    }

    private void instantTrade(Player player, Material material, boolean buy, boolean stack) {
        int amount = stack ? Math.max(1, material.getMaxStackSize()) : 1;
        var result = buy
                ? plugin.market().buy(player, material, amount)
                : plugin.market().sell(player, material, amount);
        player.sendMessage(plugin.prefix()
                + (result.success() ? "§a" : "§c") + result.message()
                + (result.total() > 0 ? " §7(" + plugin.money(result.total()) + ")" : ""));
        plugin.orderGui().open(player, material);
    }

    private void begin(Player player, Material material, MarketOrder.Type type) {
        if (!player.hasPermission(type == MarketOrder.Type.BUY ? "incogshop.orders.buy" : "incogshop.orders.sell")) {
            player.sendMessage(plugin.prefix() + "§cYou do not have permission to create that order type.");
            return;
        }
        instantBuyInputs.remove(player.getUniqueId());
        prompts.put(player.getUniqueId(), new Prompt(material, type));
        player.closeInventory();
        player.sendMessage(plugin.prefix() + "§eType §f<amount> <price each> §ein chat for your " + (type == MarketOrder.Type.BUY ? "Buy" : "Sell") + " Order.");
        player.sendMessage("§7Example: §f64 10k §8| §7Type §fcancel §7to stop.");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        InstantBuyInput instant = instantBuyInputs.remove(uuid);
        if (instant != null) {
            event.setCancelled(true);
            String input = event.getMessage().trim();
            plugin.getServer().getScheduler().runTask(plugin, () -> handleInstantBuy(event.getPlayer(), instant, input));
            return;
        }

        Prompt prompt = prompts.remove(uuid);
        if (prompt == null) return;
        event.setCancelled(true);
        String input = event.getMessage().trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> handle(event.getPlayer(), prompt, input));
    }

    private void handle(Player player, Prompt prompt, String input) {
        if (!player.isOnline()) return;
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.prefix() + "§7Market order creation cancelled.");
            plugin.orderGui().open(player, prompt.material());
            return;
        }
        String[] parts = input.split("\\s+");
        if (parts.length != 2) {
            player.sendMessage(plugin.prefix() + "§cUse: <amount> <price each>. Example: 64 10k");
            plugin.orderGui().open(player, prompt.material());
            return;
        }
        int amount;
        try { amount = Integer.parseInt(parts[0].replace(",", "")); }
        catch (NumberFormatException ex) { amount = -1; }
        double price = Money.parsePositive(parts[1]);
        if (amount <= 0 || price <= 0) {
            player.sendMessage(plugin.prefix() + "§cInvalid amount or price.");
            plugin.orderGui().open(player, prompt.material());
            return;
        }
        var result = prompt.type() == MarketOrder.Type.BUY
                ? plugin.orders().createBuy(player, prompt.material(), amount, price)
                : plugin.orders().createSell(player, prompt.material(), amount, price);
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        plugin.orderGui().open(player, prompt.material());
    }
}
