package com.rrrsvg;

import com.rrrsvg.model.SVGCircle;
import com.rrrsvg.model.SVGElement;
import com.rrrsvg.model.SVGElementFactory;
import com.rrrsvg.model.SVGGenericElement;
import com.rrrsvg.model.SVGGroup;
import com.rrrsvg.model.SVGRect;
import com.rrrsvg.model.SVGRoot;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for SVGElement and SVGElementFactory.
 */
public class SVGElementTest {

    @Test
    public void testFactoryCreation() {
        assertTrue(SVGElementFactory.createElement("svg") instanceof SVGRoot);
        assertTrue(SVGElementFactory.createElement("rect") instanceof SVGRect);
        assertTrue(SVGElementFactory.createElement("circle") instanceof SVGCircle);
        assertTrue(SVGElementFactory.createElement("g") instanceof SVGGroup);
        assertTrue(SVGElementFactory.createElement("custom-tag") instanceof SVGGenericElement);
    }

    @Test
    public void testAttributeManipulationAndUnitParsing() {
        SVGRect rect = new SVGRect();
        rect.setAttribute("x", "25px");
        rect.setAttribute("y", "35.5pt");
        rect.setAttribute("width", "150");
        rect.setAttribute("height", "invalid_num");

        assertEquals(25.0, rect.getDoubleAttribute("x", 0.0), 0.001);
        assertEquals(35.5, rect.getDoubleAttribute("y", 0.0), 0.001);
        assertEquals(150.0, rect.getDoubleAttribute("width", 0.0), 0.001);
        assertEquals(99.0, rect.getDoubleAttribute("height", 99.0), 0.001);
        assertEquals(50.0, rect.getDoubleAttribute("non_existent", 50.0), 0.001);

        assertTrue(rect.hasAttribute("x"));
        assertFalse(rect.hasAttribute("z"));
    }

    @Test
    public void testElementTreeHierarchy() {
        SVGRoot root = new SVGRoot();
        SVGGroup group = new SVGGroup();
        group.setAttribute("id", "grp1");
        SVGCircle circle = new SVGCircle();
        circle.setAttribute("id", "circ1");

        group.addChild(circle);
        root.addChild(group);

        assertEquals(root, group.getParent());
        assertEquals(group, circle.getParent());
        assertTrue(root.hasChildren());
        assertEquals(1, root.getChildren().size());
        assertEquals(1, group.getChildren().size());
        assertFalse(circle.hasChildren());

        SVGElement found = root.getElementById("circ1");
        assertNotNull(found);
        assertEquals(circle, found);
    }
}

