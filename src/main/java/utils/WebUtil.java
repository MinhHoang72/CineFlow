package utils;

import model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WebUtil {

    /** Trang dang nhap cua nhom (LoginServlet luu User vao session voi key "user"). */
    private static final String LOGIN_PAGE = "/auth/login.jsp";

    /**
     * Tra ve user dang nhap; neu chua dang nhap thi redirect sang trang login va tra ve null.
     */
    public static User requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Object user = session == null ? null : session.getAttribute("user");
        if (user instanceof User) return (User) user;

        String back = req.getRequestURI().substring(req.getContextPath().length());
        if ("GET".equals(req.getMethod()) && req.getQueryString() != null) back += "?" + req.getQueryString();
        resp.sendRedirect(req.getContextPath() + LOGIN_PAGE + "?redirect="
                + URLEncoder.encode(back, StandardCharsets.UTF_8));
        return null;
    }

    /** Nhan vien ban ve (TICKET_SALES_STAFF). STAFF giu lai cho du lieu cu. */
    public static boolean isSalesStaff(User user) {
        return "TICKET_SALES_STAFF".equals(user.getRole()) || "STAFF".equals(user.getRole());
    }

    /** Duoc doi soat / xac nhan thanh toan: nhan vien ban ve, quan ly, admin (khong gom nhan vien soat ve). */
    public static boolean canConfirmPayments(User user) {
        return isSalesStaff(user) || "MANAGER".equals(user.getRole()) || "ADMIN".equals(user.getRole());
    }

    /** Thong bao hien 1 lan o trang ke tiep (sau redirect). type: success | danger | warning | info */
    public static void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("flashType", type);
        req.getSession().setAttribute("flashMessage", message);
    }

    public static Integer parseInt(String value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
