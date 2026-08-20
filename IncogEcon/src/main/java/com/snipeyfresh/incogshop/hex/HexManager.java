package com.snipeyfresh.incogshop.hex;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.economy.WalletManager;
import com.snipeyfresh.incogshop.hex.integration.ArmorUpgradeIntegration;
import com.snipeyfresh.incogshop.hex.integration.HexIntegrations;
import com.snipeyfresh.incogshop.hex.integration.ReforgeIntegration;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The Hex: essence banking and item upgrades.
 *
 * <p>Modelled on Hypixel Skyblock's Hex, adapted to an economy plugin. Players
 * bank essence, then spend essence and coins to raise an item's Hex tier, add
 * stars and hot potato points, unlock gemstone slots, recombobulate its rarity,
 * push its enchantments past their normal ceiling, and reforge it.</p>
 *
 * <p>Where another plugin already owns part of that ladder, the Hex defers to
 * it: EcoArmor tiers and advancements are applied through EcoArmor, and
 * reforges through a reforge plugin when one is installed.</p>
 */
public final class HexManager {
    /** Upgrade ids, matching the config keys under {@code hex.upgrades}. */
    public static final String TIER = "tier";
    public static final String STARS = "stars";
    public static final String POTATO = "hot-potato";
    public static final String GEMSTONES = "gemstone-slots";
    public static final String RECOMBOBULATOR = "recombobulator";
    public static final String ENCHANTMENTS = "enchantments";

    private final IncogShopPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Long>> essence = new HashMap<>();
    private final LinkedHashMap<String, EssenceType> types = new LinkedHashMap<>();
    private final HexItems items;
    private final HexIntegrations integrations;

    public HexManager(IncogShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hex-essence.yml");
        this.items = new HexItems(plugin);
        this.integrations = new HexIntegrations(plugin);
    }

    // --------------------------------------------------------------- loading

    public void load() {
        loadTypes();
        loadEssence();
        integrations.reload();
    }

