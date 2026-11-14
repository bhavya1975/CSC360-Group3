package com.rrrsvg.interpreter;

import com.rrrsvg.reader.SvgNode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 2 — Interpreting.
 * Walks the SVG tree and converts each element into a {@link Shape}: geometry in pixel
 * coordinates (after all transforms) plus fill colour, border colour/width and opacity.
 */
public class SvgInterpreter {

    private static final List<String> STYLE_PROPERTIES =
            List.of("fill", "stroke", "stroke-width", "opacity", "fill-opacity", "stroke-opacity");
    private static final int CIRCLE_POINTS = 72;

    public Drawing interpret(SvgNode svg) {
        double width = svg.number("width", 300);
        double height = svg.number("height", 150);
        Transform transform = Transform.IDENTITY;

        // viewBox="minX minY w h" maps that area onto the image
        double[] viewBox = Transform.numbers(svg.attr("viewBox"));
        if (viewBox.length == 4 && viewBox[2] > 0 && viewBox[3] > 0) {
            if (svg.attr("width") == null) {
                width = viewBox[2];
                height = viewBox[3];
            }
            double sx = width / viewBox[2];
            double sy = height / viewBox[3];
            transform = new Transform(sx, 0, 0, sy, -viewBox[0] * sx, -viewBox[1] * sy);
        }

        Map<String, String> defaultStyle = new HashMap<>(Map.of("fill", "black", "stroke", "none", "stroke-width", "1"));
        List<Shape> shapes = new ArrayList<>();
        visit(svg, transform, defaultStyle, 1.0, shapes);
        return new Drawing((int) Math.ceil(width), (int) Math.ceil(height), shapes);
    }

    private void visit(SvgNode node, Transform parentTransform, Map<String, String> parentStyle,
                       double parentOpacity, List<Shape> shapes) {
        // Styles are inherited from the parent and can be overridden by attributes or style="..."
        Map<String, String> style = new HashMap<>(parentStyle);
        for (String property : STYLE_PROPERTIES) {
            if (node.attr(property) != null) {
                style.put(property, node.attr(property));
            }
        }
        if (node.attr("style") != null) {
            for (String declaration : node.attr("style").split(";")) {
                String[] keyValue = declaration.split(":", 2);
                if (keyValue.length == 2 && STYLE_PROPERTIES.contains(keyValue[0].trim())) {
                    style.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        double opacity = parentOpacity * number(style.remove("opacity"), 1);
        Transform transform = parentTransform.multiply(parseTransform(node.attr("transform")));

        switch (node.name()) {
            case "svg", "g" -> {
                for (SvgNode child : node.children()) {
                    visit(child, transform, style, opacity, shapes);
                }
            }
            case "rect", "circle", "ellipse", "line", "polyline", "polygon", "path" -> {
                List<Polyline> outlines = geometry(node, transform);
                if (!outlines.isEmpty()) {
                    Color fill = withAlpha(ColorParser.parse(style.get("fill")),
                            opacity * number(style.get("fill-opacity"), 1));
                    Color stroke = withAlpha(ColorParser.parse(style.get("stroke")),
                            opacity * number(style.get("stroke-opacity"), 1));
                    double strokeWidth = number(style.get("stroke-width"), 1) * transform.scaleFactor();
                    shapes.add(new Shape(node.name(), outlines, fill, stroke, strokeWidth));
                }
            }
            default -> {
                // other elements (text, images, gradients, ...) are not supported and are skipped
            }
        }
    }

    /** Converts one basic shape into outlines in pixel coordinates. */
    private List<Polyline> geometry(SvgNode n, Transform t) {
        List<Polyline> result = new ArrayList<>();
        switch (n.name()) {
            case "rect" -> {
                double x = n.number("x", 0);
                double y = n.number("y", 0);
                double w = n.number("width", 0);
                double h = n.number("height", 0);
                if (w > 0 && h > 0) {
                    result.add(polyline(t, true, x, y, x + w, y, x + w, y + h, x, y + h));
                }
            }
            case "circle" -> {
                double r = n.number("r", 0);
                if (r > 0) {
                    result.add(ellipse(t, n.number("cx", 0), n.number("cy", 0), r, r));
                }
            }
            case "ellipse" -> {
                double rx = n.number("rx", 0);
                double ry = n.number("ry", 0);
                if (rx > 0 && ry > 0) {
                    result.add(ellipse(t, n.number("cx", 0), n.number("cy", 0), rx, ry));
                }
            }
            case "line" -> result.add(polyline(t, false,
                    n.number("x1", 0), n.number("y1", 0), n.number("x2", 0), n.number("y2", 0)));
            case "polyline", "polygon" -> {
                double[] p = Transform.numbers(n.attr("points"));
                if (p.length >= 4) {
                    double[] even = java.util.Arrays.copyOf(p, p.length / 2 * 2);
                    result.add(polyline(t, n.name().equals("polygon"), even));
                }
            }
            case "path" -> result.addAll(PathParser.parse(n.attr("d"), t));
            default -> {
            }
        }
        return result;
    }

    private static Polyline polyline(Transform t, boolean closed, double... xy) {
        double[] out = new double[xy.length];
        for (int i = 0; i < xy.length; i += 2) {
            double[] p = t.apply(xy[i], xy[i + 1]);
            out[i] = p[0];
            out[i + 1] = p[1];
        }
        return new Polyline(out, closed);
    }

    private static Polyline ellipse(Transform t, double cx, double cy, double rx, double ry) {
        double[] xy = new double[CIRCLE_POINTS * 2];
        for (int i = 0; i < CIRCLE_POINTS; i++) {
            double angle = 2 * Math.PI * i / CIRCLE_POINTS;
            xy[2 * i] = cx + rx * Math.cos(angle);
            xy[2 * i + 1] = cy + ry * Math.sin(angle);
        }
        return polyline(t, true, xy);
    }

    private static Transform parseTransform(String text) {
        try {
            return Transform.parse(text);
        } catch (RuntimeException e) {
            return Transform.IDENTITY; // invalid transform: ignore it
        }
    }

    private static double number(String text, double defaultValue) {
        if (text == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(text.trim().replace("px", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Color withAlpha(Color color, double alpha) {
        if (color == null) {
            return null;
        }
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }
}
