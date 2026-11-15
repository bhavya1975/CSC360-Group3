package com.rrrsvg.ui;

import com.rrrsvg.analysis.PixelAnalyzer;
import com.rrrsvg.exception.SVGParseException;
import com.rrrsvg.exception.SVGRasterizeException;
import com.rrrsvg.model.SVGDocument;
import com.rrrsvg.raster.RasterOptions;
import com.rrrsvg.raster.SVGRasterizer;
import ij.ImagePlus;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;

/**
 * Unified Java Swing Application Window for RRR-SVGs:
 * <ul>
 *   <li><b>Module 1:</b> SVG Reader & Document Structure Inspector</li>
 *   <li><b>Module 2:</b> SVG Vector Renderer with infinite zoom & pan</li>
 *   <li><b>Module 3:</b> SVG Rasterizer, Direct Side-by-Side SVG vs PNG Comparison & ImageJ Analysis</li>
 * </ul>
 */
public class SVGViewerFrame extends JFrame {

    private final SVGRenderCanvas canvas;
    private final SVGInspectorPanel inspectorPanel;
    private final ComparisonViewPanel comparisonPanel;
    private final JSplitPane vectorSplitPane;
    private final JTabbedPane mainTabbedPane;

    private final JLabel statusLabel = new JLabel(" Ready");
    private final JLabel fileStatusLabel = new JLabel(" No file loaded ");
    private final JLabel coordinatesLabel = new JLabel(" X: - , Y: - ");
    private final JLabel zoomBadge = new JLabel(" 100% ");

    private final SVGRasterizer rasterizer = new SVGRasterizer();
    private File currentFile;

    public SVGViewerFrame() {
        super("RRR-SVGs: Reading, Rendering, and Rasterization");

        this.canvas = new SVGRenderCanvas();
        this.inspectorPanel = new SVGInspectorPanel();
        this.comparisonPanel = new ComparisonViewPanel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Vector View Split Pane (Canvas + DOM Inspector)
        vectorSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, inspectorPanel);
        vectorSplitPane.setResizeWeight(0.72);
        vectorSplitPane.setDividerLocation(820);

        // Main Tabbed Pane hosting Vector View and Direct SVG vs PNG Comparison View
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("SVG Vector Preview & DOM Inspector", vectorSplitPane);
        mainTabbedPane.addTab("Side-by-Side Comparison (SVG vs PNG)", comparisonPanel);

