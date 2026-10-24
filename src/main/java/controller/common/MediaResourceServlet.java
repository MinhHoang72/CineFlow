package controller.common;
import jakarta.servlet.annotation.WebServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.MediaStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Controller: Phục vụ các tài nguyên đa phương tiện tĩnh (ảnh đại diện avatar, poster phim, trailer video)
 * đã được tải lên máy chủ, hỗ trợ HTTP Range Request cho video streaming và ngăn chặn Path Traversal.
 * URL ánh xạ: /uploads/*
 */
@WebServlet(name = "MediaResourceServlet", urlPatterns = {"/uploads/*"})
public class MediaResourceServlet extends HttpServlet {
    private static final Set<String> CATEGORIES = Set.of("avatars", "movies", "fnb", "poster", "video_trailer");
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "mp4", "webm", "ogg");

    /**
     * Xử lý yêu cầu HTTP GET: Xác thực đường dẫn tài nguyên an toàn và truyền phát dữ liệu file nhị phân về client.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Lấy thông tin đường dẫn con từ request (PathInfo)
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 4) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 2. Tách và kiểm tra các đoạn thư mục
        String[] segments = pathInfo.substring(1).split("/", -1);
        if (segments.length != 2 || !CATEGORIES.contains(segments[0]) || segments[1].isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String rawFilename = segments[1];
        String filename;
        try {
            filename = URLDecoder.decode(rawFilename, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            filename = rawFilename;
        }

        if (filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 3. Kiểm tra định dạng phần mở rộng (extension)
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
        if (!EXTENSIONS.contains(extension)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 4. Xác định đường dẫn file vật lý trên máy chủ
        Path file = null;
        try {
            Path root = MediaStorage.resourcesRoot(getServletContext()).toAbsolutePath().normalize();
            Path candidate = root.resolve("uploads").resolve(segments[0]).resolve(filename).normalize();
            if (candidate.startsWith(root) && Files.isRegularFile(candidate)) {
                file = candidate;
            }
        } catch (Exception ignored) {}

        if (file == null) {
            String deployedPath = getServletContext().getRealPath("/uploads/" + segments[0] + "/" + filename);
            if (deployedPath != null) {
                Path candidate = Path.of(deployedPath).toAbsolutePath().normalize();
                if (Files.isRegularFile(candidate)) {
                    file = candidate;
                }
            }
        }

        if (file == null || !Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 5. Xác định chuẩn MIME Type phù hợp với trình duyệt
        String mimeType = switch (extension) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> {
                String probed = Files.probeContentType(file);
                yield (probed != null) ? probed : "application/octet-stream";
            }
        };

        long fileLength = Files.size(file);
        response.setContentType(mimeType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "public, max-age=86400");
        if (mimeType.startsWith("image/")) {
            response.setHeader("X-Content-Type-Options", "nosniff");
        }

        // 6. Xử lý HTTP Range Requests cho Video Streaming
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=") && fileLength > 0) {
            String rangeValue = rangeHeader.substring(6).trim();
            long start = 0;
            long end = fileLength - 1;

            int dash = rangeValue.indexOf('-');
            if (dash == 0) {
                try {
                    long lastBytes = Long.parseLong(rangeValue.substring(1));
                    start = Math.max(0, fileLength - lastBytes);
                } catch (NumberFormatException ignored) {}
            } else if (dash > 0) {
                try {
                    start = Long.parseLong(rangeValue.substring(0, dash));
                    if (dash < rangeValue.length() - 1) {
                        end = Long.parseLong(rangeValue.substring(dash + 1));
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (start > end || start >= fileLength) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }

            end = Math.min(end, fileLength - 1);
            long contentLength = end - start + 1;

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.setContentLengthLong(contentLength);

            try (InputStream input = Files.newInputStream(file);
                 OutputStream output = response.getOutputStream()) {
                long skipped = input.skip(start);
                long remaining = contentLength;
                byte[] buffer = new byte[8192];
                while (remaining > 0) {
                    int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) break;
                    output.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLengthLong(fileLength);
            try (InputStream input = Files.newInputStream(file);
                 OutputStream output = response.getOutputStream()) {
                input.transferTo(output);
            }
        }
    }
}
