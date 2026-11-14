package com.rrrsvg;

import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRasterizeException;
import com.rrrsvg.raster.RasterOptions;
import com.rrrsvg.raster.SVGRasterizer;
import ij.ImagePlus;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests verifying Module 3: SVG Rasterization using Apache Batik.
 */
public class SVGRasterizerTest {

    private SVGRasterizer rasterizer;
    private File sampleFile;
    private File tempOutputDir;

    @Before
    public void setUp() {
        rasterizer = new SVGRasterizer();

        sampleFile = new File("src/main/resources/sample.svg");
        if (!sampleFile.exists()) {
            sampleFile = new File("RRR-SVGs/src/main/resources/sample.svg");
        }

        tempOutputDir = new File("target/test-png");
        tempOutputDir.mkdirs();
    }

    @Test
    public void testRasterizeToFileWithCustomDimensions() throws SVGParseException, SVGRasterizeException, IOException {
        assertTrue("sample.svg should exist", sampleFile.exists());

        File outPng = new File(tempOutputDir, "sample_400x300.png");
        RasterOptions options = new RasterOptions(400, 300, 96.0, Color.WHITE);

        File result = rasterizer.rasterizeToFile(sampleFile, outPng, options);
        assertNotNull("Generated file should not be null", result);
        assertTrue("Generated PNG file must exist on disk", result.exists());
        assertTrue("PNG file size should be > 0", result.length() > 0);

        BufferedImage img = ImageIO.read(result);
        assertNotNull("Decoded BufferedImage should not be null", img);
        assertEquals(400, img.getWidth());
        assertEquals(300, img.getHeight());
    }

    @Test
    public void testRasterizeWithCustomResolutionDPI() throws SVGParseException, SVGRasterizeException {
        RasterOptions options = new RasterOptions(300, 200, 150.0, null);
        assertEquals(150.0, options.getDpi(), 0.001);
        assertTrue(options.getMillimetersPerPixel() > 0);

        byte[] pngBytes = rasterizer.rasterizeToBytes(sampleFile, options);
        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);
    }

    @Test
    public void testRasterizeToImagePlus() throws SVGParseException, SVGRasterizeException {
        RasterOptions options = new RasterOptions(350, 250);
        ImagePlus imp = rasterizer.rasterizeToImagePlus(sampleFile, options);

        assertNotNull("ImagePlus should not be null", imp);
        assertEquals(350, imp.getWidth());
        assertEquals(250, imp.getHeight());
        assertTrue("Bit depth should be 24-bit RGB", imp.getBitDepth() == 24 || imp.getBitDepth() == 32);
    }

    @Test
    public void testInvalidDimensionsThrowException() {
        RasterOptions badWidth = new RasterOptions(-50, 100);
        try {
            badWidth.validate();
            fail("Expected SVGRasterizeException for negative width");
        } catch (SVGRasterizeException e) {
            assertTrue(e.getMessage().contains("width"));
        }

        RasterOptions badHeight = new RasterOptions(100, 0);
        try {
            badHeight.validate();
            fail("Expected SVGRasterizeException for zero height");
        } catch (SVGRasterizeException e) {
            assertTrue(e.getMessage().contains("height"));
        }

        RasterOptions badDpi = new RasterOptions(100, 100, -10.0, null);
        try {
            badDpi.validate();
            fail("Expected SVGRasterizeException for negative DPI");
        } catch (SVGRasterizeException e) {
            assertTrue(e.getMessage().contains("DPI"));
        }
    }

    @Test
    public void testMissingFileThrowsGracefulException() {
        File missing = new File("non_existent_file_777.svg");
        File outPng = new File(tempOutputDir, "out.png");
        try {
            rasterizer.rasterizeToFile(missing, outPng, new RasterOptions());
            fail("Expected SVGParseException for missing file");
        } catch (SVGParseException e) {
            assertTrue(e.getMessage().contains("not found"));
        } catch (SVGRasterizeException e) {
            fail("Expected SVGParseException, got " + e);
        }
    }

    @Test
    public void testVectorToRasterExplanation() {
        String explanation = SVGRasterizer.explainVectorToRasterConversion(sampleFile, new RasterOptions(500, 400));
        assertNotNull(explanation);
        assertTrue(explanation.contains("DISCRETIZATION"));
        assertTrue(explanation.contains("ANTI-ALIASING"));
        assertTrue(explanation.contains("500 columns × 400 rows"));
    }
}

