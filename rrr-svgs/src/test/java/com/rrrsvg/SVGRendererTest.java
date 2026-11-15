package com.rrrsvg;

import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRenderException;
import com.rrrsvg.model.SVGDocument;
import com.rrrsvg.render.SVGRenderer;
import com.rrrsvg.ui.SVGRenderCanvas;
import org.apache.batik.gvt.GraphicsNode;
import org.junit.Before;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests verifying Module 2: SVG Rendering with Apache Batik.
 */
public class SVGRendererTest {

    private SVGRenderer renderer;
    private File sampleFile;
    private File transformsFile;

    @Before
    public void setUp() {
        renderer = new SVGRenderer();

        sampleFile = new File("src/main/resources/sample.svg");
        if (!sampleFile.exists()) {
            sampleFile = new File("rrr-svgs/src/main/resources/sample.svg");
        }
        if (!sampleFile.exists()) {
            sampleFile = new File("RRR-SVGs/src/main/resources/sample.svg");
        }

        transformsFile = new File("src/main/resources/transforms_colors.svg");
        if (!transformsFile.exists()) {
            transformsFile = new File("rrr-svgs/src/main/resources/transforms_colors.svg");
        }
        if (!transformsFile.exists()) {
            transformsFile = new File("RRR-SVGs/src/main/resources/transforms_colors.svg");
        }
    }

    @Test
    public void testModule1IntegrationBeforeRendering() throws SVGParseException {
        assertTrue("sample.svg should exist", sampleFile.exists());
        SVGDocument doc = renderer.validateAndRead(sampleFile);
        assertNotNull("Module 1 validation should return SVGDocument", doc);
        assertEquals(500.0, doc.getRoot().getWidth(), 0.001);
        assertEquals(400.0, doc.getRoot().getHeight(), 0.001);
        assertTrue(doc.getTotalElementCount() > 0);
    }

    @Test
    public void testBuildGraphicsNode() throws SVGParseException, SVGRenderException {
        GraphicsNode node = renderer.buildGraphicsNode(sampleFile);
        assertNotNull("GraphicsNode should not be null", node);
        assertNotNull("GraphicsNode bounds should not be null", node.getBounds());
        assertTrue("GraphicsNode width should be > 0", node.getBounds().getWidth() > 0);
        assertTrue("GraphicsNode height should be > 0", node.getBounds().getHeight() > 0);
    }

    @Test
    public void testRenderSampleToGraphicsContext() throws SVGParseException, SVGRenderException {
        int width = 500;
        int height = 400;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        try {
            renderer.renderToGraphics(sampleFile, g2d, width, height);

            // Verify that pixels were painted (not all transparent)
            boolean hasNonZeroPixel = false;
            for (int y = 0; y < height && !hasNonZeroPixel; y += 10) {
                for (int x = 0; x < width && !hasNonZeroPixel; x += 10) {
                    if ((img.getRGB(x, y) & 0xFF000000) != 0) {
                        hasNonZeroPixel = true;
                    }
                }
            }
            assertTrue("Rendered output should contain non-transparent pixels", hasNonZeroPixel);
        } finally {
            g2d.dispose();
        }
    }

    @Test
    public void testRenderTransformsAndColors() throws SVGParseException, SVGRenderException {
        if (!transformsFile.exists()) {
            return;
        }
        int width = 600;
        int height = 500;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        try {
            renderer.renderToGraphics(transformsFile, g2d, width, height);

            boolean hasNonZeroPixel = false;
            for (int y = 0; y < height && !hasNonZeroPixel; y += 10) {
                for (int x = 0; x < width && !hasNonZeroPixel; x += 10) {
                    if ((img.getRGB(x, y) & 0xFF000000) != 0) {
                        hasNonZeroPixel = true;
                    }
                }
            }
            assertTrue("Rendered transforms file should have drawn content", hasNonZeroPixel);
        } finally {
            g2d.dispose();
        }
    }

    @Test
    public void testGracefulErrorHandlingForMissingFile() {
        try {
            renderer.buildGraphicsNode(new File("does_not_exist_98765.svg"));
            fail("Expected SVGParseException for missing file");
        } catch (SVGParseException e) {
            assertTrue(e.getMessage().contains("not found"));
        } catch (SVGRenderException e) {
            fail("Expected SVGParseException from Module 1 validation, but got: " + e);
        }
    }

    @Test
    public void testCanvasZoomCalculations() {
        SVGRenderCanvas canvas = new SVGRenderCanvas();
        assertEquals(1.0, canvas.getZoomFactor(), 0.001);
        assertEquals(100, canvas.getZoomPercentage());

        // Zoom in by 1.25x
        canvas.zoomAtPoint(1.25, new Point2D.Double(200, 200));
        assertEquals(1.25, canvas.getZoomFactor(), 0.01);
        assertEquals(125, canvas.getZoomPercentage());

        // Reset
        canvas.resetZoom();
        assertEquals(1.0, canvas.getZoomFactor(), 0.001);
        assertEquals(100, canvas.getZoomPercentage());
    }
}

