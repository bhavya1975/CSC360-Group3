package com.rrrsvg.renderer;

import com.rrrsvg.interpreter.Drawing;
import com.rrrsvg.interpreter.Polyline;
import com.rrrsvg.interpreter.Shape;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 3 — Rendering.
 * Turns every shape into {@link DrawOp}s: first its fill, then its border (stroke).
 * Shapes are kept in document order, so later shapes are painted on top of earlier ones.
 */
public class Renderer {

    public List<DrawOp> render(Drawing drawing) {
        List<DrawOp> ops = new ArrayList<>();
        for (Shape shape : drawing.shapes()) {
            if (shape.fill() != null) {
                List<double[]> area = new ArrayList<>();
                for (Polyline outline : shape.outlines()) {
                    if (outline.points().length >= 6) { // a fill needs at least 3 points
                        area.add(outline.points());
                    }
                }
                if (!area.isEmpty()) {
                    ops.add(new DrawOp(shape.type() + " fill", area, shape.fill()));
                }
            }
            if (shape.stroke() != null && shape.strokeWidth() > 0) {
                ops.add(new DrawOp(shape.type() + " stroke", strokeArea(shape), shape.stroke()));
            }
        }
        return ops;
    }

    /**
     * A border is turned into an area that can be filled: one rectangle per line segment
     * and one small circle at every corner (giving rounded joins and line ends).
     */
    private List<double[]> strokeArea(Shape shape) {
        double half = shape.strokeWidth() / 2;
        List<double[]> polygons = new ArrayList<>();
        for (Polyline line : shape.outlines()) {
            double[] p = line.points();
            int n = p.length / 2;
            int segments = line.closed() ? n : n - 1;
            for (int i = 0; i < segments; i++) {
                int j = (i + 1) % n;
                double x0 = p[2 * i];
                double y0 = p[2 * i + 1];
                double x1 = p[2 * j];
                double y1 = p[2 * j + 1];
                double length = Math.hypot(x1 - x0, y1 - y0);
                if (length == 0) {
                    continue;
                }
                // offset perpendicular to the segment by half the stroke width on each side
                double nx = -(y1 - y0) / length * half;
                double ny = (x1 - x0) / length * half;
                polygons.add(counterClockwise(new double[] {
                        x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny}));
            }
            for (int i = 0; i < n; i++) {
                polygons.add(circle(p[2 * i], p[2 * i + 1], half));
            }
        }
        return polygons;
    }

    private static double[] circle(double cx, double cy, double r) {
        int count = (int) Math.max(12, Math.min(64, r * 4));
        double[] xy = new double[count * 2];
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            xy[2 * i] = cx + r * Math.cos(angle);
            xy[2 * i + 1] = cy + r * Math.sin(angle);
        }
        return xy;
    }

    /**
     * Gives every stroke polygon the same direction. Then the overlapping pieces add up
     * instead of cancelling out when the rasterizer fills them (non-zero winding rule).
     */
    private static double[] counterClockwise(double[] poly) {
        double area = 0;
        int n = poly.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += poly[2 * i] * poly[2 * j + 1] - poly[2 * j] * poly[2 * i + 1];
        }
        if (area >= 0) {
            return poly;
        }
        double[] reversed = new double[poly.length];
        for (int i = 0; i < n; i++) {
            reversed[2 * i] = poly[2 * (n - 1 - i)];
            reversed[2 * i + 1] = poly[2 * (n - 1 - i) + 1];
        }
        return reversed;
    }
}
