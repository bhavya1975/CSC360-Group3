package com.rrrsvg.ui;

import com.rrrsvg.analysis.ChannelStatistics;
import com.rrrsvg.analysis.HistogramData;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Interactive Swing component rendering 256-bin RGB and Luminance histograms.
 * <p>
 * Displays statistical frequency distributions computed by ImageJ with
 * channel toggles (RGB composite, Red, Green, Blue, Luminance) and
 * interactive mouse hovering for individual bin inspections.
 * </p>
 */
public class HistogramViewPanel extends JPanel {

    public enum DisplayChannel {
        ALL_RGB, RED, GREEN, BLUE, LUMINANCE
    }

    private HistogramData histogramData;
    private DisplayChannel currentChannel = DisplayChannel.ALL_RGB;

    private final JLabel statsLabel = new JLabel("No histogram loaded");
    private final JLabel hoverBinLabel = new JLabel(" Bin: - | Count: - ");
    private final ChartCanvas chartCanvas = new ChartCanvas();

    public HistogramViewPanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("ImageJ RGB Histogram & Channel Distribution"));
        setPreferredSize(new Dimension(380, 240));

        // Top control panel with channel radio buttons
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        ButtonGroup group = new ButtonGroup();

        JRadioButton rbAll = new JRadioButton("RGB", true);
        JRadioButton rbRed = new JRadioButton("Red");
        JRadioButton rbGreen = new JRadioButton("Green");
        JRadioButton rbBlue = new JRadioButton("Blue");
        JRadioButton rbLum = new JRadioButton("Luma");

        group.add(rbAll);
        group.add(rbRed);
        group.add(rbGreen);
        group.add(rbBlue);
        group.add(rbLum);

        rbAll.addActionListener(e -> setDisplayChannel(DisplayChannel.ALL_RGB));
        rbRed.addActionListener(e -> setDisplayChannel(DisplayChannel.RED));
        rbGreen.addActionListener(e -> setDisplayChannel(DisplayChannel.GREEN));
        rbBlue.addActionListener(e -> setDisplayChannel(DisplayChannel.BLUE));
        rbLum.addActionListener(e -> setDisplayChannel(DisplayChannel.LUMINANCE));

        controls.add(rbAll);
        controls.add(rbRed);
        controls.add(rbGreen);
        controls.add(rbBlue);
        controls.add(rbLum);

        // Header info bar
        JPanel header = new JPanel(new BorderLayout(4, 2));
        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.PLAIN, 11f));
        header.add(controls, BorderLayout.NORTH);
        header.add(statsLabel, BorderLayout.SOUTH);

        // Footer hover label
        hoverBinLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        add(header, BorderLayout.NORTH);
        add(chartCanvas, BorderLayout.CENTER);
        add(hoverBinLabel, BorderLayout.SOUTH);
    }

    public void setHistogramData(HistogramData data) {
        this.histogramData = data;
        updateStatsLabel();
        chartCanvas.repaint();
    }

    public void setDisplayChannel(DisplayChannel channel) {
        this.currentChannel = channel;
        updateStatsLabel();
        chartCanvas.repaint();
    }

    private void updateStatsLabel() {
        if (histogramData == null) {
            statsLabel.setText(" No histogram loaded");
            return;
        }

        switch (currentChannel) {
            case RED -> statsLabel.setText(" " + histogramData.getRedStats().formatSummary());
            case GREEN -> statsLabel.setText(" " + histogramData.getGreenStats().formatSummary());
            case BLUE -> statsLabel.setText(" " + histogramData.getBlueStats().formatSummary());
            case LUMINANCE -> statsLabel.setText(" " + histogramData.getLuminanceStats().formatSummary());
            case ALL_RGB -> statsLabel.setText(String.format(" R: μ=%.1f | G: μ=%.1f | B: μ=%.1f | Peak: %,d",
                    histogramData.getRedStats().getMean(),
                    histogramData.getGreenStats().getMean(),
                    histogramData.getBlueStats().getMean(),
                    histogramData.getMaxFrequency()));
        }
    }

    /**
     * Inner custom canvas drawing the actual anti-aliased histogram graph.
     */
    private class ChartCanvas extends JPanel {

        private int hoveredBin = -1;

        public ChartCanvas() {
            setBackground(new Color(250, 250, 252));
            setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (histogramData == null) {
                        return;
                    }
                    int w = getWidth() - 20;
                    int x = e.getX() - 10;
                    if (x >= 0 && x < w && w > 0) {
                        hoveredBin = (int) Math.round(((double) x / w) * 255.0);
                        hoveredBin = Math.max(0, Math.min(255, hoveredBin));

                        int r = histogramData.getRedBins()[hoveredBin];
                        int g = histogramData.getGreenBins()[hoveredBin];
                        int b = histogramData.getBlueBins()[hoveredBin];
                        int lum = histogramData.getLuminanceBins()[hoveredBin];

                        hoverBinLabel.setText(String.format(" Bin %d (Val %d) -> R: %,d | G: %,d | B: %,d | Luma: %,d",
                                hoveredBin, hoveredBin, r, g, b, lum));
                    } else {
                        hoveredBin = -1;
                        hoverBinLabel.setText(" Bin: - | Count: - ");
                    }
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (histogramData == null) {
                g.setColor(Color.GRAY);
                g.drawString("No image rasterized yet", getWidth() / 2 - 60, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padding = 10;
            int width = getWidth() - (padding * 2);
            int height = getHeight() - (padding * 2);
            int maxFreq = histogramData.getMaxFrequency();
            if (width <= 0 || height <= 0 || maxFreq <= 0) {
                return;
            }

            // Draw axis line
            g2.setColor(new Color(200, 205, 210));
            g2.drawLine(padding, padding + height, padding + width, padding + height);

            // Draw channel curves
            switch (currentChannel) {
                case ALL_RGB -> {
                    drawHistogramCurve(g2, histogramData.getRedBins(), new Color(231, 76, 60, 160), padding, width, height, maxFreq);
                    drawHistogramCurve(g2, histogramData.getGreenBins(), new Color(46, 204, 113, 160), padding, width, height, maxFreq);
                    drawHistogramCurve(g2, histogramData.getBlueBins(), new Color(52, 152, 219, 160), padding, width, height, maxFreq);
                }
                case RED -> drawHistogramBars(g2, histogramData.getRedBins(), new Color(231, 76, 60), padding, width, height, maxFreq);
                case GREEN -> drawHistogramBars(g2, histogramData.getGreenBins(), new Color(46, 204, 113), padding, width, height, maxFreq);
                case BLUE -> drawHistogramBars(g2, histogramData.getBlueBins(), new Color(52, 152, 219), padding, width, height, maxFreq);
                case LUMINANCE -> drawHistogramBars(g2, histogramData.getLuminanceBins(), new Color(108, 122, 137), padding, width, height, maxFreq);
            }

            // Draw vertical hover indicator line
            if (hoveredBin >= 0 && hoveredBin <= 255) {
                int hoverX = padding + (int) Math.round(((double) hoveredBin / 255.0) * width);
                g2.setColor(new Color(44, 62, 80, 180));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                g2.drawLine(hoverX, padding, hoverX, padding + height);
            }
        }

        private void drawHistogramCurve(Graphics2D g2, int[] bins, Color color,
                                        int padding, int width, int height, int maxFreq) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));

            int prevX = padding;
            int prevY = padding + height - (int) (((double) bins[0] / maxFreq) * height);

            for (int i = 1; i < 256; i++) {
                int curX = padding + (int) (((double) i / 255.0) * width);
                int curY = padding + height - (int) (((double) bins[i] / maxFreq) * height);
                g2.drawLine(prevX, prevY, curX, curY);
                prevX = curX;
                prevY = curY;
            }
        }

        private void drawHistogramBars(Graphics2D g2, int[] bins, Color color,
                                       int padding, int width, int height, int maxFreq) {
            g2.setColor(color);
            for (int i = 0; i < 256; i++) {
                int barX = padding + (int) (((double) i / 255.0) * width);
                int barHeight = (int) (((double) bins[i] / maxFreq) * height);
                int barY = padding + height - barHeight;
                int barWidth = Math.max(1, width / 256);
                g2.fillRect(barX, barY, barWidth, barHeight);
            }
        }
    }
}

