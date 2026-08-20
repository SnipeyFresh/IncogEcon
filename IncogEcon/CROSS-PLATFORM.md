# Cross-Platform Player Support

IncogEcon 1.9.0 is designed so **normal player-facing features do not require Java-only inventory gestures**.

The Hex follows the same rule: items are placed by clicking them in your inventory, every upgrade is a single left click, and amounts are typed in chat rather than on a sign.

This matters on servers where Bedrock clients connect to the Java server through Geyser/Floodgate. IncogEcon itself does not depend on the Geyser/Floodgate API; it uses a player UI design that translates cleanly.

## Scope

The cross-platform guarantee in IncogEcon applies to **non-admin/player features**.

Admin Studio/Admin Market still uses Java-style shortcuts such as middle-click, Q, Ctrl+Q, F/offhand, and differentiated left/right/shift clicks.

## Player UI rules

- One visible player GUI button = one primary action.
- No normal player feature requires distinguishing left-click from right-click inside an inventory.
- No normal player feature requires middle-click, Q/drop, Ctrl+Q, offhand/F, or shift-click.
- Text/numeric input used by player features is entered through normal chat prompts.

## Bazaar

Separate buttons exist for:

- Buy 1
- Buy Stack
- Custom Buy
- Sell 1
- Sell Stack
- Create Buy Order
- Create Sell Order
- Browse orders / claims

Custom buy amount and order amount/price entry use chat.

## Auction House

- Listing price uses chat input.
- Duration uses dedicated `-24h`, `-1h`, `+1h`, `+24h` buttons.
- Custom bid input uses chat.
- Other actions use dedicated buttons.

## Player Trading

- Tap/click inventory items to add them to the offer.
- Money offer uses chat input.
- Confirm and Cancel are dedicated buttons.
- Changing either offer resets confirmation state.

## Player Shops

- Buy 1 and Buy Stack are separate buttons.
- Owners have an **Open Shop Stock** button.
- `/pshop stock` is a universal fallback while looking at an owned shop.
- Sneak-interact stocking may remain as an optional shortcut but is not required.

## Other systems

Sell GUI, Stash, XP Vault, order management/claims, and Sell Wands use ordinary inventory buttons, commands, chat input, or normal block interaction.

## Geyser/Floodgate

IncogEcon does not install or configure Geyser/Floodgate. Your proxy/server setup must already allow the Bedrock platforms you want to support to connect.

Custom Java resource-pack models/items may require matching Bedrock/Geyser mappings to display the same visuals to Bedrock clients. IncogEcon still stores/trades the server-side ItemStack; visual conversion is outside the economy plugin.
