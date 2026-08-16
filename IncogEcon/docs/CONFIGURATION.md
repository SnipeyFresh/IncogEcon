# Configuration Reference

The canonical default configuration is `src/main/resources/config.yml`; `config.example.yml` is a convenience copy for repository viewers.

## Economy

```yaml
economy:
  mode: VAULT
  starting-balance: 500.0
  currency-symbol: "$"
  pay-minimum: 0.01
```

- `VAULT` uses the provider registered through Vault.
- `starting-balance` is relevant to the internal wallet fallback/legacy wallet data.
- Vault remains a hard plugin dependency in the current `plugin.yml`.

## Market

```yaml
market:
  infinite-stock-enabled: false
  initial-stock: 1000
  target-stock: 512
  maximum-stock-per-material: 1000000
  buy-sell-spread: 0.22
  minimum-price-multiplier: 0.35
  maximum-price-multiplier: 4.0
  demand-impact-per-item: 0.0035
  demand-decay-per-minute: 0.985
  maximum-demand-pressure: 1.25
  reject-custom-items: true
  transaction-fee-percent: 2.0
  auto-restock:
    enabled: true
    interval-hours: 72
    below-stock: 100
    minimum-stock: 500
    maximum-stock: 1000
  excluded-materials: []
```

### Auto Restock

The global schedule can be disabled in config. Individual materials can also be excluded from scheduled restock in Admin Studio -> Item Organizer.

### Excluded materials

`excluded-materials` accepts Bukkit material names. IncogEcon also has a small hard-excluded set of unsafe/admin-only materials. Ancient Debris is **not** hard excluded.

## Player shops

```yaml
player-shops:
  enabled: true
  creation-fee: 25.0
  sales-tax-percent: 3.0
  max-shops-per-player: 12
  protection-radius: 10
  allowed-containers:
    - CHEST
    - TRAPPED_CHEST
    - BARREL
```

## Sell Wands

```yaml
sell-wands:
  enabled: true
  material: BLAZE_ROD
  name: "&6&lSell Wand"
```

The actual wand is identified with PDC data, not only its material/name.

## Trading

```yaml
trading:
  enabled: true
  request-timeout-seconds: 30
  maximum-money-offer: 1000000000000.0
```

## Auction House

```yaml
auction-house:
  enabled: true
  listing-fee: 25.0
  sales-tax-percent: 3.0
  max-active-listings-per-player: 12
  default-duration-hours: 24
  maximum-duration-hours: 168
  minimum-bid-increment: 1.0
  minimum-bid-increment-percent: 5.0
```

## Market Orders

```yaml
market-orders:
  enabled: true
  max-active-orders-per-player: 20
  maximum-items-per-order: 100000
  sales-tax-percent: 1.0
```

## Discord price checks

```yaml
discord-price-check:
  enabled: false
  channel-id: ""
  command-prefix: "!"
  history-resolution-minutes: 5
  history-retention-days: 30
  default-windows: ["1h", "7h", "24h", "7d"]
```

Requires DiscordSRV. Leaving `channel-id` blank allows the integration to use DiscordSRV's primary linked text channel when available.

## Stash gameplay overflow

```yaml
stash:
  gameplay-overflow:
    enabled: true
    block-drops: true
    mob-drops: true
    notify: true
```

## Saving

```yaml
saving:
  autosave-seconds: 60
```

Autosave work is staggered across multiple slices rather than serializing all major datasets on a single tick.
