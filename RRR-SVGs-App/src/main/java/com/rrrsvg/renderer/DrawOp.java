package com.rrrsvg.renderer;

import java.awt.Color;
import java.util.List;

/**
 * One drawing step for the rasterizer: fill the area covered by these polygons with a colour.
 *
 * @param label    what is being drawn, e.g. "circle fill"
 * @param polygons each polygon is x0, y0, x1, y1, ... in pixel coordinates
 * @param color    colour including opacity
 */
public record DrawOp(String label, List<double[]> polygons, Color color) {
}
