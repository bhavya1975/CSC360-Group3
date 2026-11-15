package com.rrrsvg.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A 2D affine transformation: x' = a·x + c·y + e, y' = b·x + d·y + f.
 * Supports the SVG functions translate, scale, rotate and matrix.
 */
public record Transform(double a, double b, double c, double d, double e, double f) {

    public static final Transform IDENTITY = new Transform(1, 0, 0, 1, 0, 0);

    private static final Pattern FUNCTION = Pattern.compile("(\\w+)\\s*\\(([^)]*)\\)");
    private static final Pattern NUMBER = Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");

    /** Combines two transforms: {@code other} is applied first, then this one. */
    public Transform multiply(Transform o) {
        return new Transform(
                a * o.a + c * o.b, b * o.a + d * o.b,
                a * o.c + c * o.d, b * o.c + d * o.d,
                a * o.e + c * o.f + e, b * o.e + d * o.f + f);
    }

    public double[] apply(double x, double y) {
        return new double[] {a * x + c * y + e, b * x + d * y + f};
    }

    /** How much the transform enlarges things on average (used to scale the stroke width). */
    public double scaleFactor() {
        return Math.sqrt(Math.abs(a * d - b * c));
    }

    /** Parses an SVG transform attribute such as {@code "translate(10,20) rotate(45)"}. */
    public static Transform parse(String text) {
        Transform result = IDENTITY;
        if (text == null) {
            return result;
        }
        Matcher m = FUNCTION.matcher(text);
        while (m.find()) {
            double[] v = numbers(m.group(2));
            Transform t = switch (m.group(1)) {
                case "translate" -> new Transform(1, 0, 0, 1, v[0], v.length > 1 ? v[1] : 0);
                case "scale" -> new Transform(v[0], 0, 0, v.length > 1 ? v[1] : v[0], 0, 0);
                case "rotate" -> rotate(v);
                case "matrix" -> new Transform(v[0], v[1], v[2], v[3], v[4], v[5]);
                default -> IDENTITY;
            };
            result = result.multiply(t);
        }
        return result;
    }

    private static Transform rotate(double[] v) {
        double r = Math.toRadians(v[0]);
        Transform rotation = new Transform(Math.cos(r), Math.sin(r), -Math.sin(r), Math.cos(r), 0, 0);
        if (v.length < 3) {
            return rotation;
        }
        // rotate(angle, cx, cy) rotates around the point (cx, cy)
        Transform there = new Transform(1, 0, 0, 1, v[1], v[2]);
        Transform back = new Transform(1, 0, 0, 1, -v[1], -v[2]);
        return there.multiply(rotation).multiply(back);
    }

    /** Extracts all numbers from a string like {@code "10,20 30 40"}. */
    public static double[] numbers(String text) {
        List<Double> list = new ArrayList<>();
        Matcher m = NUMBER.matcher(text == null ? "" : text);
        while (m.find()) {
            list.add(Double.parseDouble(m.group()));
        }
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }
}
