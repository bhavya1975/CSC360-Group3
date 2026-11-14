package com.rrrsvg.interpreter;

import java.awt.Color;
import java.util.Map;

/** Parses SVG colours: common names, #rgb, #rrggbb and rgb(r, g, b). */
public final class ColorParser {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("black", "#000000"), Map.entry("white", "#ffffff"), Map.entry("red", "#ff0000"),
            Map.entry("green", "#008000"), Map.entry("blue", "#0000ff"), Map.entry("yellow", "#ffff00"),
            Map.entry("orange", "#ffa500"), Map.entry("purple", "#800080"), Map.entry("pink", "#ffc0cb"),
            Map.entry("brown", "#a52a2a"), Map.entry("gray", "#808080"), Map.entry("grey", "#808080"),
            Map.entry("cyan", "#00ffff"), Map.entry("magenta", "#ff00ff"), Map.entry("lime", "#00ff00"),
            Map.entry("navy", "#000080"), Map.entry("gold", "#ffd700"), Map.entry("silver", "#c0c0c0"),
            Map.entry("maroon", "#800000"), Map.entry("teal", "#008080"), Map.entry("olive", "#808000"),
            Map.entry("crimson", "#dc143c"), Map.entry("steelblue", "#4682b4"));

    private ColorParser() {
    }

    /** Returns the colour, or null for "none" (and for colours we do not understand). */
    public static Color parse(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        v = NAMES.getOrDefault(v, v);
        try {
            if (v.startsWith("#") && v.length() == 4) {
                v = "#" + v.charAt(1) + v.charAt(1) + v.charAt(2) + v.charAt(2) + v.charAt(3) + v.charAt(3);
            }
            if (v.startsWith("#") && v.length() == 7) {
                return new Color(Integer.parseInt(v.substring(1), 16));
            }
            if (v.startsWith("rgb(") && v.endsWith(")")) {
                String[] parts = v.substring(4, v.length() - 1).split(",");
                return new Color(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()));
            }
        } catch (RuntimeException e) {
            // malformed colour: treat as none
        }
        return null;
    }
}
