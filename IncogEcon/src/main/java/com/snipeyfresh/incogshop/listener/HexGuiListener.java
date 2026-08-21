package com.snipeyfresh.incogshop.listener;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.gui.HexGui;
import com.snipeyfresh.incogshop.gui.ShopGui;
import com.snipeyfresh.incogshop.hex.EssenceType;
import com.snipeyfresh.incogshop.hex.HexManager;
import com.snipeyfresh.incogshop.hex.HexResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives every Hex menu.
 *
 * <p>All clicks are cancelled and applied by hand, and the item being upgraded
 * is tracked on the menu holder rather than in a slot, so it cannot be
 * duplicated or dropped. Whenever a Hex menu closes without handing the item to
 * another Hex screen, the item goes straight back to the player.</p>
 */
public final class HexGuiListener implements Listener {
    private final IncogShopPlugin plugin;
    private final Map<UUID, String> essencePrompts = new ConcurrentHashMap<>();

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
            case HexGui.MAIN -> onMainClick(player, holder, raw);
            case HexGui.POUCH -> onPouchClick(player, holder, raw);
            case HexGui.SHOP -> onShopClick(player, holder, raw);
            case HexGui.REFORGE -> onReforgeClick(player, holder, raw);
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

    // ------------------------------------------------------------ main menu

    private void onMainClick(Player player, HexGui.Holder holder, int slot) {
        ItemStack carried = holder.carried();
        switch (slot) {
            case HexGui.SLOT_CLOSE -> player.closeInventory();
            case HexGui.ITEM_SLOT, HexGui.SLOT_TAKE -> takeBack(player, holder);
            case HexGui.SLOT_POUCH -> move(player, holder, () -> plugin.hexGui().openPouch(player, carried));
            case HexGui.SLOT_SHOP -> move(player, holder, () -> plugin.hexGui().openShop(player, carried));
            case HexGui.SLOT_REFORGE -> {
                if (require(player, carried)) move(player, holder, () -> plugin.hexGui().openReforge(player, carried, 0));
            }
            case HexGui.SLOT_TIER -> upgrade(player, holder, HexManager.TIER);
            case HexGui.SLOT_STARS -> upgrade(player, holder, HexManager.STARS);
            case HexGui.SLOT_POTATO -> upgrade(player, holder, HexManager.POTATO);
            case HexGui.SLOT_GEMS -> upgrade(player, holder, HexManager.GEMSTONES);
            case HexGui.SLOT_RECOMBOBULATOR -> upgrade(player, holder, HexManager.RECOMBOBULATOR);
            case HexGui.SLOT_ENCHANTS -> upgrade(player, holder, HexManager.ENCHANTMENTS);
            case HexGui.SLOT_ARMOR_TIER -> apply(player, holder, hexItem -> plugin.hex().upgradeArmorTier(player, hexItem));
            case HexGui.SLOT_ARMOR_ADVANCE -> apply(player, holder, hexItem -> plugin.hex().advanceArmor(player, hexItem));
            default -> { }
        }
    }

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

    private void upgrade(Player player, HexGui.Holder holder, String upgrade) {
        apply(player, holder, item -> plugin.hex().upgrade(player, item, upgrade));
    }

