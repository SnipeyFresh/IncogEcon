# Persistent Data Files

IncogEcon stores runtime data under `plugins/IncogEcon/`.

| File | Purpose |
|---|---|
| `config.yml` | Main configuration |
| `market.yml` | Stock, base prices, demand pressure, market modes, categorization, per-item Auto Restock state |
| `market-orders.yml` | Player Buy/Sell Orders, escrow/claims |
| `auctions.yml` | Auction House listings and claim data |
| `shops.yml` | Registered physical player shops |
| `stash.yml` | Player overflow stash |
| `xp-vault.yml` | Stored raw XP balances |
| `hex-essence.yml` | Player Hex essence balances |
| `custom-categories.yml` | Custom categories/subcategories and assignments |
| `gui-layout.yml` | Persisted GUI layout positions |
| `wallets.yml` | Internal/legacy wallet balances and cached player-name lookup |
| `price-history.tsv` | Historical market price snapshots used by Discord price history |
| `audit.log` | Market/admin/trade audit entries |
| `.incogecon-migrated` | Marker created after legacy Incog-Shop folder migration |

## Backup recommendation

Before upgrades, back up the entire `plugins/IncogEcon/` folder rather than individual files. Auction, order, shop, stash, XP, and Hex essence data are all persistent and should be treated as economy-critical server data.

## Hex item data

Hex upgrades are stored on the item itself, in its persistent data container, not in a plugin file. Upgraded items keep their tier, stars, hot potato points, gemstone slots, rarity bump, and reforge through drops, chests, trades, and other plugins rewriting their lore. Only `hex-essence.yml` lives on disk.

## Active trades

In-progress two-player trade sessions are intentionally not persisted across shutdown. IncogEcon cancels them and returns escrowed items safely during shutdown/reload.
