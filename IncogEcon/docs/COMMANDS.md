# Commands

All commands below are present in IncogEcon 1.9.0.

## Market / Bazaar

| Command | Description |
|---|---|
| `/market` | Open Bazaar categories |
| `/shop` | Alias for `/market` |
| `/market search <query>` | Open the market filtered by a query |
| `/market orders [item]` | Open an item's Bazaar/order page; without item opens personal orders |
| `/market buyorder <material> <amount> <price-each>` | Create a Buy Order |
| `/market sellorder <material> <amount> <price-each>` | Create a Sell Order |
| `/market myorders` | Open your orders |
| `/market cancelorder <id>` | Cancel one of your orders |
| `/market claim` | Claim filled-order items |

Order price examples: `10k`, `2.5m`, `1b`.

## The Hex

| Command | Description |
|---|---|
| `/hex` | Open The Hex item-upgrade menu |
| `/hex essence` | Show your essence balances in chat |
| `/hex buy <essence> <amount>` | Buy essence with coins |
| `/hex compat` | List detected upgrade/custom-item plugins and their hook state |
| `/hex give <player> <essence> <amount>` | Admin: grant essence |
| `/hex take <player> <essence> <amount>` | Admin: remove essence |

Essence ids come from `hex.essence.types` in `config.yml` (`WITHER`, `UNDEAD`, `DRAGON`, `SPIDER`, `ICE`, `DIAMOND`, `GOLD`, `CRIMSON` by default).

## Bulk selling

| Command | Description |
|---|---|
| `/sell` | Open the bulk Sell GUI |

## Stash

| Command | Description |
|---|---|
| `/stash` | Open persistent overflow storage |

## XP Vault

| Command | Description |
|---|---|
| `/xpvault` | Open the XP Vault GUI |
| `/xpbank` | Alias for `/xpvault` |
| `/xpvault deposit <amount|all>` | Deposit raw XP |
| `/xpvault withdraw <amount|all>` | Withdraw raw XP |

## Auction House

| Command | Description |
|---|---|
| `/ah` | Browse the Auction House |
| `/auction` | Alias for `/ah` |
| `/auctionhouse` | Alias for `/ah` |
| `/ah browse` | Browse listings |
| `/ah sell auction <start-price> [hours]` | List held stack as a timed auction |
| `/ah sell bin <price> [hours]` | List held stack as Buy It Now |
| `/ah bid <id> <amount>` | Place a bid |
| `/ah buy <id>` | Buy a BIN listing |
| `/ah my` | Show your active listing IDs |
| `/ah cancel <id>` | Cancel an eligible listing |
| `/ah claim` | Claim won/returned items |

## Physical player shops

Most `/pshop` commands act on the registered shop container the player is looking at (within 6 blocks).

| Command | Description |
|---|---|
| `/pshop create <price-each>` | Register the targeted supported container using the exact main-hand item as template |
| `/pshop remove` | Remove the targeted shop registration |
| `/pshop price <price-each>` | Change targeted shop price |
| `/pshop item` | Replace the targeted shop's exact item template with main-hand item |
| `/pshop stock` | Open the targeted shop's backing inventory (owner/bypass) |
| `/pshop info` | Show targeted shop details |
| `/pshop list` | List your registered shops |

## Player trading

| Command | Description |
|---|---|
| `/trade <player>` | Send a trade request |
| `/trade accept [player]` | Accept a pending request |
| `/trade deny [player]` | Decline a request |
| `/trade cancel` | Cancel the active trade |
| `/trade help` | Show trade help |

`/trade decline [player]` is also accepted as a deny synonym.

## Sell Wand

| Command | Description |
|---|---|
| `/sellwand` | Give yourself one genuine Sell Wand (admin/op permission by default) |

## Administration

| Command | Permission | Description |
|---|---|---|
| `/marketadmin gui` | `incogshop.admin.gui` | Open Admin Studio |
| `/marketadmin layout [categories|subcategories|items]` | `incogshop.admin.layout` | Open layout editor |
| `/marketadmin createcategory <id> <icon-material> <display name...>` | `incogshop.admin.category` | Create custom category |
| `/marketadmin deletecategory <id>` | `incogshop.admin.category` | Delete custom category |
| `/marketadmin setcategory <material> <custom-category-id|auto>` | `incogshop.admin.category` | Assign/reset custom category |
| `/marketadmin additem <material> <base-price> [buy_sell|sell_only|disabled]` | `incogshop.admin.item` | Add/enable a market material |
| `/marketadmin mode <material> <buy_sell|sell_only|disabled>` | `incogshop.admin.item` | Change market mode |
| `/marketadmin price <material> <amount|reset>` | `incogshop.admin.price` | Change/reset base price |
| `/marketadmin stock <material> <add|set> <amount>` | `incogshop.admin.stock` | Change market stock |
| `/marketadmin infinitestock <on|off|toggle|status>` | `incogshop.admin.stock` | Global infinite-stock control |
| `/marketadmin money <get|give|take|set> <player> [amount]` | `incogshop.admin.money` | Economy administration |
| `/marketadmin discord <status|test>` | `incogshop.admin.discord` | Discord integration diagnostics |
| `/marketadmin save` | `incogshop.admin.save` | Save all persistent plugin data |
| `/marketadmin reload` | `incogshop.admin.reload` | Reload config/persistent data |

`/marketadmin infinite ...` is accepted as an alias for the `infinitestock` subcommand.

## Price shorthand

Where monetary input uses IncogEcon's money parser, suffix shorthand is supported, including examples such as:

```text
10k
2.5m
1.2b
1t
```
