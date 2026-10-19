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
 * Controller: Phục vụ các tài nguyên đa phương tiện tĩnh (ảnh đại diện avatar, poster phim, trailer video)
 * đã được tải lên máy chủ, ngăn chặn tấn công Path Traversal.
 * URL ánh xạ: /uploads/*
 */
public class MediaResourceServlet extends HttpServlet {
    private static final Set<String> CATEGORIES = Set.of("avatars", "movies", "fnb");
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "mp4", "webm", "ogg");

    /**
     * Xử lý yêu cầu HTTP GET: Xác thực đường dẫn tài nguyên an toàn và truyền phát dữ liệu file nhị phân về client.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
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

        // 2. Tách và kiểm tra các đoạn thư mục (phòng chống tấn công Path Traversal: .., \, %)
        String[] segments = pathInfo.substring(1).split("/", -1);
        if (segments.length != 2 || !CATEGORIES.contains(segments[0])
                || segments[1].isBlank() || segments[1].contains("..")
                || segments[1].contains("\\") || segments[1].contains("%")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 3. Kiểm tra định dạng phần mở rộng (extension) có nằm trong danh sách cho phép hay không
        String filename = segments[1];
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
        if (!EXTENSIONS.contains(extension)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 4. Xác định đường dẫn file vật lý trên máy chủ và kiểm tra file có tồn tại hay không
        Path root = MediaStorage.resourcesRoot(getServletContext()).toAbsolutePath().normalize();
        Path file = root.resolve("uploads").resolve(segments[0]).resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 5. Cấu hình header phản hồi HTTP (Content-Type, Content-Length, Cache-Control)
        String contentType = Files.probeContentType(file);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setContentLengthLong(Files.size(file));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=86400");

        // 6. Truyền phát luồng dữ liệu nhị phân của tệp tin về trình duyệt client
        try (InputStream input = Files.newInputStream(file);
             OutputStream output = response.getOutputStream()) {
            input.transferTo(output);
        }
    }
}
