package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.MediaStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Phục vụ các tài nguyên đa phương tiện tĩnh (ảnh đại diện, poster phim, trailer) đã được tải lên máy chủ.
 */
public class MediaResourceServlet extends HttpServlet {

    private static final Set<String> CATEGORIES = Set.of("avatars", "movies");
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "mp4", "webm", "ogg");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 4) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String[] segments = pathInfo.substring(1).split("/", -1);
        if (segments.length != 2 || !CATEGORIES.contains(segments[0])
                || segments[1].isBlank() || segments[1].contains("..")
                || segments[1].contains("\\") || segments[1].contains("%")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String filename = segments[1];
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
        if (!EXTENSIONS.contains(extension)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path root = MediaStorage.resourcesRoot(getServletContext()).toAbsolutePath().normalize();
        Path file = root.resolve("uploads").resolve(segments[0]).resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(file);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setContentLengthLong(Files.size(file));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=86400");

        try (InputStream input = Files.newInputStream(file);
             OutputStream output = response.getOutputStream()) {
            input.transferTo(output);
        }
    }
}
