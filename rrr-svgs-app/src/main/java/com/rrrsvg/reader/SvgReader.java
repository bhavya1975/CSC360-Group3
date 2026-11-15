package com.rrrsvg.reader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Stage 1 — Reading.
 * Parses an SVG file with Java's XML parser and turns it into a tree of {@link SvgNode}s.
 */
public class SvgReader {

    public SvgNode read(File file) throws SvgParseException {
        try (InputStream in = new FileInputStream(file)) {
            return read(in);
        } catch (IOException e) {
            throw new SvgParseException("Could not open file: " + e.getMessage());
        }
    }

    public SvgNode readString(String svgText) throws SvgParseException {
        return read(new ByteArrayInputStream(svgText.getBytes(StandardCharsets.UTF_8)));
    }

    public SvgNode read(InputStream in) throws SvgParseException {
        Element root;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // do not download DTDs referenced by the file
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler()); // throw on errors instead of printing them
            root = builder.parse(in).getDocumentElement();
        } catch (SAXParseException e) {
            throw new SvgParseException("Invalid XML at line " + e.getLineNumber() + ": " + e.getMessage());
        } catch (Exception e) {
            throw new SvgParseException("Could not read SVG: " + e.getMessage());
        }

        if (!"svg".equals(localName(root))) {
            throw new SvgParseException("Not an SVG file: the root element is <" + root.getTagName() + ">");
        }
        return toNode(root);
    }

    /** Converts a DOM element and all its children into our own simple tree. */
    private SvgNode toNode(Element element) {
        Map<String, String> attributes = new LinkedHashMap<>();
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            attributes.put(localName(attr), attr.getNodeValue());
        }

        List<SvgNode> children = new ArrayList<>();
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i) instanceof Element child) {
                children.add(toNode(child));
            }
        }
        return new SvgNode(localName(element), attributes, children);
    }

    private static String localName(Node node) {
        return node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
    }
}
