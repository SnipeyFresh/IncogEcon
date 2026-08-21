package com.snipeyfresh.incogshop.hex;

import com.google.common.collect.Multimap;
import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.gui.GuiTheme;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and writes the Hex upgrade state stored on an item.
 *
 * <p>State lives in the persistent data container, never in the lore, so other
 * plugins are free to rewrite an item's lore without erasing its upgrades. The
 * lore block the Hex draws is tagged with an invisible marker and is the only
 * part of the lore this class ever removes.</p>
 */
public final class HexItems {
    /** Invisible prefix marking lore lines the Hex owns. */
    public static final String LORE_MARKER = "§8§r";

    public enum Kind { WEAPON, ARMOR, TOOL, OTHER }

    private final IncogShopPlugin plugin;
    private final NamespacedKey tierKey;
    private final NamespacedKey starKey;
    private final NamespacedKey potatoKey;
    private final NamespacedKey gemKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey reforgeKey;
    private final String namespace;
    private boolean attributeFailureLogged;

    public HexItems(IncogShopPlugin plugin) {
        this.plugin = plugin;
        this.tierKey = new NamespacedKey(plugin, "hex_tier");
        this.starKey = new NamespacedKey(plugin, "hex_stars");
        this.potatoKey = new NamespacedKey(plugin, "hex_potato");
        this.gemKey = new NamespacedKey(plugin, "hex_gem_slots");
        this.rarityKey = new NamespacedKey(plugin, "hex_rarity");
        this.reforgeKey = new NamespacedKey(plugin, "hex_reforge");
        this.namespace = tierKey.getNamespace();
    }

    public HexState read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return HexState.EMPTY;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return HexState.EMPTY;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return new HexState(
                pdc.getOrDefault(tierKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(starKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(potatoKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(gemKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(rarityKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(reforgeKey, PersistentDataType.STRING, ""));
    }

    /** Stores the state on the item and redraws its Hex stats and lore. */
    public void write(ItemStack item, HexState state) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        store(pdc, tierKey, state.tier());
        store(pdc, starKey, state.stars());
        store(pdc, potatoKey, state.potato());
        store(pdc, gemKey, state.gemSlots());
        store(pdc, rarityKey, state.rarityBumps());
        if (state.reforge().isEmpty()) pdc.remove(reforgeKey);
        else pdc.set(reforgeKey, PersistentDataType.STRING, state.reforge());

        Kind kind = kind(item);
        applyAttributes(item, meta, kind, state);
        applyLore(item, meta, state);
        item.setItemMeta(meta);
    }

    /** Redraws the visible Hex block without changing the stored state. */
    public void refresh(ItemStack item) {
        HexState state = read(item);
        if (state.blank()) return;
        write(item, state);
    }

    public Kind kind(ItemStack item) {
        if (item == null) return Kind.OTHER;
        String name = item.getType().name();
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("TRIDENT")
                || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("MACE")) return Kind.WEAPON;
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS") || name.equals("ELYTRA") || name.equals("TURTLE_HELMET")) return Kind.ARMOR;
        if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.equals("SHEARS") || name.equals("FISHING_ROD")) return Kind.TOOL;
        return Kind.OTHER;
    }

    /** Only single, non-stacked gear can be upgraded, exactly like Hypixel's Hex. */
    public boolean supported(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (item.getAmount() > 1) return false;
        return kind(item) != Kind.OTHER;
    }

    public HexRarity rarity(ItemStack item, HexState state) {
        return HexRarity.baseFor(item == null ? null : item.getType()).bump(state.rarityBumps());
    }

    public String displayName(ItemStack item) {
        if (item == null) return "Nothing";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.getDisplayName() != null && !meta.getDisplayName().isBlank()) {
            return meta.getDisplayName();
        }
        return "&f" + Text.prettyEnum(item.getType().name());
    }

    private static void store(PersistentDataContainer pdc, NamespacedKey key, int value) {
        if (value <= 0) pdc.remove(key);
        else pdc.set(key, PersistentDataType.INTEGER, value);
    }

    // ---------------------------------------------------------------- stats

    private void applyAttributes(ItemStack item, ItemMeta meta, Kind kind, HexState state) {
        try {
            boolean hadModifiers = stripHexModifiers(meta);
            if (!hadModifiers && !state.blank()) keepVanillaAttributes(item, meta, kind);

            double damage = perLevel("tier", "attack-damage-per-level") * state.tier()
                    + perLevel("stars", "attack-damage-per-star") * state.stars()
                    + perLevel("hot-potato", "attack-damage-per-point") * state.potato()
                    + perLevel("gemstone-slots", "attack-damage-per-slot") * state.gemSlots();
            double armor = perLevel("tier", "armor-per-level") * state.tier()
                    + perLevel("stars", "armor-per-star") * state.stars()
                    + perLevel("hot-potato", "armor-per-point") * state.potato()
                    + perLevel("gemstone-slots", "armor-per-slot") * state.gemSlots();
            double toughness = perLevel("tier", "armor-toughness-per-level") * state.tier()
                    + perLevel("stars", "armor-toughness-per-star") * state.stars();
            double health = perLevel("hot-potato", "health-per-point") * state.potato();
            double speed = 0;

            HexReforge reforge = reforge(state.reforge());
            if (reforge != null) {
                damage += reforge.attackDamage();
                armor += reforge.armor();
                toughness += reforge.armorToughness();
                health += reforge.health();
                speed += reforge.speed();
            }

            EquipmentSlotGroup group = switch (kind) {
                case ARMOR -> EquipmentSlotGroup.ARMOR;
                case WEAPON, TOOL -> EquipmentSlotGroup.MAINHAND;
                case OTHER -> EquipmentSlotGroup.ANY;
            };

            if (kind == Kind.ARMOR) {
                addModifier(meta, Attribute.ARMOR, "hex_armor", armor, group);
                addModifier(meta, Attribute.ARMOR_TOUGHNESS, "hex_toughness", toughness, group);
                addModifier(meta, Attribute.MAX_HEALTH, "hex_health", health, group);
                addModifier(meta, Attribute.MOVEMENT_SPEED, "hex_speed", speed, group);
            } else {
                addModifier(meta, Attribute.ATTACK_DAMAGE, "hex_attack_damage", damage, group);
                addModifier(meta, Attribute.MAX_HEALTH, "hex_health", health, group);
                addModifier(meta, Attribute.MOVEMENT_SPEED, "hex_speed", speed, group);
            }
        } catch (Throwable ex) {
            if (!attributeFailureLogged) {
                attributeFailureLogged = true;
                plugin.getLogger().warning("Hex stat modifiers are unavailable on this server version ("
                        + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "). Hex upgrades will still apply,"
                        + " but their stat bonuses will not.");
            }
        }
    }

