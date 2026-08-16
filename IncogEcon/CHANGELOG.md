# Changelog

All notable IncogEcon development changes preserved from the project history.

## IncogEcon 1.8.19

## Cross-platform player UI

This release makes all normal player-facing IncogEcon workflows usable without Java-only inventory gestures.

- Bazaar item pages now use dedicated **Buy 1**, **Buy Stack**, **Custom Buy**, **Sell 1**, and **Sell Stack** buttons. Shift-click is no longer required for stack trades.
- Custom instant-buy amounts now use a chat prompt instead of a virtual sign.
- Player market search now uses a chat prompt. Admin search/sign behavior is unchanged.
- Auction listing prices now use chat input instead of a virtual sign.
- Auction duration now uses four dedicated buttons: -24h, -1h, +1h, +24h. Left/right/shift click distinctions are no longer required.
- Trade money offers now use chat input instead of a virtual sign.
- Player-shop owners now get an **Open Shop Stock** GUI button.
- Added `/pshop stock` as a universal fallback for opening a shop's backing container.
- Existing sneak-right-click shop stocking remains as an optional shortcut.
- No Geyser/Floodgate API dependency was added; the same player GUI paths work for Java and Bedrock clients.
- Admin-only controls were intentionally left unchanged.

## IncogEcon 1.8.18

- Reverted the two recent Auto Restock control remaps from 1.8.16 and 1.8.17.
- Admin Studio -> Item Organizer is back to:
  - Left-click = Move/change category
  - Right-click = Cycle market mode
  - Middle-click = Toggle Auto Restock
- The actual Admin Market remains unchanged:
  - Middle-click = Reset Price
  - Q = Change Category
  - Ctrl+Q = Set Exact Stock
- No market data or Auto Restock settings are reset.

## IncogEcon 1.8.17

- Fixed another Auto Restock control conflict.
- Admin Market keeps:
  - Middle-click = Reset Price
  - Q = Change Category
  - Ctrl+Q = Set Exact Stock
- Admin Studio -> Item Organizer now uses:
  - Left-click = Move/change category
  - Right-click = Cycle market mode
  - Shift + Right-click = Toggle Auto Restock
- Updated all affected GUI lore.

## IncogEcon 1.8.16

- Fixed the Auto Restock control conflict.
- Admin Market keeps **Middle-click = Reset Price**.
- Admin Studio -> Item Organizer now uses **Q / Drop = Toggle Auto Restock**.
- Ctrl+Q in the Item Organizer also toggles Auto Restock.
- Updated GUI lore so the correct controls are shown.

## IncogEcon 1.8.15

- Renamed the plugin from **Incog-Shop** to **IncogEcon**.
- Bukkit/Paper plugin name is now `IncogEcon`.
- Maven artifact is now `incog-econ`.
- Built JAR is now `IncogEcon-1.8.15.jar`.
- Updated GUI titles, messages, Discord text, logging, and default chat prefix to IncogEcon.
- Kept the Java package (`com.snipeyfresh.incogshop`) and `incogshop.*` permission nodes unchanged for compatibility.
- Added a one-time startup migration that copies missing data from `plugins/Incog-Shop/`
  into `plugins/IncogEcon/`, leaving the old folder untouched as a backup.
- Preserves all 1.8.14 features and data formats.

## Incog-Shop 1.8.14

- Added a custom exact quantity option to Bazaar instant buying.
- Bazaar item pages now include a **Custom Buy Amount** button next to Buy Instantly.
- Clicking it opens a virtual sign that accepts positive whole numbers and shorthand such as `128` or `2k`.
- Custom instant buys are all-or-nothing: insufficient stock, inventory space, or balance cancels the purchase instead of partially filling it.
- Regular instant buy controls remain unchanged: click buys 1 and shift-click buys up to one stack.
- Improved plain market purchase delivery by splitting large purchases into legal Minecraft stack sizes.
- Preserves all 1.8.13 trading, Bazaar, player-shop, Sell Wand, XP Vault, Discord, restock, and admin features.

