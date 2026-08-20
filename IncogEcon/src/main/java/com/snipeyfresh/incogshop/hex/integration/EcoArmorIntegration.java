package com.snipeyfresh.incogshop.hex.integration;

import com.snipeyfresh.incogshop.gui.GuiTheme;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * EcoArmor support.
 *
 * <p>The Hex reads the armor set, tier, and advancement state of an EcoArmor
 * piece and can push it one tier further or apply its advancement, paying with
 * essence and coins instead of EcoArmor's own upgrade crystals and shards.
 * Everything is done through EcoArmor's own API by reflection, so the tier data
 * it writes stays exactly the data EcoArmor expects to read back.</p>
 *
 * <p>Several EcoArmor generations are supported: class and method names are
 * probed in order, and if none match the hook reports itself unavailable rather
 * than guessing at the item's NBT.</p>
 */
public final class EcoArmorIntegration implements ArmorUpgradeIntegration {
    private static final String[] ARMOR_UTILS = {
            "com.willfp.ecoarmor.sets.util.ArmorUtils",
            "com.willfp.ecoarmor.util.ArmorUtils",
            "com.willfp.ecoarmor.sets.ArmorUtils"
    };
    private static final String[] TIERS = {
            "com.willfp.ecoarmor.upgrades.Tiers",
            "com.willfp.ecoarmor.tiers.Tiers"
    };

    private final Class<?> armorUtils;
    private final Class<?> tiersClass;
    private final Method getSet;
    private final Method getTier;
    private final Method setTier;
    private final Method isAdvanced;
    private final Method setAdvanced;
    private final Method tierValues;

    public EcoArmorIntegration() {
        this.armorUtils = Reflect.findClass(ARMOR_UTILS);
        this.tiersClass = Reflect.findClass(TIERS);
        this.getSet = Reflect.looseMethod(armorUtils, new String[]{"getSetOnItem"}, 1);
        this.getTier = Reflect.looseMethod(armorUtils, new String[]{"getTier", "getTierOnItem"}, 1);
        this.setTier = Reflect.looseMethod(armorUtils, new String[]{"setTier"}, 2);
        this.isAdvanced = Reflect.looseMethod(armorUtils, new String[]{"isAdvanced"}, 1);
        this.setAdvanced = Reflect.looseMethod(armorUtils, new String[]{"setAdvanced"}, 2);
        this.tierValues = Reflect.looseMethod(tiersClass, new String[]{"values"}, 0);
    }

    @Override public String id() { return "ecoarmor"; }
    @Override public String pluginName() { return "EcoArmor"; }

    @Override
    public boolean available() {
        return Bukkit.getPluginManager().getPlugin("EcoArmor") != null
                && armorUtils != null && getSet != null && getTier != null && setTier != null;
    }

    @Override
    public boolean claims(ItemStack item) {
        return set(item) != null;
    }

    @Override
    public boolean handles(ItemStack item) {
        return available() && claims(item);
    }

    @Override
    public String currentTier(ItemStack item) {
        Object tier = tier(item);
        return tier == null ? null : Reflect.nameOf(tier);
    }

    @Override
    public String nextTier(ItemStack item) {
        Object next = nextTierObject(item);
        return next == null ? null : Reflect.nameOf(next);
    }

    @Override
    public boolean upgradeTier(ItemStack item) {
        Object next = nextTierObject(item);
        if (next == null) return false;
        Object result = invoke(setTier, item, next);
        if (result == null && setTier == null) return false;
        // EcoArmor's setter mutates the stack in place, so success is confirmed
        // by reading the tier back rather than trusting a void return value.
        String applied = currentTier(item);
        return applied != null && applied.equalsIgnoreCase(Reflect.nameOf(next));
    }

    @Override
    public boolean supportsAdvancement(ItemStack item) {
        return available() && isAdvanced != null && setAdvanced != null && claims(item);
    }

    @Override
    public boolean advanced(ItemStack item) {
        Object result = invoke(isAdvanced, item);
        return result instanceof Boolean flag && flag;
    }

    @Override
    public boolean advance(ItemStack item) {
        if (!supportsAdvancement(item) || advanced(item)) return false;
        invoke(setAdvanced, item, Boolean.TRUE);
        return advanced(item);
    }

    @Override
    public List<String> describe(ItemStack item) {
        Object set = set(item);
        if (set == null) return List.of();
        List<String> lines = new ArrayList<>();
        lines.add("&7EcoArmor set: &f" + Reflect.nameOf(set));
        String tier = currentTier(item);
        lines.add(GuiTheme.stat("EcoArmor tier", tier == null ? "Default" : tier));
        if (isAdvanced != null) lines.add("&7Advanced: " + (advanced(item) ? "&aYes" : "&8No"));
        return lines;
    }

    private Object set(ItemStack item) {
        if (item == null || item.getType().isAir() || armorUtils == null || getSet == null) return null;
        return invoke(getSet, item);
    }

    private Object tier(ItemStack item) {
        if (!available() || item == null || item.getType().isAir()) return null;
        return invoke(getTier, item);
    }

    private Object nextTierObject(ItemStack item) {
        List<Object> tiers = tiers();
        if (tiers.isEmpty() || !handles(item)) return null;
        Object current = tier(item);
        if (current == null) return tiers.get(0);
        String currentName = Reflect.nameOf(current);
        for (int i = 0; i < tiers.size(); i++) {
            if (!java.util.Objects.equals(Reflect.nameOf(tiers.get(i)), currentName)) continue;
            return i + 1 < tiers.size() ? tiers.get(i + 1) : null;
        }
        return null;
    }

    private List<Object> tiers() {
        Object values = Reflect.call(tierValues, null);
        if (values instanceof Collection<?> collection) return new ArrayList<>(collection);
        if (values instanceof Object[] array) return new ArrayList<>(List.of(array));
        return List.of();
    }

    /** EcoArmor takes either an ItemStack or an ItemMeta depending on version. */
    private Object invoke(Method method, ItemStack item, Object... extra) {
        return Reflect.invokeWithItem(method, item, extra);
    }
}
