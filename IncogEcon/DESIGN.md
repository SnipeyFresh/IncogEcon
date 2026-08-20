# IncogEcon Design / Architecture

IncogEcon combines multiple economy systems while using one balance source through Vault by default.

## Core systems

- **Global Bazaar market** - material-based server stock, dynamic supply/demand pricing, instant trades, Buy/Sell Orders.
- **Market Orders** - money/item escrow with automatic matching and claims.
- **Bulk Sell** - safely converts eligible player items into market stock.
- **Physical player shops** - exact ItemStack sales backed by a real container.
- **Auction House** - exact ItemStack listings with Auctions and Buy It Now.
- **Player trades** - two-party item/money escrow with dual confirmation.
- **Stash** - persistent overflow/delivery safety layer.
- **XP Vault** - raw XP persistence.
- **The Hex** - essence banking plus per-item upgrades, with optional hooks into other gear plugins.
- **Discord price history** - periodic market-price snapshots plus DiscordSRV command bridge.

## Economy source

`WalletManager` abstracts money operations. With the default `economy.mode: VAULT`, Vault's registered economy provider is the source of truth. The plugin disables itself if VAULT mode is selected but no provider is available.

## Market persistence

`market.yml` stores each material's stock, base price, demand pressure, market mode, category overrides, and per-item Auto Restock state.

## Escrow safety

- Buy Orders escrow money.
- Sell Orders escrow items.
- Auction bids escrow the current high bid and refund the prior bidder when outbid.
- Player trades hold offered items in session escrow and revalidate money immediately before completion.
- Shutdown/reload cancels active player trades safely.

## The Hex

`HexManager` owns essence balances (`hex-essence.yml`) and every upgrade transaction; `HexItems` owns the item side.

Upgrade state lives in the item's persistent data container rather than its lore or a plugin-side item registry, which keeps upgrades attached to the physical item through drops, chests, trades, and lore rewrites by other plugins. `HexItems` renders two things and nothing else: a lore block tagged with an invisible marker, and attribute modifiers keyed under this plugin's namespace. Both are stripped and redrawn on every write, so repeated upgrades never stack duplicates and other plugins' lore and modifiers are left untouched. Because an item carrying explicit modifiers no longer receives its material's built-in ones, the first Hex write copies those defaults in first.

## Hex plugin compatibility

`hex/integration` holds one interface per capability (armor upgrades, reforges, custom-item identity) and one hook per supported plugin. Hooks are pure reflection: IncogEcon has no build dependency on any of them, and each hook probes for its plugin's classes and methods, reporting itself unavailable rather than guessing when the shape is unrecognised. Class and method names are probed in order so several generations of the same plugin can be supported.

Where another plugin owns part of the upgrade ladder, the Hex defers rather than duplicating it: EcoArmor tiers and advancements are written through EcoArmor's API, and reforges through a reforge plugin's API when one is installed. Payment is taken first and refunded automatically when the other plugin refuses the change. Items belonging to custom-item plugins the Hex cannot upgrade safely are refused by default.

## Item identity

The server market is primarily material-based and can reject custom metadata. Physical shops, auctions, and player trades preserve exact ItemStacks, allowing custom items where appropriate.

## File I/O

Autosave is staggered across multiple major datasets rather than serializing every file on the same tick. `AsyncIoManager` handles atomic/asynchronous writes and shutdown flushing.

## Cross-platform UI

Player-facing GUIs are intentionally built around dedicated buttons and chat prompts so Java and Bedrock users do not depend on different inventory-click semantics. Admin-only controls retain Java-style shortcuts.
