package com.snipeyfresh.incogshop.hex;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.economy.WalletManager;
import com.snipeyfresh.incogshop.hex.integration.HexIntegrations;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Drives the Hex item-upgrade features: reforges and custom enchants.
 *
 * Reforges are handled by IncogRPG's ReforgeService (accessed via reflection).
 * Custom enchants come from IncogRPG's EnchantService and ExcellentEnchants.
 * All costs are coin-only and charged through IncogEcon's WalletManager.
 */
public final class HexManager {
    private final IncogShopPlugin plugin;
    private final HexIntegrations integrations;

    public HexManager(IncogShopPlugin plugin) {
        this.plugin = plugin;
        this.integrations = new HexIntegrations(plugin);
    }

    public void load() {
        integrations.reload();
    }

    /** No persistent state lives in IncogEcon — IncogRPG's PDC owns everything. */
    public void save() {}

    public boolean enabled() {
        return plugin.getConfig().getBoolean("hex.enabled", true);
    }

    public HexIntegrations integrations() { return integrations; }

    // ---- Guard ----

    /** Common pre-flight checks. Returns a failure result, or null when the item passes. */
    public HexResult guard(ItemStack item) {
        if (!enabled()) return HexResult.fail("The Hex is disabled on this server.");
        if (item == null || item.getType().isAir()) return HexResult.fail("Place an item in the Hex slot first.");
        if (item.getAmount() > 1) return HexResult.fail("The Hex works on one item at a time, not a stack.");
        if (!HexItems.supported(item)) return HexResult.fail("The Hex only works on weapons, tools, and armor.");
        return null;
    }

    // ---- Reforges ----

    public boolean reforgeEnabled() {
        return enabled()
                && plugin.getConfig().getBoolean("hex.reforge.enabled", true)
                && integrations.incogRpg().available();
    }

    public List<String> reforgeOptions(ItemStack item) {
        return integrations.incogRpg().compatibleReforges(item);
    }

    public String currentReforge(ItemStack item) {
        return integrations.incogRpg().currentReforgeId(item);
    }

    public String reforgeDisplayName(String id) {
        return integrations.incogRpg().reforgeDisplayName(id);
    }

    /**
     * Coin cost for the given reforge.
     * When {@code hex.reforge.coin-cost-override} is ≥ 0 it overrides IncogRPG's own price.
     */
    public double reforgeCost(ItemStack item, String id) {
        double override = plugin.getConfig().getDouble("hex.reforge.coin-cost-override", -1);
        if (override >= 0) return WalletManager.round(override);
        return WalletManager.round(integrations.incogRpg().reforgeCost(item, id));
    }

    public HexResult reforge(Player player, ItemStack item, String reforgeId) {
        HexResult blocked = guard(item);
        if (blocked != null) return blocked;
        if (!reforgeEnabled()) return HexResult.fail("Reforges require IncogRPG to be installed.");
        if (reforgeId == null || reforgeId.isBlank()) return HexResult.fail("Choose a reforge first.");

        double cost = reforgeCost(item, reforgeId);
        if (cost > 0 && plugin.wallets().get(player.getUniqueId()) + 1e-9 < cost) {
            return HexResult.fail("You need " + plugin.money(cost) + " to reforge.");
        }
        if (!integrations.incogRpg().applyReforge(item, reforgeId)) {
            return HexResult.fail("IncogRPG refused that reforge for this item.");
        }
        if (cost > 0) {
            plugin.wallets().withdraw(player.getUniqueId(), cost);
            plugin.market().audit("HEX_REFORGE", player.getUniqueId(), player.getName(),
                    item.getType().name(), 1, cost, reforgeId);
        }
        return HexResult.ok("Reforged to "
                + integrations.incogRpg().reforgeDisplayName(reforgeId) + ".");
    }

    // ---- Enchants ----

    public boolean enchantEnabled() {
        return enabled() && plugin.getConfig().getBoolean("hex.enchant.enabled", true);
    }

    public double enchantCostPerLevel() {
        return Math.max(0, plugin.getConfig().getDouble("hex.enchant.coin-cost-per-level", 500));
    }

    /** Apply one level of an IncogRPG custom enchant, charging coins. */
    public HexResult applyIncogRpgEnchant(Player player, ItemStack item, String enchantId) {
        HexResult blocked = guard(item);
        if (blocked != null) return blocked;
        if (!enchantEnabled()) return HexResult.fail("Enchanting is disabled.");

        int current = integrations.incogRpg().currentEnchants(item).getOrDefault(enchantId, 0);
        int max = integrations.incogRpg().enchantMaxLevel(enchantId);
        if (current >= max) return HexResult.fail("That enchant is already at its maximum level.");

        int next = current + 1;
        double cost = WalletManager.round(enchantCostPerLevel() * next);
        if (cost > 0 && plugin.wallets().get(player.getUniqueId()) + 1e-9 < cost) {
            return HexResult.fail("You need " + plugin.money(cost) + " for level " + next + ".");
        }
        if (!integrations.incogRpg().applyEnchant(item, enchantId, next)) {
            return HexResult.fail("Could not apply that enchant to this item.");
        }
        if (cost > 0) {
            plugin.wallets().withdraw(player.getUniqueId(), cost);
            plugin.market().audit("HEX_ENCHANT", player.getUniqueId(), player.getName(),
                    item.getType().name(), 1, cost, enchantId + "=" + next);
        }
        return HexResult.ok("Applied " + integrations.incogRpg().enchantDisplayName(enchantId)
                + " " + roman(next) + ".");
    }

    /** Apply one level of an ExcellentEnchants enchant (uses vanilla Bukkit meta), charging coins. */
    public HexResult applyEeEnchant(Player player, ItemStack item, Enchantment ench) {
        HexResult blocked = guard(item);
        if (blocked != null) return blocked;
        if (!enchantEnabled()) return HexResult.fail("Enchanting is disabled.");

        int current = item.getEnchantmentLevel(ench);
        int max = ench.getMaxLevel();
        if (current >= max) return HexResult.fail("That enchant is already at its maximum level.");

        int next = current + 1;
        double cost = WalletManager.round(enchantCostPerLevel() * next);
        if (cost > 0 && plugin.wallets().get(player.getUniqueId()) + 1e-9 < cost) {
            return HexResult.fail("You need " + plugin.money(cost) + " for level " + next + ".");
        }
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return HexResult.fail("Could not apply that enchant.");
            meta.addEnchant(ench, next, true);
            item.setItemMeta(meta);
        } catch (Throwable ex) {
            return HexResult.fail("Could not apply that enchant to this item.");
        }
        if (cost > 0) {
            plugin.wallets().withdraw(player.getUniqueId(), cost);
            plugin.market().audit("HEX_EE_ENCHANT", player.getUniqueId(), player.getName(),
                    item.getType().name(), 1, cost, ench.getKey().getKey() + "=" + next);
        }
        String name = integrations.excellentEnchants().displayName(ench);
        return HexResult.ok("Applied " + name + " " + roman(next) + ".");
    }

    private static String roman(int n) {
        if (n <= 0 || n >= 4000) return String.valueOf(n);
        String[] t  = {"", "M", "MM", "MMM"};
        String[] h  = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] te = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] o  = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return t[n/1000] + h[n%1000/100] + te[n%100/10] + o[n%10];
    }
}