## Incog-Shop 1.8.13

- Added secure player-to-player trading with items and money.
- `/trade <player>` sends a trade request.
- `/trade accept [player]`, `/trade deny [player]`, and `/trade cancel` manage requests/sessions.
- Shared 54-slot trade GUI with 9 item offer slots per player.
- Clicking items in the player inventory moves the entire stack into escrow.
- Clicking your own offered item returns it.
- Exact money offers use a virtual sign and support shorthand such as `25k` and `2.5m`.
- Both players must confirm the final offer. Any item or money change resets both confirmations.
- Money balances are revalidated immediately before completion.
- Trade items are returned on cancel, inventory close, disconnect, plugin reload, or shutdown.
- Inventory overflow is routed to `/stash`.
- Trade completions/cancellations are written to the existing audit log.
- New permission: `incogshop.trade` (default true).
- New config section: `trading`.
- Preserves all 1.8.12 Bazaar, restock, Ancient Debris, Sell Wand, player-shop, Discord, XP Vault, and GUI fixes.

## Incog-Shop 1.8.12

- Replaced unreliable admin double-click stock editing with **Ctrl+Q**.
  - Ctrl+Q on a market item: set exact stock using virtual sign input.
  - Q on a market item: change category.
- Added per-item automatic restock control.
  - Admin Studio -> Organize Market Items.
  - Middle-click an item to toggle Auto Restock ON/OFF.
  - State is stored in `market.yml` as `<MATERIAL>.auto-restock`.
  - Disabled items are skipped by the scheduled restock.
- Removed Ancient Debris from the hard market blacklist.
- Ancient Debris is automatically categorized under Ores & Minerals -> Ores & Raw Materials.
- Ancient Debris default base price is 1400, matching Netherite Scrap.
- Existing market data remains compatible; missing `auto-restock` values default to ON.

## Incog-Shop 1.8.11

- Reworked the player market into a Bazaar-style item flow.
- Clicking any market item now opens that item's dedicated Bazaar page instead of instantly buying/selling it.
- Added **Buy Instantly** and **Sell Instantly** actions to the item page.
  - Click = 1 item.
  - Shift-click = up to one full stack.
- Kept **Create Buy Order** and **Create Sell Order** on the same item page.
- Shows the top player Buy Orders and Sell Orders for that material.
- Shows server stock, instant prices, best bid/ask, balance, and eligible inventory count.
- While any player Bazaar screen is open, clicking a tradable item in the player's own inventory jumps directly to that item's Bazaar page.
- The inventory shortcut also works from the item page, global order browser, and My Orders screen.
- Sell Wands are excluded from the inventory-item shortcut so the special wand cannot be mistaken for its base material.
- Admin market controls remain unchanged.
- Includes all fixes/features from 1.8.10 and earlier.

## Incog-Shop 1.8.10

- Simplified Sell Wand command to `/sellwand`.
- Running `/sellwand` gives the executing player one genuine Sell Wand.
- Removed the old `give <player> [amount]` arguments and tab completion.
- Console cannot use `/sellwand` because the item is given directly to the executing player.
- Permission remains `incogshop.sellwand.give` (default: op).

## Incog-Shop 1.8.9

- Added command-only Sell Wands.
- `/sellwand give <player> [amount]` gives a genuine PDC-tagged wand; renamed lookalikes do not work.
- Right-clicking a Bukkit storage `Container` sells every eligible stack to the server market.
- Unsellable/custom items and stock that would exceed the market maximum stay in the container.
- Sell Wands use the normal dynamic server sell price, transaction fee, market stock, demand pressure, Vault payout, and audit logging.
- Registered player shops cannot be wand-drained by other players.
- The 10-block player-shop protection radius also blocks Sell Wand use on nearby protected containers.
- Added `incogshop.sellwand.use` (default true) and `incogshop.sellwand.give` (default op).
- Added configurable Sell Wand material/name/lore in `config.yml`.

