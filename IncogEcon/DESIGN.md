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

## Item identity

The server market is primarily material-based and can reject custom metadata. Physical shops, auctions, and player trades preserve exact ItemStacks, allowing custom items where appropriate.

## File I/O

Autosave is staggered across multiple major datasets rather than serializing every file on the same tick. `AsyncIoManager` handles atomic/asynchronous writes and shutdown flushing.

## Cross-platform UI

Player-facing GUIs are intentionally built around dedicated buttons and chat prompts so Java and Bedrock users do not depend on different inventory-click semantics. Admin-only controls retain Java-style shortcuts.
