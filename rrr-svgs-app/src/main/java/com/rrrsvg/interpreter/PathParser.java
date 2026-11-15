package com.rrrsvg.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the {@code d} attribute of a {@code <path>}.
 * Supports M, L, H, V, C, Q and Z (upper case = absolute, lower case = relative).
 * Curves are approximated by short straight lines.
 */
final class PathParser {

    private static final Pattern TOKEN =
            Pattern.compile("[MmLlHhVvCcQqZz]|[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");
    private static final int CURVE_STEPS = 20;

    private PathParser() {
    }

    static List<Polyline> parse(String d, Transform transform) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN.matcher(d == null ? "" : d);
        while (m.find()) {
            tokens.add(m.group());
        }

        List<Polyline> result = new ArrayList<>();
        List<Double> points = new ArrayList<>(); // current subpath
        double x = 0;
        double y = 0;
        double startX = 0;
        double startY = 0;
        char cmd = ' ';
        int i = 0;
        try {
            while (i < tokens.size()) {
                String token = tokens.get(i);
                if (Character.isLetter(token.charAt(0))) {
                    cmd = token.charAt(0);
                    i++;
                    if (cmd == 'Z' || cmd == 'z') {
                        finish(result, points, true, transform);
                        x = startX;
                        y = startY;
                        continue;
                    }
                } else if (cmd == 'M') {
                    cmd = 'L'; // extra numbers after M are treated as L
                } else if (cmd == 'm') {
                    cmd = 'l';
                } else if (cmd == ' ' || cmd == 'Z' || cmd == 'z') {
                    break; // number without a command: invalid, stop here
                }

                double ox = Character.isLowerCase(cmd) ? x : 0; // relative commands add the current point
                double oy = Character.isLowerCase(cmd) ? y : 0;
                char upper = Character.toUpperCase(cmd);
                if (upper != 'M' && points.isEmpty()) {
                    add(points, x, y);
                }
                switch (upper) {
                    case 'M' -> {
                        finish(result, points, false, transform);
                        x = ox + num(tokens, i++);
                        y = oy + num(tokens, i++);
                        startX = x;
                        startY = y;
                        add(points, x, y);
                    }
                    case 'L' -> {
                        x = ox + num(tokens, i++);
                        y = oy + num(tokens, i++);
                        add(points, x, y);
                    }
                    case 'H' -> {
                        x = ox + num(tokens, i++);
                        add(points, x, y);
                    }
                    case 'V' -> {
                        y = oy + num(tokens, i++);
                        add(points, x, y);
                    }
                    case 'C' -> {
                        double x1 = ox + num(tokens, i++);
                        double y1 = oy + num(tokens, i++);
                        double x2 = ox + num(tokens, i++);
                        double y2 = oy + num(tokens, i++);
                        double ex = ox + num(tokens, i++);
                        double ey = oy + num(tokens, i++);
                        for (int s = 1; s <= CURVE_STEPS; s++) { // cubic Bézier formula
                            double t = (double) s / CURVE_STEPS;
                            double u = 1 - t;
                            add(points,
                                    u * u * u * x + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * ex,
                                    u * u * u * y + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * ey);
                        }
                        x = ex;
                        y = ey;
                    }
                    case 'Q' -> {
                        double x1 = ox + num(tokens, i++);
                        double y1 = oy + num(tokens, i++);
                        double ex = ox + num(tokens, i++);
                        double ey = oy + num(tokens, i++);
                        for (int s = 1; s <= CURVE_STEPS; s++) { // quadratic Bézier formula
                            double t = (double) s / CURVE_STEPS;
                            double u = 1 - t;
                            add(points, u * u * x + 2 * u * t * x1 + t * t * ex, u * u * y + 2 * u * t * y1 + t * t * ey);
                        }
                        x = ex;
                        y = ey;
                    }
                    default -> throw new IllegalArgumentException("unsupported path command " + cmd);
                }
            }
        } catch (RuntimeException e) {
            // invalid path data: keep everything parsed before the error
        }
        finish(result, points, false, transform);
        return result;
    }

    private static double num(List<String> tokens, int index) {
        return Double.parseDouble(tokens.get(index));
    }

    private static void add(List<Double> points, double x, double y) {
        points.add(x);
        points.add(y);
    }

    /** Ends the current subpath: transforms its points and stores it. */
    private static void finish(List<Polyline> result, List<Double> points, boolean closed, Transform t) {
        if (points.size() >= 4) {
            double[] out = new double[points.size()];
            for (int i = 0; i < points.size(); i += 2) {
                double[] p = t.apply(points.get(i), points.get(i + 1));
                out[i] = p[0];
                out[i + 1] = p[1];
            }
            result.add(new Polyline(out, closed));
        }
        points.clear();
    }
}