## Incog-Shop 1.8.8

- Fixed physical player-shop stocking: owners/admins can now sneak-right-click a registered shop to explicitly open its backing chest/barrel inventory.
- Added configurable 10-block player-shop protection with `player-shops.protection-radius`.
- Other players cannot break or place blocks inside a shop's protection radius.
- The shop owner may still build around their own shop, while `incogshop.playershop.bypass` bypasses all shop-region protection.
- Overlapping shop regions are respected: owning one shop does not bypass another owner's protection.
- Explosions and piston movement no longer damage/move blocks inside protected shop regions.
- Registered shop containers themselves still require `/pshop remove` before they can be broken.

## Incog-Shop 1.8.7

- Added exact stock editing directly from the admin market GUI.
- Double-click a market item in admin mode to open a virtual sign and enter the exact stock amount.
- Supports whole-number shorthand such as `2.5k` -> `2500`, plus `0` to empty the stock.
- Exact stock editing works in both built-in and custom category item browsers.
- Requires `incogshop.admin.stock`.
- Stock values now consistently respect `market.maximum-stock-per-material`.
- Preserves all 1.8.6 Discord, performance, XP Vault, stash, and GUI fixes.

## Incog-Shop 1.8.6

- Fixed Maven compilation of `DiscordPriceBridge`.
- DiscordSRV 1.30.5 shades and relocates JDA, so Incog-Shop now imports
  `github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel`.
- No separate JDA dependency is added to Incog-Shop.
- Preserves the 1.8.5 performance/crash hotfixes, DiscordSRV event-bus integration,
  XP Vault persistence, stash fixes, and GUI fixes.

## Incog-Shop 1.8.5

## Crash / performance hotfix
- Moved YAML file writes onto a dedicated single-thread Incog-Shop I/O worker.
- YAML snapshots are still created on the server thread, but blocking filesystem writes no longer happen there.
- All async writes use temp-file replacement to reduce corruption risk.
- Autosaves are staggered across the autosave interval instead of writing every Incog-Shop data file on the same tick.
- `audit.log` writes are queued off the server thread.
- Price-history capture now opens/appends the history file once per capture batch instead of once per changed material.
- Hourly price-history rewrites are queued to the I/O worker.
- `/marketadmin reload` waits for pending writes before reloading files.
- Server shutdown flushes pending I/O before Incog-Shop finishes disabling.

## DiscordSRV integration
- Replaced the raw reflected JDA event listener with DiscordSRV's supported API event subscription.
- Incoming Discord commands are handed back to the Minecraft server thread before reading market/history state.
- A blank `discord-price-check.channel-id` now falls back to DiscordSRV's main linked text channel.
- Added `/marketadmin discord status`.
- Added `/marketadmin discord test`.
- More useful Discord integration startup/status logging.

## Included previous fixes
- XP Vault now loads persisted UUID balances on startup/reload.
- Stash stacking remains enabled.
- Centered subcategories and GUI layout migrations remain included.
- Item-browser Back to Subcategories bottom-navbar fix remains included.

## Incog-Shop 1.8.4

- Fixed XP Vault persistence across server restarts.
- `xp-vault.yml` is now loaded during plugin startup.
- XP Vault data is also reloaded by Incog-Shop's reload workflow.
- XP Vault balances remain keyed by player UUID, so a player does not need to be online for their stored balance to exist.
- Deposits and withdrawals still save immediately, and normal autosaves/onDisable provide additional persistence.

## Incog-Shop 1.8.3

- Fixed **Back to Subcategories** appearing in the top-left of the item browser.
- Added a one-time migration that moves legacy `items.back` positions outside the navbar back to slot 46.
- Fixed item-control collision resolution so collisions prefer the normal bottom navigation row (45-53) instead of jumping to slot 0.
- Preserves GUI Layout Designer customization after the migration; admins can still deliberately move the Back control later.
- Updated Layout Designer wording to make the Back control's purpose clearer.

