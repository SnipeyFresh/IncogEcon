# Features

This document describes the player and administrator systems included in IncogEcon 1.9.0.

## Bazaar-style server market

`/market` (alias `/shop`) opens the main server market.

### Navigation

- Category -> subcategory -> item flow
- Built-in and custom categories/subcategories
- Centered/persisted GUI layouts
- Fuzzy/alias market search
- Clicking/tapping a tradable item in the player's own inventory while browsing jumps to that material's Bazaar page

### Per-item Bazaar page

Each tradable material has a dedicated page showing:

- Material/category information
- Current server stock (or Unlimited in infinite-stock mode)
- Instant Buy price
- Instant Sell price
- Eligible inventory count
- Best Buy Order
- Best Sell Order
- Top player Buy/Sell Orders
- Player balance
- Links to all orders, personal orders, and claims

Player action buttons are cross-platform-safe:

- **Buy 1**
- **Buy Stack**
- **Custom Buy** (exact quantity entered through chat)
- **Sell 1**
- **Sell Stack**
- **Create Buy Order**
- **Create Sell Order**

Custom instant buys are all-or-nothing for the amount entered and validate money, server stock, and inventory capacity.

### Market modes

- `BUY_SELL` - instant buying and selling allowed
- `SELL_ONLY` - players may sell, but instant buying is disabled
- `DISABLED` - material unavailable in the market

### Dynamic pricing

Prices move around each item's base price using:

- Stored server stock compared with `target-stock`
- Demand pressure from market activity
- Configurable minimum/maximum multipliers
- Configurable Buy/Sell spread
- Demand decay over time

### Stock and restocking

- New tradable materials default to 1,000 stock
- Maximum stock is configurable
- Global infinite-stock mode is available
- Scheduled restock defaults to every 72 hours
- `BUY_SELL` materials below the configured threshold are replenished to a configurable random range
- Automatic restock can be disabled per material

## Market Buy/Sell Orders

Players can create limit-style market orders.

### Buy Orders

- Escrow the maximum required money when created
- Higher price receives priority
- Older order wins a tie
- If execution occurs below the buyer's limit price, the difference is refunded

### Sell Orders

- Escrow the actual items
- Lower price receives priority
- Older order wins a tie

### Matching and claims

- Compatible Buy/Sell Orders match automatically
- The older/resting order determines execution price
- Items are delivered immediately when possible
- Overflow/claimable items remain safe until claimed
- Players can browse, cancel, and claim through GUI or commands

Default limits:

- 20 active orders per player
- 100,000 items per order
- 1% seller sales tax on fills

## Auction House

`/ah` opens the Auction House.

Features:

- Timed Auction listings
- Buy It Now listings
- Exact ItemStack storage (including custom metadata)
- GUI listing creation
- Command listing creation
- Bid escrow
- Automatic refund of previous high bidder
- Configurable minimum bid increment and percentage
- Listing fee and sales tax
- Expiration/settlement
- Claim queue
- My Auctions / cancellation
- Admin permanent-listing mode

Player price and bid entry supports shorthand such as `10k`, `2.5m`, `1b`, and `1t`.

## Physical player shops

Players can register supported physical containers as shops.

Default supported containers:

- Chest
- Trapped Chest
- Barrel

Features:

- Real container inventory is the shop stock
- Exact ItemStack template matching
- Supports custom items
- Configurable creation fee/tax/shop limit
- Configurable protection radius (default 10 blocks)
- Protection against unauthorized breaking/placing, explosions, piston movement, and hopper-style manipulation
- Owner/admin stock access
- **Open Shop Stock** GUI button and `/pshop stock` cross-platform fallback
- Separate Buy 1 / Buy Stack player buttons

## Secure player trading

`/trade <player>` starts a secure two-player trade flow.

Features:

- Request/accept/deny/cancel commands
- Request timeout
- Shared trade GUI
- Item escrow
- Money offers
- Both players must confirm the final state
- Any offer change clears both confirmations
- Money balance is revalidated before completion
- Closing/disconnecting cancels safely
- Reload/shutdown cancels safely
- Overflow is routed to the stash
- Completion/cancellation is audit logged

## Bulk Sell GUI

`/sell` opens a GUI for selling multiple eligible market items.

- Eligible items use normal server-market sell pricing
- Server stock is increased by sold items
- Market stock caps and fees are respected
- Close-to-process behavior remains safe
- Return overflow is protected by the stash

## Sell Wands

`/sellwand` is an admin/op command that gives a genuine PDC-tagged Sell Wand.

- Command-only item; renamed lookalikes do not work
- Unlimited uses
- Use/interact with a storage container to sell eligible contents
- Unsellable/custom items remain in the container
- Normal market price, stock, and fee rules apply
- Sell Wand itself is never treated as ordinary sellable Blaze Rod stock
- Player-shop ownership/protection is respected

## Stash

`/stash` provides persistent overflow storage.

Used for safe overflow from systems including:

- Market-order claims
- Auction claims/returns
- Trade delivery
- Sell GUI returns
- Gameplay pickups when configured

Similar ItemStacks are compacted when possible.

## XP Vault

`/xpvault` (alias `/xpbank`) stores **raw experience points**.

- GUI deposits/withdrawals
- Command deposit/withdrawal
- `all` support
- Persistent UUID-backed balances

## The Hex

`/hex` opens The Hex, an item-upgrade hub modelled on Hypixel Skyblock's Hex and adapted to IncogEcon's economy.

