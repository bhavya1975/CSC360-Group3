package com.rrrsvg;

import com.rrrsvg.interpreter.Drawing;
import com.rrrsvg.interpreter.SvgInterpreter;
import com.rrrsvg.rasterizer.Rasterizer;
import com.rrrsvg.reader.SvgNode;
import com.rrrsvg.reader.SvgParseException;
import com.rrrsvg.reader.SvgReader;
import com.rrrsvg.renderer.DrawOp;
import com.rrrsvg.renderer.Renderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import ij.ImagePlus;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Simple desktop app: type SVG code (or open an .svg file) on the left,
 * click Render to see the image on the right, then save it as PNG.
 */
public class App extends Application {

    private static final String EXAMPLE = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="200">
              <rect x="20" y="20" width="120" height="100" fill="blue"/>
              <circle cx="180" cy="110" r="60" fill="red" opacity="0.6"/>
              <line x1="20" y1="180" x2="280" y2="180" stroke="green" stroke-width="5"/>
            </svg>
            """;

    private final TextArea codeArea = new TextArea(EXAMPLE);
    private final ImageView imageView = new ImageView();
    private final Label status = new Label();
    private BufferedImage image;
    private Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Button render = new Button("Render");
        render.setOnAction(e -> render());
        Button open = new Button("Open SVG");
        open.setOnAction(e -> openSvg());
        Button save = new Button("Save PNG");
        save.setOnAction(e -> savePng());
        Button imagej = new Button("Open in ImageJ");
        imagej.setOnAction(e -> openImageJ());

        codeArea.setStyle("-fx-font-family: monospace;");
        StackPane imageHolder = new StackPane(imageView);
        imageHolder.setAlignment(Pos.CENTER);
        ScrollPane scrollPane = new ScrollPane(imageHolder);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        SplitPane split = new SplitPane(codeArea, scrollPane);
        split.setDividerPositions(0.45);

        BorderPane root = new BorderPane();
        root.setTop(new ToolBar(render, open, save, imagej, status));
        root.setCenter(split);

        stage.setTitle("RRR-SVGs");
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
        render();
    }

    /** Runs the SVG code in the text box through the four stages and shows the result. */
    private void render() {
        try {
            SvgNode svg = new SvgReader().readString(codeArea.getText());   // 1. Reading
            Drawing drawing = new SvgInterpreter().interpret(svg);          // 2. Interpreting
            List<DrawOp> ops = new Renderer().render(drawing);              // 3. Rendering
            image = new Rasterizer().rasterize(ops, drawing.width(), drawing.height()); // 4. Rasterizing
            imageView.setImage(toFxImage(image));
            status.setText(drawing.shapes().size() + " shapes, " + image.getWidth() + "×" + image.getHeight() + " px");
        } catch (SvgParseException ex) {
            status.setText("Error");
            new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
        }
    }

    /** Loads an .svg file into the text box and renders it. */
    private void openSvg() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG files", "*.svg"));
        File samples = new File("samples");
        if (samples.isDirectory()) {
            chooser.setInitialDirectory(samples);
        }
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            codeArea.setText(Files.readString(file.toPath()));
            render();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Could not open: " + ex.getMessage()).showAndWait();
        }
    }

    private void savePng() {
        if (image == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        chooser.setInitialFileName("output.png");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            Rasterizer.savePng(image, file);
            status.setText("Saved " + file.getName());
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Could not save: " + ex.getMessage()).showAndWait();
        }
    }

    /** Opens the current rasterized output in an ImageJ window for pixel analysis. */
    private void openImageJ() {
        if (image == null) {
            new Alert(Alert.AlertType.INFORMATION, "Please click Render first before opening in ImageJ.").showAndWait();
            return;
        }
        try {
            ImagePlus imp = new ImagePlus("RRR-SVGs - ImageJ Analysis", image);
            imp.show();
            status.setText("Opened in ImageJ (" + image.getWidth() + "×" + image.getHeight() + " px)");
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "ImageJ could not be opened: " + ex.getMessage()).showAndWait();
        }
    }

    private static WritableImage toFxImage(BufferedImage source) {
        WritableImage result = new WritableImage(source.getWidth(), source.getHeight());
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                result.getPixelWriter().setArgb(x, y, source.getRGB(x, y));
            }
        }
        return result;
    }
}
