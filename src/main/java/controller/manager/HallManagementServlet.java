package controller.manager;

import dao.CentreDAO;
import dao.HallDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.Hall;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller điều khiển phân hệ Quản lý Phòng chiếu (UC-FE-03.2: Manage Cinema Halls).
 * Primary Actor: Centre Manager (hoặc Admin có toàn quyền).
 *
 * Quy tắc nghiệp vụ & Luồng xử lý:
 * - BR-HS-01: Mỗi phòng chiếu (Hall) phải thuộc về duy nhất một cụm rạp (Centre).
 * - Trường hợp A (Thêm mới): Kiểm tra không trùng tên phòng trong cùng cụm rạp (Alt 1) -> Lưu DB.
 * - Trường hợp B (Chỉnh sửa): Cập nhật thông số kỹ thuật (2D/3D), sức chứa, trạng thái.
 * - Alt 2: Không thể chuyển phòng sang Inactive nếu phòng đang có suất chiếu tương lai đã mở bán vé.
 */
@WebServlet(name = "HallManagementServlet", urlPatterns = {"/manager/halls", "/admin/halls"})
public class HallManagementServlet extends HttpServlet {

    private final HallDAO hallDAO = new HallDAO();
    private final CentreDAO centreDAO = new CentreDAO();

    /**
     * Kiểm tra người dùng hiện tại có vai trò MANAGER hoặc ADMIN hay không.
     */
    private boolean isManagerOrAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String) {
            String role = (String) roleObj;
            if ("MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String role = ((User) userObj).getRole();
            return role != null && ("MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role));
        }
        return false;
    }

    /**
     * Lấy đối tượng User đang đăng nhập trong phiên làm việc.
     */
    private User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            return (User) userObj;
        }
        return null;
    }

    /**
     * Xử lý yêu cầu HTTP GET:
     * - Tải danh sách phòng chiếu của Cụm rạp mà Manager phụ trách (hoặc Cụm rạp được Admin chọn).
     * - Hiển thị giao diện quản lý phòng chiếu (Mã phòng, Tên phòng, Định dạng 2D/3D, Tổng sức chứa, Trạng thái).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Kiểm tra xác thực đăng nhập
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String redirectUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/manager/halls", StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
            return;
        }

        // 2. Kiểm tra quyền hạn Manager hoặc Admin
        if (!isManagerOrAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang Quản lý phòng chiếu.");
            return;
        }

        User user = getCurrentUser(session);
        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRole());

        // 3. Xác định Cụm rạp (Centre) mục tiêu cần quản lý phòng chiếu
        int targetCentreId = 0;
        List<Centre> allCentres = null;

        if (isAdmin) {
            // Admin có toàn quyền quản lý mọi rạp -> Lấy danh sách tất cả cụm rạp để chọn qua Dropdown
            allCentres = centreDAO.getAllCentres(null, "ALL");
            String centreParam = request.getParameter("centreId");
            if (centreParam != null && !centreParam.trim().isEmpty()) {
                try {
                    targetCentreId = Integer.parseInt(centreParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            // Nếu Admin chưa chọn rạp cụ thể, mặc định lấy rạp đầu tiên trong hệ thống
            if (targetCentreId <= 0 && allCentres != null && !allCentres.isEmpty()) {
                targetCentreId = allCentres.get(0).getCentreId();
            }
        } else {
            // Manager chỉ phụ trách cụm rạp được bổ nhiệm
            if (user != null && user.getCentreId() != null && user.getCentreId() > 0) {
                targetCentreId = user.getCentreId();
            }
        }

        // 4. Nếu Manager chưa được phân công vào bất kỳ cụm rạp nào
        if (targetCentreId <= 0) {
            request.setAttribute("noCentreAssigned", true);
            request.setAttribute("activeNav", "halls");
            request.setAttribute("pageTitle", "Quản lý Phòng chiếu");
            request.getRequestDispatcher("/WEB-INF/views/manager/hallList.jsp").forward(request, response);
            return;
        }

        // 5. Lấy thông tin Cụm rạp hiện tại
        Centre currentCentre = centreDAO.getCentreById(targetCentreId);

        // 6. Đọc các tham số tìm kiếm, bộ lọc trạng thái và phân trang
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) {
            status = "ALL";
        }

        int page = 1;
        int pageSize = 6; // Hiển thị 6 phòng chiếu / trang
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                page = Integer.parseInt(pageParam.trim());
            }
        } catch (NumberFormatException e) {
            page = 1;
        }
        if (page < 1) {
            page = 1;
        }

        // 7. Tính toán tổng số phòng theo bộ lọc và số trang
        int totalFilteredHalls = hallDAO.countHallsByCentre(targetCentreId, search, status);
        int totalPages = (int) Math.ceil((double) totalFilteredHalls / pageSize);
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        // 8. Lấy danh sách các phòng chiếu trang hiện tại
        List<Hall> halls = hallDAO.getHallsByCentrePaged(targetCentreId, search, status, page, pageSize);

        // 9. Thống kê số lượng phòng và sức chứa tổng thể (của cụm rạp)
        int totalHalls = hallDAO.countHallsByStatus(targetCentreId, "ALL");
        int activeHalls = hallDAO.countHallsByStatus(targetCentreId, "ACTIVE");
        int inactiveHalls = hallDAO.countHallsByStatus(targetCentreId, "INACTIVE");
        int totalCapacity = hallDAO.sumCapacityByCentre(targetCentreId);

        // 10. Đặt các thuộc tính vào request scope
        request.setAttribute("currentCentre", currentCentre);
        request.setAttribute("targetCentreId", targetCentreId);
        request.setAttribute("allCentres", allCentres);
        request.setAttribute("isAdmin", isAdmin);
        request.setAttribute("halls", halls);
        request.setAttribute("currentPage", page);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalFilteredHalls", totalFilteredHalls);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);
        request.setAttribute("totalHalls", totalHalls);
        request.setAttribute("activeHalls", activeHalls);
        request.setAttribute("inactiveHalls", inactiveHalls);
        request.setAttribute("totalCapacity", totalCapacity);
        request.setAttribute("activeNav", "halls");
        request.setAttribute("pageTitle", "Quản lý Phòng chiếu");

        // 11. Chuyển tiếp tới giao diện JSP
        request.getRequestDispatcher("/WEB-INF/views/manager/hallList.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST:
     * - Thêm mới phòng chiếu (Trường hợp A)
     * - Sửa phòng chiếu (Trường hợp B)
     * - Toggle trạng thái phòng chiếu (Bật/Tắt Active <-> Inactive)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (!isManagerOrAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        User user = getCurrentUser(session);
        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRole());

        // Xác định CentreID được thao tác
        int centreId = 0;
        if (isAdmin) {
            String centreParam = request.getParameter("centreId");
            try {
                centreId = Integer.parseInt(centreParam.trim());
            } catch (Exception ignored) {
            }
        } else if (user != null && user.getCentreId() != null) {
            centreId = user.getCentreId();
        }

        if (centreId <= 0) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=no_centre");
            return;
        }

        // Đường dẫn chuyển hướng giữ nguyên centreId nếu là Admin, kèm tham số lọc và phân trang
        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/manager/halls?");
        if (isAdmin) {
            redirectUrl.append("centreId=").append(centreId).append("&");
        }

        String filterSearch = request.getParameter("search");
        String filterStatus = request.getParameter("status");
        String pageParam = request.getParameter("page");
        if (filterSearch != null && !filterSearch.trim().isEmpty()) {
            redirectUrl.append("search=").append(URLEncoder.encode(filterSearch.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (filterStatus != null && !filterStatus.trim().isEmpty()) {
            redirectUrl.append("status=").append(URLEncoder.encode(filterStatus.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (pageParam != null && !pageParam.trim().isEmpty()) {
            redirectUrl.append("page=").append(URLEncoder.encode(pageParam.trim(), StandardCharsets.UTF_8)).append("&");
        }

        String action = request.getParameter("action");

        // ==============================================================
        // TRƯỜNG HỢP A: THÊM MỚI PHÒNG CHIẾU (ADD HALL)
        // ==============================================================
        if ("create".equalsIgnoreCase(action)) {
            String hallName = request.getParameter("hallName");
            String hallType = request.getParameter("hallType"); // '2D' hoặc '3D'
            String capacityParam = request.getParameter("capacity");
            String status = request.getParameter("status"); // Mặc định là Active ('AVAILABLE')

            // Kiểm tra rỗng tên phòng
            if (hallName == null || hallName.trim().isEmpty()) {
                redirectUrl.append("error=missing_fields");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            hallName = hallName.trim();
            if (status == null || status.trim().isEmpty()) {
                status = "AVAILABLE";
            }

            int capacity = 60; // Mặc định 60 ghế
            if (capacityParam != null && !capacityParam.trim().isEmpty()) {
                try {
                    capacity = Integer.parseInt(capacityParam.trim());
                    if (capacity <= 0) {
                        capacity = 60;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // Alt 1: Kiểm tra trùng tên phòng chiếu trong cùng cụm rạp
            if (hallDAO.isHallNameExists(centreId, hallName, 0)) {
                redirectUrl.append("error=duplicate_name");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            Hall hall = new Hall();
            hall.setCentreId(centreId);
            hall.setHallName(hallName);
            hall.setHallType(hallType != null ? hallType.trim() : "2D");
            hall.setCapacity(capacity);
            hall.setStatus(status);

            boolean ok = hallDAO.addHall(hall);
            if (ok) {
                redirectUrl.append("success=created");
            } else {
                redirectUrl.append("error=failed");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // ==============================================================
        // TRƯỜNG HỢP B: CHỈNH SỬA PHÒNG CHIẾU (EDIT HALL)
        // ==============================================================
        if ("update".equalsIgnoreCase(action)) {
            String hallIdParam = request.getParameter("hallId");
            String hallName = request.getParameter("hallName");
            String hallType = request.getParameter("hallType");
            String capacityParam = request.getParameter("capacity");
            String newStatus = request.getParameter("status"); // 'AVAILABLE' (Active) hoặc 'INACTIVE'

            int hallId;
            try {
                hallId = Integer.parseInt(hallIdParam.trim());
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            Hall existingHall = hallDAO.getHallById(hallId);
            if (existingHall == null || existingHall.getCentreId() != centreId) {
                redirectUrl.append("error=invalid_hall");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            if (hallName == null || hallName.trim().isEmpty()) {
                redirectUrl.append("error=missing_fields");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            hallName = hallName.trim();
            int capacity = existingHall.getCapacity();
            if (capacityParam != null && !capacityParam.trim().isEmpty()) {
                try {
                    capacity = Integer.parseInt(capacityParam.trim());
                    if (capacity <= 0) {
                        capacity = existingHall.getCapacity();
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // Alt 2: Thay đổi trạng thái phòng đang có lịch chiếu / đặt vé
            // Nếu Actor cố gắng chuyển trạng thái phòng sang Inactive nhưng phòng đang gắn với các suất chiếu tương lai đã bán vé
            boolean isChangingToInactive = existingHall.isActive() && ("INACTIVE".equalsIgnoreCase(newStatus) || "MAINTENANCE".equalsIgnoreCase(newStatus));
            if (isChangingToInactive) {
                if (hallDAO.hasFutureBookedShowtimes(hallId)) {
                    redirectUrl.append("error=has_future_bookings");
                    response.sendRedirect(redirectUrl.toString());
                    return;
                }
            }

            // Alt 1: Kiểm tra trùng tên phòng chiếu trong cùng cụm rạp
            if (hallDAO.isHallNameExists(centreId, hallName, hallId)) {
                redirectUrl.append("error=duplicate_name");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            Hall updatedHall = new Hall();
            updatedHall.setHallId(hallId);
            updatedHall.setCentreId(centreId);
            updatedHall.setHallName(hallName);
            updatedHall.setHallType(hallType != null ? hallType.trim() : "2D");
            updatedHall.setCapacity(capacity);
            updatedHall.setStatus(newStatus != null ? newStatus.trim() : "AVAILABLE");

            boolean ok = hallDAO.updateHall(updatedHall);
            if (ok) {
                redirectUrl.append("success=updated");
            } else {
                redirectUrl.append("error=failed");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // ==============================================================
        // TOGGLE STATUS: BẬT / TẮT TRẠNG THÁI ACTIVE <-> INACTIVE
        // ==============================================================
        if ("toggleStatus".equalsIgnoreCase(action)) {
            String hallIdParam = request.getParameter("hallId");
            int hallId;
            try {
                hallId = Integer.parseInt(hallIdParam.trim());
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            Hall existingHall = hallDAO.getHallById(hallId);
            if (existingHall == null || existingHall.getCentreId() != centreId) {
                redirectUrl.append("error=invalid_hall");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            String result = hallDAO.toggleStatus(hallId);
            if ("SUCCESS".equals(result)) {
                redirectUrl.append("success=status_updated");
            } else if ("HAS_FUTURE_BOOKINGS".equals(result)) {
                redirectUrl.append("error=has_future_bookings");
            } else {
                redirectUrl.append("error=failed");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
