package com.rrrsvg;

import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.model.SVGDocument;
import com.rrrsvg.parser.SVGParser;

import java.io.File;
import java.io.InputStream;

/**
 * Main entry point and facade for Module 1: SVG Reading.
 * <p>
 * Provides high-level APIs to read and parse SVG files into a hierarchical
 * {@link SVGDocument} object model using the standard Java DOM parser.
 * </p>
 */
public class SVGReader {

    private final SVGParser parser;

    /**
     * Constructs a new SVGReader instance.
     */
    public SVGReader() {
        this.parser = new SVGParser();
    }

    /**
     * Reads and parses an SVG file from the filesystem.
     *
     * @param file SVG file to read.
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If the file is missing, empty, invalid XML, or not an SVG.
     */
    public SVGDocument read(File file) throws SVGParseException {
        return parser.parse(file);
    }

    /**
     * Reads and parses an SVG file given its path string.
     *
     * @param filePath Path to the SVG file.
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If reading or parsing fails.
     */
    public SVGDocument read(String filePath) throws SVGParseException {
        return parser.parse(filePath);
    }

    /**
     * Reads and parses an SVG document from an InputStream.
     *
     * @param in         Input stream containing SVG XML data.
     * @param sourceName Source identifier for error reporting.
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If parsing fails.
     */
    public SVGDocument read(InputStream in, String sourceName) throws SVGParseException {
        return parser.parse(in, sourceName);
    }

    /**
     * Reads and parses an SVG document from an XML string.
     *
     * @param svgContent Raw SVG string content.
     * @return Parsed {@link SVGDocument}.
     * @throws SVGParseException If parsing fails.
     */
    public SVGDocument readFromString(String svgContent) throws SVGParseException {
        return parser.parseString(svgContent);
    }

    // Static convenience methods

    /**
     * Static utility to read an SVG file from a {@link File} object.
     */
    public static SVGDocument readFile(File file) throws SVGParseException {
        return new SVGReader().read(file);
    }

    /**
     * Static utility to read an SVG file from a path string.
     */
    public static SVGDocument readFile(String filePath) throws SVGParseException {
        return new SVGReader().read(filePath);
    }

    /**
     * Static utility to parse an SVG document directly from XML string content.
     */
    public static SVGDocument readText(String svgContent) throws SVGParseException {
        return new SVGReader().readFromString(svgContent);
    }

    /**
     * Command-line demonstration of Module 1: SVG Reading.
     * Parses the specified SVG file (or default sample file) and prints
     * its hierarchical structure, attributes, and breakdown.
     * Also showcases graceful error handling for invalid files.
     *
     * @param args Optional command-line arguments (args[0] = path to SVG file).
     */
    public static void main(String[] args) {
        String targetPath = (args.length > 0 && args[0] != null && !args[0].isEmpty())
                ? args[0]
                : "src/main/resources/sample.svg";

        System.out.println("================================================================================");
        System.out.println("                     RRR-SVGs - MODULE 1: SVG READING                           ");
        System.out.println("================================================================================\n");

        SVGReader reader = new SVGReader();

        // 1. Demonstrate reading and displaying a valid SVG file
        System.out.println("--> Reading SVG file: " + targetPath + "\n");
        try {
            File targetFile = new File(targetPath);
            // Fallback for execution from root directory CSC360-Group3. vs rrr-svgs subdirectory
            if (!targetFile.exists() && new File("rrr-svgs/" + targetPath).exists()) {
                targetFile = new File("rrr-svgs/" + targetPath);
            } else if (!targetFile.exists() && new File("RRR-SVGs/" + targetPath).exists()) {
                targetFile = new File("RRR-SVGs/" + targetPath);
            }

            SVGDocument document = reader.read(targetFile);

            // Print the hierarchical structure and attributes in a readable format
            document.printTree();

            System.out.println("\n[SUCCESS] SVG file parsed and structure extracted successfully!");

        } catch (SVGParseException e) {
            System.err.println("[ERROR] Failed to parse SVG file: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("        Root Cause: " + e.getCause().getMessage());
            }
        }

        // 2. Demonstrate graceful error handling with invalid/missing inputs
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("--> Demonstrating graceful error handling for invalid files:");
        System.out.println("--------------------------------------------------------------------------------");

        // Scenario A: Non-existent file
        demonstrateErrorHandling(reader, "src/main/resources/does_not_exist.svg", "Non-existent file test");

        // Scenario B: Malformed XML
        demonstrateInlineErrorHandling(reader, "<svg><rect x='10' y='20' width='50'</svg>", "Malformed XML test");

        // Scenario C: Non-SVG XML document
        demonstrateInlineErrorHandling(reader, "<html><head><title>Test</title></head><body>Not an SVG</body></html>", "Non-SVG root element test");

        System.out.println("\n================================================================================");
        System.out.println("                     SVG Reading Module Completed                               ");
        System.out.println("================================================================================");
    }

    private static void demonstrateErrorHandling(SVGReader reader, String path, String testLabel) {
        System.out.println("\nTest: " + testLabel);
        try {
            reader.read(path);
            System.out.println("  Unexpected: parsed without error!");
        } catch (SVGParseException e) {
            System.out.println("  Handled Gracefully -> " + e.getMessage());
        }
    }

    private static void demonstrateInlineErrorHandling(SVGReader reader, String content, String testLabel) {
        System.out.println("\nTest: " + testLabel);
        try {
            reader.readFromString(content);
            System.out.println("  Unexpected: parsed without error!");
        } catch (SVGParseException e) {
            System.out.println("  Handled Gracefully -> " + e.getMessage());
        }
    }
}
