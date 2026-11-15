package com.rrrsvg.raster;

import com.rrrsvg.SVGReader;
import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRasterizeException;
import com.rrrsvg.model.SVGDocument;
import ij.ImagePlus;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Core rasterization engine converting vector SVG documents into raster PNG images
 * using Apache Batik's {@link PNGTranscoder}.
 * <p>
 * Supports user-specified output dimensions, resolutions (DPI), background colors,
 * integrates with Module 1 ({@link SVGReader}) for prior validation, and provides
 * seamless conversion into ImageJ {@link ImagePlus} structures.
 * </p>
 */
public class SVGRasterizer {

    private final SVGReader svgReader;

    public SVGRasterizer() {
        this.svgReader = new SVGReader();
    }

    /**
     * Rasterizes an SVG file directly into a PNG file on disk.
     *
     * @param svgFile    Input SVG vector file.
     * @param outputFile Output destination PNG file.
     * @param options    Rasterization settings (dimensions, DPI, background).
     * @return The created PNG file.
     * @throws SVGParseException     If Module 1 detects an invalid SVG document.
     * @throws SVGRasterizeException If rasterization parameters are invalid or transcoding fails.
     */
    public File rasterizeToFile(File svgFile, File outputFile, RasterOptions options)
            throws SVGParseException, SVGRasterizeException {
        if (svgFile == null) {
            throw new SVGRasterizeException("Input SVG file cannot be null");
        }
        if (outputFile == null) {
            throw new SVGRasterizeException("Output PNG file cannot be null");
        }

        // 1. Validation via Module 1
        SVGDocument doc = svgReader.read(svgFile);

        // Apply fallback dimensions if none provided
        RasterOptions effectiveOptions = (options != null) ? options : RasterOptions.fromSvg(doc.getRoot());
        effectiveOptions.validate();

        // 2. Ensure parent directories exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (OutputStream out = new FileOutputStream(outputFile)) {
            transcodeToStream(svgFile, out, effectiveOptions);
            return outputFile;
        } catch (IOException e) {
            throw new SVGRasterizeException("Failed to write output PNG file: " + e.getMessage(), outputFile.getPath(), e);
        } catch (TranscoderException e) {
            throw new SVGRasterizeException("Batik PNG transcoding failed: " + e.getMessage(), svgFile.getPath(), e);
        }
    }

    /**
     * Rasterizes an SVG file into an in-memory byte array of PNG data.
     *
     * @param svgFile Input SVG file.
     * @param options Rasterization settings.
     * @return PNG encoded byte array.
     */
    public byte[] rasterizeToBytes(File svgFile, RasterOptions options)
            throws SVGParseException, SVGRasterizeException {
        if (svgFile == null) {
            throw new SVGRasterizeException("Input SVG file cannot be null");
        }

        SVGDocument doc = svgReader.read(svgFile);
        RasterOptions effectiveOptions = (options != null) ? options : RasterOptions.fromSvg(doc.getRoot());
        effectiveOptions.validate();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            transcodeToStream(svgFile, baos, effectiveOptions);
            return baos.toByteArray();
        } catch (TranscoderException e) {
            throw new SVGRasterizeException("Batik PNG transcoding failed: " + e.getMessage(), svgFile.getPath(), e);
        }
    }

    /**
     * Rasterizes an SVG file and loads the result as a standard {@link BufferedImage}.
     *
     * @param svgFile Input SVG file.
     * @param options Rasterization settings.
     * @return Rasterized BufferedImage.
     */
    public BufferedImage rasterizeToImage(File svgFile, RasterOptions options)
            throws SVGParseException, SVGRasterizeException {
        byte[] pngBytes = rasterizeToBytes(svgFile, options);
        try (InputStream in = new ByteArrayInputStream(pngBytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new SVGRasterizeException("Failed to decode generated PNG bytes into BufferedImage", svgFile.getPath());
            }
            return image;
        } catch (IOException e) {
            throw new SVGRasterizeException("I/O error decoding PNG image: " + e.getMessage(), svgFile.getPath(), e);
        }
    }

    /**
     * Rasterizes an SVG file and loads it into an ImageJ {@link ImagePlus} container
     * for immediate pixel and histogram analysis.
     *
     * @param svgFile Input SVG file.
     * @param options Rasterization settings.
     * @return ImageJ ImagePlus wrapping the rasterized PNG.
     */
    public ImagePlus rasterizeToImagePlus(File svgFile, RasterOptions options)
            throws SVGParseException, SVGRasterizeException {
        BufferedImage image = rasterizeToImage(svgFile, options);
        String title = svgFile.getName().replaceAll("\\.svg[z]?$", ".png");
        return new ImagePlus(title, image);
    }

    /**
     * Executes Batik PNGTranscoder against the input file, writing to the given output stream.
     */
    private void transcodeToStream(File svgFile, OutputStream outputStream, RasterOptions options)
            throws TranscoderException {
        PNGTranscoder transcoder = new PNGTranscoder();

        // 1. Target pixel dimensions
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float) options.getWidth());
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, (float) options.getHeight());

        // 2. Resolution (DPI / pixel unit scale)
        transcoder.addTranscodingHint(
                SVGAbstractTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER,
                options.getMillimetersPerPixel()
        );

        // 3. Background color
        if (options.getBackgroundColor() != null) {
            transcoder.addTranscodingHint(
                    ImageTranscoder.KEY_BACKGROUND_COLOR,
                    options.getBackgroundColor()
            );
        }

        TranscoderInput input = new TranscoderInput(svgFile.getAbsoluteFile().toURI().toString());
        TranscoderOutput output = new TranscoderOutput(outputStream);

        transcoder.transcode(input, output);
    }

    /**
     * Demonstrates and explains how continuous vector shapes are converted into discrete raster pixels.
     *
     * @param svgFile Input SVG file.
     * @param options Target raster options.
     * @return Human-readable technical explanation of the vector-to-raster transformation.
     */
    public static String explainVectorToRasterConversion(File svgFile, RasterOptions options) {
        StringBuilder sb = new StringBuilder();
        int totalPixels = options.getWidth() * options.getHeight();
        sb.append("================================================================================\n");
        sb.append("       DEMONSTRATION: SVG VECTOR GRAPHICS TO RASTER PIXEL CONVERSION             \n");
        sb.append("================================================================================\n\n");
        sb.append("1. VECTOR INPUT STAGE (CONTINUOUS DOMAIN):\n");
        sb.append("   - Source File     : ").append(svgFile.getName()).append("\n");
        sb.append("   - Representation  : Mathematical primitives (lines, Bézier curves, coordinate points)\n");
        sb.append("   - Precision       : Resolution-independent, infinite continuous coordinates\n\n");
        sb.append("2. DISCRETIZATION & SAMPLING (RASTER GRID):\n");
        sb.append("   - Target Grid     : ").append(options.getWidth()).append(" columns × ")
          .append(options.getHeight()).append(" rows\n");
        sb.append("   - Total Pixels    : ").append(String.format("%,d pixels", totalPixels)).append("\n");
        sb.append("   - Resolution      : ").append(options.getDpi()).append(" DPI (")
          .append(String.format("%.4f mm/pixel", options.getMillimetersPerPixel())).append(")\n\n");
        sb.append("3. SCANLINE CONVERSION & ANTI-ALIASING:\n");
        sb.append("   - Boundary Eval   : Vector boundaries intersect discrete pixel coordinate centers (X, Y)\n");
        sb.append("   - Sub-pixel Area  : Edge pixels compute fractional area coverage (alpha blending)\n");
        sb.append("   - Color Quantize  : Each pixel receives 32-bit ARGB values (8-bit Red, Green, Blue, Alpha)\n");
        sb.append("   - Output Format   : Lossless PNG raster bitmap (ready for ImageJ analysis)\n");
        sb.append("================================================================================\n");
        return sb.toString();
    }
}

