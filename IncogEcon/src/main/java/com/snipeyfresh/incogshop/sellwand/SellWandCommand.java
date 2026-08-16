package com.snipeyfresh.incogshop.sellwand;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class SellWandCommand implements CommandExecutor, TabCompleter {
    private final IncogShopPlugin plugin;

    public SellWandCommand(IncogShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("incogshop.sellwand.give")) {
            sender.sendMessage(plugin.prefix() + "§cYou do not have permission to get a Sell Wand.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.prefix() + "§cOnly a player can use /sellwand.");
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(plugin.prefix() + "§eUsage: /sellwand");
            return true;
        }

        ItemStack wand = plugin.sellWands().createWand();
        var leftovers = player.getInventory().addItem(wand);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(plugin.prefix() + "§aYou received a §fSell Wand§a. Use/interact with a storage container to sell its eligible contents.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
