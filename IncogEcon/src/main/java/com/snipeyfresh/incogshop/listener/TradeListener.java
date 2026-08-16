package com.snipeyfresh.incogshop.listener;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.gui.TradeGui;
import com.snipeyfresh.incogshop.trade.TradeManager;
import com.snipeyfresh.incogshop.util.Money;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TradeListener implements Listener {
    private record MoneyInput(UUID sessionId) {}

    private final IncogShopPlugin plugin;
    private final Map<UUID, MoneyInput> moneyInputs = new HashMap<>();

    public TradeListener(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradeGui.Holder holder) || !(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        TradeManager.TradeSession session = plugin.trades().session(holder.sessionId());
        if (session == null || !session.isParticipant(player.getUniqueId())) {
            player.closeInventory();
            return;
        }

        int raw = event.getRawSlot();
        if (raw < 0) return;

        if (raw >= top.getSize()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                player.sendMessage(plugin.prefix() + "§cPut the item on your cursor away before adding items to a trade.");
                return;
            }
            if (!plugin.trades().addItem(player, clicked.clone())) {
                player.sendMessage(plugin.prefix() + "§cYour trade offer is full.");
                return;
            }
            event.setCurrentItem(null);
            return;
        }

        int ownOfferIndex = TradeGui.offerIndex(player.getUniqueId(), session, raw);
        if (ownOfferIndex >= 0) {
            plugin.trades().removeItem(player, ownOfferIndex);
            return;
        }
        if (TradeGui.isOtherOfferSlot(player.getUniqueId(), session, raw)) return;

        boolean left = session.isLeft(player.getUniqueId());
        int ownMoney = left ? TradeGui.LEFT_MONEY : TradeGui.RIGHT_MONEY;
        int ownConfirm = left ? TradeGui.LEFT_CONFIRM : TradeGui.RIGHT_CONFIRM;

        if (raw == ownMoney) {
            plugin.trades().markOfferEditing(player);
            beginMoneyInput(player, session);
            return;
        }
        if (raw == ownConfirm) {
            TradeManager.Result result = plugin.trades().confirm(player);
            if (!result.success()) player.sendMessage(plugin.prefix() + "§c" + result.message());
            else if (plugin.trades().sessionFor(player.getUniqueId()) != null) player.sendMessage(plugin.prefix() + "§a" + result.message());
            return;
        }
        if (raw == TradeGui.CANCEL) {
            plugin.trades().cancel(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TradeGui.Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TradeGui.Holder holder) || !(event.getPlayer() instanceof Player player)) return;
        if (moneyInputs.containsKey(player.getUniqueId())) return;
        TradeManager.TradeSession session = plugin.trades().session(holder.sessionId());
        if (session != null && session.isParticipant(player.getUniqueId())) plugin.trades().cancel(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        moneyInputs.remove(event.getPlayer().getUniqueId());
        plugin.trades().cancelForQuit(event.getPlayer());
    }

    private void beginMoneyInput(Player player, TradeManager.TradeSession session) {
        moneyInputs.put(player.getUniqueId(), new MoneyInput(session.id()));
        player.closeInventory();
        player.sendMessage(plugin.prefix() + "§eType your money offer in chat. §7Examples: §f1000§7, §f25k§7, §f2.5m§7. Enter §f0 §7to offer no money, or §fcancel §7to go back.");
    }

    @EventHandler
    public void onMoneyChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MoneyInput pending = moneyInputs.remove(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            TradeManager.TradeSession session = plugin.trades().session(pending.sessionId());
            if (session == null || !session.isParticipant(player.getUniqueId())) {
                player.sendMessage(plugin.prefix() + "§cThat trade is no longer active.");
                return;
            }

            double amount;
            if (input.isBlank() || input.equalsIgnoreCase("cancel")) {
                plugin.tradeGui().open(player, session);
                return;
            } else if (input.equals("0") || input.equalsIgnoreCase("none")) {
                amount = 0;
            } else {
                amount = Money.parsePositive(input);
                if (amount < 0) {
                    player.sendMessage(plugin.prefix() + "§cInvalid money amount.");
                    plugin.tradeGui().open(player, session);
                    return;
                }
            }

            TradeManager.Result result = plugin.trades().setMoney(player, amount);
            player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
            TradeManager.TradeSession current = plugin.trades().session(pending.sessionId());
            if (current != null) plugin.tradeGui().open(player, current);
        });
    }

}
