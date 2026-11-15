package com.rrrsvg.ui;

import com.rrrsvg.analysis.PixelAnalyzer;
import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRasterizeException;
import com.rrrsvg.raster.RasterOptions;
import com.rrrsvg.raster.SVGRasterizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Side-by-side visual comparison component directly juxtaposing the live
 * vector SVG preview (Batik JSVGCanvas) and the rasterized PNG (Batik PNGTranscoder).
 * <p>
 * Ensures and verifies that:
 * <ul>
 *   <li>The PNG is generated from the exact SVG file loaded in the application</li>
 *   <li>Both SVG preview and PNG share identical source files and native dimensions</li>
 *   <li>Vector paths, colours, gradients, and transforms are visually verified side-by-side</li>
 * </ul>
 * </p>
 */
public class ComparisonViewPanel extends JPanel {

    private final SVGRenderCanvas svgCanvas = new SVGRenderCanvas();
    private final PngDisplayCanvas pngCanvas = new PngDisplayCanvas();
    private final SVGRasterizer rasterizer = new SVGRasterizer();

    private final JLabel sourceLabel = new JLabel(" Source: None ");
    private final JLabel svgDimLabel = new JLabel(" SVG: - × - ");
    private final JLabel pngDimLabel = new JLabel(" PNG: - × - ");
    private final JLabel consistencyBadge = new JLabel(" Status: Waiting ");

    private final JComboBox<String> dpiCombo = new JComboBox<>(new String[]{"96 DPI (Standard)", "150 DPI", "300 DPI", "72 DPI"});
    private final JComboBox<String> bgCombo = new JComboBox<>(new String[]{"Transparent BG", "White BG"});

    private File currentSvgFile;
    private BufferedImage currentPngImage;
    private PixelAnalyzer currentAnalyzer;

