package com.snipeyfresh.incogshop.command;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** {@code /hex} — opens the Hex and reports plugin compatibility. */
public final class HexCommand implements CommandExecutor, TabCompleter {
    private final IncogShopPlugin plugin;

    public HexCommand(IncogShopPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
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
            case "compat", "compatibility" -> showCompatibility(sender);
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + "§eUsage: /hex [compat]");
    }

    private void showCompatibility(CommandSender sender) {
        sender.sendMessage(plugin.prefix() + "§bHex plugin compatibility:");
        var rpg = plugin.hex().integrations().incogRpg();
        var ee  = plugin.hex().integrations().excellentEnchants();
        sender.sendMessage("§8• §fIncogRPG §7- " + (rpg.available() ? "§aactive" : "§8not detected"));
        sender.sendMessage("§8• §fExcellentEnchants §7- " + (ee.available() ? "§aactive" : "§8not detected"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) out.add("compat");
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return out.stream().filter(o -> o.startsWith(prefix)).toList();
    }
}
