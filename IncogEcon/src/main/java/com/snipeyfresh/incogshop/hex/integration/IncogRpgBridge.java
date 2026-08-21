package com.snipeyfresh.incogshop.hex.integration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Reflection bridge to IncogRPG's reforge and custom-enchant APIs.
 *
 * Zero compile-time dependency on IncogRPG. All access is via reflection; when
 * IncogRPG is absent or its API shape differs, every method returns a safe empty
 * or null value and the Hex degrades gracefully.
 */
public final class IncogRpgBridge {

    // IncogRPG plugin-level objects (all Object-typed to avoid compile dep)
    private Object apiInst;
    private Object rfService;
    private Object enchService;

    // Cached method references for the API object
    private Method apiReforges;      // () -> Map<String, ?>
    private Method apiItemReforge;   // (ItemStack) -> Optional<?>
    private Method apiEnchants;      // () -> Map<String, ?>
    private Method apiItemEnchants;  // (ItemStack) -> Map<String, Integer>

    // Cached method references for the service objects
    private Method rfCompatible;     // (ItemStack) -> List<?>
    private Method rfApply2;         // (ItemStack, Object) -> boolean
    private Method rfCost;           // (ItemStack, Object) -> double
    private Method enchApply;        // (ItemStack, Object, int, boolean) -> Enum

    // Definition accessor methods (cached from the first definition object we see)
    private Method defId;
    private Method defDisplayName;
    private Method defDescription;
    private Method defRarity;
    private Method defMaxLevel;        // enchant definitions only
    private Method defEnchSupports;    // (Material) -> boolean, enchant definitions
    private Method defRfSupports;      // (Material) -> boolean, reforge definitions

    // Cached definition maps keyed by id
    private Map<String, Object> rfDefs   = Map.of();
    private Map<String, Object> enchDefs = Map.of();

    /** Locate IncogRPG and cache all method handles. Returns true on success. */
    public boolean init() {
        apiInst = null; rfService = null; enchService = null;
        rfDefs = Map.of(); enchDefs = Map.of();

        Plugin rpg = Bukkit.getPluginManager().getPlugin("IncogRPG");
        if (rpg == null || !rpg.isEnabled()) return false;

        apiInst    = Reflect.call(Reflect.method(rpg.getClass(), new String[]{"api"}), rpg);
        rfService  = Reflect.call(Reflect.method(rpg.getClass(), new String[]{"reforges"}), rpg);
        enchService= Reflect.call(Reflect.method(rpg.getClass(), new String[]{"enchants"}), rpg);
        if (apiInst == null) return false;

        apiReforges     = Reflect.method(apiInst.getClass(), new String[]{"reforges"});
        apiItemReforge  = Reflect.method(apiInst.getClass(), new String[]{"itemReforge"}, ItemStack.class);
        apiEnchants     = Reflect.method(apiInst.getClass(), new String[]{"enchants"});
        apiItemEnchants = Reflect.method(apiInst.getClass(), new String[]{"itemEnchants"}, ItemStack.class);

        if (rfService != null) {
            rfCompatible = Reflect.method(rfService.getClass(), new String[]{"compatible"}, ItemStack.class);
            rfApply2     = Reflect.looseMethod(rfService.getClass(), new String[]{"apply"}, 2);
            rfCost       = Reflect.looseMethod(rfService.getClass(), new String[]{"cost"}, 2);
        }
        if (enchService != null) {
            enchApply = Reflect.looseMethod(enchService.getClass(), new String[]{"apply"}, 4);
        }

        reloadData();
        return apiReforges != null;
    }

    @SuppressWarnings("unchecked")
    private void reloadData() {
        Object rfMap = Reflect.call(apiReforges, apiInst);
        rfDefs = rfMap instanceof Map<?,?> m ? (Map<String, Object>) m : Map.of();

        Object enchMap = Reflect.call(apiEnchants, apiInst);
        enchDefs = enchMap instanceof Map<?,?> m ? (Map<String, Object>) m : Map.of();

        // Cache definition accessor methods from the first available sample object
        if (!rfDefs.isEmpty()) {
            Object s = rfDefs.values().iterator().next();
            defId          = Reflect.method(s.getClass(), new String[]{"id"});
            defDisplayName = Reflect.method(s.getClass(), new String[]{"displayName"});
            defRarity      = Reflect.method(s.getClass(), new String[]{"rarity"});
            defRfSupports  = Reflect.method(s.getClass(), new String[]{"supports"}, Material.class);
        }
        if (!enchDefs.isEmpty()) {
            Object s = enchDefs.values().iterator().next();
            if (defId          == null) defId          = Reflect.method(s.getClass(), new String[]{"id"});
            if (defDisplayName == null) defDisplayName = Reflect.method(s.getClass(), new String[]{"displayName"});
            if (defRarity      == null) defRarity      = Reflect.method(s.getClass(), new String[]{"rarity"});
            defDescription  = Reflect.method(s.getClass(), new String[]{"description"});
            defMaxLevel     = Reflect.method(s.getClass(), new String[]{"maxLevel"});
            defEnchSupports = Reflect.method(s.getClass(), new String[]{"supports"}, Material.class);
        }
    }

