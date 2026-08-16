# Features

This document describes the player and administrator systems included in IncogEcon 1.8.19.

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

## Persistence and performance

- Persistent YAML/TSV/log data files
- Staggered autosave slices rather than writing every major dataset on the same tick
- Async file writer/flush manager
- Shutdown/reload safety for escrowed player trades
- Legacy Incog-Shop data-folder migration
