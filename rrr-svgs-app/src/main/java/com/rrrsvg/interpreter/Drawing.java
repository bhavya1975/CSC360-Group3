package com.rrrsvg.interpreter;

import java.util.List;

/** The result of interpreting an SVG: the image size and the shapes in drawing order. */
public record Drawing(int width, int height, List<Shape> shapes) {
}
