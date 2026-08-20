package com.snipeyfresh.incogshop.gui;

import com.snipeyfresh.incogshop.IncogShopPlugin;
import com.snipeyfresh.incogshop.util.Text;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class StashGui {
    public record Holder(int page) implements InventoryHolder { public Inventory getInventory(){return null;} }
    private final IncogShopPlugin plugin;
    public StashGui(IncogShopPlugin plugin){this.plugin=plugin;}

    public void open(Player player,int requestedPage){
        List<ItemStack> items=plugin.stash().get(player.getUniqueId());
        int pages=Math.max(1,(items.size()+44)/45);
        int page=Math.max(0,Math.min(pages-1,requestedPage));
        Inventory inv=Bukkit.createInventory(new Holder(page),54, GuiTheme.title("&d&l", "Stash"));
        int start=page*45;
        for(int i=0;i<45 && start+i<items.size();i++){
            ItemStack icon=items.get(start+i).clone();
            ItemMeta meta=icon.getItemMeta();
            List<String> lore=meta.hasLore()&&meta.getLore()!=null?new ArrayList<>(meta.getLore()):new ArrayList<>();
            lore.add(""); lore.add(Text.color(GuiTheme.RULE));
            lore.add(Text.color("&e" + GuiTheme.CHEVRON + "Click to claim this stack"));
            meta.setLore(lore); icon.setItemMeta(meta); inv.setItem(i,icon);
        }
        GuiTheme.bottomBar(inv, Material.PURPLE_STAINED_GLASS_PANE);
        if(page>0) inv.setItem(45,GuiTheme.previousPage(page,true));
        inv.setItem(47,GuiTheme.button(Material.CHEST,"&a&lCLAIM ALL",
                List.of("&7Move as much as possible","&7into your inventory."),"Click to claim"));
        inv.setItem(49,GuiTheme.panel(Material.ENDER_CHEST,"&d&lSTASH &8• &f"+items.size()+" stacks",
                List.of(GuiTheme.stat("Page", (page+1)+"/"+pages),
                        "&7Overflow from IncogEcon deliveries","&7is kept safely here.")));
        inv.setItem(51,GuiTheme.button(Material.EMERALD_BLOCK,"&a&lSELL ALL ELIGIBLE",
                List.of("&7Sell every stash item accepted","&7by the server market.","",
                        "&8Custom/ineligible items stay stashed."),"Click to sell"));
        if(page<pages-1) inv.setItem(53,GuiTheme.nextPage(page+2,true));
        player.openInventory(inv);
    }
}
