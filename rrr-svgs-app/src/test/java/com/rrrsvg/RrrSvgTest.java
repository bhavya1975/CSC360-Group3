package com.rrrsvg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.rrrsvg.interpreter.ColorParser;
import com.rrrsvg.interpreter.Drawing;
import com.rrrsvg.interpreter.Polyline;
import com.rrrsvg.interpreter.SvgInterpreter;
import com.rrrsvg.interpreter.Transform;
import com.rrrsvg.rasterizer.Rasterizer;
import com.rrrsvg.reader.SvgNode;
import com.rrrsvg.reader.SvgParseException;
import com.rrrsvg.reader.SvgReader;
import com.rrrsvg.renderer.Renderer;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

import org.junit.Test;

public class RrrSvgTest {

    /** Runs all four stages on an SVG string. */
    private static BufferedImage render(String svg) throws SvgParseException {
        SvgNode root = new SvgReader().readString(svg);
        Drawing drawing = new SvgInterpreter().interpret(root);
        return new Rasterizer().rasterize(new Renderer().render(drawing), drawing.width(), drawing.height());
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    @Test
    public void readerBuildsTree() throws Exception {
        SvgNode root = new SvgReader().readString(
                "<svg xmlns='http://www.w3.org/2000/svg' width='10'><g><rect width='5'/></g></svg>");
        assertEquals("svg", root.name());
        assertEquals("rect", root.children().get(0).children().get(0).name());
        assertEquals(5, root.children().get(0).children().get(0).number("width", 0), 0);
    }

    @Test
    public void invalidFilesAreRejected() {
        assertThrows(SvgParseException.class, () -> new SvgReader().readString("<svg><rect></svg>"));
        assertThrows(SvgParseException.class, () -> new SvgReader().readString("<html/>"));
    }

    @Test
    public void generateHousePreview() throws Exception {
        File houseFile = new File("samples/house.svg");
        if (houseFile.exists()) {
            SvgNode root = new SvgReader().read(houseFile);
            Drawing drawing = new SvgInterpreter().interpret(root);
            BufferedImage img = new Rasterizer().rasterize(new Renderer().render(drawing), drawing.width(),
                    drawing.height());
            new File("../docs").mkdirs();
            Rasterizer.savePng(img, new File("../docs/output_preview.png"));
        }
    }

    @Test
    public void parsesColours() {
        assertEquals(Color.RED, ColorParser.parse("red"));
        assertEquals(new Color(0x112233), ColorParser.parse("#123"));
        assertEquals(new Color(10, 20, 30), ColorParser.parse("rgb(10, 20, 30)"));
        assertNull(ColorParser.parse("none"));
    }

    @Test
    public void parsesTransforms() {
        double[] p = Transform.parse("translate(10,20) scale(2)").apply(1, 1);
        assertEquals(12, p[0], 1e-9);
        assertEquals(22, p[1], 1e-9);
    }

    @Test
    public void rectangleFillsExactPixels() throws Exception {
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<rect x='2' y='2' width='4' height='4' fill='red'/></svg>");
        assertEquals(0xffff0000, img.getRGB(2, 2));
        assertEquals(0xffff0000, img.getRGB(5, 5));
        assertEquals(0, img.getRGB(6, 5));
        assertEquals(0, img.getRGB(1, 2));
    }

    @Test
    public void edgesAreAntiAliased() throws Exception {
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<rect x='2.5' y='2' width='4' height='4'/></svg>");
        assertEquals(128, alpha(img, 2, 3), 1); // pixel half covered -> half transparent
    }

    @Test
    public void laterShapesAreOnTopAndOpacityBlends() throws Exception {
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<rect width='10' height='10' fill='white'/>"
                + "<rect width='5' height='10' fill='blue'/>"
                + "<rect x='5' width='5' height='10' fill='black' opacity='0.5'/></svg>");
        assertEquals(0xff0000ff, img.getRGB(2, 2));
        assertEquals(0xff7f7f7f, img.getRGB(7, 2)); // 50% black over white = mid grey
    }

