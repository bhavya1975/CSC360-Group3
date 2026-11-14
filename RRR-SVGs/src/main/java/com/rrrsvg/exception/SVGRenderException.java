package com.rrrsvg.exception;

/**
 * Custom exception thrown when an SVG cannot be rendered, transformed,
 * or displayed by the rendering pipeline.
 * <p>
 * Encapsulates the source file/URI, human-readable error explanation,
 * and underlying cause exception.
 * </p>
 */
public class SVGRenderException extends Exception {

    private final String source;

    /**
     * Constructs a new SVGRenderException with a descriptive message.
     *
     * @param message Detailed error explanation.
     */
    public SVGRenderException(String message) {
        super(message);
        this.source = null;
    }

    /**
     * Constructs a new SVGRenderException with a message and source identifier.
     *
     * @param message Detailed error explanation.
     * @param source  Path or URI of the SVG being rendered.
     */
    public SVGRenderException(String message, String source) {
        super(source != null ? message + " [Source: " + source + "]" : message);
        this.source = source;
    }

    /**
     * Constructs a new SVGRenderException with a message and underlying cause.
     *
     * @param message Detailed error explanation.
     * @param cause   Root cause exception.
     */
    public SVGRenderException(String message, Throwable cause) {
        super(message, cause);
        this.source = null;
    }

    /**
     * Constructs a new SVGRenderException with message, source, and cause.
     *
     * @param message Detailed error explanation.
     * @param source  Path or URI of the SVG being rendered.
     * @param cause   Root cause exception.
     */
    public SVGRenderException(String message, String source, Throwable cause) {
        super(source != null ? message + " [Source: " + source + "]" : message, cause);
        this.source = source;
    }

    /**
     * Returns the source file path or URI that caused the rendering failure.
     *
     * @return Source name or path, or null if unknown.
     */
    public String getSource() {
        return source;
    }
}