    public ComparisonViewPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        // Top Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        sourceLabel.setFont(sourceLabel.getFont().deriveFont(Font.BOLD, 12f));
        consistencyBadge.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
        consistencyBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(39, 174, 96)),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        consistencyBadge.setForeground(new Color(39, 174, 96));

        JButton reRasterizeBtn = new JButton("Re-Rasterize & Refresh");
        reRasterizeBtn.setToolTipText("Re-generate PNG from currently loaded SVG using Batik Transcoder");
        reRasterizeBtn.addActionListener(e -> reRasterize());

        JButton analyzeBtn = new JButton("ImageJ Pixel Analysis");
        analyzeBtn.setToolTipText("Open detailed pixel & histogram analyzer on this PNG");
        analyzeBtn.addActionListener(e -> openAnalyzerDialog());

        toolBar.add(sourceLabel);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(svgDimLabel);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(pngDimLabel);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(consistencyBadge);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(new JLabel("DPI: "));
        toolBar.add(dpiCombo);
        toolBar.add(Box.createHorizontalStrut(6));
        toolBar.add(bgCombo);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(reRasterizeBtn);
        toolBar.addSeparator();
        toolBar.add(analyzeBtn);

        // Center: Side-by-Side Split Pane
        JPanel leftContainer = new JPanel(new BorderLayout());
        leftContainer.setBorder(BorderFactory.createTitledBorder("SVG Vector Preview (Apache Batik JSVGCanvas)"));
        leftContainer.add(svgCanvas, BorderLayout.CENTER);

        JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.setBorder(BorderFactory.createTitledBorder("Rasterized PNG (Apache Batik PNGTranscoder)"));
        JScrollPane pngScroll = new JScrollPane(pngCanvas);
        rightContainer.add(pngScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftContainer, rightContainer);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(520);

        add(toolBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Loads an SVG file, rendering the live vector preview on the left
     * and generating an identical-dimension PNG on the right.
     *
     * @param svgFile The SVG file currently active in the application.
     */
    public void loadSvg(File svgFile) {
        if (svgFile == null || !svgFile.exists()) {
            return;
        }
        this.currentSvgFile = svgFile.getAbsoluteFile();

        try {
            // 1. Load vector preview
            svgCanvas.loadSVGFile(this.currentSvgFile);

            // 2. Generate and display matching PNG
            reRasterize();

        } catch (SVGParseException e) {
            consistencyBadge.setText(" Error: " + e.getMessage() + " ");
            consistencyBadge.setForeground(Color.RED);
        }
    }

    /**
     * Re-rasterizes the currently loaded SVG using Apache Batik's SVG transcoder
     * with matching native dimensions and the selected resolution and background.
     */
    public void reRasterize() {
        if (currentSvgFile == null || !currentSvgFile.exists()) {
            return;
        }

        try {
            // Ensure identical source file and dimensions
            Dimension nativeDim = svgCanvas.getNativeSvgDimensions();
            double dpi = getSelectedDpi();
            Color bg = "White BG".equals(bgCombo.getSelectedItem()) ? Color.WHITE : null;

            RasterOptions options = new RasterOptions(nativeDim.width, nativeDim.height, dpi, bg);

            // Rasterize to in-memory BufferedImage using Batik PNGTranscoder
            BufferedImage pngImg = rasterizer.rasterizeToImage(currentSvgFile, options);
            this.currentPngImage = pngImg;
            this.currentAnalyzer = PixelAnalyzer.fromImage(pngImg, currentSvgFile.getName());

            pngCanvas.setImage(pngImg);

            // Update UI status badges
            sourceLabel.setText(" Source: " + currentSvgFile.getName() + " ");
            svgDimLabel.setText(String.format(" SVG: %d × %d px ", nativeDim.width, nativeDim.height));
            pngDimLabel.setText(String.format(" PNG: %d × %d px ", pngImg.getWidth(), pngImg.getHeight()));

            boolean dimsMatch = (nativeDim.width == pngImg.getWidth() && nativeDim.height == pngImg.getHeight());
            if (dimsMatch) {
                consistencyBadge.setText(String.format(" MATCH: Exact %d × %d px (1:1 Ratio) ", nativeDim.width, nativeDim.height));
                consistencyBadge.setForeground(new Color(39, 174, 96));
            } else {
                consistencyBadge.setText(String.format(" SCALED: SVG=%dx%d, PNG=%dx%d ", nativeDim.width, nativeDim.height, pngImg.getWidth(), pngImg.getHeight()));
                consistencyBadge.setForeground(new Color(230, 126, 34));
            }

        } catch (SVGParseException | SVGRasterizeException e) {
            consistencyBadge.setText(" Transcoder Error: " + e.getMessage() + " ");
            consistencyBadge.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "Failed to rasterize current SVG:\n" + e.getMessage(), "Rasterization Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double getSelectedDpi() {
        return switch (dpiCombo.getSelectedIndex()) {
            case 1 -> RasterOptions.MEDIUM_DPI; // 150
            case 2 -> RasterOptions.PRINT_DPI;  // 300
            case 3 -> RasterOptions.SCREEN_DPI; // 72
            default -> RasterOptions.DEFAULT_DPI; // 96
        };
    }

    private void openAnalyzerDialog() {
        if (currentAnalyzer == null) {
            JOptionPane.showMessageDialog(this, "No rasterized image available to analyze.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        var parentFrame = (javax.swing.JFrame) SwingUtilities.getWindowAncestor(this);
        RasterAnalysisDialog dialog = new RasterAnalysisDialog(parentFrame, currentAnalyzer, currentSvgFile);
        dialog.setVisible(true);
    }

    public File getCurrentSvgFile() {
        return currentSvgFile;
    }

    public BufferedImage getCurrentPngImage() {
        return currentPngImage;
    }

    /**
     * Canvas drawing the rasterized PNG with a checkerboard pattern underneath
     * to clearly show transparent regions and anti-aliased edges.
     */
    private static class PngDisplayCanvas extends JPanel {

        private BufferedImage image;

        public PngDisplayCanvas() {
            setBackground(new Color(245, 245, 247));
        }

        public void setImage(BufferedImage image) {
            this.image = image;
            if (image != null) {
                setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            }
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                g.setColor(Color.GRAY);
                g.drawString("No PNG rasterized yet", getWidth() / 2 - 60, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            int x = Math.max(0, (getWidth() - image.getWidth()) / 2);
            int y = Math.max(0, (getHeight() - image.getHeight()) / 2);

            // Draw subtle transparency checkerboard behind PNG
            int checkSize = 10;
            for (int cy = y; cy < y + image.getHeight(); cy += checkSize) {
                for (int cx = x; cx < x + image.getWidth(); cx += checkSize) {
                    int w = Math.min(checkSize, x + image.getWidth() - cx);
                    int h = Math.min(checkSize, y + image.getHeight() - cy);
                    boolean isEven = ((cx / checkSize) + (cy / checkSize)) % 2 == 0;
                    g2.setColor(isEven ? new Color(240, 240, 240) : Color.WHITE);
                    g2.fillRect(cx, cy, w, h);
                }
            }

            // Draw the actual raster PNG
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, x, y, null);

            // Outer border around image
            g2.setColor(new Color(180, 180, 180));
            g2.drawRect(x, y, image.getWidth(), image.getHeight());
        }
    }
}

