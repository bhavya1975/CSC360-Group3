package com.rrrsvg.ui;

import com.rrrsvg.SVGReader;
import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.model.SVGDocument;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;
import org.apache.batik.swing.svg.GVTTreeBuilderListener;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderListener;
import org.apache.batik.swing.svg.SVGUserAgent;
import org.apache.batik.swing.svg.SVGUserAgentGUIAdapter;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance vector rendering canvas embedding Apache Batik's {@link JSVGCanvas}.
 * <p>
 * Supports:
 * <ul>
 *   <li>Full SVG vector rendering with antialiasing and subpixel precision</li>
 *   <li>Infinite zoom in/out maintaining 100% vector sharpness</li>
 *   <li>Interactive mouse wheel zooming focused at cursor position</li>
 *   <li>Interactive canvas panning (mouse drag)</li>
 *   <li>Module 1 validation integration and graceful error reporting</li>
 * </ul>
 * </p>
 */
public class SVGRenderCanvas extends JSVGCanvas {

    public static final double DEFAULT_ZOOM_STEP = 1.25;

    public interface ZoomChangeListener {
        void onZoomChanged(double zoomFactor, int zoomPercentage);
    }

    public interface RenderStateListener {
        void onRenderStarted();
        void onRenderCompleted(long durationMs);
        void onRenderFailed(String errorMessage, Throwable cause);
    }

    private final SVGReader svgReader = new SVGReader();
    private final List<ZoomChangeListener> zoomListeners = new ArrayList<>();
    private final List<RenderStateListener> renderListeners = new ArrayList<>();

    private Point lastMousePosition;
    private long renderStartTime = 0;
    private File currentFile;
    private SVGDocument currentSvgDocument;

    /**
     * Constructs a new SVGRenderCanvas with vector interaction listeners.
     */
    public SVGRenderCanvas() {
        super(createCustomUserAgent(), true, true);
        configureCanvas();
        setupInteractionListeners();
    }

    private static SVGUserAgent createCustomUserAgent() {
        return new SVGUserAgentGUIAdapter(null) {
            @Override
            public void displayError(String message) {
                // Suppress raw default popup; error listeners handle UI notification
                System.err.println("[Batik Warning/Error]: " + message);
            }

            @Override
            public void displayError(Exception e) {
                System.err.println("[Batik Exception]: " + e.getMessage());
            }
        };
    }

