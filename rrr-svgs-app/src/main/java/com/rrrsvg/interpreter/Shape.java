package com.rrrsvg.interpreter;

import java.awt.Color;
import java.util.List;

/**
 * A graphical object ready to be drawn.
 *
 * @param type        the SVG element it came from (rect, circle, path, ...)
 * @param outlines    its geometry, already transformed to pixel coordinates
 * @param fill        fill colour (with opacity), or null for no fill
 * @param stroke      border colour (with opacity), or null for no border
 * @param strokeWidth border width in pixels
 */
public record Shape(String type, List<Polyline> outlines, Color fill, Color stroke, double strokeWidth) {
}