## Incog-Shop 1.8.2

- Fixed built-in subcategory buttons appearing left-aligned when upgrading from older GUI layouts.
- Added a one-time `gui-layout.yml` migration that removes only legacy `subcategories.sub:*` positions while preserving navigation/control positions and other screen layouts.
- Reworked `centeredSlots()` to create genuinely symmetrical rows around the center column.
- Even-sized rows now leave a center gap where appropriate: 2 items use columns 3/5, 4 items use 2/3/5/6, and 6 items use 1/2/3/5/6/7.
- Larger sets are balanced across rows instead of filling 7 on the first row and leaving a single button on the next row.

## Incog-Shop 1.8.1 — GUI Redesign

## Market polish
- Rebuilt the main Market category screen around centered, symmetrical category placement.
- Built-in and custom categories now share one centered layout calculation instead of competing for separate slot ranges.
- Added cleaner black/accent framing and consistent visual hierarchy.
- Player navigation is centered around Search, Orders, Balance, and Market Guide.
- Admin browse mode uses centered Admin Studio, Infinite Stock, and Layout controls.
- Fixed Back button labeling so category-wide item views correctly say **Subcategories** when that is the destination.
- Updated remaining search hints to describe the sign-based search flow.

## Admin Studio
- `/marketadmin gui` now opens a dedicated Admin Studio dashboard.
- Added large centered actions for Categories & Sections, Item Organizer, Add Held Item, GUI Layout Designer, Admin Market, and Infinite Stock.
- Category management is now paginated and visually centered.
- Left-click a category to manage its subcategories.
- Shift-click a category to assign the held item.
- Right-click a category to remove it through a confirmation GUI.
- Subcategories can also be created, assigned, and removed directly from the GUI.
- Removing a category never deletes market items; they return to automatic built-in placement.
- Removing a subcategory keeps its items in the parent custom category.

## Item Organizer
- Added a paginated visual organizer for every market material.
- Each item shows its current category/subcategory, market mode, and stock.
- Left-click an item to move it.
- Right-click an item to cycle Buy & Sell → Sell Only → Disabled.
- The destination picker supports built-in categories, custom categories, and custom subcategories.
- Reset-to-Automatic clears both built-in and custom placement overrides.
- Moving an item from a custom category into a built-in category now clears the stale custom assignment first.

## Layout Designer
- Redesigned the layout editor into a centered screen selector.
- Added a one-click **Reset This Layout** action for each screen.
- Updated centered default control positions for categories and subcategories.
- Layout changes still save automatically to `gui-layout.yml`.

## Incog-Shop 1.8.0

- Added a **Create Listing** button directly inside `/ah`.
- New GUI supports both Auction and Buy It Now listing modes.
- The exact stack in the player's main hand is previewed before listing.
- Price entry uses a virtual sign and supports shorthand such as `10k`, `2.5m`, and `1b`.
- Duration can be adjusted by 1-hour or 24-hour increments.
- Admin permanent-listing mode is respected and shown as `Never Expires`.
- The GUI shows listing fee, balance, selected price, duration, and mode before confirmation.
- Item is only removed after AuctionManager successfully creates the listing.
- Existing `/ah sell auction ...` and `/ah sell bin ...` commands remain available.

## Incog-Shop 1.7.9

- Stash items now automatically stack with identical items.
- Uses Bukkit `ItemStack#isSimilar`, so metadata/NBT/enchantments must match before stacks merge.
- Stacks respect the item's normal maximum stack size.
- Excess quantities are split into normal full stacks instead of one oversized stack.
- Existing `stash.yml` contents are compacted automatically when the plugin loads.
- Claiming and Sell All operations re-compact the remaining stash afterwards.

## Incog-Shop 1.7.8

