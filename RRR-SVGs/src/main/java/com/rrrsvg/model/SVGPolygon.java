package com.rrrsvg.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an SVG &lt;polygon&gt; element (a closed shape made of straight line segments).
 * <p>
 * Encapsulates the 'points' attribute and provides convenient parsed
 * point coordinates as {@link Point2D.Double}.
 * </p>
 */
public class SVGPolygon extends SVGElement {

    public static final String TAG_NAME = "polygon";

    public SVGPolygon() {
        super(TAG_NAME);
    }

    /**
     * Returns the raw 'points' attribute string.
     *
     * @return Points string (e.g. "200,10 250,190 160,210").
     */
    public String getPoints() {
        return getAttribute("points", "");
    }

    /**
     * Parses the 'points' attribute into a list of 2D coordinates.
     * Handles comma and whitespace delimited coordinate pairs.
     *
     * @return List of parsed Point2D.Double vertices.
     */
    public List<Point2D.Double> getPointsList() {
        List<Point2D.Double> list = new ArrayList<>();
        String raw = getPoints().trim();
        if (raw.isEmpty()) {
            return list;
        }

        String[] tokens = raw.split("[,\\s]+");
        for (int i = 0; i + 1 < tokens.length; i += 2) {
            try {
                double x = Double.parseDouble(tokens[i]);
                double y = Double.parseDouble(tokens[i + 1]);
                list.add(new Point2D.Double(x, y));
            } catch (NumberFormatException ignored) {
                // Ignore unparseable tokens
            }
        }
        return Collections.unmodifiableList(list);
    }
}