    private void configureCanvas() {
        // Use dynamic document mode to support all SVG styling, transforms, and interactions
        setDocumentState(ALWAYS_DYNAMIC);
        setDoubleBufferedRendering(true);
        setProgressivePaint(true);

        // Register Batik lifecycle listeners
        addSVGDocumentLoaderListener(new SVGDocumentLoaderListener() {
            @Override
            public void documentLoadingStarted(SVGDocumentLoaderEvent e) {
                renderStartTime = System.currentTimeMillis();
                notifyRenderStarted();
            }

            @Override
            public void documentLoadingCompleted(SVGDocumentLoaderEvent e) {
                // Document DOM loaded
            }

            @Override
            public void documentLoadingCancelled(SVGDocumentLoaderEvent e) {
                notifyRenderFailed("SVG document loading was cancelled", null);
            }

            @Override
            public void documentLoadingFailed(SVGDocumentLoaderEvent e) {
                notifyRenderFailed("SVG document loading failed", null);
            }
        });

        addGVTTreeBuilderListener(new GVTTreeBuilderListener() {
            @Override
            public void gvtBuildStarted(GVTTreeBuilderEvent e) {}

            @Override
            public void gvtBuildCompleted(GVTTreeBuilderEvent e) {}

            @Override
            public void gvtBuildCancelled(GVTTreeBuilderEvent e) {
                notifyRenderFailed("GVT vector tree build was cancelled", null);
            }

            @Override
            public void gvtBuildFailed(GVTTreeBuilderEvent e) {
                notifyRenderFailed("Failed to build graphics vector tree", null);
            }
        });

        addGVTTreeRendererListener(new GVTTreeRendererListener() {
            @Override
            public void gvtRenderingPrepare(GVTTreeRendererEvent e) {}

            @Override
            public void gvtRenderingStarted(GVTTreeRendererEvent e) {}

            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                long duration = System.currentTimeMillis() - renderStartTime;
                notifyRenderCompleted(duration);
                notifyZoomChanged();
            }

            @Override
            public void gvtRenderingCancelled(GVTTreeRendererEvent e) {
                notifyRenderFailed("Vector rendering was cancelled", null);
            }

            @Override
            public void gvtRenderingFailed(GVTTreeRendererEvent e) {
                notifyRenderFailed("Vector rendering failed", null);
            }
        });
    }

    private void setupInteractionListeners() {
        // Mouse Wheel: Zoom in / Zoom out centered at cursor
        addMouseWheelListener((MouseWheelEvent e) -> {
            if (getSVGDocument() == null) {
                return;
            }
            double factor = (e.getWheelRotation() < 0) ? DEFAULT_ZOOM_STEP : (1.0 / DEFAULT_ZOOM_STEP);
            zoomAtPoint(factor, e.getPoint());
        });

        // Mouse Drag: Pan the rendered SVG
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePosition = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (getSVGDocument() == null || lastMousePosition == null) {
                    return;
                }
                int dx = e.getX() - lastMousePosition.x;
                int dy = e.getY() - lastMousePosition.y;

                panBy(dx, dy);
                lastMousePosition = e.getPoint();
            }
        });
    }

    /**
     * Loads and renders an SVG file with prior validation using Module 1's {@link SVGReader}.
     *
     * @param file SVG file to render.
     * @throws SVGParseException If Module 1 detects an invalid, empty, or missing SVG file.
     */
    public void loadSVGFile(File file) throws SVGParseException {
        if (file == null) {
            throw new SVGParseException("SVG file cannot be null");
        }

        // 1. Integration with Module 1: Read and validate SVG structure
        this.currentSvgDocument = svgReader.read(file);
        this.currentFile = file.getAbsoluteFile();

        // 2. Pass URI to Apache Batik for vector rendering
        SwingUtilities.invokeLater(() -> {
            try {
                loadSVGDocument(this.currentFile.toURI().toString());
            } catch (Exception ex) {
                notifyRenderFailed("Failed to render SVG: " + ex.getMessage(), ex);
            }
        });
    }

    /**
     * Retrieves the native dimensions of the loaded SVG document.
     * Prefers Module 1 parsed SVGRoot, falling back to Batik's computed document size if available.
     *
     * @return Native SVG Dimension (width × height).
     */
    public java.awt.Dimension getNativeSvgDimensions() {
        if (currentSvgDocument != null) {
            var root = currentSvgDocument.getRoot();
            if (root.getWidth() > 0 && root.getHeight() > 0) {
                return new java.awt.Dimension((int) Math.round(root.getWidth()), (int) Math.round(root.getHeight()));
            }
        }
        try {
            Dimension2D docSize = getSVGDocumentSize();
            if (docSize != null && docSize.getWidth() > 0 && docSize.getHeight() > 0) {
                return new java.awt.Dimension((int) Math.round(docSize.getWidth()), (int) Math.round(docSize.getHeight()));
            }
        } catch (Exception ignored) {
            // Batik BridgeContext may be null if async document loading has not finished yet
        }
        return new java.awt.Dimension(800, 600);
    }

    /**
     * Zooms the vector graphics in by the default factor (1.25x) centered at canvas midpoint.
     */
    public void zoomIn() {
        zoomCentered(DEFAULT_ZOOM_STEP);
    }

    /**
     * Zooms the vector graphics out by the default factor (0.8x) centered at canvas midpoint.
     */
    public void zoomOut() {
        zoomCentered(1.0 / DEFAULT_ZOOM_STEP);
    }

    /**
     * Resets the rendering transformation back to original 100% scale.
     */
    public void resetZoom() {
        resetRenderingTransform();
        notifyZoomChanged();
    }

    /**
     * Adjusts the rendering transform so that the SVG graphics fit within the current canvas viewport.
     */
    public void fitToWindow() {
        Dimension2D docSize = getSVGDocumentSize();
        if (docSize == null || docSize.getWidth() <= 0 || docSize.getHeight() <= 0) {
            resetZoom();
            return;
        }

        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        double scaleX = (double) viewWidth / docSize.getWidth();
        double scaleY = (double) viewHeight / docSize.getHeight();
        double scale = Math.min(scaleX, scaleY) * 0.95; // 5% margin

        double tx = (viewWidth - docSize.getWidth() * scale) / 2.0;
        double ty = (viewHeight - docSize.getHeight() * scale) / 2.0;

        AffineTransform fitTransform = new AffineTransform();
        fitTransform.translate(tx, ty);
        fitTransform.scale(scale, scale);

        setRenderingTransform(fitTransform);
        notifyZoomChanged();
    }

    /**
     * Zooms by the given factor centered around a specific canvas point.
     *
     * @param factor Zoom factor (&gt; 1 zooms in, &lt; 1 zooms out).
     * @param center Point of focus.
     */
    public void zoomAtPoint(double factor, Point2D center) {
        if (center == null) {
            center = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
        }

        AffineTransform at = getRenderingTransform();
        if (at == null) {
            at = new AffineTransform();
        }

        AffineTransform scaleTransform = AffineTransform.getTranslateInstance(center.getX(), center.getY());
        scaleTransform.scale(factor, factor);
        scaleTransform.translate(-center.getX(), -center.getY());

        at.preConcatenate(scaleTransform);
        setRenderingTransform(at);
        notifyZoomChanged();
    }

    private void zoomCentered(double factor) {
        Point2D center = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
        zoomAtPoint(factor, center);
    }

    private void panBy(double dx, double dy) {
        AffineTransform at = getRenderingTransform();
        if (at == null) {
            at = new AffineTransform();
        }
        AffineTransform translate = AffineTransform.getTranslateInstance(dx, dy);
        at.preConcatenate(translate);
        setRenderingTransform(at);
    }

    /**
     * Calculates the current zoom factor based on the rendering transform matrix.
     *
     * @return Current zoom scale multiplier (1.0 = 100%).
     */
    public double getZoomFactor() {
        AffineTransform at = getRenderingTransform();
        if (at == null) {
            return 1.0;
        }
        double det = Math.abs(at.getDeterminant());
        return Math.sqrt(det > 0 ? det : 1.0);
    }

    /**
     * Returns the zoom factor as an integer percentage (e.g. 100, 125, 200).
     */
    public int getZoomPercentage() {
        return (int) Math.round(getZoomFactor() * 100.0);
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public SVGDocument getCurrentSvgDocument() {
        return currentSvgDocument;
    }

    // Listener registration

    public void addZoomChangeListener(ZoomChangeListener listener) {
        if (listener != null) {
            zoomListeners.add(listener);
        }
    }

    public void addRenderStateListener(RenderStateListener listener) {
        if (listener != null) {
            renderListeners.add(listener);
        }
    }

    private void notifyZoomChanged() {
        double factor = getZoomFactor();
        int pct = getZoomPercentage();
        for (ZoomChangeListener listener : zoomListeners) {
            listener.onZoomChanged(factor, pct);
        }
    }

    private void notifyRenderStarted() {
        for (RenderStateListener l : renderListeners) {
            l.onRenderStarted();
        }
    }

    private void notifyRenderCompleted(long durationMs) {
        for (RenderStateListener l : renderListeners) {
            l.onRenderCompleted(durationMs);
        }
    }

    private void notifyRenderFailed(String message, Throwable cause) {
        for (RenderStateListener l : renderListeners) {
            l.onRenderFailed(message, cause);
        }
    }
}

