package utils;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves persistent user media outside the generated build/web directory. */
public final class MediaStorage {
    private MediaStorage() { }

    public static Path uploadDirectory(ServletContext context, String category) throws IOException {
        if (!"avatars".equals(category) && !"movies".equals(category) && !"fnb".equals(category)) {
            throw new IllegalArgumentException("Unsupported media category.");
        }
        String configuredRoot = context.getInitParameter("media.root");
        Path resourcesRoot;
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            resourcesRoot = Paths.get(configuredRoot).toAbsolutePath().normalize();
        } else {
            String deployedRoot = context.getRealPath("/");
            if (deployedRoot == null) {
                throw new IOException("Cannot resolve the application directory; configure media.root.");
            }
            Path webRoot = Paths.get(deployedRoot).toAbsolutePath().normalize();
            Path projectRoot = webRoot.getParent() == null ? null : webRoot.getParent().getParent();
            if (projectRoot == null || !Files.isDirectory(projectRoot.resolve("src"))) {
                throw new IOException("Cannot resolve the project resources directory; configure media.root.");
            }
            resourcesRoot = projectRoot.resolve("resources");
        }
        Path directory = resourcesRoot.resolve("uploads").resolve(category).normalize();
        if (!directory.startsWith(resourcesRoot)) {
            throw new IOException("Invalid media directory.");
        }
        Files.createDirectories(directory);
        return directory;
    }

    public static Path resourcesRoot(ServletContext context) throws IOException {
        uploadDirectory(context, "avatars");
        return uploadDirectory(context, "avatars").getParent().getParent();
    }
}
