# IncogEcon

**IncogEcon** is a full economy suite for **Paper/Purpur 26.2** with a Bazaar-style dynamic server market, player Buy/Sell Orders, an Auction House, physical player shops, secure item-and-money trading, Sell Wands, overflow storage, an XP Vault, Discord price history, and extensive administration tools.

**Current version:** `1.9.0`  
**Developer:** SnipeyFresh  
**Java:** 25+  
**Build system:** Maven

> IncogEcon 1.8.19 is designed so **all normal player-facing workflows work without Java-only inventory gestures**. Bedrock players connecting through a correctly configured Geyser/Floodgate setup can use the same player features as Java players. Admin-only GUI shortcuts remain Java-style.

## Highlights

- **Bazaar-style market** with categories, subcategories, search, live stock, dynamic supply/demand prices, Buy/Sell modes, custom instant-buy amounts, and per-item automatic-restock control.
- **Player Buy and Sell Orders** with escrow, matching, claims, cancellation, and price/time priority.
- **Auction House** with timed auctions, Buy It Now listings, bidding escrow, listing fees, tax, claims, and admin permanent listings.
- **Physical player shops** backed by real chest/barrel inventory, exact ItemStack matching, protection, and cross-platform stock access.
- **Secure player trading** for items and money with two-sided confirmation and automatic reset when an offer changes.
- **Bulk Sell GUI** and **command-only Sell Wands** that feed eligible items into the server market.
- **Persistent stash** for inventory overflow and safe item delivery.
- **XP Vault** storing raw experience points.
- **DiscordSRV price checks** and historical market price tracking.
- **Admin Studio** for categories, item organization, stock, pricing, market modes, infinite stock, auto restock, and GUI layout design.
- **The Hex** — a coin-based item upgrade station where players reforge weapons, tools, and armor (via IncogRPG) and apply custom enchants (IncogRPG + ExcellentEnchants). All costs are coin-only; both integrations are soft-depend and loaded via reflection at runtime with zero compile-time coupling.
- **Incog-Shop -> IncogEcon migration** that preserves older data on first startup after the rename.

## Cross-platform player support

Player features intentionally avoid controls that do not translate reliably between Java and Bedrock inventory UIs:

- No player feature requires middle-click, Q/drop, Ctrl+Q, offhand/F, or shift-click.
- Buy 1 / Buy Stack / Custom Buy and Sell 1 / Sell Stack are separate Bazaar buttons.
- Player text/number entry uses normal chat prompts rather than virtual signs.
- Auction duration changes use dedicated GUI buttons.
- Trade money offers use chat prompts.
- Player-shop owners have an **Open Shop Stock** button and `/pshop stock` fallback.

See [CROSS-PLATFORM.md](CROSS-PLATFORM.md) for the full compatibility design.

## Requirements

### Required

- Paper or Purpur `26.2`
- Java `25` or newer
- Vault
- A Vault-compatible economy provider when `economy.mode: VAULT`

### Optional

- DiscordSRV `1.30.5` for Discord price commands/history integration
- **IncogRPG** for Hex reforges and IncogRPG custom enchants (loaded via reflection — no hard dependency)
- **ExcellentEnchants** for Hex enchant support (loaded via reflection — no hard dependency)
- Geyser/Floodgate for Bedrock connectivity; IncogEcon does **not** directly depend on their APIs

## Installation

1. Install Vault and your economy provider.
2. Place `IncogEcon-1.9.0.jar` in the server's `plugins/` folder.
3. Start the server once.
4. Configure `plugins/IncogEcon/config.yml` as needed.
5. Restart or use `/marketadmin reload` after safe configuration changes.

