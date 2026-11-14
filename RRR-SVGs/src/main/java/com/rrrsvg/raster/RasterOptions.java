package com.rrrsvg.raster;

import com.rrrsvg.exception.SVGRasterizeException;
import com.rrrsvg.model.SVGRoot;

import java.awt.Color;

/**
 * Encapsulates rasterization settings for converting vector SVG into raster PNG.
 * <p>
 * Allows users to specify target image width, height, resolution (DPI),
 * and background color.
 * </p>
 */
public class RasterOptions {

    public static final double DEFAULT_DPI = 96.0;
    public static final double SCREEN_DPI = 72.0;
    public static final double MEDIUM_DPI = 150.0;
    public static final double PRINT_DPI = 300.0;

    private int width;
    private int height;
    private double dpi;
    private Color backgroundColor;
    private boolean antialiasing;

    /**
     * Constructs RasterOptions with default settings (800x600, 96 DPI, transparent background).
     */
    public RasterOptions() {
        this(800, 600, DEFAULT_DPI, null);
    }

    /**
     * Constructs RasterOptions with specified width and height at default 96 DPI.
     *
     * @param width  Target raster width in pixels.
     * @param height Target raster height in pixels.
     */
    public RasterOptions(int width, int height) {
        this(width, height, DEFAULT_DPI, null);
    }

    /**
     * Constructs RasterOptions with full parameters.
     *
     * @param width           Target raster width in pixels.
     * @param height          Target raster height in pixels.
     * @param dpi             Output resolution in dots per inch (DPI).
     * @param backgroundColor Background color, or null for transparent.
     */
    public RasterOptions(int width, int height, double dpi, Color backgroundColor) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
        this.backgroundColor = backgroundColor;
        this.antialiasing = true;
    }

    /**
     * Creates a RasterOptions instance with default dimensions extracted from an {@link SVGRoot}.
     *
     * @param root Parsed SVGRoot element from Module 1.
     * @return Configured RasterOptions.
     */
    public static RasterOptions fromSvg(SVGRoot root) {
        int w = 800;
        int h = 600;
        if (root != null) {
            double svgW = root.getWidth();
            double svgH = root.getHeight();
            if (svgW > 0 && svgH > 0) {
                w = (int) Math.round(svgW);
                h = (int) Math.round(svgH);
            }
        }
        return new RasterOptions(w, h, DEFAULT_DPI, null);
    }

    /**
     * Validates that dimensions and resolution are positive numbers.
     *
     * @throws SVGRasterizeException If any parameter is invalid.
     */
    public void validate() throws SVGRasterizeException {
        if (width <= 0) {
            throw new SVGRasterizeException("Invalid raster width: " + width + ". Must be greater than 0.");
        }
        if (height <= 0) {
            throw new SVGRasterizeException("Invalid raster height: " + height + ". Must be greater than 0.");
        }
        if (dpi <= 0) {
            throw new SVGRasterizeException("Invalid resolution (DPI): " + dpi + ". Must be greater than 0.");
        }
    }

    public int getWidth() {
        return width;
    }

    public RasterOptions setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public RasterOptions setHeight(int height) {
        this.height = height;
        return this;
    }

    public double getDpi() {
        return dpi;
    }

    public RasterOptions setDpi(double dpi) {
        this.dpi = dpi;
        return this;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public RasterOptions setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public boolean isAntialiasing() {
        return antialiasing;
    }

    public RasterOptions setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
        return this;
    }

    /**
     * Calculates the millimeter-per-pixel ratio for Batik's KEY_PIXEL_UNIT_TO_MILLIMETER hint.
     * (1 inch = 25.4 millimeters)
     *
     * @return Millimeters per pixel.
     */
    public float getMillimetersPerPixel() {
        return (float) (25.4 / (dpi > 0 ? dpi : DEFAULT_DPI));
    }

    @Override
    public String toString() {
        return "RasterOptions[width=" + width + ", height=" + height + ", dpi=" + dpi +
                ", bg=" + (backgroundColor != null ? backgroundColor : "transparent") + "]";
    }
}
