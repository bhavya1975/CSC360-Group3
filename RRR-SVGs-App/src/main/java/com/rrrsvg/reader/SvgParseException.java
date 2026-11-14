package com.rrrsvg.reader;

/** Thrown when a file is not a valid SVG document. */
public class SvgParseException extends Exception {

    public SvgParseException(String message) {
        super(message);
    }
}
