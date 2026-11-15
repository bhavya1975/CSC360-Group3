# CSC360-Group3 — RRR-SVGs: Reading, Rendering and Rasterization

| Folder | What it is |
|---|---|
| `rrr-svgs/` | Bhavya's prototype (`SVGReader` prints the elements of an SVG, ImageJ & Batik integration). |
| `rrr-svgs-app/` | The full app: type SVG code (or open an `.svg` file), click **Render** to see the image drawn by our own renderer, and click **Save PNG** to export it. |

## Run the app

Requires JDK 21+ and Maven.

```bash
cd rrr-svgs-app
mvn javafx:run    # start the app (edit the code and click Render, or open a file from samples/)
mvn test          # run the tests
```

## How it works

```
SVG code → 1. Reader → 2. Interpreter → 3. Renderer → 4. Rasterizer → image / PNG
```

| Stage | Package | What it does |
|---|---|---|
| 1. Reader | `reader` | Parses the XML into a tree of `SvgNode`s; rejects broken XML and non-SVG files. |
| 2. Interpreter | `interpreter` | Turns `rect`, `circle`, `ellipse`, `line`, `polyline`, `polygon` and `path` (M L H V C Q Z) into `Shape`s with fill, stroke, opacity and transforms (translate, scale, rotate, matrix, viewBox). |
| 3. Renderer | `renderer` | Converts each shape's fill and stroke into polygons (`DrawOp`s), in drawing order. |
| 4. Rasterizer | `rasterizer` | Scanline fill: finds which pixels each polygon covers, blends the colour with anti-aliasing, and saves the PNG. |

Not supported: text, images, gradients, arcs, dashes.
