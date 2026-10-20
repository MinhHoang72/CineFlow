package utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.*;
import model.User;

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
                        String selected = request.getParameter("centreId");
                        if (selected == null) selected = request.getParameter("branchId");
                        if (selected == null && request.getSession().getAttribute("centreId") != null)
                            selected = request.getSession().getAttribute("centreId").toString();
                        if (selected != null && Integer.parseInt(selected) > 0) {
                            int centre = Integer.parseInt(selected);
                            request.getSession().setAttribute("centreId", centre);
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
}
