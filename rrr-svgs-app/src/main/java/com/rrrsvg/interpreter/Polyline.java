package com.rrrsvg.interpreter;

/**
 * An outline made of straight lines, in pixel coordinates.
 *
 * @param points x0, y0, x1, y1, ...
 * @param closed whether the last point connects back to the first
 */
public record Polyline(double[] points, boolean closed) {
}
