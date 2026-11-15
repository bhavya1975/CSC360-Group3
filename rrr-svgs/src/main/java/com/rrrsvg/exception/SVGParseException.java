package com.rrrsvg.exception;

/**
 * Custom exception thrown when an SVG file cannot be read, parsed,
 * or when its content is not a valid SVG document.
 * <p>
 * Provides descriptive error details including the source file/resource
 * and underlying XML parsing or I/O causes.
 * </p>
 */
public class SVGParseException extends Exception {

    private final String source;

    /**
     * Constructs a new SVGParseException with a descriptive error message.
     *
     * @param message Detailed error explanation.
     */
    public SVGParseException(String message) {
        super(message);
        this.source = null;
    }

    /**
     * Constructs a new SVGParseException with a message and source location.
     *
     * @param message Detailed error explanation.
     * @param source  Path or name of the SVG source that caused the error.
     */
    public SVGParseException(String message, String source) {
        super(source != null ? message + " [Source: " + source + "]" : message);
        this.source = source;
    }

    /**
     * Constructs a new SVGParseException with a message and underlying cause.
     *
     * @param message Detailed error explanation.
     * @param cause   Underlying root cause exception.
     */
    public SVGParseException(String message, Throwable cause) {
        super(message, cause);
        this.source = null;
    }

    /**
     * Constructs a new SVGParseException with message, source, and cause.
     *
     * @param message Detailed error explanation.
     * @param source  Path or name of the SVG source that caused the error.
     * @param cause   Underlying root cause exception.
     */
    public SVGParseException(String message, String source, Throwable cause) {
        super(source != null ? message + " [Source: " + source + "]" : message, cause);
        this.source = source;
    }

    /**
     * Returns the source file path or identifier that triggered the error, if known.
     *
     * @return Source name/path, or null if unspecified.
     */
    public String getSource() {
        return source;
    }
}

