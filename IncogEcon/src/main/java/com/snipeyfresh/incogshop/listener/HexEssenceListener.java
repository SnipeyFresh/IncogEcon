package com.snipeyfresh.incogshop.listener;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.hex.EssenceType;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.concurrent.ThreadLocalRandom;

/** Grants Hex essence for kills, using the drop table in config.yml. */
public final class HexEssenceListener implements Listener {
    private final IncogShopPlugin plugin;

    public HexEssenceListener(IncogShopPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (!plugin.hex().enabled()) return;
        if (!plugin.getConfig().getBoolean("hex.essence.drops.enabled", true)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.hasPermission("incogshop.hex")) return;

        ConfigurationSection entry = plugin.getConfig()
                .getConfigurationSection("hex.essence.drops.mobs." + event.getEntityType().name());
        if (entry == null) return;

        double chance = Math.max(0, Math.min(1.0, entry.getDouble("chance", 0)));
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() > chance) return;

        int minimum = Math.max(1, entry.getInt("minimum", 1));
        int maximum = Math.max(minimum, entry.getInt("maximum", minimum));
        int amount = minimum == maximum ? minimum : ThreadLocalRandom.current().nextInt(minimum, maximum + 1);

        EssenceType type = plugin.hex().type(entry.getString("type", ""));
        if (type == null) return;

        plugin.hex().giveEssence(killer.getUniqueId(), type.id(), amount);
        if (plugin.getConfig().getBoolean("hex.essence.drops.notify", true)) {
            notify(killer, "&d+" + amount + " " + type.display());
        }
    }

    /** Prefers the action bar, but never fails a kill over a cosmetic message. */
    private void notify(Player player, String message) {
        String legacy = Text.color(message);
        try {
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(legacy));
        } catch (Throwable ignored) {
            player.sendMessage(plugin.prefix() + legacy);
        }
    }
}
