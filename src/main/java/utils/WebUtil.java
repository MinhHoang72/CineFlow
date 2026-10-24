package utils;

import model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WebUtil {

    /** Trang dang nhap cua nhom (LoginServlet luu User vao session voi key "user"). */
    private static final String LOGIN_PAGE = "/login";

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
        if (user == null || user.getRole() == null) return false;
        return "TICKET_SALES_STAFF".equalsIgnoreCase(user.getRole()) || "STAFF".equalsIgnoreCase(user.getRole());
    }

    /** Duoc xem trang theo doi giao dich: nhan vien ban ve, quan ly, admin (khong gom nhan vien soat ve). */
    public static boolean canConfirmPayments(User user) {
        if (user == null || user.getRole() == null) return false;
        return isSalesStaff(user) || "MANAGER".equalsIgnoreCase(user.getRole()) || "ADMIN".equalsIgnoreCase(user.getRole());
    }

    /**
     * Lay duong dan mac dinh theo vai tro cua nguoi dung (Silent Redirect).
     * - ADMIN -> /dashboard
     * - MANAGER -> /manager/dashboard
     * - STAFF (Sales / Checker) -> /dashboard
     * - CUSTOMER hoac chua dang nhap -> /home
     */
    public static String getRoleDefaultPath(HttpSession session) {
        if (session == null) {
            return "/home";
        }
        Object roleObj = session.getAttribute("role");
        String role = null;
        if (roleObj instanceof String) {
            role = (String) roleObj;
        }
        if (role == null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof User) {
                role = ((User) userObj).getRole();
            }
        }
        return getRoleDefaultPathByRole(role);
    }

    /**
     * Lay duong dan mac dinh theo doi tuong User.
     */
    public static String getRoleDefaultPath(User user) {
        if (user == null) {
            return "/home";
        }
        return getRoleDefaultPathByRole(user.getRole());
    }

    /**
     * Xac dinh duong dan mac dinh theo ten vai tro.
     */
    private static String getRoleDefaultPathByRole(String role) {
        if (role == null || role.isBlank()) {
            return "/home";
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return "/dashboard";
        }
        if ("MANAGER".equalsIgnoreCase(role)) {
            return "/manager/dashboard";
        }
        if ("STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES_STAFF".equalsIgnoreCase(role)
                || "TICKET_CHECKER_STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES".equalsIgnoreCase(role)
                || "TICKET_CHECKER".equalsIgnoreCase(role)) {
            return "/dashboard";
        }
        return "/home";
    }

    /**
     * Kiem tra duong dan chuyen tiep (redirect URL) co hop le voi vai tro hay khong.
     * Ngan chan lo hong Open Redirect va phan lap tuyet doi giua luong Khach hang va Noi bo.
     */
    public static boolean isAllowedRedirect(String path, String role) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String clean = path.trim();
        if (!clean.startsWith("/") || clean.startsWith("//")) {
            return false;
        }

        String lower = clean.toLowerCase();
        if (lower.contains("/login") || lower.contains("/register") || lower.contains("/logout")) {
            return false;
        }

        // Cac trang thiet lap tai khoan chung danh cho moi nguoi dung da dang nhap
        if (lower.startsWith("/account") || lower.startsWith("/change-password")) {
            return true;
        }

        boolean isCust = role == null || role.isBlank() || "CUSTOMER".equalsIgnoreCase(role);
        if (isCust) {
            // Khach hang khong duoc phep redirect vao cac module quan ly / noi bo
            if (lower.startsWith("/admin") || lower.startsWith("/manager")
                    || lower.startsWith("/staff") || lower.startsWith("/dashboard")
                    || lower.startsWith("/reports") || lower.startsWith("/halls")) {
                return false;
            }
            return true;
        }

        // Tai khoan noi bo (ADMIN, MANAGER, STAFF):
        // Tuyet doi khong redirect vao luong mua hang hoac trang cong cong cua khach hang
        if (lower.startsWith("/home") || lower.startsWith("/movie")
                || lower.startsWith("/support") || lower.startsWith("/contact")
                || lower.startsWith("/cinema") || lower.equals("/")) {
            return false;
        }
        if (lower.startsWith("/booking") && !lower.contains("action=detail")) {
            return false;
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            return lower.startsWith("/admin") || lower.startsWith("/dashboard")
                    || lower.startsWith("/manager") || lower.startsWith("/staff")
                    || lower.startsWith("/reports") || lower.contains("action=detail");
        }

        if ("MANAGER".equalsIgnoreCase(role)) {
            return lower.startsWith("/manager") || lower.startsWith("/staff")
                    || lower.startsWith("/reports") || lower.contains("action=detail");
        }

        // STAFF (TICKET_SALES_STAFF, TICKET_CHECKER_STAFF, STAFF)
        return lower.startsWith("/staff") || lower.startsWith("/dashboard")
                || lower.contains("action=detail");
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