- Rebuilt gameplay stash overflow around Paper's `PlayerAttemptPickupItemEvent`.
- Uses Paper's own `getRemaining()` value to know exactly how many items cannot fit.
- Fully-full inventories now stash the entire attempted pickup.
- Partially-full inventories receive what fits and stash only the remainder.
- Removed the old direct block-break and mob-death interception registration to prevent duplicate/conflicting item handling.
- Applies to normal ground pickups, including mined blocks/ores, mob drops, thrown items, and other item entities.
- Existing Incog-Shop delivery overflow continues to use the same persistent stash.

## Incog-Shop 1.7.7

- Fixed the stash overflow system not catching normal world item pickups.
- Mob drops, mined block drops, and other dropped Item entities now send only the portion that cannot fit in the player's inventory to `/stash`.
- Partial-stack pickups are handled correctly: the amount that fits goes into the inventory and only the remainder is stashed.
- Shop/plugin deliveries continue to use the existing `deliverOrStash` path.
- Players receive an action-bar notice when overflow is sent to the stash.

## Incog-Shop 1.7.6

- `/marketadmin gui` now includes a Market Setup GUI.
- Admins can create custom categories entirely through GUI/sign input.
- Admins can create real custom subcategories under custom categories.
- Custom categories with subcategories now show a subcategory-selection screen to players.
- Hold an item while creating a category/subcategory to use that material as its icon; CHEST is the fallback.
- Shift-click a custom category in Market Setup to assign the held item to that category.
- Click a custom subcategory in Market Setup to assign the held item directly to that subcategory.
- Added Quick Add Held Item and Add Held Item GUI actions.
- Adding a held vanilla item uses its Incog-Shop default base price and BUY_SELL mode, then opens placement controls.
- Existing `/marketadmin createcategory`, `setcategory`, and `additem` commands remain available as backup/admin automation tools.

## Incog-Shop 1.7.5

- Added persistent XP Vault.
- `/xpvault` (alias `/xpbank`) opens a GUI.
- GUI supports Deposit 25%, Deposit 50%, Deposit All, Withdraw 25%, Withdraw 50%, Withdraw All.
- Direct commands:
  - `/xpvault deposit <amount|all>`
  - `/xpvault withdraw <amount|all>`
- XP is stored as raw experience points, not just Minecraft levels.
- XP Vault balances persist in `xp-vault.yml`.
- Permission: `incogshop.xpvault` (default true).

## Incog-Shop 1.7.4

- Reworked market search into ranked smart search.
- Exact matches rank first, followed by prefixes, word/partial matches, aliases, and fuzzy typo matches.
- Spaces, underscores, hyphens, and capitalization are normalized automatically.
- Multi-word partial searches work, e.g. `red wo` and `diamond sw`.
- Minor typos are tolerated, e.g. `diamnd sword`.
- Added common aliases such as `gap`, `gapple`, `xp bottle`, `rocket`, `totem`, `cobble`, and shortened pickaxe names.
- Search still uses the sign input GUI from 1.7.2+.

## Incog-Shop 1.7.3

- `/stash` now catches overflow from player-broken block/ore drops.
- `/stash` now catches overflow from mobs directly killed by a player.
- Gameplay drops are inserted into inventory first; only items that do not fit are stashed.
- Existing shop/order/auction/sell overflow still uses `/stash`.
- Fixed Back-to-Subcategories handling by resolving GUI control slots uniquely.
- Market item slots can no longer overlap Back/Search/page-navigation control slots.

## Incog-Shop 1.7.2

- Fixed item-browser Back button: any built-in category page now returns to that category's Subcategories screen.
- `/sell` now sells eligible contents automatically when the GUI is closed; the Sell button still works immediately.
- Cancel still safely returns items; overflow is moved to `/stash`.
- Added persistent automatic market restocking every 72 hours.
- Only BUY_SELL items below 100 stock are restocked, to a random stock value from 500 through 1000.
- Restock schedule persists in `market.yml` and does not reset with server restarts.
- Market search now opens a sign editor instead of requiring chat input.
- Auction House Claims button now opens a paginated claims GUI with individual Claim and Claim All.
- Added `/stash` overflow storage with a paginated GUI.
- Stash supports individual claiming, Claim All, and Sell All Eligible.
- Auction/order/sell-GUI overflow now goes to stash instead of being lost or dropped.

