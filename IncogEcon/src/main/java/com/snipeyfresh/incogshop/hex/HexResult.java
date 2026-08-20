package com.snipeyfresh.incogshop.hex;

/** Outcome of a single Hex operation, ready to be shown to the player. */
public record HexResult(boolean success, String message) {
    public static HexResult ok(String message) { return new HexResult(true, message); }
    public static HexResult fail(String message) { return new HexResult(false, message); }
}
