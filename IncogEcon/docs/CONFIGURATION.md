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

## The Hex

```yaml
hex:
  enabled: true
  essence:
    allow-buying: true
    types:
      WITHER:
        display: "&8Wither Essence"
        icon: WITHER_SKELETON_SKULL
        buy-price: 750.0
    drops:
      enabled: true
      notify: true
      mobs:
        WITHER_SKELETON: { type: WITHER, chance: 0.35, minimum: 1, maximum: 3 }
```

### Essence

Each entry under `hex.essence.types` defines one currency. The key is the id used by `/hex buy`, `/hex give`, and every upgrade cost. `buy-price` is the coin price of a single unit; set it to `0` to make an essence earnable only.

`hex.essence.drops.mobs` accepts any Bukkit entity type. Each entry names the essence `type`, a `chance` between `0.0` and `1.0`, and a `minimum`/`maximum` amount. Only kills credited to a player with `incogshop.hex` drop essence.

### Upgrade costs

Every upgrade under `hex.upgrades` uses the same cost shape:

```
coins   = coin-base    + coin-per-level    * (level - 1)
essence = essence-base + essence-per-level * (level - 1)
```

| Upgrade | Config key | Default cap |
|---|---|---|
| Hex Tier | `hex.upgrades.tier` | 10 |
| Master Stars | `hex.upgrades.stars` | 5 |
| Hot Potato Points | `hex.upgrades.hot-potato` | 10 |
| Gemstone Slots | `hex.upgrades.gemstone-slots` | 3 |
| Recombobulator | `hex.upgrades.recombobulator` | 1 |
| Enchantment Power | `hex.upgrades.enchantments` | `max-level-above-vanilla`, default 2 |

The `*-per-level`, `*-per-star`, `*-per-point`, and `*-per-slot` stat values are applied as item attribute modifiers owned by IncogEcon. Enchantment Power raises every enchantment on the item by one level at a time, up to `max-level-above-vanilla` over its normal maximum, and its cost is multiplied by the number of enchantments actually upgraded.

Setting any upgrade's `enabled` to `false` removes it from the menu without touching items that already carry it.

### Reforges

```yaml
hex:
  reforge:
    enabled: true
    essence-type: SPIDER
    essence-cost: 3
    coin-cost: 1500.0
    native:
      SHARP:
        display: "&cSharp"
        attack-damage: 3.0
```

`hex.reforge.native` is only used when no supported reforge plugin is installed. Each entry accepts `attack-damage`, `armor`, `armor-toughness`, `health`, and `speed`, and values may be negative.

### Plugin compatibility

```yaml
hex:
  integrations:
    ecoarmor:
      enabled: true
      tier-upgrade: { essence-type: DRAGON, essence-cost: 20, coin-cost: 25000.0 }
      advancement: { essence-type: WITHER, essence-cost: 30, coin-cost: 40000.0 }
    reforges: { enabled: true }
    mmoitems: { enabled: true }
    ecoitems: { enabled: true }
    itemsadder: { enabled: true }
    oraxen: { enabled: true }
    nexo: { enabled: true }
    protect-unknown-custom-items: true
```

None of these plugins is a dependency. Each hook is detected at runtime and stays inactive when its plugin is missing. `protect-unknown-custom-items` makes the Hex refuse native upgrades on items issued by a custom-item plugin it cannot upgrade safely; items owned by a supported armor-upgrade plugin such as EcoArmor stay fully upgradable. Run `/hex compat` in game to see which hooks are active.

## Saving

```yaml
saving:
  autosave-seconds: 60
```

Autosave work is staggered across multiple slices rather than serializing all major datasets on a single tick.
