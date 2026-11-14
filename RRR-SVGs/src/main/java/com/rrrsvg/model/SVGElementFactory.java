package com.rrrsvg.model;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Factory class for instantiating concrete {@link SVGElement} objects
 * according to element tag names.
 */
public final class SVGElementFactory {

    private SVGElementFactory() {
        // Utility class
    }

    /**
     * Creates an appropriate {@link SVGElement} subclass based on the tag name.
     *
     * @param rawTagName Raw tag name from the SVG document.
     * @return Concrete SVGElement instance.
     */
    public static SVGElement createElement(String rawTagName) {
        if (rawTagName == null || rawTagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }

        String tag = normalizeTagName(rawTagName);

        return switch (tag) {
            case SVGRoot.TAG_NAME -> new SVGRoot();
            case SVGRect.TAG_NAME -> new SVGRect();
            case SVGCircle.TAG_NAME -> new SVGCircle();
            case SVGEllipse.TAG_NAME -> new SVGEllipse();
            case SVGLine.TAG_NAME -> new SVGLine();
            case SVGPolygon.TAG_NAME -> new SVGPolygon();
            case SVGPolyline.TAG_NAME -> new SVGPolyline();
            case SVGPath.TAG_NAME -> new SVGPath();
            case SVGGroup.TAG_NAME -> new SVGGroup();
            default -> new SVGGenericElement(tag);
        };
    }

    /**
     * Creates a typed {@link SVGElement} populated with attributes from a DOM {@link Element}.
     *
     * @param domElement DOM element to extract from.
     * @return Fully initialized SVGElement instance.
     */
    public static SVGElement fromDOMElement(Element domElement) {
        if (domElement == null) {
            throw new IllegalArgumentException("DOM element cannot be null");
        }

        String tagName = domElement.getLocalName() != null ? domElement.getLocalName() : domElement.getTagName();
        SVGElement svgElement = createElement(tagName);

        // Copy all attributes from DOM
        NamedNodeMap attributes = domElement.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attr = attributes.item(i);
                svgElement.setAttribute(attr.getNodeName(), attr.getNodeValue());
            }
        }

        return svgElement;
    }

    /**
     * Normalizes a tag name by removing any namespace prefix and converting to lowercase.
     *
     * @param rawTagName Tag name (e.g. "svg:rect" or "RECT").
     * @return Normalized tag name (e.g. "rect").
     */
    public static String normalizeTagName(String rawTagName) {
        String tag = rawTagName.trim();
        int colonIdx = tag.indexOf(':');
        if (colonIdx != -1 && colonIdx < tag.length() - 1) {
            tag = tag.substring(colonIdx + 1);
        }
        return tag.toLowerCase();
    }
}

