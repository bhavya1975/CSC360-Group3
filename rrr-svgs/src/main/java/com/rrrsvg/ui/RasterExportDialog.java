package com.rrrsvg.ui;

import com.rrrsvg.raster.RasterOptions;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

/**
 * Modal dialog enabling users to specify output rasterization parameters:
 * dimensions (width & height), resolution (DPI), background transparency,
 * and destination PNG file path.
 */
public class RasterExportDialog extends JDialog {

    private final JTextField widthField = new JTextField(6);
    private final JTextField heightField = new JTextField(6);
    private final JCheckBox aspectLockBox = new JCheckBox("Lock Aspect Ratio", true);
    private final JComboBox<String> dpiComboBox = new JComboBox<>(new String[]{
            "72 DPI (Screen Standard)",
            "96 DPI (CSS Standard - Recommended)",
            "150 DPI (Medium Quality)",
            "300 DPI (High Quality / Print)"
    });
    private final JCheckBox whiteBgBox = new JCheckBox("White Background (Opaque)", false);
    private final JTextField outputDirField = new JTextField(20);

    private boolean confirmed = false;
    private boolean openInAnalyzer = true;
    private final double originalAspectRatio;
    private File selectedOutputFile;

    public RasterExportDialog(JFrame parent, File currentSvgFile, int defaultW, int defaultH) {
        super(parent, "Rasterize SVG to PNG (Module 3)", true);
        setSize(480, 360);
        setLocationRelativeTo(parent);
        setResizable(false);

        this.originalAspectRatio = (defaultH > 0) ? ((double) defaultW / defaultH) : 1.0;

        widthField.setText(String.valueOf(defaultW > 0 ? defaultW : 800));
        heightField.setText(String.valueOf(defaultH > 0 ? defaultH : 600));
        dpiComboBox.setSelectedIndex(1); // 96 DPI default

        // Default output file
        String baseName = (currentSvgFile != null)
                ? currentSvgFile.getName().replaceAll("\\.svg[z]?$", ".png")
                : "rasterized_output.png";
        File defaultOut = new File("generated-png/" + baseName);
        outputDirField.setText(defaultOut.getPath());

        setupListeners();
        buildUI();
    }

    private void setupListeners() {
        widthField.addActionListener(e -> {
            if (aspectLockBox.isSelected()) {
                try {
                    int w = Integer.parseInt(widthField.getText().trim());
                    heightField.setText(String.valueOf((int) Math.round(w / originalAspectRatio)));
                } catch (NumberFormatException ignored) {}
            }
        });

        heightField.addActionListener(e -> {
            if (aspectLockBox.isSelected()) {
                try {
                    int h = Integer.parseInt(heightField.getText().trim());
                    widthField.setText(String.valueOf((int) Math.round(h * originalAspectRatio)));
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Dimensions
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Width (pixels):"), gbc);
        gbc.gridx = 1;
        form.add(widthField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Height (pixels):"), gbc);
        gbc.gridx = 1;
        form.add(heightField, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        form.add(aspectLockBox, gbc);

        // Resolution
        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Resolution (DPI):"), gbc);
        gbc.gridx = 1;
        form.add(dpiComboBox, gbc);

        // Background
        gbc.gridx = 1; gbc.gridy = 4;
        form.add(whiteBgBox, gbc);

        // Output file
        gbc.gridx = 0; gbc.gridy = 5;
        form.add(new JLabel("Output PNG File:"), gbc);
        gbc.gridx = 1;
        JPanel fileChooserPanel = new JPanel(new BorderLayout(4, 0));
        fileChooserPanel.add(outputDirField, BorderLayout.CENTER);
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> chooseOutputFile());
        fileChooserPanel.add(browseBtn, BorderLayout.EAST);
        form.add(fileChooserPanel, gbc);

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton saveOnlyBtn = new JButton("Save PNG Only");
        saveOnlyBtn.addActionListener(e -> {
            if (validateInputs()) {
                confirmed = true;
                openInAnalyzer = false;
                dispose();
            }
        });

        JButton analyzeBtn = new JButton("Rasterize & Open in Analyzer");
        analyzeBtn.setFont(analyzeBtn.getFont().deriveFont(java.awt.Font.BOLD));
        analyzeBtn.addActionListener(e -> {
            if (validateInputs()) {
                confirmed = true;
                openInAnalyzer = true;
                dispose();
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveOnlyBtn);
        buttonPanel.add(analyzeBtn);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void chooseOutputFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setSelectedFile(new File(outputDirField.getText().trim()));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".png")) {
                f = new File(f.getPath() + ".png");
            }
            outputDirField.setText(f.getPath());
        }
    }

    private boolean validateInputs() {
        try {
            int w = Integer.parseInt(widthField.getText().trim());
            int h = Integer.parseInt(heightField.getText().trim());
            if (w <= 0 || h <= 0) {
                JOptionPane.showMessageDialog(this, "Width and Height must be positive numbers.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            this.selectedOutputFile = new File(outputDirField.getText().trim());
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric dimensions.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public boolean isOpenInAnalyzer() {
        return openInAnalyzer;
    }

    public File getOutputFile() {
        return selectedOutputFile;
    }

    public RasterOptions getRasterOptions() {
        int w = Integer.parseInt(widthField.getText().trim());
        int h = Integer.parseInt(heightField.getText().trim());
        double dpi = switch (dpiComboBox.getSelectedIndex()) {
            case 0 -> RasterOptions.SCREEN_DPI; // 72
            case 2 -> RasterOptions.MEDIUM_DPI; // 150
            case 3 -> RasterOptions.PRINT_DPI;  // 300
            default -> RasterOptions.DEFAULT_DPI; // 96
        };
        Color bg = whiteBgBox.isSelected() ? Color.WHITE : null;
        return new RasterOptions(w, h, dpi, bg);
    }
}
