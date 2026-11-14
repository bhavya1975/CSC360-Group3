package com.rrrsvg.analysis;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * ImageJ-powered analysis engine for inspecting rasterized SVG images.
 * <p>
 * Provides:
 * <ul>
 *   <li>Native ImageJ {@link ImagePlus} integration and interactive inspection</li>
 *   <li>Discrete pixel coordinate readout and RGB channel decomposition</li>
 *   <li>256-bin RGB and Luminance histogram calculations</li>
 *   <li>Neighborhood pixel sampling for anti-aliasing / discretization analysis</li>
 * </ul>
 * </p>
 */
public class PixelAnalyzer {

    private final ImagePlus imagePlus;
    private final ColorProcessor colorProcessor;
    private final int width;
    private final int height;
    private HistogramData cachedHistogram;

    public PixelAnalyzer(ImagePlus imagePlus) {
        if (imagePlus == null) {
            throw new IllegalArgumentException("ImagePlus cannot be null");
        }
        this.imagePlus = imagePlus;
        ImageProcessor ip = imagePlus.getProcessor();
        if (ip instanceof ColorProcessor cp) {
            this.colorProcessor = cp;
        } else {
            this.colorProcessor = (ColorProcessor) ip.convertToRGB();
        }
        this.width = this.colorProcessor.getWidth();
        this.height = this.colorProcessor.getHeight();
    }

