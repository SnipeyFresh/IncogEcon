# IncogEcon

**IncogEcon** is a full economy suite for **Paper/Purpur 26.2** with a Bazaar-style dynamic server market, player Buy/Sell Orders, an Auction House, physical player shops, secure item-and-money trading, Sell Wands, overflow storage, an XP Vault, Discord price history, and extensive administration tools.

**Current version:** `1.8.19`  
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
- Geyser/Floodgate for Bedrock connectivity; IncogEcon does **not** directly depend on their APIs

## Installation

1. Install Vault and your economy provider.
2. Place `IncogEcon-1.8.19.jar` in the server's `plugins/` folder.
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
target/IncogEcon-1.8.19.jar
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

Full syntax: [docs/COMMANDS.md](docs/COMMANDS.md)

## Permissions

Normal player permissions default to `true`; administration/bypass permissions default to `op`.

Full permission reference: [docs/PERMISSIONS.md](docs/PERMISSIONS.md)

Example with LuckPerms:

```text
/lp group admin permission set incogshop.admin true
```

> Permission nodes intentionally remain under `incogshop.*` for compatibility with existing servers after the rename to IncogEcon.

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
