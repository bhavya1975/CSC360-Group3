package com.rrrsvg.ui;

import com.rrrsvg.analysis.HistogramData;
import com.rrrsvg.analysis.PixelAnalyzer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Interactive ImageJ-powered analysis window for inspecting rasterized SVG images.
 * <p>
 * Displays the raster PNG, live mouse hover pixel coordinates and RGB values,
 * a pixel magnifier grid demonstrating discretization/anti-aliasing, and
 * full 256-bin RGB + Luminance histograms.
 * </p>
 */
public class RasterAnalysisDialog extends JDialog {

    private final PixelAnalyzer analyzer;
    private final File sourceImageFile;

    private final PixelInspectorPanel pixelInspector = new PixelInspectorPanel();
    private final HistogramViewPanel histogramView = new HistogramViewPanel();
    private final ImageCanvas imageCanvas;

    public RasterAnalysisDialog(JFrame parent, PixelAnalyzer analyzer, File sourceImageFile) {
        super(parent, "Module 3: SVG Rasterization & ImageJ Pixel Analysis", false);
        this.analyzer = analyzer;
        this.sourceImageFile = sourceImageFile;
        this.imageCanvas = new ImageCanvas(analyzer.getImagePlus().getBufferedImage());

        setSize(1150, 780);
        setLocationRelativeTo(parent);

        // Populate analyzer data
        pixelInspector.setAnalyzer(analyzer);
        HistogramData histData = analyzer.computeHistogramData();
        histogramView.setHistogramData(histData);

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // 1. Top toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        String info = String.format("Image: %s | Dimensions: %d × %d | Pixels: %,d | 24-bit RGB",
                sourceImageFile != null ? sourceImageFile.getName() : "In-Memory Raster",
                analyzer.getWidth(), analyzer.getHeight(), analyzer.getTotalPixels());
        JLabel infoLabel = new JLabel("  " + info + "  ");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD, 12f));

        JButton openImageJBtn = new JButton("Open in ImageJ Window");
        openImageJBtn.setToolTipText("Open the image in native ImageJ window with full measurement tools");
        openImageJBtn.addActionListener(e -> analyzer.showInImageJWindow());

        JButton reportBtn = new JButton("View Report");
        reportBtn.setToolTipText("Show detailed statistical report in console / dialog");
        reportBtn.addActionListener(e -> showReportDialog());

        toolBar.add(infoLabel);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(openImageJBtn);
        toolBar.addSeparator();
        toolBar.add(reportBtn);

        // 2. Center: SplitPane with Image on Left, Analysis on Right
        JScrollPane imageScroll = new JScrollPane(imageCanvas);
        imageScroll.setBorder(BorderFactory.createTitledBorder("Rasterized Image (Move mouse to inspect pixels)"));

        JPanel rightPanel = new JPanel(new BorderLayout(4, 4));
        rightPanel.add(pixelInspector, BorderLayout.CENTER);
        rightPanel.add(histogramView, BorderLayout.SOUTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imageScroll, rightPanel);
        mainSplit.setResizeWeight(0.60);
        mainSplit.setDividerLocation(680);

        add(toolBar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
    }

    private void showReportDialog() {
        String report = analyzer.generateAnalysisReport();
        System.out.println(report);
        JOptionPane.showMessageDialog(this,
                "<html><pre style='font-family: monospace; font-size: 11px;'>" + report + "</pre></html>",
                "ImageJ Pixel Analysis Summary",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Interactive image canvas that tracks mouse movement to inspect pixels in real-time.
     */
    private class ImageCanvas extends JPanel {

        private final BufferedImage image;

        public ImageCanvas(BufferedImage image) {
            this.image = image;
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            setBackground(new Color(230, 230, 230));

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (e.getX() >= 0 && e.getX() < image.getWidth() &&
                        e.getY() >= 0 && e.getY() < image.getHeight()) {
                        pixelInspector.inspectPixel(e.getX(), e.getY());
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getX() >= 0 && e.getX() < image.getWidth() &&
                        e.getY() >= 0 && e.getY() < image.getHeight()) {
                        pixelInspector.inspectPixel(e.getX(), e.getY());
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(image, 0, 0, null);
            }
        }
    }
}

