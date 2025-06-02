package top.fifthlight.fabazel.jarmerger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

// Pool man's shadow
public class JarMerger {
    public static void main(String[] args) throws IOException {
        var outputPath = Path.of(args[0]);
        var processedEntryNames = new HashSet<String>();
        try (var outputStream = new JarOutputStream(Files.newOutputStream(outputPath))) {
            for (var i = 1; i < args.length; i++) {
                var inputPath = Path.of(args[i]);
                try (var inputStream = new JarInputStream(Files.newInputStream(inputPath))) {
                    JarEntry entry;
                    while ((entry = inputStream.getNextJarEntry()) != null) {
                        if (processedEntryNames.contains(entry.getName())) {
                            continue;
                        }
                        processedEntryNames.add(entry.getName());
                        outputStream.putNextEntry(entry);
                        inputStream.transferTo(outputStream);
                    }
                }
            }
        }
    }
}