    /**
     * Loads an image file from disk into an ImageJ-powered PixelAnalyzer.
     *
     * @param imageFile Target image file (PNG).
     * @return PixelAnalyzer instance.
     * @throws IOException If reading fails.
     */
    public static PixelAnalyzer fromFile(File imageFile) throws IOException {
        if (imageFile == null || !imageFile.exists()) {
            throw new IOException("Image file not found: " + (imageFile != null ? imageFile.getPath() : "null"));
        }
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) {
            throw new IOException("Failed to decode image file: " + imageFile.getPath());
        }
        ImagePlus imp = new ImagePlus(imageFile.getName(), img);
        return new PixelAnalyzer(imp);
    }

    /**
     * Creates a PixelAnalyzer from an in-memory {@link BufferedImage}.
     */
    public static PixelAnalyzer fromImage(BufferedImage image, String title) {
        if (image == null) {
            throw new IllegalArgumentException("BufferedImage cannot be null");
        }
        ImagePlus imp = new ImagePlus(title != null ? title : "Rasterized SVG", image);
        return new PixelAnalyzer(imp);
    }

    /**
     * Returns the underlying ImageJ {@link ImagePlus} instance.
     */
    public ImagePlus getImagePlus() {
        return imagePlus;
    }

    /**
     * Returns the ImageJ {@link ColorProcessor}.
     */
    public ColorProcessor getColorProcessor() {
        return colorProcessor;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getTotalPixels() {
        return (long) width * height;
    }

    /**
     * Inspects a specific pixel at coordinates (x, y).
     *
     * @param x X coordinate (0 to width - 1).
     * @param y Y coordinate (0 to height - 1).
     * @return PixelData containing RGBA, hex, and luminance, or null if coordinates are out of bounds.
     */
    public PixelData getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        int argb = colorProcessor.getPixel(x, y);
        return new PixelData(x, y, argb);
    }

    /**
     * Computes complete 256-bin RGB and Luminance histograms along with channel statistics
     * using ImageJ's {@link ByteProcessor} channel extractors.
     *
     * @return Cached or freshly computed {@link HistogramData}.
     */
    public synchronized HistogramData computeHistogramData() {
        if (cachedHistogram != null) {
            return cachedHistogram;
        }

        // ImageJ channel extraction: 1 = Red, 2 = Green, 3 = Blue
        ByteProcessor redChannel = colorProcessor.getChannel(1, null);
        ByteProcessor greenChannel = colorProcessor.getChannel(2, null);
        ByteProcessor blueChannel = colorProcessor.getChannel(3, null);

        int[] redBins = redChannel.getHistogram();
        int[] greenBins = greenChannel.getHistogram();
        int[] blueBins = blueChannel.getHistogram();
        int[] lumBins = colorProcessor.getHistogram(); // ImageJ weighted luminance histogram

        long total = getTotalPixels();
        ChannelStatistics redStats = computeStats("Red", redBins, total);
        ChannelStatistics greenStats = computeStats("Green", greenBins, total);
        ChannelStatistics blueStats = computeStats("Blue", blueBins, total);
        ChannelStatistics lumStats = computeStats("Luminance", lumBins, total);

        cachedHistogram = new HistogramData(redBins, greenBins, blueBins, lumBins,
                redStats, greenStats, blueStats, lumStats);
        return cachedHistogram;
    }

    private ChannelStatistics computeStats(String name, int[] bins, long totalPixels) {
        if (totalPixels <= 0) {
            return new ChannelStatistics(name, 0, 0, 0, 0, 0, 0, bins);
        }

        int min = 255;
        int max = 0;
        double sum = 0;
        long count = 0;

        for (int i = 0; i < bins.length; i++) {
            int freq = bins[i];
            if (freq > 0) {
                min = Math.min(min, i);
                max = Math.max(max, i);
                sum += (double) i * freq;
                count += freq;
            }
        }

        double mean = (count > 0) ? (sum / count) : 0.0;

        // Variance & standard deviation
        double varianceSum = 0;
        for (int i = 0; i < bins.length; i++) {
            int freq = bins[i];
            if (freq > 0) {
                double diff = i - mean;
                varianceSum += diff * diff * freq;
            }
        }
        double stdDev = (count > 0) ? Math.sqrt(varianceSum / count) : 0.0;

        // Median
        long half = count / 2;
        long accum = 0;
        int median = 0;
        for (int i = 0; i < bins.length; i++) {
            accum += bins[i];
            if (accum >= half) {
                median = i;
                break;
            }
        }

        return new ChannelStatistics(name, min, max, mean, stdDev, median, count, bins);
    }

    /**
     * Extracts a square neighborhood grid of pixels centered at (centerX, centerY).
     * Used for the raster pixel magnifier to visually demonstrate anti-aliased edge sampling.
     *
     * @param centerX Center X coordinate.
     * @param centerY Center Y coordinate.
     * @param radius  Grid radius (e.g. 5 produces an 11x11 grid).
     * @return 2D array of PixelData.
     */
    public PixelData[][] getNeighborhoodPixels(int centerX, int centerY, int radius) {
        int size = radius * 2 + 1;
        PixelData[][] grid = new PixelData[size][size];

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int px = centerX - radius + col;
                int py = centerY - radius + row;
                grid[row][col] = getPixel(px, py);
            }
        }
        return grid;
    }

    /**
     * Opens ImageJ's native interactive window displaying this image.
     * Enables standard ImageJ analysis tools (profile plotting, ROI measurement, lookup tables).
     */
    public void showInImageJWindow() {
        imagePlus.show();
    }

    /**
     * Generates a textual summary report of the pixel analysis.
     */
    public String generateAnalysisReport() {
        HistogramData data = computeHistogramData();
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                  IMAGEJ RASTER PIXEL ANALYSIS REPORT                           \n");
        sb.append("================================================================================\n");
        sb.append("Image Title    : ").append(imagePlus.getTitle()).append("\n");
        sb.append("Dimensions     : ").append(width).append(" × ").append(height).append(" pixels\n");
        sb.append("Total Pixels   : ").append(String.format("%,d", getTotalPixels())).append("\n");
        sb.append("Bit Depth      : ").append(imagePlus.getBitDepth()).append("-bit RGB\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("CHANNEL STATISTICS:\n");
        sb.append("  * ").append(data.getRedStats().formatSummary()).append("\n");
        sb.append("  * ").append(data.getGreenStats().formatSummary()).append("\n");
        sb.append("  * ").append(data.getBlueStats().formatSummary()).append("\n");
        sb.append("  * ").append(data.getLuminanceStats().formatSummary()).append("\n");
        sb.append("================================================================================\n");
        return sb.toString();
    }
}

