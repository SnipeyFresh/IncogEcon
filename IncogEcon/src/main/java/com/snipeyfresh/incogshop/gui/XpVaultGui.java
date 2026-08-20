package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import java.util.List;

public final class XpVaultGui {
    public static final int DEPOSIT_25 = 10;
    public static final int DEPOSIT_50 = 11;
    public static final int DEPOSIT_ALL = 12;
    public static final int WITHDRAW_25 = 14;
    public static final int WITHDRAW_50 = 15;
    public static final int WITHDRAW_ALL = 16;

    public static final class Holder implements InventoryHolder {
        public Inventory getInventory(){ return null; }
    }

    private final IncogShopPlugin plugin;
    public XpVaultGui(IncogShopPlugin plugin){ this.plugin = plugin; }

    public void open(Player player) {
        Inventory inv=Bukkit.createInventory(new Holder(),27, GuiTheme.title("&a&l", "XP Vault"));
        GuiTheme.fill(inv, GuiTheme.TRIM);

        long current=plugin.xpVault().current(player);
        long stored=plugin.xpVault().stored(player.getUniqueId());
        long total=Math.max(1L, current+stored);

        inv.setItem(4,GuiTheme.panel(Material.EXPERIENCE_BOTTLE,"&a&lXP VAULT",
                List.of(GuiTheme.stat("Carried XP", current),
                        GuiTheme.stat("Stored XP", stored),
                        GuiTheme.stat("Current Level", player.getLevel()),
                        GuiTheme.bar((double)stored/(double)total, 12, "&a", "&8") + " &8stored share",
                        "",
                        "&8XP is stored as raw points, not levels.")));

        inv.setItem(DEPOSIT_25,GuiTheme.button(Material.LIME_DYE,"&aDeposit 25%",
                List.of("&7About &f"+Math.max(0,current/4)+" XP&7 leaves your bar."),"Click to deposit"));
        inv.setItem(DEPOSIT_50,GuiTheme.button(Material.LIME_CONCRETE,"&aDeposit 50%",
                List.of("&7About &f"+Math.max(0,current/2)+" XP&7 leaves your bar."),"Click to deposit"));
        inv.setItem(DEPOSIT_ALL,GuiTheme.button(Material.EMERALD_BLOCK,"&a&lDEPOSIT ALL",
                List.of("&7Stores all &f"+current+" XP&7 you carry."),"Click to deposit everything"));

        inv.setItem(WITHDRAW_25,GuiTheme.button(Material.YELLOW_DYE,"&eWithdraw 25%",
                List.of("&7About &f"+Math.max(0,stored/4)+" XP&7 returns to you."),"Click to withdraw"));
        inv.setItem(WITHDRAW_50,GuiTheme.button(Material.GOLD_INGOT,"&6Withdraw 50%",
                List.of("&7About &f"+Math.max(0,stored/2)+" XP&7 returns to you."),"Click to withdraw"));
        inv.setItem(WITHDRAW_ALL,GuiTheme.button(Material.GOLD_BLOCK,"&6&lWITHDRAW ALL",
                List.of("&7Returns all &f"+stored+" XP&7 you stored."),"Click to withdraw everything"));

        inv.setItem(22,GuiTheme.close());
        player.openInventory(inv);
    }
}
