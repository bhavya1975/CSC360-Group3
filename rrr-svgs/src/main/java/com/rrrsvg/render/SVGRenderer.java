package com.rrrsvg.render;

import com.rrrsvg.SVGReader;
import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRenderException;
import com.rrrsvg.model.SVGDocument;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Core rendering service utilizing Apache Batik to convert SVG vector elements,
 * paths, colours, transformations, and viewBox attributes into a visual representation.
 * <p>
 * Integrates directly with Module 1 ({@link SVGReader}) to ensure files are valid
 * and structured before rendering, preventing corrupt files from reaching the Batik engine.
 * </p>
 */
public class SVGRenderer {

    private final SVGReader svgReader;

    /**
     * Constructs a new SVGRenderer instance with default configuration.
     */
    public SVGRenderer() {
        this.svgReader = new SVGReader();
    }

    /**
     * Validates and reads an SVG file using Module 1 before rendering.
     *
     * @param file Target SVG file.
     * @return Extracted {@link SVGDocument} from Module 1.
     * @throws SVGParseException If the SVG file is missing, empty, or malformed.
     */
    public SVGDocument validateAndRead(File file) throws SVGParseException {
        return svgReader.read(file);
    }

    /**
     * Builds an Apache Batik {@link GraphicsNode} vector tree from a given SVG file.
     *
     * @param file SVG file to process.
     * @return The root GraphicsNode ready for vector painting.
     * @throws SVGParseException   If file validation via Module 1 fails.
     * @throws SVGRenderException If Batik fails to construct the graphics vector tree.
     */
    public GraphicsNode buildGraphicsNode(File file) throws SVGParseException, SVGRenderException {
        if (file == null) {
            throw new SVGRenderException("SVG file argument cannot be null");
        }

        // 1. Module 1 validation: ensure valid SVG syntax & structure
        validateAndRead(file);

        try {
            String xmlParser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(xmlParser);
            org.w3c.dom.svg.SVGDocument batikDoc = factory.createSVGDocument(file.toURI().toString());

            UserAgent userAgent = new UserAgentAdapter();
            DocumentLoader loader = new DocumentLoader(userAgent);
            BridgeContext bridgeContext = new BridgeContext(userAgent, loader);
            bridgeContext.setDynamicState(BridgeContext.STATIC);

            GVTBuilder builder = new GVTBuilder();
            GraphicsNode rootNode = builder.build(bridgeContext, batikDoc);

            if (rootNode == null) {
                throw new SVGRenderException("GVTBuilder produced a null GraphicsNode", file.getPath());
            }

            return rootNode;

        } catch (IOException e) {
            throw new SVGRenderException("I/O error loading SVG for rendering: " + e.getMessage(), file.getPath(), e);
        } catch (Exception e) {
            throw new SVGRenderException("Failed to construct SVG vector tree: " + e.getMessage(), file.getPath(), e);
        }
    }

    /**
     * Renders an SVG file directly onto a {@link Graphics2D} canvas scaled to fit the given dimensions,
     * maintaining vector quality, aspect ratio, and applying high-fidelity anti-aliasing hints.
     *
     * @param file   SVG file to render.
     * @param g2d    Target Graphics2D context.
     * @param width  Target drawing area width.
     * @param height Target drawing area height.
     * @throws SVGParseException   If file validation fails.
     * @throws SVGRenderException If rendering fails.
     */
    public void renderToGraphics(File file, Graphics2D g2d, int width, int height)
            throws SVGParseException, SVGRenderException {
        if (g2d == null) {
            throw new SVGRenderException("Graphics2D context cannot be null");
        }
        if (width <= 0 || height <= 0) {
            return;
        }

        // Apply high-quality rendering hints for antialiasing & subpixel accuracy
        applyQualityRenderingHints(g2d);

        GraphicsNode rootNode = buildGraphicsNode(file);

        // Calculate scaling to preserve aspect ratio
        var bounds = rootNode.getBounds();
        if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
            double scaleX = (double) width / bounds.getWidth();
            double scaleY = (double) height / bounds.getHeight();
            double scale = Math.min(scaleX, scaleY);

            double offsetX = (width - bounds.getWidth() * scale) / 2.0;
            double offsetY = (height - bounds.getHeight() * scale) / 2.0;

            AffineTransform originalTransform = g2d.getTransform();
            AffineTransform transform = new AffineTransform();
            transform.translate(offsetX, offsetY);
            transform.scale(scale, scale);
            transform.translate(-bounds.getX(), -bounds.getY());

            g2d.transform(transform);
            rootNode.paint(g2d);
            g2d.setTransform(originalTransform);
        } else {
            rootNode.paint(g2d);
        }
    }

    /**
     * Applies standard vector rendering quality hints to the given Graphics2D context.
     *
     * @param g2d Target graphics context.
     */
    public static void applyQualityRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }
}

