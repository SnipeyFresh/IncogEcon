package com.snipeyfresh.incogshop.hex.integration;

import com.snipeyfresh.incogshop.IncogShopPlugin;

/**
 * Holds the IncogRPG and ExcellentEnchants reflection bridges used by the Hex.
 */
public final class HexIntegrations {
    private final IncogShopPlugin plugin;
    private final IncogRpgBridge incogRpg = new IncogRpgBridge();
    private final ExcellentEnchantsBridge excellentEnchants = new ExcellentEnchantsBridge();

    public HexIntegrations(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        boolean rpgOk = incogRpg.init();
        boolean eeOk  = excellentEnchants.init();

        if (rpgOk) plugin.getLogger().info("Hex: IncogRPG reforge + enchant integration active.");
        else        plugin.getLogger().info("Hex: IncogRPG not detected — reforge and custom-enchant features require it.");
        if (eeOk)  plugin.getLogger().info("Hex: ExcellentEnchants integration active.");
    }

    public IncogRpgBridge incogRpg() { return incogRpg; }
    public ExcellentEnchantsBridge excellentEnchants() { return excellentEnchants; }
}
