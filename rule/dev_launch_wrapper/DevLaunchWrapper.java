package top.fifthlight.fabazel.devlaunchwrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

// Bazel don't allow us to transfer arguments in BUILD file, so let's hack
public class DevLaunchWrapper {
    private static final String version = System.getProperty("dev.launch.version", null);
    private static final String type = System.getProperty("dev.launch.type", null);
    private static final String assetsPath = System.getProperty("dev.launch.assetsPath", null);
    private static final String mainClass = System.getProperty("dev.launch.mainClass", null);

    public static void main(String[] args) throws ReflectiveOperationException, IOException {
        var argsList = new ArrayList<String>(Arrays.asList(args));
        if (assetsPath != null) {
            var path = Path.of(assetsPath).toRealPath();
            argsList.add("--assetsDir");
            argsList.add(path.toString());
            if (version != null) {
                var versionPath = path.resolve(Path.of("versions", version));
                argsList.add("--assetIndex");
                argsList.add(Files.readString(versionPath));
            }
        }

        switch (type) {
            case "client" -> {
                var allowSymlinksPath = Path.of("allowed_symlinks.txt");
                Files.writeString(allowSymlinksPath, "[regex].*\n");
            }
            case "server" -> {
                var serverPropertiesPath = Path.of("server.properties");
                if (!Files.exists(serverPropertiesPath)) {
                    Files.writeString(serverPropertiesPath, "online-mode=false\n");
                }
                argsList.add("--nogui");
                var eulaPath = Path.of("eula.txt");
                Files.writeString(eulaPath, "eula=true\n");
            }
        }

        System.err.println("Launching game with arguments: " + String.join(" ", argsList));
        var array = new String[argsList.size()];
        array = argsList.toArray(array);

        if (mainClass == null) {
            throw new IllegalArgumentException("No main class specified. Specify your real main class with dev.launch.mainClass JVM property.");
        }
        var clazz = ClassLoader.getSystemClassLoader().loadClass(mainClass);
        var mainMethod = clazz.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) array);
    }
}