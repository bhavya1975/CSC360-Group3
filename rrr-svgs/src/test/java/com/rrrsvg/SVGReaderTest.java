package com.rrrsvg;

import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.model.SVGCircle;
import com.rrrsvg.model.SVGDocument;
import com.rrrsvg.model.SVGElement;
import com.rrrsvg.model.SVGEllipse;
import com.rrrsvg.model.SVGGroup;
import com.rrrsvg.model.SVGLine;
import com.rrrsvg.model.SVGPath;
import com.rrrsvg.model.SVGPolygon;
import com.rrrsvg.model.SVGPolyline;
import com.rrrsvg.model.SVGRect;
import com.rrrsvg.model.SVGRoot;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests verifying Module 1: SVG Reading functionality.
 */
public class SVGReaderTest {

    private SVGReader reader;
    private File sampleFile;

    @Before
    public void setUp() {
        reader = new SVGReader();
        sampleFile = new File("src/main/resources/sample.svg");
        if (!sampleFile.exists()) {
            sampleFile = new File("rrr-svgs/src/main/resources/sample.svg");
        }
        if (!sampleFile.exists()) {
            sampleFile = new File("RRR-SVGs/src/main/resources/sample.svg");
        }
    }

    @Test
    public void testReadValidSampleFile() throws SVGParseException {
        assertTrue("sample.svg should exist", sampleFile.exists());
        SVGDocument doc = reader.read(sampleFile);

        assertNotNull("Parsed document should not be null", doc);
        SVGRoot root = doc.getRoot();
        assertNotNull("Root element should not be null", root);
        assertEquals("svg", root.getTagName());
        assertEquals(500.0, root.getWidth(), 0.001);
        assertEquals(400.0, root.getHeight(), 0.001);
        assertEquals("0 0 500 400", root.getViewBox());

        double[] vb = root.getViewBoxBounds();
        assertNotNull(vb);
        assertEquals(4, vb.length);
        assertEquals(0.0, vb[0], 0.001);
        assertEquals(0.0, vb[1], 0.001);
        assertEquals(500.0, vb[2], 0.001);
        assertEquals(400.0, vb[3], 0.001);
    }

    @Test
    public void testExtractHierarchicalStructure() throws SVGParseException {
        SVGDocument doc = reader.read(sampleFile);
        SVGRoot root = doc.getRoot();

        // Root has 3 top-level children: basic-shapes (g), poly-group (g), path1 (path)
        List<SVGElement> rootChildren = root.getChildren();
        assertEquals(3, rootChildren.size());

        SVGElement group1 = rootChildren.get(0);
        assertTrue("First child should be SVGGroup", group1 instanceof SVGGroup);
        assertEquals("basic-shapes", group1.getId());
        assertEquals(root, group1.getParent());

        // basic-shapes contains 4 child elements (rect, circle, ellipse, line)
        assertEquals(4, group1.getChildren().size());
        for (SVGElement child : group1.getChildren()) {
            assertEquals("Parent of nested element must be group1", group1, child.getParent());
        }

        SVGElement group2 = rootChildren.get(1);
        assertTrue("Second child should be SVGGroup", group2 instanceof SVGGroup);
        assertEquals("poly-group", group2.getId());
        assertEquals(2, group2.getChildren().size());

        SVGElement pathChild = rootChildren.get(2);
        assertTrue("Third child should be SVGPath", pathChild instanceof SVGPath);
    }

