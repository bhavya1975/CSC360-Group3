package com.rrrsvg.rasterizer;

import com.rrrsvg.renderer.DrawOp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Stage 4 — Rasterization.
 * Converts the draw operations into pixels using a scanline algorithm:
 * <ol>
 *   <li>For each pixel row, find where the polygon edges cross it.</li>
 *   <li>Between crossings the row is inside the shape (non-zero winding rule).</li>
 *   <li>Anti-aliasing: each row is sampled 4 times, and edge pixels get partial coverage,
 *       so edges look smooth instead of jagged.</li>
 *   <li>The colour is blended onto the pixel according to its coverage and opacity.</li>
 * </ol>
 */
public class Rasterizer {

    private static final int SAMPLES_PER_PIXEL = 4;

    public BufferedImage rasterize(List<DrawOp> ops, int width, int height) {
        // red, green, blue, alpha for every pixel (colour is premultiplied by alpha)
        float[] pixels = new float[width * height * 4];
        for (DrawOp op : ops) {
            fill(op, pixels, width, height);
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (y * width + x) * 4;
                float a = pixels[i + 3];
                if (a > 0) {
                    image.setRGB(x, y, (to255(a) << 24) | (to255(pixels[i] / a) << 16)
                            | (to255(pixels[i + 1] / a) << 8) | to255(pixels[i + 2] / a));
                }
            }
        }
        return image;
    }

    public static void savePng(BufferedImage image, File file) throws IOException {
        ImageIO.write(image, "png", file);
    }

    private void fill(DrawOp op, float[] pixels, int width, int height) {
        // Collect all non-horizontal edges as {topX, topY, bottomX, bottomY, direction}
        List<double[]> edges = new ArrayList<>();
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (double[] poly : op.polygons()) {
            int n = poly.length / 2;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double xa = poly[2 * i];
                double ya = poly[2 * i + 1];
                double xb = poly[2 * j];
                double yb = poly[2 * j + 1];
                minY = Math.min(minY, ya);
                maxY = Math.max(maxY, ya);
                if (ya < yb) {
                    edges.add(new double[] {xa, ya, xb, yb, 1});
                } else if (ya > yb) {
                    edges.add(new double[] {xb, yb, xa, ya, -1});
                }
            }
        }
        if (edges.isEmpty()) {
            return;
        }

        float red = op.color().getRed() / 255f;
        float green = op.color().getGreen() / 255f;
        float blue = op.color().getBlue() / 255f;
        float alpha = op.color().getAlpha() / 255f;
        float[] coverage = new float[width];
        double[] crossX = new double[edges.size()];
        int[] crossDir = new int[edges.size()];

        int firstRow = Math.max(0, (int) Math.floor(minY));
        int lastRow = Math.min(height - 1, (int) Math.ceil(maxY));
        for (int y = firstRow; y <= lastRow; y++) {
            boolean rowTouched = false;
            for (int s = 0; s < SAMPLES_PER_PIXEL; s++) {
                double sampleY = y + (s + 0.5) / SAMPLES_PER_PIXEL;

                // 1. find the edge crossings on this line, sorted from left to right
                int count = 0;
                for (double[] e : edges) {
                    if (e[1] <= sampleY && sampleY < e[3]) {
                        double x = e[0] + (sampleY - e[1]) * (e[2] - e[0]) / (e[3] - e[1]);
                        int k = count++;
                        while (k > 0 && crossX[k - 1] > x) {
                            crossX[k] = crossX[k - 1];
                            crossDir[k] = crossDir[k - 1];
                            k--;
                        }
                        crossX[k] = x;
                        crossDir[k] = (int) e[4];
                    }
                }

                // 2. walk the crossings; where the winding number is not zero we are inside
                int winding = 0;
                double spanStart = 0;
                for (int k = 0; k < count; k++) {
                    if (winding == 0) {
                        spanStart = crossX[k];
                    }
                    winding += crossDir[k];
                    if (winding == 0) {
                        addCoverage(coverage, spanStart, crossX[k], width);
                        rowTouched = true;
                    }
                }
            }
            if (!rowTouched) {
                continue;
            }

            // 3. blend the colour into each covered pixel of this row
            for (int x = 0; x < width; x++) {
                float c = Math.min(coverage[x], 1f);
                coverage[x] = 0;
                if (c <= 0) {
                    continue;
                }
                float a = alpha * c;
                int i = (y * width + x) * 4;
                pixels[i] = red * a + pixels[i] * (1 - a);
                pixels[i + 1] = green * a + pixels[i + 1] * (1 - a);
                pixels[i + 2] = blue * a + pixels[i + 2] * (1 - a);
                pixels[i + 3] = a + pixels[i + 3] * (1 - a);
            }
        }
    }

    /** Adds the inside span [x0, x1) of one sample line; pixels at the ends get partial coverage. */
    private static void addCoverage(float[] coverage, double x0, double x1, int width) {
        x0 = Math.max(0, x0);
        x1 = Math.min(width, x1);
        float weight = 1f / SAMPLES_PER_PIXEL;
        for (int x = (int) x0; x < x1 && x < width; x++) {
            double covered = Math.min(x + 1, x1) - Math.max(x, x0); // fraction of this pixel inside
            coverage[x] += (float) (covered * weight);
        }
    }

    private static int to255(float v) {
        return Math.max(0, Math.min(255, Math.round(v * 255)));
    }
}
