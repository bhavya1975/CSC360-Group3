package com.rrrsvg.analysis;

import java.awt.Color;

/**
 * Encapsulates data for a single discrete pixel in a raster image,
 * including coordinates, individual channel values, hex notation, and luminance.
 */
public class PixelData {

    private final int x;
    private final int y;
    private final int red;
    private final int green;
    private final int blue;
    private final int alpha;
    private final int argb;
    private final double luminance;

    public PixelData(int x, int y, int argb) {
        this.x = x;
        this.y = y;
        this.argb = argb;
        this.alpha = (argb >> 24) & 0xFF;
        this.red = (argb >> 16) & 0xFF;
        this.green = (argb >> 8) & 0xFF;
        this.blue = argb & 0xFF;
        // Standard Rec. 601 luma formula
        this.luminance = 0.299 * red + 0.587 * green + 0.114 * blue;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getArgb() {
        return argb;
    }

    public double getLuminance() {
        return luminance;
    }

    public Color toColor() {
        return new Color(red, green, blue, alpha);
    }

    public String getHexColor() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }

    public String formatRGB() {
        return String.format("RGB(%d, %d, %d)", red, green, blue);
    }

    public String formatRGBA() {
        return String.format("RGBA(%d, %d, %d, %d)", red, green, blue, alpha);
    }

    @Override
    public String toString() {
        return String.format("Pixel[%d, %d]: %s, Alpha=%d, Luma=%.1f (%s)",
                x, y, formatRGB(), alpha, luminance, getHexColor());
    }
}

