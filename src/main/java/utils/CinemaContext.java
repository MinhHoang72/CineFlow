package utils;

import dao.CentreDAO;
import dao.UserDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.User;

import java.util.List;

/**
 * Tiện ích quản lý ngữ cảnh người dùng và cụm rạp chiếu (Cinema Context) trong phiên làm việc.
 * Chịu trách nhiệm xác định rạp chiếu và tài khoản đang thao tác theo vai trò (ADMIN / MANAGER).
 */
public final class CinemaContext {

    private static final UserDAO userDAO = new UserDAO();
    private static final CentreDAO centreDAO = new CentreDAO();

    private CinemaContext() { }

    /**
     * Lấy ID người dùng hiện tại từ phiên đăng nhập (Session).
     *
     * @param request yêu cầu HTTP
     * @return ID người dùng
     * @throws IllegalStateException nếu chưa đăng nhập
     */
    public static int requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute("userId");
        if (id instanceof Number) {
            return ((Number) id).intValue();
        }
        Object user = session == null ? null : session.getAttribute("user");
        if (user instanceof User) {
            return ((User) user).getUserId();
        }
        throw new IllegalStateException("Vui lòng đăng nhập để thực hiện thao tác này.");
    }

    /**
     * Xác định ID cụm rạp chiếu đang thao tác của người dùng.
     * - Nếu là ADMIN: Lấy rạp qua parameter, session, hoặc rạp hoạt động đầu tiên.
     * - Nếu là MANAGER: Lấy rạp được bổ nhiệm hoặc rạp trực thuộc.
     *
     * @param request yêu cầu HTTP
     * @return ID cụm rạp chiếu
     * @throws IllegalStateException nếu không tìm thấy rạp khả dụng
     */
    public static int requireCentreId(HttpServletRequest request) {
        int userId = requireUserId(request);
        String[] roleAndCentre = userDAO.getUserRoleAndAssignedCentre(userId);
        if (roleAndCentre != null) {
            String role = roleAndCentre[0];
            if ("ADMIN".equalsIgnoreCase(role)) {
                List<Centre> centres;
                try {
                    centres = centreDAO.getActiveCentres();
                } catch (java.sql.SQLException e) {
                    throw new IllegalStateException("Không thể tải danh sách cụm rạp hoạt động.", e);
                }
                Integer centre = parseCentre(request.getParameter("centreId"));
                if (centre == null) {
                    centre = parseCentre(request.getParameter("branchId"));
                }
                if (centre == null && request.getSession().getAttribute("centreId") != null) {
                    centre = parseCentre(request.getSession().getAttribute("centreId").toString());
                }
                final Integer chosen = centre;
                if (chosen == null || centres.stream().noneMatch(c -> c.getCentreId() == chosen)) {
                    centre = centres.isEmpty() ? null : centres.get(0).getCentreId();
                }
                if (centre != null) {
                    request.getSession().setAttribute("centreId", centre);
                    // Hỗ trợ cả tên chuẩn và tên cũ để tương thích hoàn toàn
                    request.setAttribute("cinemaCentres", centres);
                    request.setAttribute("cinemaCentreId", centre);
                    request.setAttribute("fe04Centres", centres);
                    request.setAttribute("fe04CentreId", centre);
                    return centre;
                }
            } else {
                String centreStr = roleAndCentre[1];
                if (centreStr != null) {
                    try {
                        int centre = Integer.parseInt(centreStr);
                        if (centre > 0) {
                            return centre;
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        throw new IllegalStateException("Vui lòng chọn cụm rạp hoặc cấu hình phân công rạp hoạt động.");
    }

    private static Integer parseCentre(String value) {
        if (value == null) {
            return null;
        }
        try {
            int id = Integer.parseInt(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