    private void loadTypes() {
        types.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("hex.essence.types");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                if (entry == null) continue;
                String id = key.toUpperCase(Locale.ROOT);
                Material icon = Material.matchMaterial(entry.getString("icon", "AMETHYST_SHARD"));
                types.put(id, new EssenceType(
                        id,
                        entry.getString("display", "&d" + Text.prettyEnum(id) + " Essence"),
                        icon == null ? Material.AMETHYST_SHARD : icon,
                        Math.max(0, entry.getDouble("buy-price", 0))));
            }
        }
        if (types.isEmpty()) {
            // Keeps the Hex usable when the config section is missing entirely.
            types.put("WITHER", new EssenceType("WITHER", "&8Wither Essence", Material.WITHER_SKELETON_SKULL, 750));
            types.put("UNDEAD", new EssenceType("UNDEAD", "&2Undead Essence", Material.ROTTEN_FLESH, 120));
            types.put("DRAGON", new EssenceType("DRAGON", "&5Dragon Essence", Material.DRAGON_BREATH, 900));
            types.put("SPIDER", new EssenceType("SPIDER", "&aSpider Essence", Material.SPIDER_EYE, 150));
            types.put("ICE", new EssenceType("ICE", "&bIce Essence", Material.BLUE_ICE, 200));
            types.put("DIAMOND", new EssenceType("DIAMOND", "&bDiamond Essence", Material.DIAMOND, 500));
            types.put("GOLD", new EssenceType("GOLD", "&6Gold Essence", Material.GOLD_INGOT, 300));
            types.put("CRIMSON", new EssenceType("CRIMSON", "&cCrimson Essence", Material.CRIMSON_FUNGUS, 650));
        }
    }

    private void loadEssence() {
        essence.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yml.getConfigurationSection("players");
        if (players == null) return;
        for (String rawId : players.getKeys(false)) {
            ConfigurationSection wallet = players.getConfigurationSection(rawId);
            if (wallet == null) continue;
            try {
                UUID id = UUID.fromString(rawId);
                Map<String, Long> balances = new LinkedHashMap<>();
                for (String type : wallet.getKeys(false)) {
                    long amount = Math.max(0, wallet.getLong(type, 0));
                    if (amount > 0) balances.put(type.toUpperCase(Locale.ROOT), amount);
                }
                if (!balances.isEmpty()) essence.put(id, balances);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed player entries instead of dropping the whole file.
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> entry : essence.entrySet()) {
            for (Map.Entry<String, Long> balance : entry.getValue().entrySet()) {
                if (balance.getValue() > 0) yml.set("players." + entry.getKey() + "." + balance.getKey(), balance.getValue());
            }
        }
        plugin.io().writeTextAtomic(file.toPath(), yml.saveToString(), "hex-essence.yml");
    }

    // ---------------------------------------------------------------- access

    public boolean enabled() { return plugin.getConfig().getBoolean("hex.enabled", true); }
    public HexItems items() { return items; }
    public HexIntegrations integrations() { return integrations; }
    public Collection<EssenceType> types() { return types.values(); }

    public EssenceType type(String id) {
        return id == null ? null : types.get(id.toUpperCase(Locale.ROOT));
    }

    public long essence(UUID player, String type) {
        if (player == null || type == null) return 0;
        return essence.getOrDefault(player, Map.of()).getOrDefault(type.toUpperCase(Locale.ROOT), 0L);
    }

    public Map<String, Long> essence(UUID player) {
        return Map.copyOf(essence.getOrDefault(player, Map.of()));
    }

    public long totalEssence(UUID player) {
        long total = 0;
        for (long amount : essence.getOrDefault(player, Map.of()).values()) total += amount;
        return total;
    }

    public void giveEssence(UUID player, String type, long amount) {
        if (player == null || amount <= 0) return;
        EssenceType essenceType = type(type);
        if (essenceType == null) return;
        Map<String, Long> wallet = essence.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
        wallet.merge(essenceType.id(), amount, Long::sum);
        save();
    }

    public boolean takeEssence(UUID player, String type, long amount) {
        if (player == null || amount <= 0) return true;
        EssenceType essenceType = type(type);
        if (essenceType == null) return false;
        Map<String, Long> wallet = essence.get(player);
        if (wallet == null) return false;
        long current = wallet.getOrDefault(essenceType.id(), 0L);
        if (current < amount) return false;
        long remaining = current - amount;
        if (remaining <= 0) wallet.remove(essenceType.id());
        else wallet.put(essenceType.id(), remaining);
        if (wallet.isEmpty()) essence.remove(player);
        save();
        return true;
    }

    // ----------------------------------------------------------------- costs

    public int maxLevel(String upgrade) {
        int fallback = switch (upgrade) {
            case TIER -> 10;
            case STARS -> 5;
            case POTATO -> 10;
            case GEMSTONES -> 3;
            case RECOMBOBULATOR -> 1;
            default -> 1;
        };
        return Math.max(0, plugin.getConfig().getInt("hex.upgrades." + upgrade + ".max-level", fallback));
    }

    public boolean upgradeEnabled(String upgrade) {
        return enabled() && plugin.getConfig().getBoolean("hex.upgrades." + upgrade + ".enabled", true);
    }

    public int level(HexState state, String upgrade) {
        return switch (upgrade) {
            case TIER -> state.tier();
            case STARS -> state.stars();
            case POTATO -> state.potato();
            case GEMSTONES -> state.gemSlots();
            case RECOMBOBULATOR -> state.rarityBumps();
            default -> 0;
        };
    }

    /** Cost of moving an upgrade to {@code nextLevel}, which is 1-based. */
    public HexCost cost(String upgrade, int nextLevel) {
        String root = "hex.upgrades." + upgrade + ".";
        int steps = Math.max(0, nextLevel - 1);
        double coins = plugin.getConfig().getDouble(root + "coin-base", 0)
                + plugin.getConfig().getDouble(root + "coin-per-level", 0) * steps;
        long amount = plugin.getConfig().getLong(root + "essence-base", 0)
                + plugin.getConfig().getLong(root + "essence-per-level", 0) * steps;
        String type = plugin.getConfig().getString(root + "essence-type", firstType());
        return HexCost.of(WalletManager.round(coins), type, amount);
    }

    private String firstType() {
        return types.isEmpty() ? "WITHER" : types.keySet().iterator().next();
    }

    /** Null when the player can pay, otherwise a message naming what is missing. */
    public String missing(UUID player, HexCost cost) {
        if (cost == null || cost.free()) return null;
        if (cost.coins() > 0 && plugin.wallets().get(player) + 1e-9 < cost.coins()) {
            return "You need " + plugin.money(cost.coins()) + " to pay for that upgrade.";
        }
        for (Map.Entry<String, Long> entry : cost.essence().entrySet()) {
            long held = essence(player, entry.getKey());
            if (held < entry.getValue()) {
                EssenceType type = type(entry.getKey());
                String name = type == null ? entry.getKey() : Text.color(type.display()) + "§r";
                return "You need " + (entry.getValue() - held) + " more " + name + ".";
            }
        }
        return null;
    }

    private boolean charge(Player player, HexCost cost, String reason, String subject) {
        if (cost == null || cost.free()) return true;
        UUID id = player.getUniqueId();
        if (missing(id, cost) != null) return false;
        if (cost.coins() > 0 && !plugin.wallets().withdraw(id, cost.coins())) return false;
        for (Map.Entry<String, Long> entry : cost.essence().entrySet()) {
            takeEssence(id, entry.getKey(), entry.getValue());
        }
        plugin.market().audit(reason, id, player.getName(), subject, 1, cost.coins(), essenceSummary(cost));
        return true;
    }

    private static String essenceSummary(HexCost cost) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Long> entry : cost.essence().entrySet()) parts.add(entry.getValue() + " " + entry.getKey());
        return String.join(", ", parts);
    }

    // -------------------------------------------------------------- upgrades

    /** Shared safety checks run before the Hex writes anything to an item. */
    public HexResult guard(ItemStack item) {
        if (!enabled()) return HexResult.fail("The Hex is disabled on this server.");
        if (item == null || item.getType().isAir()) return HexResult.fail("Place an item in the Hex slot first.");
        if (item.getAmount() > 1) return HexResult.fail("The Hex upgrades one item at a time, not a stack.");
        if (!items.supported(item)) return HexResult.fail("The Hex can only upgrade weapons, tools, and armor.");
        String owner = integrations.protectedCustomItem(item) ? integrations.ownerPlugin(item) : null;
        if (owner != null) {
            return HexResult.fail("That item belongs to " + owner + " and is protected from Hex changes.");
        }
        return null;
    }

    public HexResult upgrade(Player player, ItemStack item, String upgrade) {
        HexResult blocked = guard(item);
        if (blocked != null) return blocked;
        if (!upgradeEnabled(upgrade)) return HexResult.fail("That Hex upgrade is disabled on this server.");
        if (ENCHANTMENTS.equals(upgrade)) return upgradeEnchantments(player, item);

        HexState state = items.read(item);
        int current = level(state, upgrade);
        int max = maxLevel(upgrade);
        if (current >= max) return HexResult.fail("That upgrade is already maxed on this item.");

        HexCost cost = cost(upgrade, current + 1);
        String missing = missing(player.getUniqueId(), cost);
        if (missing != null) return HexResult.fail(missing);
        if (!charge(player, cost, "HEX_" + upgrade.toUpperCase(Locale.ROOT).replace('-', '_'),
                item.getType().name())) {
            return HexResult.fail("The payment could not be taken. Nothing was changed.");
        }

        int next = current + 1;
        HexState updated = switch (upgrade) {
            case TIER -> state.withTier(next);
            case STARS -> state.withStars(next);
            case POTATO -> state.withPotato(next);
            case GEMSTONES -> state.withGemSlots(next);
            case RECOMBOBULATOR -> state.withRarityBumps(next);
            default -> state;
        };
        items.write(item, updated);
        return HexResult.ok(label(upgrade) + " is now " + next + "/" + max + ".");
    }

    private HexResult upgradeEnchantments(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getEnchants().isEmpty()) {
            return HexResult.fail("That item has no enchantments to strengthen.");
        }
        int above = Math.max(0, plugin.getConfig().getInt("hex.upgrades.enchantments.max-level-above-vanilla", 2));
        if (above == 0) return HexResult.fail("Enchantment upgrades are disabled on this server.");

        Map<Enchantment, Integer> current = new LinkedHashMap<>(meta.getEnchants());
        int overflow = 0;
        List<Enchantment> upgradable = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : current.entrySet()) {
            int ceiling = entry.getKey().getMaxLevel() + above;
            overflow = Math.max(overflow, entry.getValue() - entry.getKey().getMaxLevel());
            if (entry.getValue() < ceiling) upgradable.add(entry.getKey());
        }
        if (upgradable.isEmpty()) {
            return HexResult.fail("Every enchantment on this item is already at the Hex ceiling.");
        }

        HexCost base = cost(ENCHANTMENTS, overflow + 1);
        HexCost scaled = scale(base, upgradable.size());
        String missing = missing(player.getUniqueId(), scaled);
        if (missing != null) return HexResult.fail(missing);
        if (!charge(player, scaled, "HEX_ENCHANTMENTS", item.getType().name())) {
            return HexResult.fail("The payment could not be taken. Nothing was changed.");
        }

        for (Enchantment enchantment : upgradable) {
            meta.addEnchant(enchantment, current.get(enchantment) + 1, true);
        }
        item.setItemMeta(meta);
        items.refresh(item);
        return HexResult.ok("Strengthened " + upgradable.size() + " enchantment(s) by one level.");
    }

    private static HexCost scale(HexCost cost, int factor) {
        int multiplier = Math.max(1, factor);
        Map<String, Long> essence = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : cost.essence().entrySet()) {
            essence.put(entry.getKey(), entry.getValue() * multiplier);
        }
        return new HexCost(WalletManager.round(cost.coins() * multiplier), essence);
    }

    public String label(String upgrade) {
        return switch (upgrade) {
            case TIER -> "Hex Tier";
            case STARS -> "Master Stars";
            case POTATO -> "Hot Potato Points";
            case GEMSTONES -> "Gemstone Slots";
            case RECOMBOBULATOR -> "Recombobulation";
            case ENCHANTMENTS -> "Enchantment Power";
            default -> Text.prettyEnum(upgrade.replace('-', '_'));
        };
    }

    // -------------------------------------------------------------- reforges

    public List<HexReforge> nativeReforges() {
        List<HexReforge> out = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("hex.reforge.native");
        if (section == null) return out;
        for (String key : section.getKeys(false)) {
            out.add(HexReforge.from(key, section.getConfigurationSection(key)));
        }
        return out;
    }

    public HexCost reforgeCost() {
        return HexCost.of(
                WalletManager.round(plugin.getConfig().getDouble("hex.reforge.coin-cost", 0)),
                plugin.getConfig().getString("hex.reforge.essence-type", firstType()),
                plugin.getConfig().getLong("hex.reforge.essence-cost", 0));
    }

    public boolean reforgeEnabled() {
        return enabled() && plugin.getConfig().getBoolean("hex.reforge.enabled", true);
    }

    /** Reforge ids offered for this item, from a reforge plugin when present. */
    public List<String> reforgeOptions(ItemStack item) {
        ReforgeIntegration provider = integrations.reforgeProviderFor(item);
        if (provider != null) return provider.options(item);
        return nativeReforges().stream().map(HexReforge::id).toList();
    }

    public String reforgeDisplay(ItemStack item, String reforgeId) {
        if (reforgeId == null || reforgeId.isBlank()) return "&8None";
        ReforgeIntegration provider = integrations.reforgeProviderFor(item);
        if (provider != null) return "&f" + provider.display(reforgeId);
        HexReforge reforge = items.reforge(reforgeId);
        return reforge == null ? "&f" + reforgeId : reforge.display();
    }

    public String currentReforge(ItemStack item) {
        ReforgeIntegration provider = integrations.reforgeProviderFor(item);
        if (provider != null) {
            String applied = provider.current(item);
            if (applied != null) return applied;
        }
        String own = items.read(item).reforge();
        return own.isEmpty() ? null : own;
    }

    public HexResult reforge(Player player, ItemStack item, String reforgeId) {
        HexResult blocked = guard(item);
        if (blocked != null) return blocked;
        if (!reforgeEnabled()) return HexResult.fail("Reforging is disabled on this server.");
        if (reforgeId == null || reforgeId.isBlank()) return HexResult.fail("Choose a reforge first.");

        ReforgeIntegration provider = integrations.reforgeProviderFor(item);
        if (provider == null && items.reforge(reforgeId) == null) {
            return HexResult.fail("That reforge does not exist.");
        }

        HexCost cost = reforgeCost();
        String missing = missing(player.getUniqueId(), cost);
        if (missing != null) return HexResult.fail(missing);
        if (!charge(player, cost, "HEX_REFORGE", item.getType().name())) {
            return HexResult.fail("The payment could not be taken. Nothing was changed.");
        }

        if (provider != null) {
            if (!provider.apply(item, reforgeId)) {
                return HexResult.fail(provider.pluginName() + " refused that reforge for this item.");
            }
            return HexResult.ok("Reforged through " + provider.pluginName() + ": " + provider.display(reforgeId) + ".");
        }

        HexState state = items.read(item).withReforge(reforgeId.toUpperCase(Locale.ROOT));
        items.write(item, state);
        HexReforge applied = items.reforge(reforgeId);
        return HexResult.ok("Reforged to " + Text.color(applied == null ? reforgeId : applied.display()) + "§a.");
    }

    // ------------------------------------------------------------- EcoArmor

    public ArmorUpgradeIntegration armorProvider(ItemStack item) {
        return integrations.armorUpgradeFor(item);
    }

    public HexCost armorTierCost(ArmorUpgradeIntegration provider) {
        String root = "hex.integrations." + provider.id() + ".tier-upgrade.";
        return HexCost.of(
                WalletManager.round(plugin.getConfig().getDouble(root + "coin-cost", 25000)),
                plugin.getConfig().getString(root + "essence-type", firstType()),
                plugin.getConfig().getLong(root + "essence-cost", 20));
    }

    public HexCost armorAdvancementCost(ArmorUpgradeIntegration provider) {
        String root = "hex.integrations." + provider.id() + ".advancement.";
        return HexCost.of(
                WalletManager.round(plugin.getConfig().getDouble(root + "coin-cost", 40000)),
                plugin.getConfig().getString(root + "essence-type", firstType()),
                plugin.getConfig().getLong(root + "essence-cost", 30));
    }

    /** Pushes an armor piece one tier up through the plugin that owns it. */
    public HexResult upgradeArmorTier(Player player, ItemStack item) {
        if (!enabled()) return HexResult.fail("The Hex is disabled on this server.");
        ArmorUpgradeIntegration provider = armorProvider(item);
        if (provider == null) return HexResult.fail("No supported armor-upgrade plugin owns that item.");
        String next = provider.nextTier(item);
        if (next == null) return HexResult.fail("That piece is already at its highest " + provider.pluginName() + " tier.");

        HexCost cost = armorTierCost(provider);
        String missing = missing(player.getUniqueId(), cost);
        if (missing != null) return HexResult.fail(missing);
        if (!charge(player, cost, "HEX_ARMOR_TIER", provider.pluginName() + ":" + item.getType().name())) {
            return HexResult.fail("The payment could not be taken. Nothing was changed.");
        }
        if (!provider.upgradeTier(item)) {
            refund(player, cost);
            return HexResult.fail(provider.pluginName() + " refused the tier upgrade. Your payment was returned.");
        }
        return HexResult.ok(provider.pluginName() + " tier is now " + next + ".");
    }

    /** Applies the owning plugin's one-off advancement upgrade, such as EcoArmor's. */
    public HexResult advanceArmor(Player player, ItemStack item) {
        if (!enabled()) return HexResult.fail("The Hex is disabled on this server.");
        ArmorUpgradeIntegration provider = armorProvider(item);
        if (provider == null || !provider.supportsAdvancement(item)) {
            return HexResult.fail("That item has no advancement upgrade available.");
        }
        if (provider.advanced(item)) return HexResult.fail("That piece is already advanced.");

        HexCost cost = armorAdvancementCost(provider);
        String missing = missing(player.getUniqueId(), cost);
        if (missing != null) return HexResult.fail(missing);
        if (!charge(player, cost, "HEX_ARMOR_ADVANCE", provider.pluginName() + ":" + item.getType().name())) {
            return HexResult.fail("The payment could not be taken. Nothing was changed.");
        }
        if (!provider.advance(item)) {
            refund(player, cost);
            return HexResult.fail(provider.pluginName() + " refused the advancement. Your payment was returned.");
        }
        return HexResult.ok("Applied the " + provider.pluginName() + " advancement upgrade.");
    }

    private void refund(Player player, HexCost cost) {
        if (cost.coins() > 0) plugin.wallets().deposit(player.getUniqueId(), cost.coins());
        for (Map.Entry<String, Long> entry : cost.essence().entrySet()) {
            giveEssence(player.getUniqueId(), entry.getKey(), entry.getValue());
        }
    }

    // --------------------------------------------------------------- essence

    public boolean buyingAllowed() {
        return enabled() && plugin.getConfig().getBoolean("hex.essence.allow-buying", true);
    }

    public HexResult buyEssence(Player player, String typeId, long amount) {
        if (!buyingAllowed()) return HexResult.fail("Essence cannot be bought on this server.");
        EssenceType type = type(typeId);
        if (type == null) return HexResult.fail("Unknown essence type.");
        if (!type.purchasable()) return HexResult.fail(Text.color(type.display()) + "§c cannot be bought.");
        if (amount <= 0) return HexResult.fail("Enter a positive amount.");

        double price = WalletManager.round(type.buyPrice() * amount);
        if (plugin.wallets().get(player.getUniqueId()) + 1e-9 < price) {
            return HexResult.fail("That costs " + plugin.money(price) + " and you cannot afford it.");
        }
        if (!plugin.wallets().withdraw(player.getUniqueId(), price)) {
            return HexResult.fail("The economy provider rejected the payment.");
        }
        giveEssence(player.getUniqueId(), type.id(), amount);
        plugin.market().audit("HEX_ESSENCE_BUY", player.getUniqueId(), player.getName(), type.id(), (int) Math.min(Integer.MAX_VALUE, amount), price, "");
        return HexResult.ok("Bought " + amount + "x " + Text.color(type.display()) + "§a for " + plugin.money(price) + ".");
    }
}