    public boolean available() { return apiInst != null && apiReforges != null; }

    // ---- internal helpers ----

    private String str(Method m, Object target) {
        Object r = Reflect.call(m, target);
        return r instanceof String s ? s : r != null ? String.valueOf(r) : "";
    }

    private int intOf(Method m, Object target) {
        Object r = Reflect.call(m, target);
        return r instanceof Integer i ? i : r instanceof Number n ? n.intValue() : 0;
    }

    // ---- Reforges ----

    public List<String> compatibleReforges(ItemStack item) {
        if (rfService == null || rfCompatible == null) return List.of();
        Object list = Reflect.call(rfCompatible, rfService, item);
        if (!(list instanceof Iterable<?> it)) return List.of();
        List<String> ids = new ArrayList<>();
        for (Object def : it) {
            String id = str(defId, def);
            if (!id.isBlank()) ids.add(id);
        }
        return ids;
    }

    public String currentReforgeId(ItemStack item) {
        if (apiItemReforge == null) return null;
        Object opt = Reflect.call(apiItemReforge, apiInst, item);
        if (!(opt instanceof Optional<?> o)) return null;
        return o.map(def -> str(defId, def)).filter(s -> !s.isBlank()).orElse(null);
    }

    public String reforgeDisplayName(String id) {
        Object def = rfDefs.get(id);
        return def == null ? id : str(defDisplayName, def);
    }

    public String reforgeRarity(String id) {
        Object def = rfDefs.get(id);
        return def == null ? "" : str(defRarity, def);
    }

    /** Cost in coins, as configured in IncogRPG. */
    public double reforgeCost(ItemStack item, String id) {
        if (rfService == null || rfCost == null) return 0;
        Object def = rfDefs.get(id);
        if (def == null) return 0;
        Object result = Reflect.call(rfCost, rfService, item, def);
        return result instanceof Number n ? n.doubleValue() : 0;
    }

    public boolean applyReforge(ItemStack item, String id) {
        if (rfService == null || rfApply2 == null) return false;
        Object def = rfDefs.get(id);
        if (def == null) return false;
        Object result = Reflect.call(rfApply2, rfService, item, def);
        return Boolean.TRUE.equals(result);
    }

    // ---- Enchants ----

    @SuppressWarnings("unchecked")
    public Map<String, Integer> currentEnchants(ItemStack item) {
        if (apiItemEnchants == null) return Map.of();
        Object r = Reflect.call(apiItemEnchants, apiInst, item);
        return r instanceof Map<?,?> m ? (Map<String, Integer>) m : Map.of();
    }

    public List<String> compatibleEnchants(ItemStack item) {
        if (item == null || defEnchSupports == null) return List.of();
        Material mat = item.getType();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Object> e : enchDefs.entrySet()) {
            Object r = Reflect.call(defEnchSupports, e.getValue(), mat);
            if (Boolean.TRUE.equals(r)) out.add(e.getKey());
        }
        return out;
    }

    public String enchantDisplayName(String id) {
        Object def = enchDefs.get(id);
        return def == null ? id : str(defDisplayName, def);
    }

    public int enchantMaxLevel(String id) {
        Object def = enchDefs.get(id);
        return def == null ? 1 : Math.max(1, intOf(defMaxLevel, def));
    }

    @SuppressWarnings("unchecked")
    public List<String> enchantDescription(String id) {
        Object def = enchDefs.get(id);
        if (def == null || defDescription == null) return List.of();
        Object r = Reflect.call(defDescription, def);
        return r instanceof List<?> l ? (List<String>) l : List.of();
    }

    public String enchantRarity(String id) {
        Object def = enchDefs.get(id);
        return def == null ? "" : str(defRarity, def);
    }

    /** Applies the enchant at the given level. Returns true on success. */
    public boolean applyEnchant(ItemStack item, String id, int level) {
        if (enchService == null || enchApply == null) return false;
        Object def = enchDefs.get(id);
        if (def == null) return false;
        Object result = Reflect.call(enchApply, enchService, item, def, level, false);
        return result != null && "SUCCESS".equals(String.valueOf(result));
    }
}
