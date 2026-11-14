package com.rrrsvg.parser;

import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.model.SVGDocument;
import com.rrrsvg.model.SVGElement;
import com.rrrsvg.model.SVGElementFactory;
import com.rrrsvg.model.SVGRoot;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Core SVG parser implementing DOM-based reading and extraction.
 * <p>
 * Uses the Java DOM Parser ({@link DocumentBuilderFactory}) to parse SVG files,
 * validates document structure, and builds a rich, typed, hierarchical tree
 * of {@link SVGElement} objects.
 * </p>
 */
public class SVGParser {

    private final DocumentBuilderFactory factory;

    /**
     * Initializes the SVGParser and configures the underlying DocumentBuilderFactory
     * with security protections and namespace awareness.
     */
    public SVGParser() {
        this.factory = DocumentBuilderFactory.newInstance();
        configureFactory(this.factory);
    }

    /**
     * Configures the DOM factory for secure and robust XML parsing.
     * Disables external DTD resolution to prevent network delays and XXE attacks.
     */
    private void configureFactory(DocumentBuilderFactory dbf) {
        dbf.setNamespaceAware(true);
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
        } catch (ParserConfigurationException | AbstractMethodError ignored) {
            // Some parsers might not support specific features; proceed safely
        }
    }

    /**
     * Parses an SVG file from the file system.
     *
     * @param file The SVG file to parse.
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If the file is missing, empty, malformed, or not an SVG.
     */
    public SVGDocument parse(File file) throws SVGParseException {
        if (file == null) {
            throw new SVGParseException("File argument cannot be null");
        }
        if (!file.exists()) {
            throw new SVGParseException("SVG file not found: " + file.getPath(), file.getPath());
        }
        if (file.isDirectory()) {
            throw new SVGParseException("Expected a file but found a directory: " + file.getPath(), file.getPath());
        }
        if (file.length() == 0) {
            throw new SVGParseException("SVG file is empty: " + file.getPath(), file.getPath());
        }

        try (InputStream in = new FileInputStream(file)) {
            return parse(in, file.getPath());
        } catch (IOException e) {
            throw new SVGParseException("Failed to read SVG file: " + e.getMessage(), file.getPath(), e);
        }
    }

    /**
     * Parses an SVG file given its file path.
     *
     * @param filePath Path to the SVG file.
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If parsing fails.
     */
    public SVGDocument parse(String filePath) throws SVGParseException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SVGParseException("File path cannot be null or empty");
        }
        return parse(new File(filePath));
    }

    /**
     * Parses an SVG document from a raw XML string.
     *
     * @param svgContent Raw SVG XML text.
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If content is empty or malformed.
     */
    public SVGDocument parseString(String svgContent) throws SVGParseException {
        if (svgContent == null || svgContent.trim().isEmpty()) {
            throw new SVGParseException("SVG content string cannot be null or empty", "inline-string");
        }
        byte[] bytes = svgContent.getBytes(StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return parse(in, "inline-string");
        } catch (IOException e) {
            throw new SVGParseException("Failed to read inline SVG: " + e.getMessage(), "inline-string", e);
        }
    }

    /**
     * Parses an SVG document from an {@link InputStream}.
     *
     * @param inputStream Source stream.
     * @param sourceName  Identifier for the source (for error reporting).
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If the input stream is invalid, malformed, or not an SVG document.
     */
    public SVGDocument parse(InputStream inputStream, String sourceName) throws SVGParseException {
        if (inputStream == null) {
            throw new SVGParseException("InputStream cannot be null", sourceName);
        }

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Set strict error handler to capture XML parsing errors cleanly
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    // Suppress or record warning
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });

            // Parse XML stream into DOM tree
            Document doc = builder.parse(inputStream);

            // Validate root element
            Element domRoot = doc.getDocumentElement();
            if (domRoot == null) {
                throw new SVGParseException("Invalid SVG document: Document contains no root element", sourceName);
            }

            String rootTag = SVGElementFactory.normalizeTagName(
                    domRoot.getLocalName() != null ? domRoot.getLocalName() : domRoot.getTagName()
            );

            if (!SVGRoot.TAG_NAME.equalsIgnoreCase(rootTag)) {
                throw new SVGParseException(
                        "Invalid SVG document: Expected root element '<svg>', but found '<" + domRoot.getTagName() + ">'",
                        sourceName
                );
            }

            // Build hierarchical SVGElement tree recursively
            SVGElement rootElement = buildElementTree(domRoot);
            if (!(rootElement instanceof SVGRoot svgRoot)) {
                throw new SVGParseException("Failed to instantiate SVGRoot element", sourceName);
            }

            return new SVGDocument(svgRoot, sourceName);

        } catch (ParserConfigurationException e) {
            throw new SVGParseException("XML parser configuration error: " + e.getMessage(), sourceName, e);
        } catch (SAXParseException e) {
            throw new SVGParseException(
                    "XML syntax error at line " + e.getLineNumber() + ", column " + e.getColumnNumber() + ": " + e.getMessage(),
                    sourceName,
                    e
            );
        } catch (SAXException e) {
            throw new SVGParseException("Malformed XML in SVG: " + e.getMessage(), sourceName, e);
        } catch (IOException e) {
            throw new SVGParseException("I/O error while reading SVG stream: " + e.getMessage(), sourceName, e);
        }
    }

    /**
     * Recursively traverses DOM Nodes, extracting elements and attributes,
     * and building the typed {@link SVGElement} tree structure.
     *
     * @param domElement The DOM element to convert.
     * @return The corresponding SVGElement with all children populated.
     */
    private SVGElement buildElementTree(Element domElement) {
        SVGElement svgElement = SVGElementFactory.fromDOMElement(domElement);

        NodeList childNodes = domElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childDomElement = (Element) childNode;
                SVGElement childSvgElement = buildElementTree(childDomElement);
                svgElement.addChild(childSvgElement);
            }
        }

        return svgElement;
    }
}