## Incog-Shop 1.7.1

- Layout editor now supports three screens:
  - Categories
  - Subcategories
  - Item Browser
- Subcategory button positions are persistent and shared across built-in category submenus.
- All 36 market item display positions are customizable.
- Item browser control buttons (previous/back/search/clear/status/orders/section/page/next) are customizable.
- `/marketadmin layout [categories|subcategories|items]` opens a specific editor.
- Added admin-only Auction House permanent-listing mode.
- Admins see a toggle in `/ah`.
- While enabled, newly created Auction House listings have no expiry time.
- Permanent listings stay active until sold or manually cancelled.
- Permanent listing state is visibly marked as `Expires: Never`.

## Incog-Shop 1.7.0

## Admin GUI customization
- Added `/marketadmin layout` and an Admin Market button for a persistent GUI Layout Editor.
- Category buttons and key main-market controls can be moved to custom inventory slots.
- Layout is saved in `gui-layout.yml`.

## Custom categories
- Added persistent custom categories in `custom-categories.yml`.
- `/marketadmin createcategory <id> <icon-material> <display name...>`
- `/marketadmin deletecategory <id>`
- `/marketadmin setcategory <material> <custom-category-id|auto>`
- Custom categories appear on the main `/market` category GUI and can be positioned with the Layout Editor.
- Items assigned to a custom category are removed from their built-in category view but still appear under All Items.

## Market item modes
- Replaced the old simple enabled/disabled state with:
  - BUY_SELL
  - SELL_ONLY
  - DISABLED
- Admin GUI: press F on an item to cycle its mode.
- Sell Only items can be sold into server stock but cannot be bought directly from server stock.
- Player-to-player Auction House and Market Order systems remain independent.
- Legacy `.enabled` values in `market.yml` migrate automatically.

## Add/configure market items
- Added `/marketadmin additem <material> <base-price> [buy_sell|sell_only|disabled]`.
- Added `/marketadmin mode <material> <buy_sell|sell_only|disabled>`.
- Ancient Debris and unsafe/admin-only vanilla materials remain hard-blocked.

## /market permission handling
- Removed Bukkit's command-level permission gate for `/market`.
- Incog-Shop now checks `incogshop.market` itself and prints the exact missing node if access is denied.

## Incog-Shop 1.6.2

- Fixed `/ah` incorrectly requiring the removed `incogshop.balance` permission.
- `/ah` now correctly requires only `incogshop.auction`.
- Normal player permission defaults remain enabled.
- No data format or economy changes.

## Incog-Shop 1.6.1

- Fixed Java compile error in the global market-order browser.
- Normalized the order filter into an effectively-final variable before stream/lambda use.
- No data or behavior changes beyond the compile fix.

## Incog-Shop 1.6.0

- Added **All Market Orders** browser directly to `/market`.
- Browse All Orders, Buy Orders only, or Sell Orders only.
- Clicking an order opens that material's normal Order Book.
- Added pagination, My Orders access, and claim access from the global browser.
- Added Discord price-check startup diagnostics for incorrectly-indented YAML config.

## Incog-Shop 1.5.9

- Added My Auctions GUI in /ah.
- Players can cancel their own Buy It Now listings and auctions with no bids from the GUI.
- Auctions with bids remain locked from cancellation.
- Cancelled items go to the Auction House claim queue.
- Added AH_CANCEL audit logging.

## Incog-Shop 1.5.8

- Fixed Discord price-check compilation by removing direct JDA imports.
- Discord integration now crosses the DiscordSRV/JDA boundary through reflection.
- Incog-Shop no longer depends on a specific JDA artifact/version at compile time.
- This avoids JDA version/classloader conflicts with DiscordSRV.
- Discord price output is sent as clean formatted text rather than a JDA EmbedBuilder.

