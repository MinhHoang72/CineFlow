package utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.*;
import java.util.List;
import dao.CentreDAO;
import model.entity.Centre;
import model.entity.User;

/** Resolves FE-04 actors and centres without demo IDs. */
public final class FE04Context {
    private FE04Context() { }

    public static int requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute("userId");
        if (id instanceof Number) return ((Number) id).intValue();
        Object user = session == null ? null : session.getAttribute("user");
        if (user instanceof User) return ((User) user).getUserId();
        throw new IllegalStateException("Please sign in to manage showtimes and pricing.");
    }

    public static int requireCentreId(HttpServletRequest request) {
        int userId = requireUserId(request);

        String sql = "SELECT u.Role, u.CentreID, a.CentreID AS AssignedCentreID "
                + "FROM Users u LEFT JOIN CentreManagerAssignments a ON a.ManagerID = u.UserID "
                + "AND a.Status = 'ACTIVE' AND a.StartDate <= GETDATE() "
                + "AND (a.EndDate IS NULL OR a.EndDate > GETDATE()) "
                + "WHERE u.UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("Role");
                    if ("ADMIN".equals(role)) {
                        // ADMIN khong thuoc rap nao: chon rap qua o "Chi nhanh" tren trang (navigation.jspf),
                        // chua chon thi lay rap da chon truoc do, roi den rap dang hoat dong dau tien.
                        List<Centre> centres = new CentreDAO().getActiveCentres();
                        Integer centre = parseCentre(request.getParameter("centreId"));
                        if (centre == null) centre = parseCentre(request.getParameter("branchId"));
                        if (centre == null && request.getSession().getAttribute("centreId") != null)
                            centre = parseCentre(request.getSession().getAttribute("centreId").toString());
                        final Integer chosen = centre;
                        if (chosen == null || centres.stream().noneMatch(c -> c.getCentreId() == chosen)) {
                            centre = centres.isEmpty() ? null : centres.get(0).getCentreId();
                        }
                        if (centre != null) {
                            request.getSession().setAttribute("centreId", centre);
                            request.setAttribute("fe04Centres", centres);
                            request.setAttribute("fe04CentreId", centre);
                            return centre;
                        }
                    } else {
                        int centre = rs.getInt("MANAGER".equals(role) ? "AssignedCentreID" : "CentreID");
                        if (!rs.wasNull() && centre > 0) return centre;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to resolve assigned centre.", e);
        }
        throw new IllegalStateException("Select a centre or configure an active centre assignment.");
    }

    private static Integer parseCentre(String value) {
        if (value == null) return null;
        try {
            int id = Integer.parseInt(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
