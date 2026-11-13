
package com.rrrsvg;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

// Apache Batik

public class SVGReader {

    public static void main(String[] args) {

        try {
            // Load the SVG file
            File file = new File("src/main/resources/sample.svg");

            // Create XML parser
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            // Parse SVG into a DOM tree
            Document document = builder.parse(file);

            // Get root SVG element
            Element root = document.getDocumentElement();

            System.out.println("Root Element: " + root.getTagName());

            // Extract all SVG elements
            NodeList elements = root.getElementsByTagName("*");

            System.out.println("\nSVG Elements:");

            for (int i = 0; i < elements.getLength(); i++) {

                Element element = (Element) elements.item(i);

                System.out.println("\nElement: " + element.getTagName());

                // Print all attributes
                for (int j = 0; j < element.getAttributes().getLength(); j++) {

                    var attribute = element.getAttributes().item(j);

                    System.out.println(
                            attribute.getNodeName() + " = " +
                            attribute.getNodeValue()
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
