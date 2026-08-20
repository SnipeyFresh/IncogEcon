package com.snipeyfresh.incogshop.hex;

import java.util.LinkedHashMap;
import java.util.Map;

/** Coin and essence price of one Hex upgrade step. */
public record HexCost(double coins, Map<String, Long> essence) {
    public HexCost {
        essence = essence == null ? Map.of() : Map.copyOf(essence);
    }

    public static HexCost of(double coins, String essenceType, long amount) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (essenceType != null && !essenceType.isBlank() && amount > 0) map.put(essenceType.toUpperCase(java.util.Locale.ROOT), amount);
        return new HexCost(Math.max(0, coins), map);
    }

    public boolean free() {
        if (coins > 0) return false;
        for (long amount : essence.values()) if (amount > 0) return false;
        return true;
    }
}
