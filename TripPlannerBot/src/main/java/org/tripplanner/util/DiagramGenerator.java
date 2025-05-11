package org.tripplanner.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

@Component
public class DiagramGenerator {

    public void generateDiagram(String moduleName, String outputPath) throws IOException {
        // Создаем директорию для диаграмм, если она не существует
        Path directory = Paths.get(outputPath).getParent();
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        // Создаем PlantUML диаграмму
        String plantUmlSource = """
            @startuml
            package "%s" {
                [Controller]
                [Service]
                [Repository]
            }
            @enduml
            """.formatted(moduleName);

        // Генерируем изображение
        SourceStringReader reader = new SourceStringReader(plantUmlSource);
        try (FileOutputStream os = new FileOutputStream(outputPath)) {
            reader.generateImage(os, new FileFormatOption(FileFormat.PNG));
        }
    }

    public void generateModuleDiagram(String moduleName, String outputPath) throws IOException {
        generateDiagram(moduleName, outputPath);
    }
} 