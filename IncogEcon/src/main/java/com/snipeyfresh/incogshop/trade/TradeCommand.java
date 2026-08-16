package com.snipeyfresh.incogshop.trade;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TradeCommand implements CommandExecutor, TabCompleter {
    private final IncogShopPlugin plugin;

    public TradeCommand(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("incogshop.trade")) {
            player.sendMessage(plugin.prefix() + "§cYou do not have permission to trade.");
            return true;
        }
        if (!plugin.trades().enabled()) {
            player.sendMessage(plugin.prefix() + "§cTrading is currently disabled.");
            return true;
        }

        if (args.length == 0) {
            usage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("accept")) {
            var result = plugin.trades().accept(player, args.length >= 2 ? args[1] : null);
            player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
            return true;
        }
        if (sub.equals("deny") || sub.equals("decline")) {
            var result = plugin.trades().deny(player, args.length >= 2 ? args[1] : null);
            player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
            return true;
        }
        if (sub.equals("cancel")) {
            var result = plugin.trades().cancel(player);
            if (!result.success()) player.sendMessage(plugin.prefix() + "§c" + result.message());
            return true;
        }
        if (sub.equals("help")) {
            usage(player);
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.prefix() + "§cThat player must be online.");
            return true;
        }
        var result = plugin.trades().request(player, target);
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(plugin.prefix() + "§6Player Trading");
        player.sendMessage("§e/trade <player> §7- send a trade request");
        player.sendMessage("§e/trade accept [player] §7- accept a request");
        player.sendMessage("§e/trade deny [player] §7- decline a request");
        player.sendMessage("§e/trade cancel §7- cancel your active trade");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>(List.of("accept", "deny", "cancel", "help"));
            if (sender instanceof Player player) {
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(player.getUniqueId())) out.add(online.getName());
                }
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return out.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny") || args[0].equalsIgnoreCase("decline"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
        }
        return List.of();
    }
}
