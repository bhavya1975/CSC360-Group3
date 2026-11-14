package com.rrrsvg.exception;

/**
 * Custom exception thrown when SVG rasterization or PNG generation fails.
 * <p>
 * Encapsulates error descriptions, source file paths, and underlying causes
 * (such as transcoding exceptions, I/O failures, or invalid dimensions).
 * </p>
 */
public class SVGRasterizeException extends Exception {

    private final String source;

    public SVGRasterizeException(String message) {
        super(message);
        this.source = null;
    }

    public SVGRasterizeException(String message, String source) {
        super(source != null ? message + " [Source: " + source + "]" : message);
        this.source = source;
    }

    public SVGRasterizeException(String message, Throwable cause) {
        super(message, cause);
        this.source = null;
    }

    public SVGRasterizeException(String message, String source, Throwable cause) {
        super(source != null ? message + " [Source: " + source + "]" : message, cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}

