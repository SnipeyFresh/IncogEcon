# Permissions

IncogEcon intentionally keeps the legacy `incogshop.*` permission namespace after the rename for compatibility.

## Player permissions

| Permission | Default | Purpose |
|---|---|---|
| `incogshop.market` | `true` | Use `/market` and `/shop` |
| `incogshop.sell` | `true` | Use `/sell` |
| `incogshop.stash` | `true` | Use `/stash` |
| `incogshop.xpvault` | `true` | Use `/xpvault` / `/xpbank` |
| `incogshop.orders` | `true` | Parent permission for normal market orders |
| `incogshop.orders.buy` | `true` | Create Buy Orders |
| `incogshop.orders.sell` | `true` | Create Sell Orders |
| `incogshop.playershop` | `true` | Use `/pshop` |
| `incogshop.playershop.create` | `true` | Create physical player shops |
| `incogshop.playershop.remove` | `true` | Remove owned player shops |
| `incogshop.auction` | `true` | Use `/ah` |
| `incogshop.auction.sell` | `true` | Create Auction House listings |
| `incogshop.auction.bid` | `true` | Bid on auctions |
| `incogshop.auction.buy` | `true` | Buy BIN listings |
| `incogshop.trade` | `true` | Send/accept/complete player trades |
| `incogshop.sellwand.use` | `true` | Use a genuine Sell Wand |

## Bypass / elevated permissions

| Permission | Default | Purpose |
|---|---|---|
| `incogshop.orders.bypasslimit` | `op` | Ignore active Market Order limit |
| `incogshop.orders.admin` | `op` | Administrative Market Order controls |
| `incogshop.playershop.bypass` | `op` | Bypass player-shop ownership/limits/protection checks |
| `incogshop.auction.bypasslimit` | `op` | Ignore Auction House listing limit |
| `incogshop.auction.admin` | `op` | Auction administration/permanent listings |
| `incogshop.sellwand.give` | `op` | Use `/sellwand` to obtain a wand |

## IncogEcon administration

| Permission | Default | Purpose |
|---|---|---|
| `incogshop.admin` | `op` | Parent permission for full IncogEcon administration |
| `incogshop.admin.discord` | `op` | Discord status/test tools |
| `incogshop.admin.gui` | `op` | Open Admin Studio/Admin Market |
| `incogshop.admin.layout` | `op` | Edit GUI layouts |
| `incogshop.admin.category` | `op` | Manage categories |
| `incogshop.admin.item` | `op` | Add/configure market items and modes |
| `incogshop.admin.price` | `op` | Change/reset base prices |
| `incogshop.admin.stock` | `op` | Change stock, infinite stock, and Auto Restock controls |
| `incogshop.admin.money` | `op` | Read/modify player balances through admin tools |
| `incogshop.admin.reload` | `op` | Reload IncogEcon |
| `incogshop.admin.save` | `op` | Force-save IncogEcon data |

`incogshop.admin` grants the individual admin nodes plus `incogshop.sellwand.give`, `incogshop.auction.admin`, and `incogshop.orders.admin` as children.

## LuckPerms examples

Grant full administration:

```text
/lp group admin permission set incogshop.admin true
```

Allow a moderator to inspect/administer Auction House listings without full IncogEcon admin:

```text
/lp group moderator permission set incogshop.auction.admin true
```

Remove trading from a group:

```text
/lp group default permission set incogshop.trade false
```
