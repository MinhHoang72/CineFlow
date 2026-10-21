package controller.voucher;

import dao.VoucherDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import model.dto.common.PageDTO;
import model.entity.User;
import model.entity.Voucher;

String redirect = request.getRequestURI();
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
            return null;
        }
        boolean isAdmin = user.isAdmin();
        boolean isManager = user.isManager() && user.getCentreId() != null;
        if (!isAdmin && !isManager) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Admin hoặc Manager chi nhánh mới được quản lý voucher.");
            return null;
        }
        // Kiểm tra đường dẫn phù hợp với vai trò
        String uri = request.getRequestURI();
        if (uri.contains("/admin/vouchers") && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Admin mới truy cập được trang quản lý voucher toàn chuỗi.");
            return null;
        }
        if (uri.contains("/manager/vouchers") && !isManager && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Không có quyền truy cập.");
            return null;
        }
        return user;
    }

    /**
     * Trả về base URL của trang voucher tương ứng với role hiện tại.
     * Admin → /admin/vouchers  |  Manager → /manager/vouchers
     */
    private String baseUrl(HttpServletRequest request, User user) {
        return request.getContextPath()
                + (user.isAdmin() && request.getRequestURI().contains("/admin/") 
                   ? "/admin/vouchers" : "/manager/vouchers");
    }

    /**
     * Trả về scopeCentreId để truyền vào DAO.search():
     *   - Admin (đường /admin/vouchers): null → xem tất cả
     *   - Manager: centreId của manager
     */
    private Integer scopeCentreId(HttpServletRequest request, User user) {
        if (user.isAdmin() && request.getRequestURI().contains("/admin/")) {
            return null; // Admin xem toàn chuỗi
        }
        return user.getCentreId(); // Manager chỉ xem của chi nhánh mình
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CSRF
    // ─────────────────────────────────────────────────────────────────────────

    private String csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("voucherCsrf");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("voucherCsrf", token);
        }
        return token;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FLASH
    // ─────────────────────────────────────────────────────────────────────────

    private void flash(HttpServletRequest request, String type, String message) {
        HttpSession session = request.getSession();
        session.setAttribute("voucherFlashType", type);
        session.setAttribute("voucherFlashMessage", message);
    }

    private void showFlash(HttpServletRequest request) {
        HttpSession session = request.getSession();
        request.setAttribute("flashType", session.getAttribute("voucherFlashType"));
        request.setAttribute("flashMessage", session.getAttribute("voucherFlashMessage"));
        session.removeAttribute("voucherFlashType");
        session.removeAttribute("voucherFlashMessage");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHOW LIST
    // ─────────────────────────────────────────────────────────────────────────

    private void showList(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String q = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
        if (q.length() > 100) {
            q = q.substring(0, 100);
        }
        String status = request.getParameter("status") == null ? "" : request.getParameter("status").trim();
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)
                && !"EXPIRED".equals(status) && !"EXHAUSTED".equals(status)) {
            status = "";
        }
        String type = request.getParameter("type") == null ? "" : request.getParameter("type").trim();
        if (!"FIXED".equals(type) && !"PERCENT".equals(type)) {
            type = "";
        }
        int page = 1;
        try {
            page = Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException ignored) {
        }

        String base = baseUrl(request, user);
        Integer scope = scopeCentreId(request, user);
        PageDTO<Voucher> result = dao.search(q, status, type, scope, page, PAGE_SIZE);

        String filters = "q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8)
                + "&type=" + URLEncoder.encode(type, StandardCharsets.UTF_8);
        request.setAttribute("result", result);
        request.setAttribute("q", q);
        request.setAttribute("status", status);
        request.setAttribute("type", type);
        request.setAttribute("pagerBase", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + result.getPage());
        request.setAttribute("csrf", csrfToken(request));
        request.setAttribute("pageTitle", "Voucher");
        request.setAttribute("pageBreadcrumb", "Khuyến mãi");
        request.setAttribute("baseUrl", base);
        request.setAttribute("currentUser", user);
        request.setAttribute("isAdminScope", user.isAdmin() && request.getRequestURI().contains("/admin/"));
        showFlash(request);
        request.getRequestDispatcher("/WEB-INF/views/voucher/voucherList.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHOW FORM
    // ─────────────────────────────────────────────────────────────────────────

    private void showForm(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String base = baseUrl(request, user);
        Voucher voucher = new Voucher();
        voucher.setDiscountType("PERCENT");
        voucher.setStatus("ACTIVE");
        voucher.setMinimumOrderValue(BigDecimal.ZERO);
        voucher.setStartDate(LocalDateTime.now().withSecond(0).withNano(0));
        voucher.setEndDate(LocalDateTime.now().plusMonths(1).withSecond(0).withNano(0));
        // Mặc định scope theo role
        if (user.isManager()) {
            voucher.setCentreId(user.getCentreId());
        }

        String idText = request.getParameter("id");
        if (idText != null && idText.matches("[0-9]+") && idText.length() < 10) {
            voucher = dao.find(Integer.parseInt(idText));
            if (voucher == null) {
                flash(request, "error", "Không tìm thấy voucher.");
                response.sendRedirect(base);
                return;
            }
            // Manager không được sửa voucher của chi nhánh khác hoặc voucher toàn chuỗi
            if (user.isManager() && !dao.belongsToCentre(voucher.getVoucherId(), user.getCentreId())) {
                flash(request, "error", "Bạn không có quyền chỉnh sửa voucher này.");
                response.sendRedirect(base);
                return;
            }
        }
        forwardForm(request, response, voucher, null, null, base, user);
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response,
                             Voucher voucher, Map<String, String> errors, Map<String, String> raw,
                             String base, User user) throws ServletException, IOException {
        request.setAttribute("voucher", voucher);
        request.setAttribute("errors", errors);
        request.setAttribute("raw", raw);
        request.setAttribute("csrf", csrfToken(request));
        request.setAttribute("pageTitle", voucher.getVoucherId() == 0 ? "Thêm voucher" : "Sửa voucher");
        request.setAttribute("pageBreadcrumb", "Khuyến mãi");
request.setAttribute("baseUrl", base);
        request.setAttribute("currentUser", user);
        request.setAttribute("isAdminScope", user.isAdmin() && request.getRequestURI().contains("/admin/"));
        request.getRequestDispatcher("/WEB-INF/views/voucher/voucherForm.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAVE
    // ─────────────────────────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String base = baseUrl(request, user);
        Voucher voucher = new Voucher();
        String idText = request.getParameter("id");
        boolean isEdit = false;
        if (idText != null && idText.matches("[0-9]+") && idText.length() < 10) {
            voucher = dao.find(Integer.parseInt(idText));
            if (voucher == null) {
                flash(request, "error", "Không tìm thấy voucher.");
                response.sendRedirect(base);
                return;
            }
            isEdit = true;
            // Manager không được sửa voucher của chi nhánh khác
            if (user.isManager() && !dao.belongsToCentre(voucher.getVoucherId(), user.getCentreId())) {
                flash(request, "error", "Bạn không có quyền chỉnh sửa voucher này.");
                response.sendRedirect(base);
                return;
            }
        }

        // Gán scope: Admin đường /admin → null (toàn chuỗi); Manager → centreId của mình
        // Khi edit, giữ nguyên CentreID gốc (không cho thay đổi scope sau khi tạo)
        if (!isEdit) {
            if (user.isAdmin() && request.getRequestURI().contains("/admin/")) {
                voucher.setCentreId(null);
            } else {
                voucher.setCentreId(user.getCentreId());
            }
            voucher.setCreatedBy(user.getUserId());
        }

        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, String> raw = new LinkedHashMap<>();
        for (String name : new String[]{"discountValue", "minimumOrderValue", "maximumDiscount",
                "startDate", "endDate", "usageLimit"}) {
            raw.put(name, param(request, name));
        }

        String code = param(request, "code").toUpperCase();
        String name = param(request, "name");
        String description = param(request, "description");
        String type = param(request, "type");
        String status = param(request, "status");

        boolean locked = voucher.getVoucherId() > 0 && voucher.getUsedCount() > 0;
        if (locked) {
            code = voucher.getVoucherCode();
        }
        voucher.setVoucherCode(code);
        voucher.setVoucherName(name);
        voucher.setDescription(description);
        voucher.setDiscountType(type);
        voucher.setStatus(status);

        if (!code.matches("[A-Z0-9_-]{3,50}")) {
            errors.put("code", "Mã gồm 3-50 ký tự chữ hoa, số, gạch ngang hoặc gạch dưới.");
        } else if (dao.codeExists(code, voucher.getVoucherId())) {
            errors.put("code", "Mã voucher đã tồn tại.");
        }
        if (name.isEmpty()) {
            errors.put("name", "Tên voucher là bắt buộc.");
        } else if (name.length() > 150) {
            errors.put("name", "Tên voucher tối đa 150 ký tự.");
        }
        if (description.length() > 500) {
            errors.put("description", "Mô tả tối đa 500 ký tự.");
        }
        if (!"FIXED".equals(type) && !"PERCENT".equals(type)) {
            errors.put("type", "Hãy chọn loại giảm giá.");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            errors.put("status", "Hãy chọn trạng thái.");
        }

        try {
            BigDecimal value = money(raw.get("discountValue"));
            voucher.setDiscountValue(value);
            if (value.signum() <= 0) {
                errors.put("discountValue", "Giá trị giảm phải lớn hơn 0.");
            } else if ("PERCENT".equals(type) && value.compareTo(new BigDecimal("100")) > 0) {
                errors.put("discountValue", "Giảm theo phần trăm tối đa 100.");
            } else if ("FIXED".equals(type) && value.compareTo(MAX_MONEY) > 0) {
                errors.put("discountValue", "Giá trị giảm quá lớn.");
            }
        } catch (NumberFormatException exception) {
            errors.put("discountValue", "Hãy nhập giá trị giảm hợp lệ.");
        }

        try {
            BigDecimal minimum = raw.get("minimumOrderValue").isEmpty()
                    ? BigDecimal.ZERO : money(raw.get("minimumOrderValue"));
            voucher.setMinimumOrderValue(minimum);
            if (minimum.signum() < 0 || minimum.compareTo(MAX_MONEY) > 0) {
                errors.put("minimumOrderValue", "Giá trị đơn tối thiểu không hợp lệ.");
            }
        } catch (NumberFormatException exception) {
            errors.put("minimumOrderValue", "Hãy nhập giá trị đơn tối thiểu hợp lệ.");
        }

        voucher.setMaximumDiscount(null);
        if (!raw.get("maximumDiscount").isEmpty()) {
            try {
                BigDecimal maximum = money(raw.get("maximumDiscount"));
                voucher.setMaximumDiscount(maximum);
                if (maximum.signum() < 0 || maximum.compareTo(MAX_MONEY) > 0) {
                    errors.put("maximumDiscount", "Mức giảm tối đa không hợp lệ.");
                }
            } catch (NumberFormatException exception) {
                errors.put("maximumDiscount", "Hãy nhập mức giảm tối đa hợp lệ.");
            }
        }

        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            start = LocalDateTime.parse(raw.get("startDate"));
            voucher.setStartDate(start);
        } catch (DateTimeParseException exception) {
            errors.put("startDate", "Hãy chọn ngày bắt đầu.");
        }
        try {
            end = LocalDateTime.parse(raw.get("endDate"));
            voucher.setEndDate(end);
        } catch (DateTimeParseException exception) {
            errors.put("endDate", "Hãy chọn ngày kết thúc.");
        }
        if (start != null && end != null && !end.isAfter(start)) {
            errors.put("endDate", "Ngày kết thúc phải sau ngày bắt đầu.");
        }

        voucher.setUsageLimit(null);
        if (!raw.get("usageLimit").isEmpty()) {
            String limitText = raw.get("usageLimit");
            if (!limitText.matches("[0-9]+") || limitText.length() > 9) {
                errors.put("usageLimit", "Giới hạn lượt dùng phải là số nguyên không âm.");
            } else {
                int limit = Integer.parseInt(limitText);
                voucher.setUsageLimit(limit);
                if (limit < voucher.getUsedCount()) {
                    errors.put("usageLimit", "Giới hạn không được nhỏ hơn số lượt đã dùng ("
                            + voucher.getUsedCount() + ").");
                }
            }
        }

        if (!errors.isEmpty()) {
            forwardForm(request, response, voucher, errors, raw, base, user);
            return;
        }

        try {
            if (voucher.getVoucherId() == 0) {
                dao.insert(voucher);
                flash(request, "success", "Đã tạo voucher " + voucher.getVoucherCode() + ".");
            } else {
                dao.update(voucher);
                flash(request, "success", "Đã cập nhật voucher " + voucher.getVoucherCode() + ".");
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            flash(request, "error", "Không lưu được voucher, vui lòng thử lại.");
        }
        response.sendRedirect(base);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    private void changeStatus(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        String base = baseUrl(request, user);
        String back = request.getParameter("back") == null ? ""
                : request.getParameter("back").replaceAll("[^A-Za-z0-9%&=._~+-]", "");
        String target = base + (back.isEmpty() ? "" : "?" + back);

        String idText = request.getParameter("id");
        String status = param(request, "newStatus");
        if (idText == null || !idText.matches("[0-9]+") || idText.length() >= 10
                || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))) {
            flash(request, "error", "Yêu cầu không hợp lệ.");
            response.sendRedirect(target);
            return;
        }
        int id = Integer.parseInt(idText);
        Voucher voucher = dao.find(id);
        if (voucher == null) {
            flash(request, "error", "Không tìm thấy voucher.");
            response.sendRedirect(target);
            return;
        }
        // Manager chỉ được đổi trạng thái voucher của chi nhánh mình
        if (user.isManager() && !dao.belongsToCentre(id, user.getCentreId())) {
            flash(request, "error", "Bạn không có quyền thao tác voucher này.");
            response.sendRedirect(target);
            return;
        }
        try {
            dao.setStatus(id, status);
            flash(request, "success",
                    "ACTIVE".equals(status) ? "Đã kích hoạt voucher." : "Đã tạm ngưng voucher.");
        } catch (SQLException exception) {
            exception.printStackTrace();
            flash(request, "error", "Không đổi được trạng thái, vui lòng thử lại.");
        }
        response.sendRedirect(target);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILS
    // ─────────────────────────────────────────────────────────────────────────

    private String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private BigDecimal money(String text) {
        BigDecimal value = new BigDecimal(text.replace(",", ""));
        if (value.scale() > 2) {
            throw new NumberFormatException();
        }
        return value;
    }
}
