package controller.admin.cinema;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.dto.common.PageDTO;
import model.entity.Centre;
import model.entity.Hall;
import model.entity.User;
import service.CinemaService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller: Quản lý Phòng chiếu (Halls).
 * Toàn bộ logic nghiệp vụ được ủy quyền cho CinemaService (Mô hình 3 lớp).
 */
@WebServlet(name = "HallManagementServlet", urlPatterns = {"/manager/halls", "/admin/halls"})
public class HallManagementServlet extends HttpServlet {

    private final CinemaService cinemaService = new CinemaService();

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

    private User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userObj = session.getAttribute("user");
        return userObj instanceof User ? (User) userObj : null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String redirectUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/manager/halls", StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
            return;
        }

        if (!isManagerOrAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang Quản lý phòng chiếu.");
            return;
        }

        User user = getCurrentUser(session);
        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRole());

        int targetCentreId = 0;
        List<Centre> allCentres = null;

        if (isAdmin) {
            allCentres = cinemaService.getAllCentres(null, "ALL");
            String centreParam = request.getParameter("centreId");
            if (centreParam != null && !centreParam.trim().isEmpty()) {
                try {
                    targetCentreId = Integer.parseInt(centreParam.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (targetCentreId <= 0 && allCentres != null && !allCentres.isEmpty()) {
                targetCentreId = allCentres.get(0).getCentreId();
            }
        } else {
            if (user != null && user.getCentreId() != null && user.getCentreId() > 0) {
                targetCentreId = user.getCentreId();
            }
        }

        if (targetCentreId <= 0) {
            request.setAttribute("noCentreAssigned", true);
            request.setAttribute("activeNav", "halls");
            request.setAttribute("pageTitle", "Quản lý Phòng chiếu");
            request.getRequestDispatcher("/WEB-INF/views/admin/cinema/hallList.jsp").forward(request, response);
            return;
        }

        Centre currentCentre = cinemaService.getCentreById(targetCentreId);

        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) {
            status = "ALL";
        }

        int page = 1;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty())
                page = Integer.parseInt(pageParam.trim());
        } catch (NumberFormatException ignored) { }

        PageDTO<Hall> pageResult = cinemaService.searchHalls(targetCentreId, search, status, page, 6);
        CinemaService.HallKpi kpi = cinemaService.getHallsKpi(targetCentreId);

        request.setAttribute("page", pageResult);
        request.setAttribute("currentCentre", currentCentre);
        request.setAttribute("targetCentreId", targetCentreId);
        request.setAttribute("allCentres", allCentres);
        request.setAttribute("isAdmin", isAdmin);
        request.setAttribute("halls", pageResult.getItems());
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);
        request.setAttribute("totalHalls", kpi.totalHalls());
        request.setAttribute("activeHalls", kpi.activeHalls());
        request.setAttribute("inactiveHalls", kpi.inactiveHalls());
        request.setAttribute("totalCapacity", kpi.totalCapacity());
        request.setAttribute("activeNav", "halls");
        request.setAttribute("pageTitle", "Quản lý Phòng chiếu");

        request.getRequestDispatcher("/WEB-INF/views/admin/cinema/hallList.jsp").forward(request, response);
    }

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

        // Thêm mới phòng chiếu
        if ("create".equalsIgnoreCase(action)) {
            String hallName = request.getParameter("hallName");
            String hallType = request.getParameter("hallType");
            String capacityParam = request.getParameter("capacity");
            String status = request.getParameter("status");

            CinemaService.Result<Hall> result = cinemaService.createHall(centreId, hallName, hallType, capacityParam, status);
            if (result.isSuccess()) {
                redirectUrl.append("success=created");
            } else {
                String err = switch (result.getStatus()) {
                    case MISSING_FIELDS -> "missing_fields";
                    case DUPLICATE_NAME -> "duplicate_name";
                    default -> "failed";
                };
                redirectUrl.append("error=").append(err);
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // Chỉnh sửa phòng chiếu
        if ("update".equalsIgnoreCase(action)) {
            String hallIdParam = request.getParameter("hallId");
            String hallName = request.getParameter("hallName");
            String hallType = request.getParameter("hallType");
            String capacityParam = request.getParameter("capacity");
            String newStatus = request.getParameter("status");

            int hallId;
            try {
                hallId = Integer.parseInt(hallIdParam.trim());
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            CinemaService.Result<Hall> result = cinemaService.updateHall(centreId, hallId, hallName, hallType, capacityParam, newStatus);
            if (result.isSuccess()) {
                redirectUrl.append("success=updated");
            } else {
                String err = switch (result.getStatus()) {
                    case INVALID_HALL -> "invalid_hall";
                    case MISSING_FIELDS -> "missing_fields";
                    case HAS_FUTURE_BOOKINGS -> "has_future_bookings";
                    case DUPLICATE_NAME -> "duplicate_name";
                    default -> "failed";
                };
                redirectUrl.append("error=").append(err);
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // Toggle Status
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

            CinemaService.Result<Void> result = cinemaService.toggleHallStatus(centreId, hallId);
            if (result.isSuccess()) {
                redirectUrl.append("success=status_updated");
            } else {
                String err = switch (result.getStatus()) {
                    case INVALID_HALL -> "invalid_hall";
                    case HAS_FUTURE_BOOKINGS -> "has_future_bookings";
                    default -> "failed";
                };
                redirectUrl.append("error=").append(err);
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