### Working on an item

- Click a weapon, tool, or armor piece in your own inventory to move it into the Hex slot.
- Click an upgrade to pay for it and apply it immediately.
- Click **Take Item Back**, or simply close the menu, to get the item back. Anything that does not fit goes to `/stash`.

The Hex works on one non-stacked item at a time. Every control is a plain left click, so Java and Bedrock players use it identically.

### Essence

Essence is the Hex currency. Each type is defined in `config.yml` and is earned two ways:

- **Mob drops** from a configurable drop table (Wither Skeletons drop Wither Essence, the Ender Dragon drops Dragon Essence, and so on).
- **Coin purchase** in the Hex essence shop or with `/hex buy <essence> <amount>`, when buying is enabled.

Balances are shown in the Essence Pouch and with `/hex essence`, and are stored in `hex-essence.yml`.

### Upgrades

| Upgrade | Effect |
|---|---|
| **Hex Tier** | The core upgrade ladder; adds damage, armor, and toughness per level |
| **Master Stars** | Adds damage and defence, shown as stars on the item |
| **Hot Potato Points** | Small flat stat gains per point |
| **Gemstone Slots** | Each unlocked slot adds a permanent stat bonus |
| **Recombobulator** | Raises the item one rarity step |
| **Enchantment Power** | Pushes every enchantment on the item past its normal maximum |
| **Reforge** | Applies a reforge, rerollable at any time |

Every upgrade costs coins plus a configurable amount of one essence type. Costs, caps, and stat values are all configurable, and any upgrade can be turned off.

Upgrade state is written to the item's persistent data container, not to its lore, so upgrades survive drops, chests, trades, and other plugins rewriting the item's lore. The Hex only ever rewrites its own lore block and its own attribute modifiers, and it re-applies the item's built-in attributes when needed so an upgrade never strips a sword's base damage.

## Plugin compatibility

The Hex is built to share items with other gear plugins rather than compete with them. Each hook is optional, detected at runtime, and inactive when its plugin is not installed. IncogEcon does not depend on any of them, and `/hex compat` reports what was detected.

### EcoArmor

EcoArmor is a first-class target:

- The Hex reads the armor set, current tier, and advancement state of an EcoArmor piece.
- **Armor Tier Upgrade** pushes the piece one EcoArmor tier up, paid for with essence and coins instead of EcoArmor's own upgrade crystals.
- **Armor Advancement** applies EcoArmor's advanced upgrade to the piece.
- Both are performed through EcoArmor's own API, so the data written is exactly what EcoArmor expects to read back.
- If the tier or advancement is refused by EcoArmor, the payment is returned automatically.
- EcoArmor pieces can still take normal Hex upgrades on top, so a player can max both ladders on the same item.

### Reforges

When Auxilor's **Reforges** plugin is installed, the Hex reforge station lists that plugin's reforges for the item and applies them through its API, so stat calculation and lore stay that plugin's responsibility. IncogEcon's own reforge table is used only when no reforge plugin is present.

### Custom item plugins

**MMOItems**, **EcoItems**, **ItemsAdder**, **Oraxen**, and **Nexo** items are recognised and shown in the Hex compatibility panel. With `protect-unknown-custom-items` enabled (the default), the Hex refuses its own upgrades on those items rather than risking damage to another plugin's item data. Items owned by a supported armor-upgrade plugin such as EcoArmor are exempt and stay fully upgradable.

### Enchantment plugins

Custom enchantments that register as normal Bukkit enchantments, such as those from EcoEnchants, are picked up by the Enchantment Power upgrade with no extra configuration.

## Discord price checks

Optional DiscordSRV integration provides price lookup/history commands through Discord.

Examples:

```text
!price diamond
!price diamond 7h
!price diamond 24h
!pricehelp
```

- Uses DiscordSRV's existing bot connection
- No second bot token
- Configurable command prefix/channel
- Price history stored in TSV form
- Default windows: 1h, 7h, 24h, 7d
- Admin status/test diagnostics

## Admin Studio

`/marketadmin gui` provides GUI-based administration for:

- Categories/subcategories
- Custom category creation/removal
- Item organization
- Held-item addition
- Market mode control
- Base price control
- Stock control
- Exact stock input
- Per-item Auto Restock toggle
- Infinite stock
- GUI layout design

Admin-only Java-style gestures are intentionally not redesigned for Bedrock because the cross-platform requirement applies to player-facing features.

## Menu design

Every IncogEcon menu is built from one shared theme (`GuiTheme`), so titles, borders, buttons, and stat lines look and read the same across the plugin:

- Consistent titles in the form `IncogEcon » Section · Detail`.
- Buttons carry a header rule, their stats, and one highlighted call to action, so it is always clear what a click does.
- Controls that cannot be used right now are shown greyed out with the reason instead of disappearing.
- Progress bars for market supply, order fill, XP stored, auction duration, and Hex upgrade levels.
- Standard navigation items for back, close, previous page, next page, and page counters.
- Stat lines share one `Label: value` format across the Bazaar, Auction House, order book, stash, trade, and admin screens.

Slot positions, click behaviour, and the Layout Designer are unchanged, so existing `gui-layout.yml` files and admin muscle memory still apply.

## Persistence and performance

- Persistent YAML/TSV/log data files
- Staggered autosave slices rather than writing every major dataset on the same tick
- Async file writer/flush manager
- Shutdown/reload safety for escrowed player trades
- Legacy Incog-Shop data-folder migration
