package com.rrrsvg.ui;

import com.rrrsvg.model.SVGDocument;
import com.rrrsvg.model.SVGRoot;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Map;

/**
 * Side panel displaying metadata and hierarchical tree structure
 * extracted by Module 1 ({@link SVGDocument}).
 * <p>
 * Provides deep visual integration between the SVG Reader (Module 1)
 * and the SVG Renderer (Module 2).
 * </p>
 */
public class SVGInspectorPanel extends JPanel {

    private final JLabel fileLabel = new JLabel("File: -");
    private final JLabel dimensionsLabel = new JLabel("Dimensions: -");
    private final JLabel viewBoxLabel = new JLabel("ViewBox: -");
    private final JLabel nodeCountLabel = new JLabel("Total Elements: -");

    private final DefaultTableModel breakdownTableModel;
    private final JTextArea treeTextArea;

    public SVGInspectorPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder("SVG Document Inspector (Module 1 Integration)"));
        setPreferredSize(new Dimension(340, 600));

        // 1. Metadata summary panel
        JPanel metaPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        metaPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD, 12f));
        metaPanel.add(fileLabel);
        metaPanel.add(dimensionsLabel);
        metaPanel.add(viewBoxLabel);
        metaPanel.add(nodeCountLabel);

        // 2. Element breakdown table
        String[] columnNames = {"Element Type", "Count"};
        breakdownTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(breakdownTableModel);
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(320, 110));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Element Breakdown"));

        JPanel topContainer = new JPanel(new BorderLayout(4, 4));
        topContainer.add(metaPanel, BorderLayout.NORTH);
        topContainer.add(tableScroll, BorderLayout.CENTER);

        // 3. Hierarchical tree display
        treeTextArea = new JTextArea();
        treeTextArea.setEditable(false);
        treeTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        treeTextArea.setBackground(new Color(248, 249, 250));
        JScrollPane treeScroll = new JScrollPane(treeTextArea);
        treeScroll.setBorder(BorderFactory.createTitledBorder("Hierarchical Structure (DOM Tree)"));

        add(topContainer, BorderLayout.NORTH);
        add(treeScroll, BorderLayout.CENTER);
    }

    /**
     * Updates the inspector panel with data extracted by Module 1.
     *
     * @param document The parsed SVGDocument, or null to clear.
     */
    public void updateDocument(SVGDocument document) {
        breakdownTableModel.setRowCount(0);

        if (document == null) {
            fileLabel.setText("File: -");
            dimensionsLabel.setText("Dimensions: -");
            viewBoxLabel.setText("ViewBox: -");
            nodeCountLabel.setText("Total Elements: -");
            treeTextArea.setText("");
            return;
        }

        SVGRoot root = document.getRoot();
        fileLabel.setText("File: " + new java.io.File(document.getSource()).getName());
        dimensionsLabel.setText("Dimensions: " + root.getWidth() + " × " + root.getHeight());
        viewBoxLabel.setText("ViewBox: " + (root.getViewBox() != null ? root.getViewBox() : "None"));
        nodeCountLabel.setText("Total Elements: " + document.getTotalElementCount());

        for (Map.Entry<String, Integer> entry : document.getElementTypeCounts().entrySet()) {
            breakdownTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        treeTextArea.setText(document.toTreeString());
        treeTextArea.setCaretPosition(0);
    }
}

