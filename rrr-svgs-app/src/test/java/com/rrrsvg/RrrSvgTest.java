package com.rrrsvg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.rrrsvg.interpreter.ColorParser;
import com.rrrsvg.interpreter.Drawing;
import com.rrrsvg.interpreter.SvgInterpreter;
import com.rrrsvg.interpreter.Transform;
import com.rrrsvg.rasterizer.Rasterizer;
import com.rrrsvg.reader.SvgNode;
import com.rrrsvg.reader.SvgParseException;
import com.rrrsvg.reader.SvgReader;
import com.rrrsvg.renderer.Renderer;

import java.awt.Color;
import java.awt.image.BufferedImage;

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
        java.io.File houseFile = new java.io.File("samples/house.svg");
        if (houseFile.exists()) {
            SvgNode root = new SvgReader().read(houseFile);
            Drawing drawing = new SvgInterpreter().interpret(root);
            BufferedImage img = new Rasterizer().rasterize(new Renderer().render(drawing), drawing.width(), drawing.height());
            new java.io.File("../docs").mkdirs();
            Rasterizer.savePng(img, new java.io.File("../docs/output_preview.png"));
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
}
