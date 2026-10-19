package utils;

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tiện ích: Quản lý thư mục lưu trữ tệp tin đa phương tiện vật lý (avatar,
 * poster phim, trailer)
 * bên ngoài thư mục build tạm thời để đảm bảo không bị mất file khi redeploy
 * hoặc rebuild server.
 */
public final class MediaStorage {

    private MediaStorage() {
        // Constructor riêng tư ngăn chặn việc khởi tạo đối tượng tiện ích tĩnh
    }

    /**
     * Xác định và tạo thư mục tải lên theo danh mục tương ứng (avatars, movies).
     *
     * @param context  ngữ cảnh ServletContext của ứng dụng
     * @param category danh mục file ("avatars" hoặc "movies")
     * @return đường dẫn Path tới thư mục lưu trữ
     * @throws IOException              nếu không thể giải quyết thư mục hoặc tạo
     *                                  thư mục thất bại
     * @throws IllegalArgumentException nếu danh mục không được hỗ trợ
     */
    public static Path uploadDirectory(ServletContext context, String category) throws IOException {
        // 1. Kiểm tra danh mục hợp lệ (hỗ trợ avatars, poster, video_trailer và movies)
        if (!"avatars".equals(category) && !"movies".equals(category)
                && !"poster".equals(category) && !"video_trailer".equals(category)) {
            throw new IllegalArgumentException("Unsupported media category: " + category);
        }

        // 2. Xác định thư mục gốc lưu trữ từ cấu hình init-param hoặc tự động dò tìm
        // thư mục resources của dự án
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

        // 3. Chuẩn hóa đường dẫn thư mục lưu trữ uploads/{category} và phòng chống Path
        // Traversal
        Path directory = resourcesRoot.resolve("uploads").resolve(category).normalize();
        if (!directory.startsWith(resourcesRoot)) {
            throw new IOException("Invalid media directory.");
        }

        // 4. Tự động tạo thư mục trên ổ đĩa nếu chưa tồn tại
        Files.createDirectories(directory);
        return directory;
    }

    /**
     * Lấy đường dẫn thư mục gốc resources lưu trữ toàn bộ file uploads.
     *
     * @param context ngữ cảnh ServletContext
     * @return đường dẫn Path thư mục gốc resources
     * @throws IOException nếu không thể giải quyết thư mục
     */
    public static Path resourcesRoot(ServletContext context) throws IOException {
        uploadDirectory(context, "avatars");
        return uploadDirectory(context, "avatars").getParent().getParent();
    }
}
