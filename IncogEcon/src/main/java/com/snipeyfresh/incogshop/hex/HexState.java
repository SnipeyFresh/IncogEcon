package com.snipeyfresh.incogshop.hex;

/**
 * Everything the Hex has done to a single item.
 *
 * <p>The state is stored in the item's persistent data container, so it
 * survives drops, chests, trades, and other plugins rewriting lore.</p>
 */
public record HexState(int tier, int stars, int potato, int gemSlots, int rarityBumps, String reforge) {
    public static final HexState EMPTY = new HexState(0, 0, 0, 0, 0, "");

    public HexState {
        tier = Math.max(0, tier);
        stars = Math.max(0, stars);
        potato = Math.max(0, potato);
        gemSlots = Math.max(0, gemSlots);
        rarityBumps = Math.max(0, rarityBumps);
        reforge = reforge == null ? "" : reforge;
    }

    public boolean blank() {
        return tier == 0 && stars == 0 && potato == 0 && gemSlots == 0 && rarityBumps == 0 && reforge.isEmpty();
    }

    public HexState withTier(int value) { return new HexState(value, stars, potato, gemSlots, rarityBumps, reforge); }
    public HexState withStars(int value) { return new HexState(tier, value, potato, gemSlots, rarityBumps, reforge); }
    public HexState withPotato(int value) { return new HexState(tier, stars, value, gemSlots, rarityBumps, reforge); }
    public HexState withGemSlots(int value) { return new HexState(tier, stars, potato, value, rarityBumps, reforge); }
    public HexState withRarityBumps(int value) { return new HexState(tier, stars, potato, gemSlots, value, reforge); }
    public HexState withReforge(String value) { return new HexState(tier, stars, potato, gemSlots, rarityBumps, value); }
}
