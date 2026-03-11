package io.ajcm.db2.ibmi.mcp.util;

import java.util.Locale;

/**
 * Utility for reading environment variables with small conveniences.
 */
public final class Env {
    private Env() {}

    /**
     * Returns the trimmed value of the environment variable.
     * 
     * @param name environment variable name
     * @return trimmed value
     */
    public static String get(String name) {
        String v = System.getenv(name);
        return v.trim();
    }

    /**
     * Reads an integer environment variable with clamping.
     * 
     * @param name environment variable name
     * @param defaultValue default value if not found
     * @param minAllowed minimum allowed value
     * @param maxAllowed maximum allowed value
     * @return clamped integer value
     */
    public static int getInt(String name, int defaultValue, int minAllowed, int maxAllowed) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            int v = Integer.parseInt(raw.trim());
            return Math.max(minAllowed, Math.min(maxAllowed, v));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Reads an integer environment variable without clamping.
     * 
     * @param name environment variable name
     * @param defaultValue default value if not found
     * @return integer value
     */
    public static int getInt(String name, int defaultValue) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Reads a boolean environment variable.
     * 
     * @param name environment variable name
     * @param defaultValue default value if not found
     * @return boolean value
     */
    public static boolean getBool(String name, boolean defaultValue) {
        String raw = System.getenv(name);
        if (raw == null) return defaultValue;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "true", "1" -> true;
            case "false", "0" -> false;
            default -> defaultValue;
        };
    }
}