    @Test
    public void testExtractAllRequiredElementTypes() throws SVGParseException {
        SVGDocument doc = reader.read(sampleFile);

        // rect
        SVGElement rectElem = doc.getElementById("rect1");
        assertNotNull("rect1 should be found", rectElem);
        assertTrue(rectElem instanceof SVGRect);
        SVGRect rect = (SVGRect) rectElem;
        assertEquals(20.0, rect.getX(), 0.001);
        assertEquals(20.0, rect.getY(), 0.001);
        assertEquals(120.0, rect.getWidth(), 0.001);
        assertEquals(80.0, rect.getHeight(), 0.001);
        assertEquals(10.0, rect.getRx(), 0.001);
        assertEquals(10.0, rect.getRy(), 0.001);
        assertEquals("#3498db", rect.getFill());

        // circle
        SVGElement circleElem = doc.getElementById("circle1");
        assertNotNull("circle1 should be found", circleElem);
        assertTrue(circleElem instanceof SVGCircle);
        SVGCircle circle = (SVGCircle) circleElem;
        assertEquals(240.0, circle.getCx(), 0.001);
        assertEquals(60.0, circle.getCy(), 0.001);
        assertEquals(40.0, circle.getR(), 0.001);
        assertEquals("#e74c3c", circle.getFill());

        // ellipse
        SVGElement ellipseElem = doc.getElementById("ellipse1");
        assertNotNull("ellipse1 should be found", ellipseElem);
        assertTrue(ellipseElem instanceof SVGEllipse);
        SVGEllipse ellipse = (SVGEllipse) ellipseElem;
        assertEquals(380.0, ellipse.getCx(), 0.001);
        assertEquals(60.0, ellipse.getCy(), 0.001);
        assertEquals(60.0, ellipse.getRx(), 0.001);
        assertEquals(35.0, ellipse.getRy(), 0.001);
        assertEquals("#2ecc71", ellipse.getFill());

        // line
        SVGElement lineElem = doc.getElementById("line1");
        assertNotNull("line1 should be found", lineElem);
        assertTrue(lineElem instanceof SVGLine);
        SVGLine line = (SVGLine) lineElem;
        assertEquals(20.0, line.getX1(), 0.001);
        assertEquals(130.0, line.getY1(), 0.001);
        assertEquals(460.0, line.getX2(), 0.001);
        assertEquals(130.0, line.getY2(), 0.001);
        assertEquals("#34495e", line.getStroke());
        assertEquals(4.0, line.getStrokeWidth(), 0.001);

        // polyline
        SVGElement polylineElem = doc.getElementById("polyline1");
        assertNotNull("polyline1 should be found", polylineElem);
        assertTrue(polylineElem instanceof SVGPolyline);
        SVGPolyline polyline = (SVGPolyline) polylineElem;
        List<Point2D.Double> polylinePoints = polyline.getPointsList();
        assertEquals(4, polylinePoints.size());
        assertEquals(30.0, polylinePoints.get(0).x, 0.001);
        assertEquals(160.0, polylinePoints.get(0).y, 0.001);
        assertEquals(180.0, polylinePoints.get(3).x, 0.001);
        assertEquals(220.0, polylinePoints.get(3).y, 0.001);

        // polygon
        SVGElement polygonElem = doc.getElementById("polygon1");
        assertNotNull("polygon1 should be found", polygonElem);
        assertTrue(polygonElem instanceof SVGPolygon);
        SVGPolygon polygon = (SVGPolygon) polygonElem;
        List<Point2D.Double> polygonPoints = polygon.getPointsList();
        assertEquals(3, polygonPoints.size());
        assertEquals(260.0, polygonPoints.get(0).x, 0.001);
        assertEquals(160.0, polygonPoints.get(0).y, 0.001);

        // path
        SVGElement pathElem = doc.getElementById("path1");
        assertNotNull("path1 should be found", pathElem);
        assertTrue(pathElem instanceof SVGPath);
        SVGPath path = (SVGPath) pathElem;
        assertTrue(path.getD().startsWith("M 50,300"));
        assertEquals("#1abc9c", path.getStroke());
    }

    @Test
    public void testDocumentQueryUtilities() throws SVGParseException {
        SVGDocument doc = reader.read(sampleFile);

        assertEquals(10, doc.getTotalElementCount());
        assertEquals(1, doc.getElementsByTagName("rect").size());
        assertEquals(1, doc.getElementsByTagName("circle").size());
        assertEquals(1, doc.getElementsByTagName("ellipse").size());
        assertEquals(1, doc.getElementsByTagName("line").size());
        assertEquals(1, doc.getElementsByTagName("polyline").size());
        assertEquals(1, doc.getElementsByTagName("polygon").size());
        assertEquals(1, doc.getElementsByTagName("path").size());
        assertEquals(2, doc.getElementsByTagName("g").size());
        assertEquals(1, doc.getElementsByTagName("svg").size());

        assertNotNull(doc.getElementById("rect1"));
        assertNull(doc.getElementById("non-existent-id"));
    }

    @Test
    public void testReadableTreeFormatting() throws SVGParseException {
        SVGDocument doc = reader.read(sampleFile);
        String tree = doc.toTreeString();
        assertNotNull(tree);
        assertFalse(tree.isEmpty());
        assertTrue(tree.contains("[svg]"));
        assertTrue(tree.contains("[rect]"));
        assertTrue(tree.contains("[circle]"));
        assertTrue(tree.contains("[path]"));
        assertTrue(tree.contains("rect1"));
    }

    @Test
    public void testFileNotFoundThrowsGracefulException() {
        try {
            reader.read("non_existent_file_12345.svg");
            fail("Expected SVGParseException for missing file");
        } catch (SVGParseException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }

    @Test
    public void testMalformedXmlThrowsGracefulException() {
        String malformed = "<svg width='100'><rect x='10'</svg>";
        try {
            reader.readFromString(malformed);
            fail("Expected SVGParseException for malformed XML");
        } catch (SVGParseException e) {
            assertTrue(e.getMessage().contains("XML syntax error") || e.getMessage().contains("Malformed XML"));
        }
    }

    @Test
    public void testNonSvgRootThrowsGracefulException() {
        String htmlXml = "<html xmlns='http://www.w3.org/1999/xhtml'><body>hello</body></html>";
        try {
            reader.readFromString(htmlXml);
            fail("Expected SVGParseException for non-svg root element");
        } catch (SVGParseException e) {
            assertTrue(e.getMessage().contains("Expected root element '<svg>'"));
        }
    }

    @Test
    public void testEmptyContentThrowsGracefulException() {
        try {
            reader.readFromString("");
            fail("Expected SVGParseException for empty string");
        } catch (SVGParseException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }
    }
}

