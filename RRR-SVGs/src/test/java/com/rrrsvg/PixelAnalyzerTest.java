package com.rrrsvg;

import com.rrrsvg.analysis.HistogramData;
import com.rrrsvg.analysis.PixelAnalyzer;
import com.rrrsvg.analysis.PixelData;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying Module 3: ImageJ Pixel Analysis and Histograms.
 */
public class PixelAnalyzerTest {

    private BufferedImage testImage;
    private PixelAnalyzer analyzer;

    @Before
    public void setUp() {
        // Create a 20x20 test image with distinct colored quadrants
        testImage = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = testImage.createGraphics();
        try {
            // Top-left: Pure Red (255, 0, 0)
            g2.setColor(Color.RED);
            g2.fillRect(0, 0, 10, 10);

            // Top-right: Pure Green (0, 255, 0)
            g2.setColor(Color.GREEN);
            g2.fillRect(10, 0, 10, 10);

            // Bottom-left: Pure Blue (0, 0, 255)
            g2.setColor(Color.BLUE);
            g2.fillRect(0, 10, 10, 10);

            // Bottom-right: Pure White (255, 255, 255)
            g2.setColor(Color.WHITE);
            g2.fillRect(10, 10, 10, 10);
        } finally {
            g2.dispose();
        }

        analyzer = PixelAnalyzer.fromImage(testImage, "Test Pattern");
    }

    @Test
    public void testPixelInspectionCoordinatesAndRGB() {
        assertEquals(20, analyzer.getWidth());
        assertEquals(20, analyzer.getHeight());
        assertEquals(400, analyzer.getTotalPixels());

        // Inspect Top-Left (Red quadrant)
        PixelData redPixel = analyzer.getPixel(5, 5);
        assertNotNull(redPixel);
        assertEquals(5, redPixel.getX());
        assertEquals(5, redPixel.getY());
        assertEquals(255, redPixel.getRed());
        assertEquals(0, redPixel.getGreen());
        assertEquals(0, redPixel.getBlue());
        assertEquals("#FF0000", redPixel.getHexColor());

        // Inspect Top-Right (Green quadrant)
        PixelData greenPixel = analyzer.getPixel(15, 5);
        assertNotNull(greenPixel);
        assertEquals(0, greenPixel.getRed());
        assertEquals(255, greenPixel.getGreen());
        assertEquals(0, greenPixel.getBlue());
        assertEquals("#00FF00", greenPixel.getHexColor());

        // Inspect Bottom-Left (Blue quadrant)
        PixelData bluePixel = analyzer.getPixel(5, 15);
        assertNotNull(bluePixel);
        assertEquals(0, bluePixel.getRed());
        assertEquals(0, bluePixel.getGreen());
        assertEquals(255, bluePixel.getBlue());
        assertEquals("#0000FF", bluePixel.getHexColor());

        // Inspect Bottom-Right (White quadrant)
        PixelData whitePixel = analyzer.getPixel(15, 15);
        assertNotNull(whitePixel);
        assertEquals(255, whitePixel.getRed());
        assertEquals(255, whitePixel.getGreen());
        assertEquals(255, whitePixel.getBlue());
        assertEquals("#FFFFFF", whitePixel.getHexColor());

        // Out of bounds check
        assertNull(analyzer.getPixel(-1, 0));
        assertNull(analyzer.getPixel(20, 20));
    }

    @Test
    public void testHistogramComputationAndChannelStats() {
        HistogramData hist = analyzer.computeHistogramData();
        assertNotNull(hist);

        int[] redBins = hist.getRedBins();
        int[] greenBins = hist.getGreenBins();
        int[] blueBins = hist.getBlueBins();
        int[] lumBins = hist.getLuminanceBins();

        assertEquals(256, redBins.length);
        assertEquals(256, greenBins.length);
        assertEquals(256, blueBins.length);
        assertEquals(256, lumBins.length);

        // In a 20x20 image (400 pixels total),
        // Red is 255 in Red quadrant (100 px) + White quadrant (100 px) = 200 pixels at bin 255
        assertEquals(200, redBins[255]);
        // Red is 0 in Green quadrant (100 px) + Blue quadrant (100 px) = 200 pixels at bin 0
        assertEquals(200, redBins[0]);

        assertEquals(400, hist.getRedStats().getTotalPixels());
        assertEquals(0, hist.getRedStats().getMin());
        assertEquals(255, hist.getRedStats().getMax());
        assertEquals(127.5, hist.getRedStats().getMean(), 0.01);

        assertTrue(hist.getMaxFrequency() >= 200);
    }

    @Test
    public void testNeighborhoodPixelGrid() {
        PixelData[][] grid = analyzer.getNeighborhoodPixels(10, 10, 2); // 5x5 grid around border
        assertNotNull(grid);
        assertEquals(5, grid.length);
        assertEquals(5, grid[0].length);

        // Center pixel (10, 10) is in bottom-right white quadrant
        PixelData center = grid[2][2];
        assertNotNull(center);
        assertEquals(10, center.getX());
        assertEquals(10, center.getY());
    }

    @Test
    public void testReportGeneration() {
        String report = analyzer.generateAnalysisReport();
        assertNotNull(report);
        assertTrue(report.contains("IMAGEJ RASTER PIXEL ANALYSIS REPORT"));
        assertTrue(report.contains("20 × 20 pixels"));
        assertTrue(report.contains("Red Channel"));
    }
}

