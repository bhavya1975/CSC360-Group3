package com.rrrsvg.ui;

import com.rrrsvg.analysis.ChannelStatistics;
import com.rrrsvg.analysis.HistogramData;
import com.rrrsvg.analysis.PixelAnalyzer;
import com.rrrsvg.analysis.PixelData;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Interactive pixel inspection panel displaying exact pixel coordinates,
 * RGBA channel values, luminance, hex color swatch, and a 13x13 pixel
 * magnifier grid demonstrating how vector curves turn into discrete raster pixels.
 */
public class PixelInspectorPanel extends JPanel {

    private final JLabel coordLabel = new JLabel("Coordinates: (X: -, Y: -)");
    private final JLabel rgbLabel = new JLabel("RGB: -");
    private final JLabel hexLabel = new JLabel("Hex: -");
    private final JLabel lumaLabel = new JLabel("Luminance: -");
    private final JPanel colorSwatch = new JPanel();

    private final PixelMagnifierCanvas magnifierCanvas = new PixelMagnifierCanvas();
    private final DefaultTableModel statsTableModel;

    private PixelAnalyzer analyzer;
    private PixelData currentPixel;

    public PixelInspectorPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("Pixel Inspector & Rasterization Grid"));
        setPreferredSize(new Dimension(360, 480));

        // 1. Pixel details header
        JPanel detailsPanel = new JPanel(new BorderLayout(8, 8));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JPanel textGrid = new JPanel(new GridLayout(4, 1, 2, 2));
        coordLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        rgbLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hexLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lumaLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        textGrid.add(coordLabel);
        textGrid.add(rgbLabel);
        textGrid.add(hexLabel);
        textGrid.add(lumaLabel);

        colorSwatch.setPreferredSize(new Dimension(60, 60));
        colorSwatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        colorSwatch.setBackground(Color.WHITE);

        detailsPanel.add(textGrid, BorderLayout.CENTER);
        detailsPanel.add(colorSwatch, BorderLayout.EAST);

        // 2. Center: Pixel Magnifier Grid
        JPanel magnifierContainer = new JPanel(new BorderLayout(4, 4));
        magnifierContainer.setBorder(BorderFactory.createTitledBorder("Vector-to-Raster Pixel Grid (13×13 Magnifier)"));
        magnifierContainer.add(magnifierCanvas, BorderLayout.CENTER);

        // 3. Bottom: Channel Statistics Table
        String[] cols = {"Channel", "Min", "Max", "Mean", "StdDev"};
        statsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable statsTable = new JTable(statsTableModel);
        statsTable.setFillsViewportHeight(true);
        statsTable.setPreferredScrollableViewportSize(new Dimension(340, 90));
        JScrollPane scrollTable = new JScrollPane(statsTable);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Channel Statistics (ImageJ)"));

        JPanel centerContainer = new JPanel(new BorderLayout(4, 4));
        centerContainer.add(magnifierContainer, BorderLayout.CENTER);
        centerContainer.add(scrollTable, BorderLayout.SOUTH);

        add(detailsPanel, BorderLayout.NORTH);
        add(centerContainer, BorderLayout.CENTER);
    }

    public void setAnalyzer(PixelAnalyzer analyzer) {
        this.analyzer = analyzer;
        updateStatisticsTable();
        // Default inspect center pixel
        if (analyzer != null && analyzer.getWidth() > 0 && analyzer.getHeight() > 0) {
            inspectPixel(analyzer.getWidth() / 2, analyzer.getHeight() / 2);
        } else {
            clear();
        }
    }

    public void inspectPixel(int x, int y) {
        if (analyzer == null) {
            return;
        }

        PixelData pixel = analyzer.getPixel(x, y);
        if (pixel == null) {
            return;
        }

        this.currentPixel = pixel;
        coordLabel.setText(String.format("Coordinates: (X: %d, Y: %d)", pixel.getX(), pixel.getY()));
        rgbLabel.setText(String.format("RGB: %d, %d, %d (A: %d)", pixel.getRed(), pixel.getGreen(), pixel.getBlue(), pixel.getAlpha()));
        hexLabel.setText(String.format("Hex: %s", pixel.getHexColor()));
        lumaLabel.setText(String.format("Luminance: %.1f", pixel.getLuminance()));

        colorSwatch.setBackground(pixel.toColor());
        magnifierCanvas.updateCenter(pixel.getX(), pixel.getY());
    }

    private void updateStatisticsTable() {
        statsTableModel.setRowCount(0);
        if (analyzer == null) {
            return;
        }

        HistogramData hist = analyzer.computeHistogramData();
        addChannelRow(hist.getRedStats());
        addChannelRow(hist.getGreenStats());
        addChannelRow(hist.getBlueStats());
        addChannelRow(hist.getLuminanceStats());
    }

    private void addChannelRow(ChannelStatistics stats) {
        if (stats != null) {
            statsTableModel.addRow(new Object[]{
                    stats.getChannelName(),
                    stats.getMin(),
                    stats.getMax(),
                    String.format("%.2f", stats.getMean()),
                    String.format("%.2f", stats.getStdDev())
            });
        }
    }

    public void clear() {
        this.currentPixel = null;
        coordLabel.setText("Coordinates: (X: -, Y: -)");
        rgbLabel.setText("RGB: -");
        hexLabel.setText("Hex: -");
        lumaLabel.setText("Luminance: -");
        colorSwatch.setBackground(Color.WHITE);
        statsTableModel.setRowCount(0);
        magnifierCanvas.repaint();
    }

    /**
     * Canvas drawing a magnified discrete pixel grid around the inspected pixel.
     * Demonstrates anti-aliased edge blending and discrete rasterization.
     */
    private class PixelMagnifierCanvas extends JPanel {

        private static final int RADIUS = 6; // 13x13 grid
        private int centerX = -1;
        private int centerY = -1;

        public PixelMagnifierCanvas() {
            setPreferredSize(new Dimension(190, 190));
            setBackground(Color.DARK_GRAY);
        }

        public void updateCenter(int cx, int cy) {
            this.centerX = cx;
            this.centerY = cy;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (analyzer == null || centerX < 0 || centerY < 0) {
                g.setColor(Color.LIGHT_GRAY);
                g.drawString("Hover image to magnify pixels", getWidth() / 2 - 80, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            PixelData[][] neighborhood = analyzer.getNeighborhoodPixels(centerX, centerY, RADIUS);
            int gridSize = neighborhood.length;
            int cellSize = Math.min(getWidth(), getHeight()) / gridSize;
            int startX = (getWidth() - (cellSize * gridSize)) / 2;
            int startY = (getHeight() - (cellSize * gridSize)) / 2;

            for (int r = 0; r < gridSize; r++) {
                for (int c = 0; c < gridSize; c++) {
                    PixelData pd = neighborhood[r][c];
                    int px = startX + c * cellSize;
                    int py = startY + r * cellSize;

                    if (pd != null) {
                        g2.setColor(pd.toColor());
                        g2.fillRect(px, py, cellSize, cellSize);
                    } else {
                        g2.setColor(Color.LIGHT_GRAY);
                        g2.fillRect(px, py, cellSize, cellSize);
                    }

                    // Grid lines
                    g2.setColor(new Color(180, 180, 180, 60));
                    g2.drawRect(px, py, cellSize, cellSize);

                    // Highlight the target inspected center pixel with a bold red box
                    if (r == RADIUS && c == RADIUS) {
                        g2.setColor(Color.RED);
                        g2.drawRect(px, py, cellSize - 1, cellSize - 1);
                        g2.drawRect(px + 1, py + 1, cellSize - 3, cellSize - 3);
                    }
                }
            }
        }
    }
}