    @Test
    public void strokeIsDrawnAroundTheLine() throws Exception {
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'>"
                + "<line x1='0' y1='10' x2='20' y2='10' stroke='black' stroke-width='4'/></svg>");
        assertEquals(255, alpha(img, 10, 8));
        assertEquals(255, alpha(img, 10, 11));
        assertEquals(0, alpha(img, 10, 13));
    }

    @Test
    public void pathAndViewBoxWork() throws Exception {
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20' viewBox='0 0 10 10'>"
                + "<path d='M0 0 h5 v5 h-5 z' fill='lime'/></svg>");
        assertEquals(0xff00ff00, img.getRGB(9, 9)); // (5,5) in the viewBox is (10,10) in pixels
        assertEquals(0, img.getRGB(10, 10));
    }

    @Test
    public void circleAndEllipseRasterization() throws Exception {
        // Circle centered at (15, 15) with radius 8
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='30' height='30'>"
                + "<circle cx='15' cy='15' r='8' fill='red'/></svg>");
        // Center and points clearly inside circle should be opaque red
        assertEquals(0xffff0000, img.getRGB(15, 15));
        assertEquals(0xffff0000, img.getRGB(15, 10));
        assertEquals(0xffff0000, img.getRGB(15, 20));
        // Outside points should be completely transparent
        assertEquals(0, alpha(img, 2, 2));
        assertEquals(0, alpha(img, 28, 28));

        // Ellipse centered at (20, 10) with rx=15, ry=5
        BufferedImage ellipseImg = render("<svg xmlns='http://www.w3.org/2000/svg' width='40' height='20'>"
                + "<ellipse cx='20' cy='10' rx='15' ry='5' fill='blue'/></svg>");
        assertEquals(0xff0000ff, ellipseImg.getRGB(20, 10));
        assertEquals(0xff0000ff, ellipseImg.getRGB(30, 10)); // within horizontal radius rx=15
        assertEquals(0, alpha(ellipseImg, 20, 18)); // outside vertical radius ry=5
    }

    @Test
    public void polygonFillsAndPolylineStrokes() throws Exception {
        // Closed triangle polygon
        BufferedImage polyImg = render("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'>"
                + "<polygon points='2,2 18,2 10,18' fill='green'/></svg>");
        // Interior centroid point should be colored green
        assertEquals(0xff008000, polyImg.getRGB(10, 6));
        // Points outside triangle bounds should be transparent
        assertEquals(0, alpha(polyImg, 2, 18));
        assertEquals(0, alpha(polyImg, 18, 18));

        // Open polyline with stroke
        BufferedImage lineImg = render("<svg xmlns='http://www.w3.org/2000/svg' width='30' height='30'>"
                + "<polyline points='5,15 15,5 25,15' fill='none' stroke='red' stroke-width='4'/></svg>");
        // Along the stroked corner at (15, 5)
        assertTrue(alpha(lineImg, 15, 5) > 200);
        // Away from the stroke
        assertEquals(0, alpha(lineImg, 15, 25));
    }

    @Test
    public void bezierCurvesInPath() throws Exception {
        // Cubic Bézier curve (C) generates 20 interpolated segments (21 points)
        SvgNode cubicNode = new SvgReader().readString(
                "<svg width='30' height='20'><path d='M 0 0 C 10 20 20 20 30 0' fill='none' stroke='black'/></svg>");
        Drawing cubicDrawing = new SvgInterpreter().interpret(cubicNode);
        assertEquals(1, cubicDrawing.shapes().size());
        Polyline cLine = cubicDrawing.shapes().get(0).outlines().get(0);
        assertEquals(42, cLine.points().length);
        assertEquals(0.0, cLine.points()[0], 1e-6);
        assertEquals(0.0, cLine.points()[1], 1e-6);
        assertEquals(30.0, cLine.points()[40], 1e-6);
        assertEquals(0.0, cLine.points()[41], 1e-6);

        // Quadratic Bézier curve (Q)
        SvgNode quadNode = new SvgReader().readString(
                "<svg width='40' height='20'><path d='M 0 0 Q 20 40 40 0' fill='none' stroke='black'/></svg>");
        Drawing quadDrawing = new SvgInterpreter().interpret(quadNode);
        Polyline qLine = quadDrawing.shapes().get(0).outlines().get(0);
        assertEquals(42, qLine.points().length);
        assertEquals(40.0, qLine.points()[40], 1e-6);
        assertEquals(0.0, qLine.points()[41], 1e-6);

        // Relative commands (c and z)
        SvgNode relNode = new SvgReader().readString(
                "<svg width='40' height='40'><path d='m 10 10 c 5 5 10 5 15 0 z' fill='blue'/></svg>");
        Drawing relDrawing = new SvgInterpreter().interpret(relNode);
        Polyline relLine = relDrawing.shapes().get(0).outlines().get(0);
        assertTrue(relLine.closed());
        assertEquals(10.0, relLine.points()[0], 1e-6);
        assertEquals(10.0, relLine.points()[1], 1e-6);
    }

    @Test
    public void groupStyleInheritanceAndInlineStyles() throws Exception {
        // Group style inherited by child rectangle
        String svgGroup = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<g fill='blue' opacity='0.8'>"
                + "  <rect x='0' y='0' width='10' height='10'/>"
                + "</g></svg>";
        BufferedImage img = render(svgGroup);
        Color pixel = new Color(img.getRGB(5, 5), true);
        assertEquals(0, pixel.getRed());
        assertEquals(0, pixel.getGreen());
        assertEquals(255, pixel.getBlue());
        assertEquals(204, pixel.getAlpha(), 2); // 0.8 * 255 = 204

        // Inline CSS style="..." overriding attribute
        String svgInline = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<rect x='0' y='0' width='10' height='10' fill='red' style='fill: #00ff00;'/>"
                + "</svg>";
        BufferedImage inlineImg = render(svgInline);
        assertEquals(0xff00ff00, inlineImg.getRGB(5, 5));
    }

    @Test
    public void centerPointRotationAndMatrix() {
        // rotate(angle, cx, cy): rotating (15, 10) by 90 degrees around (10, 10) yields
        // (10, 15)
        Transform tRot = Transform.parse("rotate(90 10 10)");
        double[] rotPt = tRot.apply(15, 10);
        assertEquals(10.0, rotPt[0], 1e-6);
        assertEquals(15.0, rotPt[1], 1e-6);

        // matrix(a, b, c, d, e, f): x' = a*x + c*y + e; y' = b*x + d*y + f
        Transform tMat = Transform.parse("matrix(2 0 0 3 5 7)");
        double[] matPt = tMat.apply(1, 1);
        assertEquals(7.0, matPt[0], 1e-6); // 2*1 + 0*1 + 5 = 7
        assertEquals(10.0, matPt[1], 1e-6); // 0*1 + 3*1 + 7 = 10
    }

    @Test
    public void separateFillAndStrokeOpacity() throws Exception {
        // fill-opacity should only affect fill, not stroke
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'>"
                + "<rect x='2' y='2' width='16' height='16' fill='red' fill-opacity='0.5' stroke='none'/>"
                + "</svg>";
        BufferedImage img = render(svg);
        Color c = new Color(img.getRGB(10, 10), true);
        assertEquals(255, c.getRed());
        assertEquals(128, c.getAlpha(), 2);
    }

    @Test
    public void bundledSampleFilesRenderSuccessfully() throws Exception {
        SvgReader reader = new SvgReader();
        SvgInterpreter interpreter = new SvgInterpreter();
        Renderer renderer = new Renderer();
        Rasterizer rasterizer = new Rasterizer();

        String[] sampleNames = { "sample.svg", "shapes.svg", "house.svg" };
        for (String sampleName : sampleNames) {
            File file = new File("samples", sampleName);
            assertTrue("Sample file " + sampleName + " must exist", file.exists());

            SvgNode node = reader.read(file);
            assertNotNull(node);
            assertEquals("svg", node.name());

            Drawing drawing = interpreter.interpret(node);
            assertTrue("Drawing must have width > 0", drawing.width() > 0);
            assertTrue("Drawing must have height > 0", drawing.height() > 0);
            assertTrue("Drawing must have shapes", !drawing.shapes().isEmpty());

            BufferedImage image = rasterizer.rasterize(renderer.render(drawing), drawing.width(), drawing.height());
            assertNotNull("Rendered image must not be null for " + sampleName, image);
            assertEquals(drawing.width(), image.getWidth());
            assertEquals(drawing.height(), image.getHeight());
        }
    }

    @Test
    public void savePngWritesValidImageFile() throws Exception {
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<rect width='10' height='10' fill='blue'/></svg>");
        File temp = File.createTempFile("test_export", ".png");
        temp.deleteOnExit();

        Rasterizer.savePng(img, temp);
        assertTrue("Saved file must exist", temp.exists());
        assertTrue("Saved file must have positive size", temp.length() > 0);

        BufferedImage readBack = ImageIO.read(temp);
        assertNotNull("Saved PNG must be decodable by ImageIO", readBack);
        assertEquals(10, readBack.getWidth());
        assertEquals(10, readBack.getHeight());
    }

    @Test
    public void unsupportedElementsAreSkippedGracefully() throws Exception {
        // Unknown or unsupported elements (like text, defs, title) must not break
        // rendering of supported shapes
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<title>Sample Document</title>"
                + "<defs><linearGradient id='g1'/></defs>"
                + "<text x='0' y='5'>Ignored Text</text>"
                + "<rect x='0' y='0' width='10' height='10' fill='red'/>"
                + "</svg>";
        BufferedImage img = render(svg);
        assertNotNull(img);
        assertEquals(0xffff0000, img.getRGB(5, 5));
    }

    @Test
    public void imageJIntegration() throws Exception {
        // Verify rasterized output seamlessly bridges to ImageJ's ImagePlus for pixel
        // analysis
        BufferedImage img = render("<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'>"
                + "<rect x='0' y='0' width='20' height='20' fill='white'/>"
                + "<rect x='5' y='5' width='10' height='10' fill='black'/></svg>");
        ImagePlus imp = new ImagePlus("Test ImageJ", img);
        assertNotNull("ImagePlus wrapper should be created", imp);
        assertEquals(20, imp.getWidth());
        assertEquals(20, imp.getHeight());
        ImageProcessor ip = imp.getProcessor();
        assertNotNull(ip);
        assertEquals(20, ip.getWidth());
        assertEquals(20, ip.getHeight());
    }

}