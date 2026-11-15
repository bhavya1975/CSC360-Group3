package com.rrrsvg.raster;

import com.rrrsvg.analysis.HistogramData;
import com.rrrsvg.analysis.PixelAnalyzer;
import com.rrrsvg.analysis.PixelData;
import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRasterizeException;

import java.io.File;

/**
 * Command-line runner and demonstration for Module 3: SVG Rasterization & ImageJ Pixel Analysis.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>SVG to PNG rasterization with custom dimensions and resolution (DPI)</li>
 *   <li>Vector-to-raster discretization explanation</li>
 *   <li>ImageJ-powered pixel coordinate inspection and RGB values</li>
 *   <li>RGB channel analysis and histogram computation</li>
 *   <li>Graceful handling of invalid inputs and dimensions</li>
 * </ul>
 * </p>
 */
public class RasterizerCliDemo {

    public static void main(String[] args) {
        String svgPath = (args.length > 0 && !args[0].isEmpty())
                ? args[0]
                : "src/main/resources/sample.svg";

        File svgFile = new File(svgPath);
        if (!svgFile.exists() && new File("rrr-svgs/" + svgPath).exists()) {
            svgFile = new File("rrr-svgs/" + svgPath);
        } else if (!svgFile.exists() && new File("RRR-SVGs/" + svgPath).exists()) {
            svgFile = new File("RRR-SVGs/" + svgPath);
        }

        System.out.println("================================================================================");
        System.out.println("       RRR-SVGs - MODULE 3: SVG RASTERIZATION & PIXEL ANALYSIS                  ");
        System.out.println("================================================================================\n");

        if (!svgFile.exists()) {
            System.err.println("[ERROR] SVG file not found: " + svgPath);
            return;
        }

        SVGRasterizer rasterizer = new SVGRasterizer();

        // 1. Configure raster options (custom dimensions & 150 DPI)
        RasterOptions options = new RasterOptions(600, 480, 150.0, null);
        File outputDir = new File("generated-png");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File pngOut = new File(outputDir, svgFile.getName().replaceAll("\\.svg[z]?$", ".png"));

        System.out.println("--> [STEP 1] Rasterizing SVG to PNG via Apache Batik...");
        System.out.println("    Input  : " + svgFile.getPath());
        System.out.println("    Output : " + pngOut.getPath());
        System.out.println("    Params : " + options);

        try {
            long startTime = System.currentTimeMillis();
            rasterizer.rasterizeToFile(svgFile, pngOut, options);
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("    [SUCCESS] Rasterized in " + duration + " ms! File size: " + pngOut.length() + " bytes\n");

            // 2. Vector-to-raster discretization demonstration
            System.out.println(SVGRasterizer.explainVectorToRasterConversion(svgFile, options));

            // 3. ImageJ Pixel and Channel Analysis
            System.out.println("--> [STEP 2] Inspecting Pixels with ImageJ (net.imagej:ij)...");
            PixelAnalyzer analyzer = PixelAnalyzer.fromFile(pngOut);
            System.out.println("    Image loaded in ImageJ: " + analyzer.getWidth() + "x" + analyzer.getHeight() + " (" +
                    analyzer.getTotalPixels() + " pixels)\n");

            // Inspect sample pixel coordinates
            int[][] sampleCoords = {
                    {50, 50},
                    {100, 100},
                    {240, 60},
                    {options.getWidth() / 2, options.getHeight() / 2}
            };

            System.out.println("--> Sample Pixel Coordinate Readouts:");
            for (int[] pt : sampleCoords) {
                PixelData pd = analyzer.getPixel(pt[0], pt[1]);
                if (pd != null) {
                    System.out.printf("    (X=%3d, Y=%3d) -> %-17s | Alpha=%3d | Luma=%5.1f | Hex=%s%n",
                            pd.getX(), pd.getY(), pd.formatRGB(), pd.getAlpha(), pd.getLuminance(), pd.getHexColor());
                }
            }

            // 4. Histogram and Channel Analysis
            System.out.println("\n--> [STEP 3] Computing 256-Bin RGB Histograms with ImageJ...");
            HistogramData hist = analyzer.computeHistogramData();
            System.out.println("    * " + hist.getRedStats().formatSummary());
            System.out.println("    * " + hist.getGreenStats().formatSummary());
            System.out.println("    * " + hist.getBlueStats().formatSummary());
            System.out.println("    * " + hist.getLuminanceStats().formatSummary());
            System.out.println("    * Highest bin frequency: " + hist.getMaxFrequency());

        } catch (SVGParseException e) {
            System.err.println("[Module 1 Validation Error]: " + e.getMessage());
        } catch (SVGRasterizeException e) {
            System.err.println("[Rasterization Error]: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Analysis Error]: " + e.getMessage());
            e.printStackTrace();
        }

        // 5. Demonstrate Graceful Error Handling
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("--> [STEP 4] Demonstrating Graceful Error Handling:");
        System.out.println("--------------------------------------------------------------------------------");

        // Error test A: Invalid negative dimensions
        try {
            System.out.println("Test A: Invalid dimensions (-100x400):");
            RasterOptions invalidOpts = new RasterOptions(-100, 400);
            rasterizer.rasterizeToFile(svgFile, pngOut, invalidOpts);
            System.out.println("  Unexpected success!");
        } catch (SVGRasterizeException e) {
            System.out.println("  Handled Gracefully -> " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  Handled -> " + e.getMessage());
        }

        // Error test B: Non-existent SVG input file
        try {
            System.out.println("\nTest B: Non-existent input SVG:");
            rasterizer.rasterizeToFile(new File("missing_file.svg"), pngOut, options);
            System.out.println("  Unexpected success!");
        } catch (SVGParseException e) {
            System.out.println("  Handled Gracefully via Module 1 -> " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  Handled -> " + e.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println("          Module 3 Rasterization & Pixel Analysis Completed                     ");
        System.out.println("================================================================================");

        System.exit(0);
    }
}
