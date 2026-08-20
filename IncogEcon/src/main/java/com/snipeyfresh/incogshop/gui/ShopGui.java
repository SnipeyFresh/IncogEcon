package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.market.MarketEntry;
import com.snipeyfresh.incogshop.market.MarketMode;
import com.snipeyfresh.incogshop.custom.CustomCategoryManager.CustomCategory;
import com.snipeyfresh.incogshop.custom.CustomCategoryManager.CustomSubcategory;
import com.snipeyfresh.incogshop.shop.PlayerShop;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ShopGui {
    public static final int ITEMS_PER_PAGE = 36;
    public static int[] centeredSlots(int count) {
        if (count <= 0) return new int[0];

        int[] slots = new int[count];
        int rows = Math.max(1, (int) Math.ceil(count / 7.0));
        int basePerRow = count / rows;
        int extra = count % rows;
        int written = 0;

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            int inRow = basePerRow + (rowIndex < extra ? 1 : 0);
            int inventoryRow = rowIndex + 1;
            int[] columns = centeredColumns(inRow);
            for (int column : columns) {
                if (written >= count) break;
                slots[written++] = inventoryRow * 9 + column;
            }
        }
        return slots;
    }

    private static int[] centeredColumns(int count) {
        return switch (count) {
            case 1 -> new int[]{4};
            case 2 -> new int[]{3, 5};
            case 3 -> new int[]{3, 4, 5};
            case 4 -> new int[]{2, 3, 5, 6};
            case 5 -> new int[]{2, 3, 4, 5, 6};
            case 6 -> new int[]{1, 2, 3, 5, 6, 7};
            default -> new int[]{1, 2, 3, 4, 5, 6, 7};
        };
    }
    public int[] subcategoryLayoutSlots(int count) {
        int[] out = new int[count];
        int[] defaults = centeredSlots(count);
        for (int i = 0; i < count; i++) {
            int fallback = i < defaults.length ? defaults[i] : 10 + i;
            out[i] = plugin.layouts().slot("subcategories", "sub:" + i, fallback);
        }
        return out;
    }

    public java.util.Map<String,Integer> itemControlSlots() {
        java.util.LinkedHashMap<String,Integer> out = new java.util.LinkedHashMap<>();
        String[] keys = {"previous","back","search","clear","status","orders","section","page","next"};
        int[] defaults = {45,46,47,48,49,50,51,52,53};
        java.util.Set<Integer> used = new java.util.HashSet<>();

        for (int i=0;i<keys.length;i++) {
            int desired = plugin.layouts().slot("items", keys[i], defaults[i]);

            if (used.contains(desired)) {
                // Prefer this control's normal bottom-nav position first.
                if (!used.contains(defaults[i])) {
                    desired = defaults[i];
                } else {
                    // If that is also occupied, keep navigation controls in
                    // the bottom row instead of pushing them into slot 0.
                    desired = -1;
                    for (int candidate=45; candidate<=53; candidate++) {
                        if (!used.contains(candidate)) {
                            desired = candidate;
                            break;
                        }
                    }

                    // Only fall back outside the navbar if every bottom slot
                    // is genuinely occupied by distinct configured controls.
                    if (desired < 0) {
                        for (int candidate=0; candidate<45; candidate++) {
                            if (!used.contains(candidate)) {
                                desired = candidate;
                                break;
                            }
                        }
                    }
                }
            }

            used.add(desired);
            out.put(keys[i], desired);
        }
        return out;
    }

    public int[] itemLayoutSlots() {
        java.util.Set<Integer> reserved = new java.util.HashSet<>(itemControlSlots().values());
        int[] out = new int[ITEMS_PER_PAGE];
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int i=0;i<ITEMS_PER_PAGE;i++) {
            int desired = plugin.layouts().slot("items","item:"+i,i);
            if (reserved.contains(desired) || used.contains(desired)) {
                desired = -1;
                for (int candidate=0;candidate<54;candidate++) {
                    if (!reserved.contains(candidate) && !used.contains(candidate)) { desired=candidate; break; }
                }
            }
            out[i]=desired;
            used.add(desired);
        }
        return out;
    }
    private final IncogShopPlugin plugin;

    public ShopGui(IncogShopPlugin plugin) { this.plugin = plugin; }

    public record CategoryHolder(boolean admin) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record SubcategoryHolder(boolean admin, MarketCategory category) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record MarketHolder(int page, boolean admin, MarketCategory category, MarketSubcategory subcategory, String query) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record CustomSubcategoryHolder(String categoryId, boolean admin) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record CustomMarketHolder(String categoryId, String subcategoryId, int page, boolean admin) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record PlacementCategoryHolder(Material material) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record PlacementSubcategoryHolder(Material material, MarketCategory category) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }
    public record PlayerShopHolder(UUID shopId) implements InventoryHolder { @Override public Inventory getInventory() { return null; } }

    public void openCategories(Player player, boolean admin) {
        Inventory inv = Bukkit.createInventory(new CategoryHolder(admin), 54,
                GuiTheme.title(admin ? "&d&l" : "&6&l", admin ? "Admin Browse" : "Market"));
        fancyFrame(inv, admin ? Material.PURPLE_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE);

        List<String> keys = new ArrayList<>();
        List<Material> icons = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<List<String>> lores = new ArrayList<>();

        for (MarketCategory c : MarketCategory.values()) {
            long count = filtered(c, null, "", admin).size();
            keys.add("builtin:" + c.name());
            icons.add(c.icon());
            names.add("&6" + c.display());
            lores.add(List.of(GuiTheme.RULE,
                    GuiTheme.stat("Market items", count),
                    c == MarketCategory.ALL ? "&8Browse the entire market." : "&8Open its subcategories.",
                    "", "&e" + GuiTheme.CHEVRON + "Click to browse"));
        }
        for (CustomCategory c : plugin.customCategories().all()) {
            long count = plugin.customCategories().materials(c.id()).stream()
                    .filter(m -> admin || plugin.market().isMarketEnabled(m)).count();
            keys.add("custom:" + c.id());
            icons.add(c.icon());
            names.add("&d" + c.display());
            lores.add(List.of(GuiTheme.RULE, GuiTheme.stat("Market items", count),
                    "&8Custom category", "", "&e" + GuiTheme.CHEVRON + "Click to browse"));
        }

        int[] defaults = centeredSlots(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            int fallback = i < defaults.length ? defaults[i] : 10 + i;
            int slot = plugin.layouts().slot("categories", keys.get(i), fallback);
            inv.setItem(slot, named(icons.get(i), names.get(i), lores.get(i)));
        }

        int searchSlot = plugin.layouts().slot("categories", "search", 46);
        int ordersSlot = plugin.layouts().slot("categories", "orders", 48);
        int helpSlot = plugin.layouts().slot("categories", "help", 52);
        int balanceSlot = plugin.layouts().slot("categories", "balance", 50);
        inv.setItem(searchSlot, GuiTheme.button(Material.COMPASS, "&b&lSEARCH", List.of(
                "&7Smart matching supports aliases,",
                "&7partial names, and minor typos."), "Click, then type your search in chat"));

        if (!admin) {
            inv.setItem(ordersSlot, GuiTheme.button(Material.WRITABLE_BOOK, "&b&lMARKET ORDERS",
                    List.of("&7Browse every active Buy and Sell Order."), "Click to browse"));
            inv.setItem(balanceSlot, GuiTheme.panel(Material.GOLD_INGOT,
                    "&6&lBALANCE &8• &f" + plugin.money(plugin.wallets().get(player.getUniqueId())),
                    List.of(GuiTheme.stat("Economy", plugin.wallets().providerName()),
                            "&7Buy and sell directly from the market.")));
            inv.setItem(helpSlot, GuiTheme.panel(Material.BOOK, "&e&lBAZAAR GUIDE", List.of(
                    "&7Click a market item to open its",
                    "&7Buy / Sell / Orders page.",
                    "&7You can also click an eligible item",
                    "&7in your own inventory to jump to it.", "",
                    "&8Every action has its own button, so no",
                    "&8Java-only clicks are required.")));
        } else {
            inv.setItem(ordersSlot, GuiTheme.button(Material.NETHER_STAR, "&d&lADMIN STUDIO", List.of(
                    "&7Create/remove categories, organize items,",
                    "&7add materials, and edit the GUI."), "Click to open"));
            inv.setItem(balanceSlot, infiniteStockButton());
            inv.setItem(helpSlot, GuiTheme.button(Material.ARMOR_STAND, "&5&lLAYOUT DESIGNER", List.of(
                    "&7Move buttons and item slots visually."), "Click to customize"));
        }
        fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    public void openSubcategories(Player player, boolean admin, MarketCategory category) {
        if (category == MarketCategory.ALL) { openMarket(player, 0, admin, MarketCategory.ALL, null, ""); return; }
        Inventory inv = Bukkit.createInventory(new SubcategoryHolder(admin, category), 54, GuiTheme.title(admin ? "&d&l" : "&6&l", category.display(), admin ? "Admin" : null));
        fancyFrame(inv, admin ? Material.PURPLE_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(category.icon(), "&6&l" + category.display().toUpperCase(Locale.ROOT),
                List.of("&7Choose a section below.", "&8Buttons stay centered for quick scanning.")));
        List<MarketSubcategory> subs = MarketSubcategory.forCategory(category);
        int[] subcategorySlots = subcategoryLayoutSlots(subs.size());
        for (int i = 0; i < subs.size() && i < subcategorySlots.length; i++) {
            MarketSubcategory sub = subs.get(i);
            long count = filtered(category, sub, "", admin).size();
            inv.setItem(subcategorySlots[i], GuiTheme.button(sub.icon(), "&e" + sub.display(),
                    List.of(GuiTheme.stat("Items", count)), "Click to browse"));
        }
        inv.setItem(plugin.layouts().slot("subcategories","all",47), GuiTheme.button(Material.CHEST, "&6All " + category.display(),
                List.of("&7Show every item in this category."), "Click to browse"));
        inv.setItem(plugin.layouts().slot("subcategories","search",49), GuiTheme.button(Material.COMPASS, "&bSearch " + category.display(),
                List.of("&7Smart search within this category.", "&7Typos and partial names are supported."), "Click, then type in chat"));
        inv.setItem(plugin.layouts().slot("subcategories","back",45), GuiTheme.back("categories"));
        inv.setItem(plugin.layouts().slot("subcategories","status",51), named(admin ? Material.COMMAND_BLOCK : Material.GOLD_INGOT, admin ? "&cAdmin Mode" : "&6Balance: &f" + plugin.money(plugin.wallets().get(player.getUniqueId())), admin ? List.of("&7Category management is available", "&7from the item list using Q.") : List.of("&7Select a subcategory to continue.")));
        if (admin) inv.setItem(50, infiniteStockButton());
        fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(inv);
    }

    public void openMarket(Player player, int page, boolean admin) { openMarket(player, page, admin, MarketCategory.ALL, null, ""); }
    public void openMarket(Player player, int page, boolean admin, MarketCategory category, String query) { openMarket(player, page, admin, category, null, query); }

    public void openMarket(Player player, int requestedPage, boolean admin, MarketCategory category, MarketSubcategory subcategory, String query) {
        List<Material> materials = filtered(category, subcategory, query, admin);
        int pages = Math.max(1, (int)Math.ceil(materials.size() / (double)ITEMS_PER_PAGE));
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        String section = subcategory != null ? subcategory.display() : category.display();
        String suffix = query == null || query.isBlank() ? section : "Search: " + query;
        Inventory inv = Bukkit.createInventory(new MarketHolder(page, admin, category, subcategory, query == null ? "" : query), 54, GuiTheme.title(admin ? "&d&l" : "&6&l", suffix, admin ? "Admin" : null));
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 36; i < 54; i++) inv.setItem(i, filler);
        int start = page * ITEMS_PER_PAGE;
        int[] itemSlots = itemLayoutSlots();
        for (int logical = 0; logical < ITEMS_PER_PAGE && start + logical < materials.size(); logical++) {
            inv.setItem(itemSlots[logical], marketIcon(materials.get(start + logical), admin));
        }

        inv.setItem(itemControlSlots().get("previous"), GuiTheme.previousPage(page, page > 0));
        inv.setItem(itemControlSlots().get("back"), GuiTheme.button(Material.CHEST,
                category == MarketCategory.ALL ? "&6Categories" : "&6Subcategories",
                List.of(category == MarketCategory.ALL ? "&7Return to category selection." : "&7Return to " + category.display() + " subcategories."),
                "Click to go back"));
        inv.setItem(itemControlSlots().get("search"), GuiTheme.button(Material.COMPASS, "&b&lSEARCH",
                List.of(GuiTheme.stat("Current", (query == null || query.isBlank()) ? "None" : query)),
                "Click, then type your search in chat"));
        if (query != null && !query.isBlank()) inv.setItem(itemControlSlots().get("clear"), GuiTheme.button(Material.BARRIER, "&cClear Search",
                List.of("&7Drop the search filter and return", "&7to the current section."), "Click to clear"));
        inv.setItem(itemControlSlots().get("status"), GuiTheme.panel(admin ? Material.COMMAND_BLOCK : Material.GOLD_INGOT, admin ? "&c&lADMIN CONTROLS" : "&6&lBALANCE &8• &f" + plugin.money(plugin.wallets().get(player.getUniqueId())), admin ? List.of("&aLeft &7+64 stock", "&cRight &7-64 stock", "&dCtrl+Q &7set exact stock", "&aShift-left &7+10% base price", "&cShift-right &7-10% base price", "&eMiddle &7reset price/pressure", "&bF &7cycle Buy+Sell / Sell Only / Disabled", "&dQ &7change category") : List.of("&eClick any market item", "&7to open its Bazaar page.", "&7Buy, sell, and order actions", "&7each have their own button.")));
        if (admin) inv.setItem(itemControlSlots().get("orders"), infiniteStockButton());
        else inv.setItem(itemControlSlots().get("orders"), GuiTheme.button(Material.WRITABLE_BOOK, "&b&lALL MARKET ORDERS",
                List.of("&7Browse all active Buy/Sell Orders."), "Click to browse"));
        inv.setItem(itemControlSlots().get("section"), GuiTheme.panel(category.icon(), "&6" + section, List.of("&7Current market section.")));
        inv.setItem(itemControlSlots().get("page"), GuiTheme.pageIndicator(page + 1, pages, "Items shown", materials.size()));
        inv.setItem(itemControlSlots().get("next"), GuiTheme.nextPage(page + 2, page + 1 < pages));
        player.openInventory(inv);
    }


    public void openCustomCategory(Player player, String categoryId, int requestedPage, boolean admin) {
        CustomCategory category = plugin.customCategories().get(categoryId);
        if (category == null) { openCategories(player, admin); return; }
        List<CustomSubcategory> subs = plugin.customCategories().subcategories(category.id());
        if (!subs.isEmpty()) {
            openCustomSubcategories(player, category.id(), admin);
            return;
        }
        openCustomMarket(player, category.id(), "", requestedPage, admin);
    }

    public void openCustomSubcategories(Player player, String categoryId, boolean admin) {
        CustomCategory category = plugin.customCategories().get(categoryId);
        if (category == null) { openCategories(player, admin); return; }
        Inventory inv = Bukkit.createInventory(new CustomSubcategoryHolder(category.id(), admin), 54,
                GuiTheme.title(admin ? "&d&l" : "&b&l", category.display(), admin ? "Admin" : null));
        decorate(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(4, GuiTheme.panel(category.icon(), "&b&l" + category.display().toUpperCase(Locale.ROOT), List.of("&7Choose a custom subcategory.")));
        List<CustomSubcategory> subs = plugin.customCategories().subcategories(category.id());
        int[] slots = centeredSlots(subs.size());
        for (int i=0;i<subs.size()&&i<slots.length;i++) {
            CustomSubcategory sub=subs.get(i);
            long count=plugin.customCategories().materials(category.id(),sub.id()).stream()
                    .filter(m->admin||plugin.market().isMarketEnabled(m)).count();
            inv.setItem(slots[i],GuiTheme.button(sub.icon(),"&e"+sub.display(),List.of(GuiTheme.stat("Items", count)),"Click to browse"));
        }
        inv.setItem(39,GuiTheme.button(Material.CHEST,"&6All "+category.display(),List.of("&7Show all items in this custom category."),"Click to browse"));
        inv.setItem(45,GuiTheme.back("categories"));
        player.openInventory(inv);
    }

    public void openCustomMarket(Player player, String categoryId, String subcategoryId, int requestedPage, boolean admin) {
        CustomCategory category = plugin.customCategories().get(categoryId);
        if (category == null) { openCategories(player, admin); return; }
        boolean specific=subcategoryId!=null&&!subcategoryId.isBlank();
        CustomSubcategory sub=specific?plugin.customCategories().subcategory(category.id(),subcategoryId):null;
        List<Material> materials=(specific&&sub!=null
                ? plugin.customCategories().materials(category.id(),sub.id())
                : plugin.customCategories().materials(category.id())).stream()
                .filter(m->admin||plugin.market().isMarketEnabled(m)).toList();
        int pages=Math.max(1,(int)Math.ceil(materials.size()/(double)ITEMS_PER_PAGE));
        int page=Math.max(0,Math.min(pages-1,requestedPage));
        String title=sub!=null?sub.display():category.display();
        Inventory inv=Bukkit.createInventory(new CustomMarketHolder(category.id(),sub==null?"":sub.id(),page,admin),54,
                GuiTheme.title(admin?"&d&l":"&b&l", title, admin?"Admin":null));
        ItemStack filler=named(Material.BLACK_STAINED_GLASS_PANE," ",List.of());
        for(int i=36;i<54;i++)inv.setItem(i,filler);
        int start=page*ITEMS_PER_PAGE;
        for(int slot=0;slot<ITEMS_PER_PAGE&&start+slot<materials.size();slot++)inv.setItem(slot,marketIcon(materials.get(start+slot),admin));
        inv.setItem(45,GuiTheme.previousPage(page,page>0));
        inv.setItem(46,GuiTheme.button(Material.CHEST,specific?"&6Subcategories":"&6Categories",List.of("&7Return to the previous menu."),"Click to go back"));
        inv.setItem(49,GuiTheme.panel(sub!=null?sub.icon():category.icon(),"&b&l"+title.toUpperCase(Locale.ROOT),List.of(GuiTheme.stat("Items", materials.size()))));
        inv.setItem(52,GuiTheme.pageIndicator(page+1,pages,"Items shown",materials.size()));
        inv.setItem(53,GuiTheme.nextPage(page+2,page+1<pages));
        player.openInventory(inv);
    }

    private ItemStack infiniteStockButton() {
        boolean enabled = plugin.market().isInfiniteStockEnabled();
        return GuiTheme.toggle(enabled, "INFINITE STOCK",
                enabled
                        ? List.of("&7Players can buy items even when", "&7stored market stock is 0.", "&7Purchases do not reduce stock.", "&7Selling still adds to stock.")
                        : List.of("&7Purchases require actual stock", "&7and reduce stored supply normally."),
                enabled ? "Click to disable infinite stock" : "Click to enable infinite stock");
    }

    public List<Material> filtered(MarketCategory category, String query) { return filtered(category, null, query, false); }
    public List<Material> filtered(MarketCategory category, String query, boolean includeDisabled) { return filtered(category, null, query, includeDisabled); }

    private static final Map<String, List<String>> SEARCH_ALIASES = Map.ofEntries(
            Map.entry("gap", List.of("golden apple")),
            Map.entry("gapple", List.of("golden apple")),
            Map.entry("god apple", List.of("enchanted golden apple")),
            Map.entry("notch apple", List.of("enchanted golden apple")),
            Map.entry("xp bottle", List.of("experience bottle")),
            Map.entry("exp bottle", List.of("experience bottle")),
            Map.entry("bottle o enchanting", List.of("experience bottle")),
            Map.entry("rocket", List.of("firework rocket")),
            Map.entry("firework", List.of("firework rocket")),
            Map.entry("totem", List.of("totem of undying")),
            Map.entry("ender pearl", List.of("ender pearl")),
            Map.entry("pearl", List.of("ender pearl")),
            Map.entry("elytra", List.of("elytra")),
            Map.entry("netherite sword", List.of("netherite sword")),
            Map.entry("netherite pick", List.of("netherite pickaxe")),
            Map.entry("diamond pick", List.of("diamond pickaxe")),
            Map.entry("iron pick", List.of("iron pickaxe")),
            Map.entry("gold pick", List.of("golden pickaxe")),
            Map.entry("wood pick", List.of("wooden pickaxe")),
            Map.entry("redstone dust", List.of("redstone")),
            Map.entry("lapis", List.of("lapis lazuli")),
            Map.entry("cobble", List.of("cobblestone")),
            Map.entry("logs", List.of("log")),
            Map.entry("planks", List.of("planks")),
            Map.entry("wool", List.of("wool"))
    );

    public List<Material> filtered(MarketCategory category, MarketSubcategory subcategory, String query, boolean includeDisabled) {
        List<Material> candidates = plugin.market().tradableMaterials().stream()
                .filter(m -> includeDisabled || plugin.market().isMarketEnabled(m))
                .filter(m -> category == MarketCategory.ALL || plugin.customCategories().assigned(m) == null)
                .filter(m -> category == MarketCategory.ALL || plugin.market().categoryOf(m) == category)
                .filter(m -> subcategory == null || plugin.market().subcategoryOf(m) == subcategory)
                .toList();

        String q = normalizeSearch(query);
        if (q.isBlank()) return candidates;

        return candidates.stream()
                .map(m -> new AbstractMap.SimpleEntry<>(m, searchScore(m, q)))
                .filter(e -> e.getValue() < Integer.MAX_VALUE)
                .sorted(Comparator
                        .comparingInt((AbstractMap.SimpleEntry<Material, Integer> e) -> e.getValue())
                        .thenComparing(e -> e.getKey().name()))
                .map(AbstractMap.SimpleEntry::getKey)
                .toList();
    }

    private int searchScore(Material material, String query) {
        String pretty = normalizeSearch(Text.prettyEnum(material.name()));
        String raw = normalizeSearch(material.name());

        // Exact and nearly-exact matches always win.
        if (pretty.equals(query) || raw.equals(query)) return 0;
        if (pretty.startsWith(query) || raw.startsWith(query)) return 10;
        if (containsWholeWordSequence(pretty, query)) return 20;

        // Alias resolution. An alias can point to one or more normal search phrases.
        for (var alias : SEARCH_ALIASES.entrySet()) {
            String aliasKey = normalizeSearch(alias.getKey());
            if (!aliasKey.equals(query) && !aliasKey.startsWith(query) && !query.startsWith(aliasKey)) continue;
            for (String target : alias.getValue()) {
                String normalizedTarget = normalizeSearch(target);
                if (pretty.equals(normalizedTarget)) return 5;
                if (pretty.contains(normalizedTarget) || raw.contains(normalizedTarget)) return 15;
            }
        }

        // Multi-word searches work in any useful partial form:
        // "red wo" -> Red Wool, "diamond sw" -> Diamond Sword.
        String[] terms = query.split(" ");
        boolean everyTermMatches = true;
        int partialPenalty = 0;
        for (String term : terms) {
            if (term.isBlank()) continue;
            if (pretty.contains(term) || raw.contains(term)) partialPenalty += 2;
            else {
                boolean fuzzyWord = false;
                for (String word : pretty.split(" ")) {
                    if (editDistance(term, word) <= allowedTypos(term.length())) {
                        fuzzyWord = true;
                        partialPenalty += 10;
                        break;
                    }
                }
                if (!fuzzyWord) {
                    everyTermMatches = false;
                    break;
                }
            }
        }
        if (everyTermMatches) return 30 + partialPenalty;

        if (pretty.contains(query) || raw.contains(query)) return 40;

        // Whole-name typo tolerance. Conservative enough that random searches
        // do not flood the results with unrelated materials.
        int distance = editDistance(query, pretty);
        int allowed = Math.max(1, Math.min(3, query.length() / 4));
        if (distance <= allowed) return 60 + distance * 5;

        return Integer.MAX_VALUE;
    }

    private static String normalizeSearch(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsWholeWordSequence(String text, String query) {
        return (" " + text + " ").contains(" " + query + " ");
    }

    private static int allowedTypos(int length) {
        if (length <= 3) return 0;
        if (length <= 6) return 1;
        return 2;
    }

    private static int editDistance(String a, String b) {
        if (a.equals(b)) return 0;
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();

        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    public void openPlacementCategories(Player player, Material material) {
        Inventory inv = Bukkit.createInventory(new PlacementCategoryHolder(material), 54, GuiTheme.title("&d&l", "Organize Item", "Category"));
        decorate(inv, Material.PURPLE_STAINED_GLASS_PANE);
        inv.setItem(4, marketIcon(material, true));
        List<MarketCategory> categories = java.util.Arrays.stream(MarketCategory.values()).filter(c -> c != MarketCategory.ALL).toList();
        int[] categorySlots = centeredSlots(categories.size());
        for (int i = 0; i < categories.size() && i < categorySlots.length; i++) {
            MarketCategory c = categories.get(i);
            boolean current = plugin.market().categoryOf(material) == c;
            inv.setItem(categorySlots[i], GuiTheme.button(c.icon(), (current ? "&a" : "&6") + c.display(),
                    List.of(current ? "&aCurrent category" : "&7Move item here"), current ? "Already selected" : "Click to continue"));
        }
        inv.setItem(40, GuiTheme.button(Material.RECOVERY_COMPASS, "&bReset Automatic Placement",
                List.of("&7Remove your custom category choice", "&7and use IncogEcon's automatic grouping."), "Click to reset"));
        inv.setItem(45, GuiTheme.button(Material.ARROW, "&eCancel", List.of("&7Return to the admin market."), "Click to cancel"));
        player.openInventory(inv);
    }

    public void openPlacementSubcategories(Player player, Material material, MarketCategory category) {
        Inventory inv = Bukkit.createInventory(new PlacementSubcategoryHolder(material, category), 54, GuiTheme.title("&d&l", "Organize Item", category.display()));
        decorate(inv, Material.PURPLE_STAINED_GLASS_PANE);
        inv.setItem(4, marketIcon(material, true));
        List<MarketSubcategory> subs = MarketSubcategory.forCategory(category);
        int[] subcategorySlots = subcategoryLayoutSlots(subs.size());
        for (int i = 0; i < subs.size() && i < subcategorySlots.length; i++) {
            MarketSubcategory sub = subs.get(i);
            boolean current = plugin.market().categoryOf(material) == category && plugin.market().subcategoryOf(material) == sub;
            inv.setItem(subcategorySlots[i], GuiTheme.button(sub.icon(), (current ? "&a" : "&e") + sub.display(),
                    List.of(current ? "&aCurrent subcategory" : "&7Place item here"), "Click to save"));
        }
        inv.setItem(45, GuiTheme.back("the category list"));
        player.openInventory(inv);
    }

    private ItemStack marketIcon(Material material, boolean admin) {
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        MarketEntry e = plugin.market().entry(material);
        meta.setDisplayName(Text.color("&f" + Text.prettyEnum(material.name())));
        List<String> lore = new ArrayList<>();
        lore.add(Text.color(GuiTheme.RULE));
        lore.add(Text.color(GuiTheme.stat("Category", plugin.market().categoryOf(material).display())));
        lore.add(Text.color(GuiTheme.stat("Section", plugin.market().subcategoryOf(material).display())));
        lore.add("");
        boolean infinite = plugin.market().isInfiniteStockEnabled();
        lore.add(Text.color(GuiTheme.stat("Server Stock", infinite ? "Unlimited" : String.valueOf(e.stock()))));
        if (!infinite) {
            double reference = Math.max(1.0, plugin.getConfig().getDouble("market.target-stock", 512) * 2.0);
            lore.add(Text.color(GuiTheme.bar(e.stock() / reference, 10, "&a", "&8") + " &8supply"));
        }
        MarketMode mode = plugin.market().marketMode(material);
        lore.add(Text.color("&7Market Mode: " + (mode == MarketMode.BUY_SELL ? "&aBuy & Sell" : mode == MarketMode.SELL_ONLY ? "&eSell Only" : "&cDisabled")));
        lore.add(Text.color((plugin.market().isBuyAllowed(material) ? "&aBuy: &f" + plugin.money(plugin.market().buyUnitPrice(material)) : "&cBuy: &8Unavailable")));
        lore.add(Text.color("&cSell: &f" + plugin.money(plugin.market().sellUnitPrice(material))));
        if (admin) {
            lore.add(Text.color("&7Base: &f" + plugin.money(e.basePrice())));
            lore.add(Text.color("&7Status: " + (plugin.market().isMarketEnabled(material) ? "&aEnabled" : "&cRemoved")));
            lore.add("");
            lore.add(Text.color("&aLeft &7+64  &cRight &7-64 stock"));
            lore.add(Text.color("&dCtrl+Q &7Set exact stock"));
            lore.add(Text.color("&aShift-left &7+10%  &cShift-right &7-10%"));
            lore.add(Text.color("&eMiddle &7Reset price  &bF &7Toggle market"));
            lore.add(Text.color("&dQ / Drop &7Change category"));
            lore.add(Text.color("&8Auto Restock: Admin Studio → Item Organizer"));
        } else {
            lore.add("");
            lore.add(Text.color("&e" + GuiTheme.CHEVRON + "&e&lCLICK TO OPEN BAZAAR"));
            lore.add(Text.color("&7Buy or sell instantly, or create"));
            lore.add(Text.color("&7Buy and Sell Orders for this item."));
            double bestBuyOrder = plugin.orders().bestBuy(material);
            double bestSellOrder = plugin.orders().bestSell(material);
            if (bestBuyOrder > 0 || bestSellOrder > 0) {
                lore.add("");
                lore.add(Text.color("&7Best order bid: " + (bestBuyOrder > 0 ? "&a" + plugin.money(bestBuyOrder) : "&8None")));
                lore.add(Text.color("&7Best order ask: " + (bestSellOrder > 0 ? "&c" + plugin.money(bestSellOrder) : "&8None")));
            }
            lore.add("");
            lore.add(Text.color("&8Tip: click this material in your inventory"));
            lore.add(Text.color("&8while the Bazaar is open to jump here."));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        icon.setItemMeta(meta);
        return icon;
    }

    public void openPlayerShop(Player player, PlayerShop shop) {
        Inventory inv = Bukkit.createInventory(new PlayerShopHolder(shop.id()), 45, GuiTheme.title("&a&l", "Player Shop", shop.ownerName()));
        decorate(inv, Material.BLACK_STAINED_GLASS_PANE);
        ItemStack display = shop.item().clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(""); lore.add(Text.color(GuiTheme.RULE));
        lore.add(Text.color(GuiTheme.stat("Price each", plugin.money(shop.price()))));
        lore.add(Text.color(GuiTheme.stat("Stock", plugin.shops().stock(shop))));
        lore.add(Text.color(GuiTheme.stat("Seller", shop.ownerName())));
        meta.setLore(lore); display.setItemMeta(meta);
        inv.setItem(13, display);
        inv.setItem(29, GuiTheme.button(Material.LIME_CONCRETE, "&a&lBUY 1", List.of(GuiTheme.stat("Cost", plugin.money(shop.price()))), "Click to buy one"));
        int stack = Math.max(1, shop.item().getMaxStackSize());
        inv.setItem(31, GuiTheme.button(Material.LIME_CONCRETE, "&a&lBUY UP TO " + stack,
                List.of("&7Limited by stock, balance,", "&7and inventory space."), "Click to buy a stack"));
        inv.setItem(33, GuiTheme.panel(Material.BOOK, "&e&lSHOP INFORMATION",
                List.of(GuiTheme.stat("Owner", shop.ownerName()), GuiTheme.stat("Stock", plugin.shops().stock(shop)),
                        GuiTheme.stat("Price", plugin.money(shop.price())), "",
                        "&8Owners can use the stock button",
                        "&8or /pshop stock while looking at it.")));
        if (shop.owner().equals(player.getUniqueId()) || player.hasPermission("incogshop.playershop.bypass")) {
            inv.setItem(40, GuiTheme.button(Material.CHEST, "&6&lOPEN SHOP STOCK",
                    List.of("&7Open the physical chest/barrel inventory", "&7and add matching items to restock."), "Click to open stock"));
        }
        player.openInventory(inv);
    }

    private static void fillEmpty(Inventory inv, Material material) {
        GuiTheme.fillEmpty(inv, material);
    }

    private static void fancyFrame(Inventory inv, Material accent) {
        GuiTheme.frame(inv, accent);
    }

    private static void decorate(Inventory inv, Material material) {
        GuiTheme.fill(inv, material);
    }

    /** Kept as the project-wide item builder; the styling itself lives in {@link GuiTheme}. */
    public static ItemStack named(Material material, String name, List<String> lore) {
        return GuiTheme.item(material, name, lore);
    }
}