        setLayout(new BorderLayout());
        add(createToolBar(), BorderLayout.NORTH);
        add(mainTabbedPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        setJMenuBar(createMenuBar());
        setupCanvasListeners();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // 1. File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open SVG File...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKey));
        openItem.addActionListener(e -> chooseAndOpenFile());

        JMenuItem reloadItem = new JMenuItem("Reload");
        reloadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutKey));
        reloadItem.addActionListener(e -> reloadCurrentFile());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(openItem);
        fileMenu.add(reloadItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 2. View Menu (Module 2)
        JMenu viewMenu = new JMenu("View");
        JMenuItem zoomInItem = new JMenuItem("Zoom In (+25%)");
        zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutKey));
        zoomInItem.addActionListener(e -> canvas.zoomIn());

        JMenuItem zoomOutItem = new JMenuItem("Zoom Out (-20%)");
        zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutKey));
        zoomOutItem.addActionListener(e -> canvas.zoomOut());

        JMenuItem resetZoomItem = new JMenuItem("Actual Size (100%)");
        resetZoomItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, shortcutKey));
        resetZoomItem.addActionListener(e -> canvas.resetZoom());

        JMenuItem fitItem = new JMenuItem("Fit to Window");
        fitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, shortcutKey));
        fitItem.addActionListener(e -> canvas.fitToWindow());

        JMenuItem compareViewItem = new JMenuItem("Direct SVG vs PNG Comparison");
        compareViewItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey));
        compareViewItem.addActionListener(e -> mainTabbedPane.setSelectedIndex(1));

        viewMenu.add(zoomInItem);
        viewMenu.add(zoomOutItem);
        viewMenu.add(resetZoomItem);
        viewMenu.add(fitItem);
        viewMenu.addSeparator();
        viewMenu.add(compareViewItem);

        // 3. Rasterize & Analysis Menu (Module 3)
        JMenu rasterMenu = new JMenu("Rasterize & Analyze");
        JMenuItem rasterizeItem = new JMenuItem("Rasterize to PNG...");
        rasterizeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, shortcutKey));
        rasterizeItem.addActionListener(e -> openRasterizeDialog());

        JMenuItem analyzeItem = new JMenuItem("Analyze in ImageJ Window...");
        analyzeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, shortcutKey));
        analyzeItem.addActionListener(e -> quickAnalyzeInImageJ());

        rasterMenu.add(rasterizeItem);
        rasterMenu.add(analyzeItem);

        // 4. Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About RRR-SVGs");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(rasterMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openBtn = new JButton("Open SVG");
        openBtn.setToolTipText("Open an SVG file from disk (Ctrl/Cmd+O)");
        openBtn.addActionListener(e -> chooseAndOpenFile());

        JButton reloadBtn = new JButton("Reload");
        reloadBtn.setToolTipText("Reload the current SVG file (Ctrl/Cmd+R)");
        reloadBtn.addActionListener(e -> reloadCurrentFile());

        JButton zoomInBtn = new JButton("Zoom In (+)");
        zoomInBtn.setToolTipText("Zoom in vector graphics without loss of quality (Ctrl/Cmd++)");
        zoomInBtn.addActionListener(e -> canvas.zoomIn());

        JButton zoomOutBtn = new JButton("Zoom Out (-)");
        zoomOutBtn.setToolTipText("Zoom out vector graphics (Ctrl/Cmd+-)");
        zoomOutBtn.addActionListener(e -> canvas.zoomOut());

        JButton resetZoomBtn = new JButton("100%");
        resetZoomBtn.setToolTipText("Reset zoom to actual size 100% (Ctrl/Cmd+0)");
        resetZoomBtn.addActionListener(e -> canvas.resetZoom());

        JButton fitBtn = new JButton("Fit Window");
        fitBtn.setToolTipText("Fit SVG drawing inside viewport (Ctrl/Cmd+F)");
        fitBtn.addActionListener(e -> canvas.fitToWindow());

        zoomBadge.setFont(zoomBadge.getFont().deriveFont(Font.BOLD, 12f));
        zoomBadge.setBorder(BorderFactory.createEtchedBorder());

        JButton compareBtn = new JButton("Compare (SVG vs PNG)");
        compareBtn.setFont(compareBtn.getFont().deriveFont(Font.BOLD));
        compareBtn.setToolTipText("Display live SVG preview directly side-by-side with generated PNG (Ctrl/Cmd+C)");
        compareBtn.addActionListener(e -> {
            mainTabbedPane.setSelectedIndex(1);
            comparisonPanel.reRasterize();
        });

        JButton rasterizeBtn = new JButton("Rasterize to PNG");
        rasterizeBtn.setToolTipText("Module 3: Convert vector SVG to raster PNG image (Ctrl/Cmd+E)");
        rasterizeBtn.addActionListener(e -> openRasterizeDialog());

        JButton analyzeBtn = new JButton("ImageJ Analysis");
        analyzeBtn.setToolTipText("Module 3: Inspect pixels, RGB channels, and histograms with ImageJ (Ctrl/Cmd+A)");
        analyzeBtn.addActionListener(e -> quickAnalyzeInImageJ());

        JToggleButton toggleInspectorBtn = new JToggleButton("DOM Inspector", true);
        toggleInspectorBtn.setToolTipText("Toggle Module 1 DOM structure inspector panel");
        toggleInspectorBtn.addActionListener(e -> {
            inspectorPanel.setVisible(toggleInspectorBtn.isSelected());
            vectorSplitPane.resetToPreferredSizes();
        });

        toolBar.add(openBtn);
        toolBar.add(reloadBtn);
        toolBar.addSeparator();
        toolBar.add(zoomInBtn);
        toolBar.add(zoomOutBtn);
        toolBar.add(resetZoomBtn);
        toolBar.add(fitBtn);
        toolBar.add(Box.createHorizontalStrut(6));
        toolBar.add(new JLabel("Zoom: "));
        toolBar.add(zoomBadge);
        toolBar.addSeparator();
        toolBar.add(compareBtn);
        toolBar.add(rasterizeBtn);
        toolBar.add(analyzeBtn);
        toolBar.addSeparator();
        toolBar.add(toggleInspectorBtn);

        return toolBar;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        fileStatusLabel.setFont(fileStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        coordinatesLabel.setFont(coordinatesLabel.getFont().deriveFont(Font.PLAIN, 11f));

        statusBar.add(fileStatusLabel, BorderLayout.WEST);
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(coordinatesLabel, BorderLayout.EAST);

        return statusBar;
    }

    private void setupCanvasListeners() {
        canvas.addZoomChangeListener((zoomFactor, percentage) -> {
            zoomBadge.setText(" " + percentage + "% ");
        });

        canvas.addRenderStateListener(new SVGRenderCanvas.RenderStateListener() {
            @Override
            public void onRenderStarted() {
                statusLabel.setForeground(Color.DARK_GRAY);
                statusLabel.setText(" Rendering vector graphics with Apache Batik...");
            }

            @Override
            public void onRenderCompleted(long durationMs) {
                statusLabel.setForeground(new Color(39, 174, 96));
                statusLabel.setText(" Rendered successfully in " + durationMs + " ms");
            }

            @Override
            public void onRenderFailed(String errorMessage, Throwable cause) {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(" " + errorMessage);
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                coordinatesLabel.setText(String.format(" X: %d, Y: %d ", e.getX(), e.getY()));
            }
        });
    }

    public void chooseAndOpenFile() {
        JFileChooser chooser = new JFileChooser(currentFile != null ? currentFile.getParentFile() : new File("."));
        chooser.setDialogTitle("Select SVG File to Open");
        chooser.setFileFilter(new FileNameExtensionFilter("SVG Vector Graphics (*.svg, *.svgz)", "svg", "svgz"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile());
        }
    }

    public void reloadCurrentFile() {
        if (currentFile != null && currentFile.exists()) {
            openFile(currentFile);
        }
    }

    /**
     * Loads, validates, and renders an SVG file across all tabs (Vector preview, DOM inspector,
     * and direct SVG vs PNG side-by-side comparison).
     *
     * @param file Target SVG file.
     */
    public void openFile(File file) {
        if (file == null) {
            return;
        }

        try {
            this.currentFile = file.getAbsoluteFile();

            // 1. Load vector canvas
            canvas.loadSVGFile(this.currentFile);

            // 2. Update status and DOM inspector panel
            fileStatusLabel.setText(" File: " + this.currentFile.getName() + " ");
            SVGDocument doc = canvas.getCurrentSvgDocument();
            inspectorPanel.updateDocument(doc);

            // 3. Update side-by-side comparison panel with the EXACT same file
            comparisonPanel.loadSvg(this.currentFile);

            Dimension dims = canvas.getNativeSvgDimensions();
            setTitle(String.format("RRR-SVGs - %s (%d × %d px)", this.currentFile.getName(), dims.width, dims.height));

        } catch (SVGParseException e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText(" Error: " + e.getMessage());
            inspectorPanel.updateDocument(null);

            JOptionPane.showMessageDialog(
                    this,
                    "<html><b>Failed to open SVG file:</b><br><br>" +
                    e.getMessage() +
                    (e.getCause() != null ? "<br><br><i>Cause: " + e.getCause().getMessage() + "</i>" : "") +
                    "</html>",
                    "SVG Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText(" Rendering Error: " + e.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    "An unexpected error occurred while rendering:\n" + e.getMessage(),
                    "Rendering Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Opens the rasterization dialog allowing the user to configure dimensions,
     * resolution (DPI), background color, and export destination.
     */
    public void openRasterizeDialog() {
        if (currentFile == null || !currentFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please open an SVG file first.", "No SVG Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Use exact native SVG dimensions as default
        Dimension nativeDim = canvas.getNativeSvgDimensions();
        int defW = nativeDim.width;
        int defH = nativeDim.height;

        RasterExportDialog dialog = new RasterExportDialog(this, currentFile, defW, defH);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        RasterOptions options = dialog.getRasterOptions();
        File outFile = dialog.getOutputFile();

        try {
            long start = System.currentTimeMillis();
            rasterizer.rasterizeToFile(currentFile, outFile, options);
            long duration = System.currentTimeMillis() - start;

            statusLabel.setForeground(new Color(39, 174, 96));
            statusLabel.setText(String.format(" Rasterized to PNG in %d ms (%dx%d, %.0f DPI)",
                    duration, options.getWidth(), options.getHeight(), options.getDpi()));

            // Refresh comparison view with newly generated PNG
            comparisonPanel.reRasterize();

            if (dialog.isOpenInAnalyzer()) {
                PixelAnalyzer analyzer = PixelAnalyzer.fromFile(outFile);
                RasterAnalysisDialog analysisDialog = new RasterAnalysisDialog(this, analyzer, outFile);
                analysisDialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Successfully rasterized SVG to:\n" + outFile.getAbsolutePath(),
                        "Rasterization Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SVGParseException | SVGRasterizeException e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText(" Rasterization Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to rasterize SVG:\n" + e.getMessage(),
                    "Rasterization Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText(" Unexpected Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "An unexpected error occurred during rasterization:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Quick action to rasterize the currently loaded SVG at its native dimensions
     * and immediately open the ImageJ pixel & histogram analysis window.
     */
    public void quickAnalyzeInImageJ() {
        if (currentFile == null || !currentFile.exists()) {
            JOptionPane.showMessageDialog(this, "Please open an SVG file first.", "No SVG Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Dimension nativeDim = canvas.getNativeSvgDimensions();
            RasterOptions options = new RasterOptions(nativeDim.width, nativeDim.height);

            ImagePlus imp = rasterizer.rasterizeToImagePlus(currentFile, options);
            PixelAnalyzer analyzer = new PixelAnalyzer(imp);

            RasterAnalysisDialog analysisDialog = new RasterAnalysisDialog(this, analyzer, currentFile);
            analysisDialog.setVisible(true);

        } catch (SVGParseException | SVGRasterizeException e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText(" Analysis Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Failed to analyze SVG in ImageJ:\n" + e.getMessage(),
                    "Analysis Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText(" Unexpected Error: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "An unexpected error occurred during ImageJ analysis:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(
                this,
                "<html><h3>RRR-SVGs: Reading, Rendering, and Rasterization System</h3>" +
                "<b>Module 1: SVG Reading</b><br>" +
                "  • DOM XML parsing, hierarchical structure, and attribute extraction.<br><br>" +
                "<b>Module 2: SVG Rendering</b><br>" +
                "  • Apache Batik vector engine, lossless zoom, and Swing UI.<br><br>" +
                "<b>Module 3: SVG Rasterization & Pixel Analysis</b><br>" +
                "  • Direct side-by-side SVG vs PNG visual comparison.<br>" +
                "  • Batik PNGTranscoder for high-resolution PNG generation.<br>" +
                "  • ImageJ pixel inspection, RGB channel analysis, and histogram visualization.<br><br>" +
                "Developed for CSC360 Group 3." +
                "</html>",
                "About RRR-SVGs",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public static SVGViewerFrame launch(File initialFile) {
        SVGViewerFrame viewer = new SVGViewerFrame();
        viewer.setVisible(true);

        if (initialFile != null && initialFile.exists()) {
            SwingUtilities.invokeLater(() -> viewer.openFile(initialFile));
        }

        return viewer;
    }
}
