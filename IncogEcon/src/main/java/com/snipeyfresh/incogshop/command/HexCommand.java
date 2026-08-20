package com.snipeyfresh.incogshop.command;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.hex.EssenceType;
import com.snipeyfresh.incogshop.hex.HexResult;
import com.snipeyfresh.incogshop.hex.integration.HexIntegration;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** {@code /hex} - opens the Hex, manages essence, and reports plugin compatibility. */
public final class HexCommand implements CommandExecutor, TabCompleter {
    private final IncogShopPlugin plugin;

    public HexCommand(IncogShopPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!plugin.hex().enabled()) {
            sender.sendMessage(plugin.prefix() + "§cThe Hex is disabled on this server.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
            if (!player.hasPermission("incogshop.hex")) {
                player.sendMessage(plugin.prefix() + "§cYou do not have permission: §fincogshop.hex");
                return true;
            }
            plugin.hexGui().open(player, null);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "essence" -> showEssence(sender);
            case "buy" -> buy(sender, args);
            case "compat", "compatibility" -> showCompatibility(sender);
            case "give" -> give(sender, args, true);
            case "take" -> give(sender, args, false);
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + "§eUsage: /hex [essence|buy|compat]");
        if (sender.hasPermission("incogshop.hex.admin")) {
            sender.sendMessage("§7Admin: §f/hex give <player> <essence> <amount>§7, §f/hex take <player> <essence> <amount>");
        }
    }

    private void showEssence(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return; }
        Map<String, Long> balances = plugin.hex().essence(player.getUniqueId());
        player.sendMessage(plugin.prefix() + "§dYour essence:");
        for (EssenceType type : plugin.hex().types()) {
            player.sendMessage("§8• " + Text.color(type.display()) + " §7x§f" + balances.getOrDefault(type.id(), 0L));
        }
    }

    private void showCompatibility(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + "§bHex plugin compatibility:");
        List<HexIntegration> all = plugin.hex().integrations().all();
        if (all.isEmpty()) {
            sender.sendMessage("§8• §7No hooks are enabled in config.yml.");
            return;
        }
        for (HexIntegration integration : all) {
            boolean installed = Bukkit.getPluginManager().getPlugin(integration.pluginName()) != null;
            String state = integration.available() ? "§aactive"
                    : installed ? "§einstalled, API not recognised" : "§8not installed";
            sender.sendMessage("§8• §f" + integration.pluginName() + " §7- " + state);
        }
    }

    private void buy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return; }
        if (!player.hasPermission("incogshop.hex.buy")) {
            player.sendMessage(plugin.prefix() + "§cYou do not have permission: §fincogshop.hex.buy");
            return;
        }
        if (args.length != 3) {
            player.sendMessage(plugin.prefix() + "§eUsage: /hex buy <essence> <amount>");
            return;
        }
        long amount = parseAmount(args[2]);
        if (amount <= 0) {
            player.sendMessage(plugin.prefix() + "§cAmount must be a whole number above zero.");
            return;
        }
        HexResult result = plugin.hex().buyEssence(player, args[1], amount);
        player.sendMessage(plugin.prefix() + (result.success() ? "§a" : "§c") + result.message());
    }

    private void give(CommandSender sender, String[] args, boolean adding) {
        if (!sender.hasPermission("incogshop.hex.admin")) {
            sender.sendMessage(plugin.prefix() + "§cYou do not have permission: §fincogshop.hex.admin");
            return;
        }
        if (args.length != 4) {
            sender.sendMessage(plugin.prefix() + "§eUsage: /hex " + (adding ? "give" : "take") + " <player> <essence> <amount>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.prefix() + "§cThat player is not online.");
            return;
        }
        EssenceType type = plugin.hex().type(args[2]);
        if (type == null) {
            sender.sendMessage(plugin.prefix() + "§cUnknown essence type: §f" + args[2]);
            return;
        }
        long amount = parseAmount(args[3]);
        if (amount <= 0) {
            sender.sendMessage(plugin.prefix() + "§cAmount must be a whole number above zero.");
            return;
        }

        if (adding) {
            plugin.hex().giveEssence(target.getUniqueId(), type.id(), amount);
            sender.sendMessage(plugin.prefix() + "§aGave §f" + amount + "x " + Text.color(type.display()) + " §ato §f" + target.getName() + "§a.");
            target.sendMessage(plugin.prefix() + "§aYou received §f" + amount + "x " + Text.color(type.display()) + "§a.");
        } else if (plugin.hex().takeEssence(target.getUniqueId(), type.id(), amount)) {
            sender.sendMessage(plugin.prefix() + "§aTook §f" + amount + "x " + Text.color(type.display()) + " §afrom §f" + target.getName() + "§a.");
        } else {
            sender.sendMessage(plugin.prefix() + "§c" + target.getName() + " does not have that much essence.");
        }
    }

    private static long parseAmount(String input) {
        try {
            return Long.parseLong(input.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("essence");
            out.add("buy");
            out.add("compat");
            if (sender.hasPermission("incogshop.hex.admin")) { out.add("give"); out.add("take"); }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            for (Player player : Bukkit.getOnlinePlayers()) out.add(player.getName());
        } else if ((args.length == 2 && args[0].equalsIgnoreCase("buy"))
                || (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take")))) {
            for (EssenceType type : plugin.hex().types()) out.add(type.id());
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
