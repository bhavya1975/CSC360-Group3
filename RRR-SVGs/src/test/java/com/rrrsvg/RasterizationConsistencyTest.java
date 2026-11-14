package com.rrrsvg;

import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRasterizeException;
import com.rrrsvg.model.SVGDocument;
import com.rrrsvg.model.SVGRoot;
import com.rrrsvg.raster.RasterOptions;
import com.rrrsvg.raster.SVGRasterizer;
import com.rrrsvg.ui.ComparisonViewPanel;
import com.rrrsvg.ui.SVGRenderCanvas;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying SVG-to-PNG rasterization consistency across Modules 1, 2, and 3.
 */
public class RasterizationConsistencyTest {

    private SVGRasterizer rasterizer;
    private File sampleFile;
    private File transformsFile;

    @Before
    public void setUp() {
        rasterizer = new SVGRasterizer();

        sampleFile = new File("src/main/resources/sample.svg");
        if (!sampleFile.exists()) {
            sampleFile = new File("RRR-SVGs/src/main/resources/sample.svg");
        }

        transformsFile = new File("src/main/resources/transforms_colors.svg");
        if (!transformsFile.exists()) {
            transformsFile = new File("RRR-SVGs/src/main/resources/transforms_colors.svg");
        }
    }

    @Test
    public void testSourceFileAndDimensionsConsistency() throws SVGParseException, SVGRasterizeException {
        SVGRenderCanvas canvas = new SVGRenderCanvas();
        canvas.loadSVGFile(sampleFile);

        assertEquals(sampleFile.getAbsoluteFile(), canvas.getCurrentFile());

        Dimension nativeDims = canvas.getNativeSvgDimensions();
        assertEquals(500, nativeDims.width);
        assertEquals(400, nativeDims.height);

        // Rasterize using matching native dimensions
        RasterOptions options = new RasterOptions(nativeDims.width, nativeDims.height);
        BufferedImage png = rasterizer.rasterizeToImage(sampleFile, options);

        assertNotNull(png);
        assertEquals(nativeDims.width, png.getWidth());
        assertEquals(nativeDims.height, png.getHeight());
    }

    @Test
    public void testViewBoxFallbackWithPercentageDimensions() throws SVGParseException, SVGRasterizeException {
        String svgWithPercentage =
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100%\" height=\"100%\" viewBox=\"0 0 350 250\">" +
                "  <rect x=\"10\" y=\"10\" width=\"330\" height=\"230\" fill=\"#27ae60\"/>" +
                "</svg>";

        SVGDocument doc = SVGReader.readText(svgWithPercentage);
        SVGRoot root = doc.getRoot();

        // Must resolve to viewBox dimensions instead of 100 or 0
        assertEquals(350.0, root.getWidth(), 0.001);
        assertEquals(250.0, root.getHeight(), 0.001);

        RasterOptions options = RasterOptions.fromSvg(root);
        assertEquals(350, options.getWidth());
        assertEquals(250, options.getHeight());
    }

    @Test
    public void testGradientsColoursAndTransformsPreservation() throws SVGParseException, SVGRasterizeException {
        assertTrue("transforms_colors.svg should exist", transformsFile.exists());

        RasterOptions options = new RasterOptions(600, 500, 96.0, null);
        BufferedImage png = rasterizer.rasterizeToImage(transformsFile, options);

        assertNotNull(png);
        assertEquals(600, png.getWidth());
        assertEquals(500, png.getHeight());

        // Inspect pixel at rotated star center (300, 200) -> should be red (#E74C3C / #C0392B)
        int starPixel = png.getRGB(300, 200);
        int starRed = (starPixel >> 16) & 0xFF;
        int starGreen = (starPixel >> 8) & 0xFF;
        int starBlue = starPixel & 0xFF;

        assertTrue("Rotated star center should have strong red channel", starRed > 160);
        assertTrue("Rotated star center should have low green channel compared to red", starRed > starGreen);

        // Inspect pixel in radial gradient sun (100, 100) -> should be yellow/orange
        int sunPixel = png.getRGB(100, 100);
        int sunRed = (sunPixel >> 16) & 0xFF;
        int sunGreen = (sunPixel >> 8) & 0xFF;
        assertTrue("Radial gradient sun should have high red channel", sunRed > 200);
        assertTrue("Radial gradient sun should have high green channel", sunGreen > 180);
    }

    @Test
    public void testComparisonPanelDirectDisplay() {
        ComparisonViewPanel compPanel = new ComparisonViewPanel();
        compPanel.loadSvg(sampleFile);

        assertEquals(sampleFile.getAbsoluteFile(), compPanel.getCurrentSvgFile());
        assertNotNull("Generated PNG in comparison panel must not be null", compPanel.getCurrentPngImage());
        assertEquals(500, compPanel.getCurrentPngImage().getWidth());
        assertEquals(400, compPanel.getCurrentPngImage().getHeight());
    }
}

