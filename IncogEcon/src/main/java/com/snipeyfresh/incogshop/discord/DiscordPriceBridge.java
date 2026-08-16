package com.snipeyfresh.incogshop.discord;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.history.PriceHistoryManager.PricePoint;
import com.snipeyfresh.incogshop.market.MarketEntry;
import com.snipeyfresh.incogshop.util.Text;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import org.bukkit.Material;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

public final class DiscordPriceBridge {
    private static final Pattern WINDOW = Pattern.compile("^(\\d+)([mhd])$", Pattern.CASE_INSENSITIVE);

    private final IncogShopPlugin plugin;
    private boolean subscribed;

    public DiscordPriceBridge(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!enabled()) {
            plugin.getLogger().info("Discord price checks are disabled (discord-price-check.enabled=false or missing).");
            return;
        }

        if (plugin.getServer().getPluginManager().getPlugin("DiscordSRV") == null) {
            plugin.getLogger().warning("Discord price checks are enabled, but DiscordSRV is not installed.");
            return;
        }

        try {
            // Subscribe to DiscordSRV's supported event bus instead of attaching
            // a raw JDA listener. This also works when JDA becomes ready later.
            DiscordSRV.api.subscribe(this);
            subscribed = true;

            if (DiscordSRV.isReady) {
                logReadyState();
            } else {
                plugin.getLogger().info("Discord price bridge subscribed; waiting for DiscordSRV to become ready.");
            }
        } catch (Throwable ex) {
            subscribed = false;
            plugin.getLogger().warning("Could not subscribe to DiscordSRV API: " + rootMessage(ex));
        }
    }

    public void stop() {
        if (!subscribed) return;
        try {
            DiscordSRV.api.unsubscribe(this);
        } catch (Throwable ignored) {}
        subscribed = false;
    }

    @Subscribe
    public void onDiscordReady(DiscordReadyEvent event) {
        logReadyState();
    }

    @Subscribe
    public void onDiscordMessage(DiscordGuildMessageReceivedEvent event) {
        try {
            if (event.getAuthor() == null || event.getAuthor().isBot()) return;
            TextChannel channel = event.getChannel();
            if (channel == null) return;

            String wanted = effectiveChannelId();
            if (wanted == null || wanted.isBlank() || !channel.getId().equals(wanted)) return;

            String raw = event.getMessage() == null ? "" : event.getMessage().getContentRaw().trim();
            if (raw.isEmpty()) return;

            // DiscordSRV calls API subscribers from its Discord/JDA threads.
            // All IncogEcon market/history access stays on the Minecraft server
            // thread to avoid concurrent access to mutable market data.
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    handleMessage(channel, raw);
                } catch (Throwable ex) {
                    plugin.getLogger().warning("Discord price command failed: " + rootMessage(ex));
                }
            });
        } catch (Throwable ex) {
            plugin.getLogger().warning("Discord price event failed: " + rootMessage(ex));
        }
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public String status() {
        if (!enabled()) return "disabled in config";
        if (plugin.getServer().getPluginManager().getPlugin("DiscordSRV") == null) return "DiscordSRV not installed";
        if (!subscribed) return "not subscribed";
        if (!DiscordSRV.isReady) return "subscribed; DiscordSRV not ready yet";
        String id = effectiveChannelId();
        if (id == null || id.isBlank()) return "DiscordSRV ready, but no usable text channel was found";
        TextChannel channel = DiscordSRV.getPlugin().getJda() == null ? null
                : DiscordSRV.getPlugin().getJda().getTextChannelById(id);
        if (channel == null) return "configured channel " + id + " is not visible to the DiscordSRV bot";
        return "ready in #" + channel.getName() + " (" + id + ")";
    }

    public boolean sendTest() {
        if (!enabled() || !subscribed || !DiscordSRV.isReady || DiscordSRV.getPlugin().getJda() == null) return false;
        String id = effectiveChannelId();
        if (id == null || id.isBlank()) return false;
        TextChannel channel = DiscordSRV.getPlugin().getJda().getTextChannelById(id);
        if (channel == null) return false;
        send(channel, "✅ **IncogEcon Discord integration is working.** Try `" + commandPrefix() + "price diamond`.");
        return true;
    }

    private void logReadyState() {
        try {
            String state = status();
            if (state.startsWith("ready in #")) {
                plugin.getLogger().info("Discord price checks " + state + ".");
            } else {
                plugin.getLogger().warning("Discord price bridge: " + state + ".");
            }
        } catch (Throwable ex) {
            plugin.getLogger().warning("Discord price bridge readiness check failed: " + rootMessage(ex));
        }
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("discord-price-check.enabled", false);
    }

    private String commandPrefix() {
        String prefix = plugin.getConfig().getString("discord-price-check.command-prefix", "!");
        return prefix == null || prefix.isBlank() ? "!" : prefix;
    }

    /**
     * A blank IncogEcon channel-id now falls back to DiscordSRV's main linked
     * text channel. Explicit IDs still take priority.
     */
    private String effectiveChannelId() {
        String configured = plugin.getConfig().getString("discord-price-check.channel-id", "");
        configured = configured == null ? "" : configured.trim();
        if (!configured.isBlank()) return configured;

        try {
            TextChannel main = DiscordSRV.getPlugin().getMainTextChannel();
            return main == null ? "" : main.getId();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private void handleMessage(TextChannel channel, String raw) {
        String command = commandPrefix() + "price";
        String help = commandPrefix() + "pricehelp";

        if (raw.equalsIgnoreCase(help)) {
            send(channel, "Use `" + command + " <item>` or `" + command
                    + " <item> <window>`. Examples: `" + command + " diamond`, `"
                    + command + " netherite_ingot 24h`.");
            return;
        }

        if (!raw.regionMatches(true, 0, command, 0, command.length())) return;
        if (raw.length() > command.length() && !Character.isWhitespace(raw.charAt(command.length()))) return;

        String query = raw.substring(command.length()).trim();
        if (query.isEmpty()) {
            send(channel, "Usage: `" + command + " <item> [1h|7h|24h|7d]`");
            return;
        }

        String requestedWindow = null;
        String[] words = query.split("\\s+");
        if (words.length > 1 && WINDOW.matcher(words[words.length - 1]).matches()) {
            requestedWindow = words[words.length - 1].toLowerCase(Locale.ROOT);
            query = String.join(" ", Arrays.copyOf(words, words.length - 1));
        }

        Material material = resolveMaterial(query);
        if (material == null || !plugin.market().isTradable(material)) {
            List<Material> suggestions = suggestions(query);
            String suffix = suggestions.isEmpty() ? "" : " Did you mean: "
                    + suggestions.stream().map(m -> "`" + Text.prettyEnum(m.name()) + "`").toList() + "?";
            send(channel, "I couldn't find that market item." + suffix);
            return;
        }

        sendPrice(channel, material, requestedWindow);
    }

    private void sendPrice(TextChannel channel, Material material, String requestedWindow) {
        MarketEntry entry = plugin.market().entry(material);
        if (entry == null) return;

        double current = plugin.market().buyUnitPrice(material);
        double currentSell = plugin.market().sellUnitPrice(material);
        double configuredBase = entry.basePrice();
        double startingPrice = com.snipeyfresh.incogshop.market.MarketManager.defaultBasePrice(material);
        double versusStart = percentChange(startingPrice, current);

        StringBuilder out = new StringBuilder();
        out.append("**").append(Text.prettyEnum(material.name())).append(" — Market Price**\n")
                .append("Current Buy: **").append(plugin.money(current)).append("**\n")
                .append("Current Sell: **").append(plugin.money(currentSell)).append("**\n")
                .append("Original Starting Price: **").append(plugin.money(startingPrice)).append("**\n")
                .append("Vs. Starting Price: **").append(signedPercent(versusStart)).append("**\n");

        if (Math.abs(configuredBase - startingPrice) >= 0.005) {
            out.append("Admin Base Price: **").append(plugin.money(configuredBase)).append("**\n");
        }

        out.append("Stored Stock: **").append(entry.stock());
        if (plugin.market().isInfiniteStockEnabled()) out.append(" (∞ buying)");
        out.append("**\n")
                .append("Stock Mode: **")
                .append(plugin.market().isInfiniteStockEnabled() ? "Infinite" : "Limited")
                .append("**\n");

        List<String> windows = new ArrayList<>();
        if (requestedWindow != null) {
            windows.add(requestedWindow);
        } else {
            windows.addAll(plugin.getConfig().getStringList("discord-price-check.default-windows"));
            if (windows.isEmpty()) windows.addAll(List.of("1h", "7h", "24h", "7d"));
        }

        long now = System.currentTimeMillis();
        for (String window : windows) {
            long duration = plugin.history().parseWindowMillis(window);
            if (duration <= 0) continue;

            PricePoint old = plugin.history().atOrBefore(material, now - duration);
            out.append("\n").append(window.toUpperCase(Locale.ROOT)).append(" Change: ");
            if (old == null) {
                out.append("`Not enough history yet`");
            } else {
                double change = percentChange(old.buyPrice(), current);
                out.append("**").append(plugin.money(old.buyPrice()))
                        .append(" → ").append(plugin.money(current))
                        .append(" (").append(signedPercent(change)).append(")**");
            }
        }

        PricePoint oldest = plugin.history().oldest(material);
        if (oldest != null) {
            out.append("\n\n_IncogEcon history since ").append(Instant.ofEpochMilli(oldest.timestamp())).append("_");
        } else {
            out.append("\n\n_IncogEcon price history begins when IncogEcon starts recording it._");
        }

        send(channel, out.toString());
    }

    private void send(TextChannel channel, String text) {
        try {
            channel.sendMessage(text).queue(
                    ignored -> {},
                    error -> plugin.getLogger().warning("Discord message send failed: " + rootMessage(error))
            );
        } catch (Throwable ex) {
            plugin.getLogger().warning("Discord message send failed: " + rootMessage(ex));
        }
    }

    private Material resolveMaterial(String query) {
        String normalized = query.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        Material exact = Material.matchMaterial(normalized);
        if (exact != null) return exact;

        String compact = normalized.replace("_", "");
        for (Material material : plugin.market().tradableMaterials()) {
            if (material.name().replace("_", "").equalsIgnoreCase(compact)) return material;
            if (Text.prettyEnum(material.name()).replace(" ", "").equalsIgnoreCase(query.replace(" ", ""))) return material;
        }
        return null;
    }

    private List<Material> suggestions(String query) {
        String q = query.toLowerCase(Locale.ROOT).replace("_", " ").trim();
        return plugin.market().tradableMaterials().stream()
                .filter(m -> Text.prettyEnum(m.name()).toLowerCase(Locale.ROOT).contains(q)
                        || m.name().toLowerCase(Locale.ROOT).contains(q.replace(" ", "_")))
                .limit(5)
                .toList();
    }

    private double percentChange(double oldValue, double newValue) {
        if (oldValue <= 0) return 0;
        return ((newValue - oldValue) / oldValue) * 100.0;
    }

    private String signedPercent(double value) {
        return String.format(Locale.US, "%+.2f%%", value);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getClass().getSimpleName() + ": " + String.valueOf(current.getMessage());
    }
}
