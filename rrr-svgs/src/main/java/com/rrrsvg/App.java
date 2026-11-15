package com.rrrsvg;

import com.rrrsvg.raster.RasterizerCliDemo;
import com.rrrsvg.ui.SVGViewerFrame;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.File;

/**
 * Main application launcher for RRR-SVGs (Reading, Rendering, and Rasterization).
 * <p>
 * Supports:
 * <ul>
 *   <li>Swing GUI Mode (default): Full vector viewer, DOM inspector, and raster/ImageJ analysis</li>
 *   <li>CLI Reader Mode ({@code --cli}): Module 1 DOM structure extraction</li>
 *   <li>CLI Rasterizer Mode ({@code --raster}): Module 3 PNG rasterization & ImageJ analysis</li>
 * </ul>
 * </p>
 */
public class App {

    public static void main(String[] args) {
        boolean cliMode = false;
        boolean rasterMode = false;
        String filePath = null;

        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "--reader".equalsIgnoreCase(arg)) {
                cliMode = true;
            } else if ("--raster".equalsIgnoreCase(arg) || "--rasterize".equalsIgnoreCase(arg)) {
                rasterMode = true;
            } else if (!arg.startsWith("--") && filePath == null) {
                filePath = arg;
            }
        }

        // CLI Mode for Module 3 (Rasterization & ImageJ Analysis)
        if (rasterMode) {
            RasterizerCliDemo.main(filePath != null ? new String[]{filePath} : new String[0]);
            return;
        }

        // CLI Mode for Module 1 (SVG Reading & Structure Extraction)
        if (GraphicsEnvironment.isHeadless() || cliMode) {
            System.out.println("Running RRR-SVGs in CLI Mode (Module 1 Reader)...");
            SVGReader.main(filePath != null ? new String[]{filePath} : new String[0]);
            return;
        }

        // Unified GUI Mode: Launch Swing Viewer (Modules 1, 2, and 3)
        final File initialFile;
        if (filePath != null) {
            initialFile = new File(filePath);
        } else {
            File sample = new File("src/main/resources/sample.svg");
            if (!sample.exists()) {
                sample = new File("rrr-svgs/src/main/resources/sample.svg");
            }
            if (!sample.exists()) {
                sample = new File("RRR-SVGs/src/main/resources/sample.svg");
            }
            initialFile = sample.exists() ? sample : null;
        }

        System.out.println("Launching RRR-SVGs Unified Desktop GUI (Modules 1, 2 & 3)...");
        SwingUtilities.invokeLater(() -> {
            SVGViewerFrame viewer = SVGViewerFrame.launch(initialFile);
            if (initialFile != null) {
                System.out.println("Loaded initial file: " + initialFile.getPath());
            }
        });
    }
}