For an upgrade from the old **Incog-Shop** name, see [docs/INSTALLATION.md](docs/INSTALLATION.md#migrating-from-incog-shop).

## Build from source

```bash
mvn clean package
```

Output:

```text
target/IncogEcon-1.9.0.jar
```

See [MAVEN-BUILD.md](MAVEN-BUILD.md) for build prerequisites and Arch Linux notes.

## Main player commands

| Command | Purpose |
|---|---|
| `/market` / `/shop` | Open the Bazaar |
| `/sell` | Open the bulk Sell GUI |
| `/stash` | Open overflow storage |
| `/xpvault` / `/xpbank` | Open the XP Vault |
| `/ah` / `/auction` / `/auctionhouse` | Open the Auction House |
| `/pshop ...` | Create/manage physical player shops |
| `/trade ...` | Securely trade items and money |
| `/market ...order...` | Create/manage player market orders |
| `/hex` | Open The Hex — reforge and enchant items with coins |
| `/hex compat` | Show IncogRPG + ExcellentEnchants integration status |

Full syntax: [docs/COMMANDS.md](docs/COMMANDS.md)

# Permissions

IncogEcon intentionally keeps the legacy `incogshop.*` permission namespace after the rename for compatibility.

## Player permissions

| Permission | Default | Purpose |
|---|---|---|
| `incogshop.market` | `true` | Use `/market` and `/shop` |
| `incogshop.sell` | `true` | Use `/sell` |
| `incogshop.stash` | `true` | Use `/stash` |
| `incogshop.xpvault` | `true` | Use `/xpvault` / `/xpbank` |
| `incogshop.orders` | `true` | Parent permission for normal market orders |
| `incogshop.orders.buy` | `true` | Create Buy Orders |
| `incogshop.orders.sell` | `true` | Create Sell Orders |
| `incogshop.playershop` | `true` | Use `/pshop` |
| `incogshop.playershop.create` | `true` | Create physical player shops |
| `incogshop.playershop.remove` | `true` | Remove owned player shops |
| `incogshop.auction` | `true` | Use `/ah` |
| `incogshop.auction.sell` | `true` | Create Auction House listings |
| `incogshop.auction.bid` | `true` | Bid on auctions |
| `incogshop.auction.buy` | `true` | Buy BIN listings |
| `incogshop.trade` | `true` | Send/accept/complete player trades |
| `incogshop.sellwand.use` | `true` | Use a genuine Sell Wand |
| `incogshop.hex` | `true` | Open `/hex` and use The Hex |

## Bypass / elevated permissions

| Permission | Default | Purpose |
|---|---|---|
| `incogshop.orders.bypasslimit` | `op` | Ignore active Market Order limit |
| `incogshop.orders.admin` | `op` | Administrative Market Order controls |
| `incogshop.playershop.bypass` | `op` | Bypass player-shop ownership/limits/protection checks |
| `incogshop.auction.bypasslimit` | `op` | Ignore Auction House listing limit |
| `incogshop.auction.admin` | `op` | Auction administration/permanent listings |
| `incogshop.sellwand.give` | `op` | Use `/sellwand` to obtain a wand |
| `incogshop.hex.admin` | `op` | Use `/hex compat` and future Hex admin features |

## IncogEcon administration

| Permission | Default | Purpose |
|---|---|---|
| `incogshop.admin` | `op` | Parent permission for full IncogEcon administration |
| `incogshop.admin.discord` | `op` | Discord status/test tools |
| `incogshop.admin.gui` | `op` | Open Admin Studio/Admin Market |
| `incogshop.admin.layout` | `op` | Edit GUI layouts |
| `incogshop.admin.category` | `op` | Manage categories |
| `incogshop.admin.item` | `op` | Add/configure market items and modes |
| `incogshop.admin.price` | `op` | Change/reset base prices |
| `incogshop.admin.stock` | `op` | Change stock, infinite stock, and Auto Restock controls |
| `incogshop.admin.money` | `op` | Read/modify player balances through admin tools |
| `incogshop.admin.reload` | `op` | Reload IncogEcon |
| `incogshop.admin.save` | `op` | Force-save IncogEcon data |

`incogshop.admin` grants the individual admin nodes plus `incogshop.sellwand.give`, `incogshop.auction.admin`, and `incogshop.orders.admin` as children.

## LuckPerms examples

Grant full administration:

```text
/lp group admin permission set incogshop.admin true
```

Allow a moderator to inspect/administer Auction House listings without full IncogEcon admin:

```text
/lp group moderator permission set incogshop.auction.admin true
```

Remove trading from a group:

```text
/lp group default permission set incogshop.trade false
```
## Documentation

- [Features](docs/FEATURES.md)
- [Commands](docs/COMMANDS.md)
- [Permissions](docs/PERMISSIONS.md)
- [Installation & upgrades](docs/INSTALLATION.md)
- [Configuration](docs/CONFIGURATION.md)
- [Admin guide](docs/ADMIN-GUIDE.md)
- [Persistent data files](docs/DATA-FILES.md)
- [Cross-platform support](CROSS-PLATFORM.md)
- [Architecture/design](DESIGN.md)
- [Changelog](CHANGELOG.md)
- [GitHub publishing checklist](docs/GITHUB-SETUP.md)
- [Licensing notes](docs/LICENSING.md)

## Default market behavior

| Setting | Default |
|---|---:|
| Initial stock | 1,000 |
| Target stock | 512 |
| Maximum stock per material | 1,000,000 |
| Buy/Sell spread | 22% |
| Transaction fee | 2% |
| Auto-restock interval | 72 hours |
| Restock threshold | Below 100 |
| Restock target | Random 500-1,000 |

Ancient Debris is tradable by default and is categorized under **Ores & Minerals -> Ores & Raw Materials**.

## Repository notes

The Java package remains:

```text
com.snipeyfresh.incogshop
```

This is intentional for compatibility. The public plugin name/artifact is **IncogEcon**.

## License

A software license has **not** been chosen automatically for this repository package. Before publishing the repository publicly, choose the license that matches how you want others to use and redistribute the project. See [docs/LICENSING.md](docs/LICENSING.md).
