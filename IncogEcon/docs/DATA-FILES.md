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
| `custom-categories.yml` | Custom categories/subcategories and assignments |
| `gui-layout.yml` | Persisted GUI layout positions |
| `wallets.yml` | Internal/legacy wallet balances and cached player-name lookup |
| `price-history.tsv` | Historical market price snapshots used by Discord price history |
| `audit.log` | Market/admin/trade audit entries |
| `.incogecon-migrated` | Marker created after legacy Incog-Shop folder migration |

## Backup recommendation

Before upgrades, back up the entire `plugins/IncogEcon/` folder rather than individual files. Auction, order, shop, stash, and XP data are all persistent and should be treated as economy-critical server data.

## Active trades

In-progress two-player trade sessions are intentionally not persisted across shutdown. IncogEcon cancels them and returns escrowed items safely during shutdown/reload.