    /** Removes only the modifiers this plugin added. Returns true when the item still has others. */
    private boolean stripHexModifiers(ItemMeta meta) {
        Multimap<Attribute, AttributeModifier> existing = meta.getAttributeModifiers();
        if (existing == null || existing.isEmpty()) return false;
        boolean foreignRemains = false;
        for (Map.Entry<Attribute, AttributeModifier> entry : List.copyOf(existing.entries())) {
            AttributeModifier modifier = entry.getValue();
            NamespacedKey key = modifier.getKey();
            if (key != null && namespace.equals(key.getNamespace()) && key.getKey().startsWith("hex_")) {
                meta.removeAttributeModifier(entry.getKey(), modifier);
            } else {
                foreignRemains = true;
            }
        }
        return foreignRemains;
    }

    /**
     * Once an item carries explicit modifiers, the client stops applying the
     * material's built-in ones. Copy those in first so a Hex upgrade never
     * removes a sword's base damage or a chestplate's base armor.
     */
    private void keepVanillaAttributes(ItemStack item, ItemMeta meta, Kind kind) {
        EquipmentSlot slot = vanillaSlot(item.getType(), kind);
        Multimap<Attribute, AttributeModifier> defaults = item.getType().getDefaultAttributeModifiers(slot);
        if (defaults == null || defaults.isEmpty()) return;
        for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
            meta.addAttributeModifier(entry.getKey(), entry.getValue());
        }
    }

    private static EquipmentSlot vanillaSlot(Material material, Kind kind) {
        String name = material.name();
        if (kind == Kind.ARMOR) {
            if (name.endsWith("_HELMET") || name.equals("TURTLE_HELMET")) return EquipmentSlot.HEAD;
            if (name.endsWith("_CHESTPLATE") || name.equals("ELYTRA")) return EquipmentSlot.CHEST;
            if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
            if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        }
        return EquipmentSlot.HAND;
    }

    private void addModifier(ItemMeta meta, Attribute attribute, String key, double amount, EquipmentSlotGroup group) {
        if (amount == 0 || !Double.isFinite(amount)) return;
        meta.addAttributeModifier(attribute, new AttributeModifier(
                new NamespacedKey(plugin, key), amount, AttributeModifier.Operation.ADD_NUMBER, group));
    }

    private double perLevel(String upgrade, String path) {
        return plugin.getConfig().getDouble("hex.upgrades." + upgrade + "." + path, 0);
    }

    public HexReforge reforge(String id) {
        if (id == null || id.isBlank()) return null;
        String key = id.toUpperCase(Locale.ROOT);
        var section = plugin.getConfig().getConfigurationSection("hex.reforge.native." + key);
        if (section == null) return null;
        return HexReforge.from(key, section);
    }

    // ----------------------------------------------------------------- lore

    private void applyLore(ItemStack item, ItemMeta meta, HexState state) {
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line != null && line.startsWith(LORE_MARKER));
        while (!lore.isEmpty() && lore.get(lore.size() - 1).isBlank()) lore.remove(lore.size() - 1);

        if (!state.blank()) {
            if (!lore.isEmpty()) lore.add(marked(""));
            lore.add(marked(GuiTheme.RULE));
            lore.add(marked("&d&lTHE HEX"));

            HexReforge reforge = reforge(state.reforge());
            if (reforge != null) lore.add(marked("&7Reforge: " + reforge.display() + " &8(" + reforge.summary() + ")"));
            if (state.tier() > 0) {
                int max = Math.max(1, plugin.getConfig().getInt("hex.upgrades.tier.max-level", 10));
                lore.add(marked("&7Hex Tier: &b" + state.tier() + "&7/&b" + max + " "
                        + GuiTheme.bar(state.tier() / (double) max, 10, "&b", "&8")));
            }
            if (state.stars() > 0) {
                int max = Math.max(1, plugin.getConfig().getInt("hex.upgrades.stars.max-level", 5));
                lore.add(marked("&7Stars: " + GuiTheme.stars(state.stars(), max)));
            }
            if (state.potato() > 0) lore.add(marked("&7Hot Potato Points: &6" + state.potato()));
            if (state.gemSlots() > 0) lore.add(marked("&7Gemstone Slots: &d" + state.gemSlots()));
            lore.add(marked("&7Rarity: " + rarity(item, state).display()
                    + (state.rarityBumps() > 0 ? " &d(Recombobulated)" : "")));
        }

        meta.setLore(lore);
    }

    private static String marked(String line) {
        return LORE_MARKER + Text.color(line);
    }
}