## Incog-Shop 1.5.7

- Added optional DiscordSRV integration for market price checks.
- Configure one Discord text channel by channel ID.
- `!price <item>` shows current buy/sell price, base price, percent vs starting/base price, stock, and default historical windows.
- `!price <item> <window>` supports focused windows such as 7h, 24h, or 7d.
- `!pricehelp` displays Discord usage.
- Added rolling 5-minute-resolution price history with configurable retention (30 days by default).
- History is written only when an item's price changes, keeping the file substantially smaller than full snapshots.
- Existing market/economy/auction/shop data formats are unchanged.

## Incog-Shop 1.5.6

- Fixed the missing `isInfiniteStockEnabled()` and `setInfiniteStockEnabled(...)` methods that caused 1.5.5 to fail compilation.
- Fixed the actual market purchase path so Infinite Stock bypasses stored-stock limits.
- Infinite Stock purchases do not reduce stored market stock.
- Selling still increases stored stock normally.
- When Infinite Stock is disabled, purchases require and consume actual stored stock.

## Incog-Shop 1.5.5

- Added a global Infinite Stock toggle for the entire server market.
- When enabled, players can buy globally tradable items even at 0 stored stock.
- Infinite-stock purchases do not reduce stored market stock.
- Player selling still adds items to stored stock.
- When disabled, purchases again require and consume actual stored stock.
- Added admin GUI toggle.
- Added `/marketadmin infinitestock on|off|toggle|status`.

## Incog-Shop 1.5.4

- Global market selling ignores ordinary durability damage.
- Used tools, weapons, armor, shields, elytra, fishing rods, etc. can be sold at any durability.
- `/sell` uses the same durability-tolerant eligibility rules.
- Market Sell Orders accept ordinary damaged items.
- Names, lore, enchantments, custom model/data, attributes, unbreakable state, and other meaningful custom metadata are still protected.
- Physical player shops and Auction House listings still preserve exact ItemStacks.

## Incog-Shop 1.5.3

- Auction listings now open a dedicated detail GUI.
- Auction bids can be placed directly through GUI buttons.
- Added Minimum Bid, +10%, +25%, and Custom Bid controls.
- Custom Bid accepts shorthand such as 10k, 2.5m, and 1b through a short chat prompt.
- Buy It Now listings now use a dedicated GUI purchase button.
- `/ah bid` remains available as an optional fallback.

## Incog-Shop 1.5.2

- VAULT mode now uses Vault's currently registered Economy provider.
- Removed the ExcellentEconomy-specific provider requirement.
- Removed Incog-Shop `/balance`, `/bal`, `/money`, and `/pay` command registrations to avoid collisions with EssentialsX.
- Market, Auction House, physical shops, orders, and admin money operations still use the active Vault economy provider.

## Incog-Shop 1.5.1

- Added explicit Vault economy-provider selection.
- Default preferred provider is `ExcellentEconomy`.
- Incog-Shop will no longer silently use EssentialsX Economy when ExcellentEconomy is configured.
- Startup logs list the selected provider, or all providers Vault currently exposes if the preferred one is unavailable.
- Vault is now a hard plugin dependency in VAULT builds so its API is loaded first.

## Incog-Shop 1.5.0

- Vault + ExcellentEconomy integration; Vault is the default source of truth for balances.
- Auction House with Auction and Buy It Now modes, escrowed bidding, expiry settlement, and claims.
- `/sell` bulk market-selling GUI.
- Netherite items added to the global market; Ancient Debris remains excluded.
- One-time 1,000-stock bootstrap for every global-market material.
- Shorthand prices such as 10k, 2.5m, 5m, 1.2b and 1t.
- Player Buy Orders and Sell Orders with price-time priority, escrow, automatic matching, cancellation, and item claims.
- Complete Incog-Shop/SnipeyFresh branding and `incogshop.*` permission namespace.