    private void apply(Player player, HexGui.Holder holder, java.util.function.Function<ItemStack, HexResult> action) {
        ItemStack carried = holder.carried();
        if (!require(player, carried)) return;
        HexResult result = action.apply(carried);
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        holder.setMoved(true);
        plugin.hexGui().open(player, carried);
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

    // -------------------------------------------------------- other screens

    private void onPouchClick(Player player, HexGui.Holder holder, int slot) {
        if (slot == HexGui.SLOT_BACK) {
            ItemStack carried = holder.carried();
            move(player, holder, () -> plugin.hexGui().open(player, carried));
        } else if (slot == HexGui.SLOT_SUB_CLOSE) {
            player.closeInventory();
        }
    }

    private void onShopClick(Player player, HexGui.Holder holder, int slot) {
        if (slot == HexGui.SLOT_BACK) {
            ItemStack carried = holder.carried();
            move(player, holder, () -> plugin.hexGui().open(player, carried));
            return;
        }
        if (slot == HexGui.SLOT_SUB_CLOSE) { player.closeInventory(); return; }

        EssenceType type = essenceAt(slot);
        if (type == null) return;
        if (!type.purchasable() || !plugin.hex().buyingAllowed()) {
            player.sendMessage(plugin.prefix() + "§cThat essence cannot be bought.");
            return;
        }
        essencePrompts.put(player.getUniqueId(), type.id());
        player.closeInventory();
        player.sendMessage(plugin.prefix() + "§eType how much " + com.snipeyfresh.incogshop.util.Text.color(type.display())
                + " §eyou want to buy.");
        player.sendMessage("§7Price: §f" + plugin.money(type.buyPrice()) + " §7each §8| §7Type §fcancel §7to stop.");
    }

    /** Maps a shop/pouch slot back to the essence rendered there. */
    private EssenceType essenceAt(int slot) {
        List<EssenceType> types = new ArrayList<>(plugin.hex().types());
        int[] slots = ShopGui.centeredSlots(types.size());
        for (int i = 0; i < types.size() && i < slots.length; i++) {
            if (slots[i] == slot) return types.get(i);
        }
        return null;
    }

    private void onReforgeClick(Player player, HexGui.Holder holder, int slot) {
        ItemStack carried = holder.carried();
        if (slot == 46) {
            move(player, holder, () -> plugin.hexGui().open(player, carried));
            return;
        }
        if (slot == HexGui.SLOT_PREVIOUS) {
            if (holder.page() > 0) move(player, holder, () -> plugin.hexGui().openReforge(player, carried, holder.page() - 1));
            else move(player, holder, () -> plugin.hexGui().open(player, carried));
            return;
        }
        if (slot == HexGui.SLOT_NEXT) {
            List<String> options = carried == null ? List.of() : plugin.hex().reforgeOptions(carried);
            int pages = Math.max(1, (options.size() + HexGui.REFORGE_PER_PAGE - 1) / HexGui.REFORGE_PER_PAGE);
            if (holder.page() + 1 < pages) move(player, holder, () -> plugin.hexGui().openReforge(player, carried, holder.page() + 1));
            else player.closeInventory();
            return;
        }
        if (slot < HexGui.REFORGE_START || slot >= HexGui.REFORGE_START + HexGui.REFORGE_PER_PAGE) return;
        if (!require(player, carried)) return;

        List<String> options = plugin.hex().reforgeOptions(carried);
        int index = holder.page() * HexGui.REFORGE_PER_PAGE + (slot - HexGui.REFORGE_START);
        if (index < 0 || index >= options.size()) return;

        HexResult result = plugin.hex().reforge(player, carried, options.get(index));
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        move(player, holder, () -> plugin.hexGui().openReforge(player, carried, holder.page()));
    }

    // --------------------------------------------------------- chat prompts

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String type = essencePrompts.remove(event.getPlayer().getUniqueId());
        if (type == null) return;
        event.setCancelled(true);
        String input = event.getMessage().trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> completePurchase(event.getPlayer(), type, input));
    }

    private void completePurchase(Player player, String type, String input) {
        if (!player.isOnline()) return;
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.prefix() + "§7Essence purchase cancelled.");
            plugin.hexGui().openShop(player, null);
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(input.toLowerCase(Locale.ROOT).replace(",", "").trim());
        } catch (NumberFormatException ex) {
            amount = -1;
        }
        if (amount <= 0) {
            player.sendMessage(plugin.prefix() + "§cEnter a whole number above zero, or 'cancel'.");
            plugin.hexGui().openShop(player, null);
            return;
        }
        HexResult result = plugin.hex().buyEssence(player, type, amount);
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        plugin.hexGui().openShop(player, null);
    }
}
