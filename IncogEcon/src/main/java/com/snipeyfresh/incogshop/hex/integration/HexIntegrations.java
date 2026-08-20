package com.snipeyfresh.incogshop.hex.integration;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of every third-party hook the Hex can use.
 *
 * <p>Hooks are discovered at load time and re-discovered on
 * {@code /marketadmin reload}. None of them is a build dependency: each one
 * probes for its plugin's classes and quietly stays inactive when they are not
 * there, so IncogEcon runs the same with or without them installed.</p>
 */
public final class HexIntegrations {
    private final IncogShopPlugin plugin;
    private final List<HexIntegration> registered = new ArrayList<>();

    public HexIntegrations(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        registered.clear();
        register(new EcoArmorIntegration());
        register(new ReforgesIntegration());
        register(CustomItemHook.mmoItems());
        register(CustomItemHook.ecoItems());
        register(CustomItemHook.itemsAdder());
        register(CustomItemHook.oraxen());
        register(CustomItemHook.nexo());

        List<String> active = active().stream().map(HexIntegration::pluginName).toList();
        if (active.isEmpty()) {
            plugin.getLogger().info("Hex compatibility: no supported upgrade or custom-item plugins detected.");
        } else {
            plugin.getLogger().info("Hex compatibility active for: " + String.join(", ", active) + ".");
        }
    }

    private void register(HexIntegration integration) {
        if (integration == null) return;
        if (!plugin.getConfig().getBoolean("hex.integrations." + integration.id() + ".enabled", true)) return;
        registered.add(integration);
        if (!integration.available() && plugin.getServer().getPluginManager().getPlugin(integration.pluginName()) != null) {
            plugin.getLogger().warning("Hex found " + integration.pluginName()
                    + " but could not recognise its API. Hex upgrades for that plugin are disabled.");
        }
    }

    /** Every hook whose plugin is installed and whose API was recognised. */
    public List<HexIntegration> active() {
        return registered.stream().filter(HexIntegration::available).toList();
    }

    public List<HexIntegration> all() {
        return List.copyOf(registered);
    }

    /** The armor-upgrade plugin that owns this item, if any. */
    public ArmorUpgradeIntegration armorUpgradeFor(ItemStack item) {
        for (HexIntegration integration : active()) {
            if (integration instanceof ArmorUpgradeIntegration armor && armor.handles(item)) return armor;
        }
        return null;
    }

    /** The reforge plugin that should handle this item, if any. */
    public ReforgeIntegration reforgeProviderFor(ItemStack item) {
        for (HexIntegration integration : active()) {
            if (integration instanceof ReforgeIntegration reforge && !reforge.options(item).isEmpty()) return reforge;
        }
        return null;
    }

    /** Name of the plugin an item belongs to, or null when it is a normal item. */
    public String ownerPlugin(ItemStack item) {
        for (HexIntegration integration : active()) {
            if (integration.claims(item)) return integration.pluginName();
        }
        return null;
    }

    /**
     * True when the item belongs to a plugin that the Hex cannot safely modify
     * itself. Armor-upgrade hooks are excluded because those items are handled
     * through their own plugin's API.
     */
    public boolean protectedCustomItem(ItemStack item) {
        if (!plugin.getConfig().getBoolean("hex.integrations.protect-unknown-custom-items", true)) return false;
        if (armorUpgradeFor(item) != null) return false;
        for (HexIntegration integration : active()) {
            if (integration instanceof CustomItemIntegration custom && custom.itemId(item) != null) return true;
        }
        return false;
    }

    /** Lore lines contributed by every hook that recognises the item. */
    public List<String> describe(ItemStack item) {
        List<String> lines = new ArrayList<>();
        for (HexIntegration integration : active()) lines.addAll(integration.describe(item));
        return lines;
    }
}
