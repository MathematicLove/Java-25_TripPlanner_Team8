package org.tripplanner.util;


import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class DiagramGeneratorTest {

    @Test
    public void testGenerateModuleDiagram() throws Exception {
        System.out.println("Starting test...");

        DiagramGenerator diagramGenerator = new DiagramGenerator();
        System.out.println("DiagramGenerator created");
        
        String outputPath = "src/test/resources/diagrams/test-module.png";
        System.out.println("Generating diagram to: " + outputPath);
        
        diagramGenerator.generateModuleDiagram("TestModule", outputPath);
        System.out.println("Diagram generation completed");

        File diagramFile = new File(outputPath);
        System.out.println("Checking if file exists: " + diagramFile.getAbsolutePath());
        assertTrue(diagramFile.exists(), "Диаграмма должна быть создана");
        assertTrue(diagramFile.length() > 0, "Файл диаграммы не должен быть пустым");
        System.out.println("Test completed successfully");
    }
} 