package com.snipeyfresh.incogshop.history;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import org.bukkit.Material;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

public final class PriceHistoryManager {
    public record PricePoint(long timestamp, double buyPrice, double sellPrice, long stock) {}

    private final IncogShopPlugin plugin;
    private final File file;
    private final Map<Material, NavigableMap<Long, PricePoint>> history = new EnumMap<>(Material.class);
    private final Map<Material, PricePoint> lastWritten = new EnumMap<>(Material.class);

    public PriceHistoryManager(IncogShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "price-history.tsv");
    }

    public synchronized void load() {
        history.clear();
        lastWritten.clear();
        if (!file.exists()) return;

        long cutoff = System.currentTimeMillis() - retentionMillis();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split("\t");
                if (parts.length < 5) continue;
                try {
                    long time = Long.parseLong(parts[0]);
                    if (time < cutoff) continue;
                    Material material = Material.matchMaterial(parts[1]);
                    if (material == null) continue;
                    PricePoint point = new PricePoint(
                            time,
                            Double.parseDouble(parts[2]),
                            Double.parseDouble(parts[3]),
                            Long.parseLong(parts[4])
                    );
                    history.computeIfAbsent(material, k -> new TreeMap<>()).put(time, point);
                    PricePoint old = lastWritten.get(material);
                    if (old == null || point.timestamp() > old.timestamp()) lastWritten.put(material, point);
                } catch (RuntimeException ignored) {}
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not load price-history.tsv: " + ex.getMessage());
        }
    }

    /**
     * Captures one in-memory snapshot of all changed prices and performs a
     * single append for the whole batch. Older versions opened/closed the
     * history file once per changed material.
     */
    public synchronized void captureChangedPrices() {
        long now = System.currentTimeMillis();
        StringBuilder batch = new StringBuilder();

        for (Material material : plugin.market().tradableMaterials()) {
            PricePoint point = capturePointIfChanged(material, now);
            if (point != null) appendLine(batch, material, point);
        }

        if (!batch.isEmpty()) {
            plugin.io().appendText(file.toPath(), batch.toString(), "price-history.tsv append");
        }
    }

    public synchronized void captureIfChanged(Material material) {
        PricePoint point = capturePointIfChanged(material, System.currentTimeMillis());
        if (point == null) return;
        StringBuilder line = new StringBuilder();
        appendLine(line, material, point);
        plugin.io().appendText(file.toPath(), line.toString(), "price-history.tsv append");
    }

    private PricePoint capturePointIfChanged(Material material, long now) {
        if (!plugin.market().isTradable(material)) return null;

        double buy = plugin.market().buyUnitPrice(material);
        double sell = plugin.market().sellUnitPrice(material);
        long stock = plugin.market().entry(material) == null ? 0 : plugin.market().entry(material).stock();
        PricePoint previous = lastWritten.get(material);

        if (previous != null
                && Math.abs(previous.buyPrice() - buy) < 0.005
                && Math.abs(previous.sellPrice() - sell) < 0.005) {
            return null;
        }

        PricePoint point = new PricePoint(now, buy, sell, stock);
        history.computeIfAbsent(material, k -> new TreeMap<>()).put(now, point);
        lastWritten.put(material, point);
        return point;
    }

    private static void appendLine(StringBuilder out, Material material, PricePoint point) {
        out.append(point.timestamp()).append('\t')
                .append(material.name()).append('\t')
                .append(point.buyPrice()).append('\t')
                .append(point.sellPrice()).append('\t')
                .append(point.stock()).append('\n');
    }

    public synchronized PricePoint atOrBefore(Material material, long timestamp) {
        NavigableMap<Long, PricePoint> map = history.get(material);
        if (map == null || map.isEmpty()) return null;
        Map.Entry<Long, PricePoint> entry = map.floorEntry(timestamp);
        return entry == null ? null : entry.getValue();
    }

    public synchronized PricePoint oldest(Material material) {
        NavigableMap<Long, PricePoint> map = history.get(material);
        return map == null || map.isEmpty() ? null : map.firstEntry().getValue();
    }

    public synchronized boolean hasHistory(Material material) {
        NavigableMap<Long, PricePoint> map = history.get(material);
        return map != null && !map.isEmpty();
    }

    /**
     * Prunes in memory on the server thread, but moves the potentially large
     * rewrite to the dedicated IncogEcon I/O worker.
     */
    public synchronized void pruneAndRewrite() {
        long cutoff = System.currentTimeMillis() - retentionMillis();
        for (Iterator<Map.Entry<Material, NavigableMap<Long, PricePoint>>> it = history.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Material, NavigableMap<Long, PricePoint>> e = it.next();
            e.getValue().headMap(cutoff, false).clear();
            if (e.getValue().isEmpty()) {
                lastWritten.remove(e.getKey());
                it.remove();
            }
        }

        StringBuilder snapshot = new StringBuilder("# timestamp<TAB>material<TAB>buy<TAB>sell<TAB>stock\n");
        for (Map.Entry<Material, NavigableMap<Long, PricePoint>> e : history.entrySet()) {
            for (PricePoint point : e.getValue().values()) appendLine(snapshot, e.getKey(), point);
        }
        plugin.io().writeTextAtomic(file.toPath(), snapshot.toString(), "price-history.tsv rewrite");
    }

    public long parseWindowMillis(String input) {
        if (input == null) return -1;
        String value = input.trim().toLowerCase(Locale.ROOT);
        try {
            if (value.endsWith("m")) return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 1))).toMillis();
            if (value.endsWith("h")) return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1))).toMillis();
            if (value.endsWith("d")) return Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1))).toMillis();
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    private long retentionMillis() {
        long days = Math.max(1, plugin.getConfig().getLong("discord-price-check.history-retention-days", 30));
        return Duration.ofDays(days).toMillis();
    }
}
