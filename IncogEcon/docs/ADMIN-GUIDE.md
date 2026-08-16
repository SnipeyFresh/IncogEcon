# Admin Guide

## Admin Studio

Open with:

```text
/marketadmin gui
```

Admin Studio provides access to category/section management, item organization, held-item market addition, the Admin Market, global infinite stock, and GUI layout design.

## Admin Market item controls

These controls are intentionally Java-style/admin-only and were not redesigned as part of the 1.8.19 cross-platform player UI work.

| Control | Action |
|---|---|
| Left-click | Add 64 stock |
| Right-click | Remove 64 stock |
| Ctrl+Q | Set exact stock through the admin input flow |
| Shift-left | Increase base price 10% |
| Shift-right | Decrease base price 10% |
| Middle-click | Reset base price/demand pressure |
| F / offhand | Cycle market mode |
| Q / Drop | Change category |

## Item Organizer controls

| Control | Action |
|---|---|
| Left-click | Move/change category |
| Right-click | Cycle market mode |
| Middle-click | Toggle Auto Restock for that material |

The item lore shows the current market mode, stock, location, and Auto Restock state.

## Exact stock

From the Admin Market, hover the material and use **Ctrl+Q**. Enter the exact stock value in the admin input UI. Command fallback:

```text
/marketadmin stock <material> set <amount>
```

## Auto Restock

Open Admin Studio -> Item Organizer and middle-click the desired material. The material's saved `market.yml` entry records `auto-restock: true|false`.

## Infinite stock

GUI toggle is available in Admin Studio, or use:

```text
/marketadmin infinitestock on
/marketadmin infinitestock off
/marketadmin infinitestock toggle
/marketadmin infinitestock status
```

## Market modes

- `BUY_SELL`
- `SELL_ONLY`
- `DISABLED`

Command example:

```text
/marketadmin mode diamond sell_only
```

## Discord diagnostics

```text
/marketadmin discord status
/marketadmin discord test
```

## Permissions

Grant `incogshop.admin` for the full admin permission tree, or grant individual nodes from `docs/PERMISSIONS.md`.
