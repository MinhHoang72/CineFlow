package controller.admin.voucher;

import dao.VoucherDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.dto.common.PageDTO;
import model.entity.User;
import model.entity.Voucher;
import service.VoucherService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller Quản lý Voucher (Voucher Management).
 * Chuẩn hóa cấu trúc tinh gọn theo phong cách CentreManagementServlet:
 * - Kiểm tra phiên và phân quyền tập trung qua authorizedUser().
 * - Phân tách 4 luồng rõ ràng: showList, showForm, save, toggleStatus.
 */
@WebServlet(name = "VoucherManagementServlet", urlPatterns = {"/admin/vouchers", "/admin/vouchers/*", "/manager/vouchers", "/manager/vouchers/*"})
public class VoucherManagementServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final VoucherService voucherService = new VoucherService();

    // ─────────────────────────────────────────────────────────────────────────
    // AUTHENTICATION & AUTHORIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private User authorizedUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        // 1. Kiểm tra đăng nhập
        if (user == null || session.getAttribute("userId") == null) {
            String redirect = request.getRequestURI();
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode(redirect, StandardCharsets.UTF_8));
            return null;
        }

        boolean isAdmin = user.isAdmin() || "ADMIN".equalsIgnoreCase(user.getRole());
        boolean isManager = (user.isManager() || "MANAGER".equalsIgnoreCase(user.getRole())) && user.getCentreId() != null;

        // 2. Kiểm tra quyền truy cập theo vai trò
        if (!isAdmin && !isManager) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin hoặc Manager chi nhánh mới được quản lý voucher.");
            return null;
        }

        String uri = request.getRequestURI();
        if (uri.contains("/admin/vouchers") && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin mới truy cập được trang quản lý voucher toàn chuỗi.");
            return null;
        }
        if (uri.contains("/manager/vouchers") && !isManager && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập.");
            return null;
        }

        return user;
    }

    private String baseUrl(HttpServletRequest request, User user) {
        boolean isAdminPath = user.isAdmin() && request.getRequestURI().contains("/admin/");
        return request.getContextPath() + (isAdminPath ? "/admin/vouchers" : "/manager/vouchers");
    }

    private Integer scopeCentreId(HttpServletRequest request, User user) {
        return (user.isAdmin() && request.getRequestURI().contains("/admin/")) ? null : user.getCentreId();
    }

    private void setFlash(HttpServletRequest request, String type, String message) {
        HttpSession session = request.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP GET
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = authorizedUser(request, response);
        if (user == null) return;

        String path = request.getPathInfo();
        if ("/form".equals(path)) {
            showForm(request, response, user);
        } else {
            showList(request, response, user);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP POST
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = authorizedUser(request, response);
        if (user == null) return;

        String path = request.getPathInfo();
        if ("/save".equals(path)) {
            save(request, response, user);
        } else if ("/status".equals(path)) {
            toggleStatus(request, response, user);
        } else {
            response.sendRedirect(baseUrl(request, user));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SHOW LIST (Hiển thị danh sách & phân trang)
    // ─────────────────────────────────────────────────────────────────────────

    private void showList(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String q = param(request, "q");
        String status = param(request, "status");
        String type = param(request, "type");
        int page = 1;
        try {
            String p = request.getParameter("page");
            if (p != null && !p.isBlank()) page = Integer.parseInt(p.trim());
        } catch (NumberFormatException ignored) {}

        String base = baseUrl(request, user);
        Integer scope = scopeCentreId(request, user);
        PageDTO<Voucher> result = voucherService.searchVouchers(q, status, type, scope, page, PAGE_SIZE);

        String filters = "q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8)
                + "&type=" + URLEncoder.encode(type, StandardCharsets.UTF_8);

        request.setAttribute("result", result);
        request.setAttribute("q", q);
        request.setAttribute("status", status);
        request.setAttribute("type", type);
        request.setAttribute("pagerBase", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + result.getPage());
        request.setAttribute("pageTitle", "Voucher");
        request.setAttribute("pageBreadcrumb", "Khuyến mãi");
        request.setAttribute("baseUrl", base);
        request.setAttribute("currentUser", user);
        request.setAttribute("isAdminScope", user.isAdmin() && request.getRequestURI().contains("/admin/"));

        request.getRequestDispatcher("/WEB-INF/views/admin/voucher/voucherList.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SHOW FORM (Mở Form Thêm mới / Sửa)
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
        if (user.isManager()) {
            voucher.setCentreId(user.getCentreId());
        }

        String idText = request.getParameter("id");
        if (idText != null && idText.matches("\\d+")) {
            voucher = voucherService.getVoucherById(Integer.parseInt(idText));
            if (voucher == null) {
                setFlash(request, "error", "Không tìm thấy voucher.");
                response.sendRedirect(base);
                return;
            }
            if (user.isManager() && !voucherService.belongsToCentre(voucher.getVoucherId(), user.getCentreId())) {
                setFlash(request, "error", "Bạn không có quyền chỉnh sửa voucher này.");
                response.sendRedirect(base);
                return;
            }
        }
        renderForm(request, response, voucher, null, null, base, user);
    }

    private void renderForm(HttpServletRequest request, HttpServletResponse response,
                            Voucher voucher, Map<String, String> errors, Map<String, String> raw,
                            String base, User user) throws ServletException, IOException {
        request.setAttribute("voucher", voucher);
        request.setAttribute("errors", errors);
        request.setAttribute("raw", raw);
        request.setAttribute("pageTitle", voucher.getVoucherId() == 0 ? "Thêm voucher" : "Sửa voucher");
        request.setAttribute("pageBreadcrumb", "Khuyến mãi");
        request.setAttribute("baseUrl", base);
        request.setAttribute("currentUser", user);
        request.setAttribute("isAdminScope", user.isAdmin() && request.getRequestURI().contains("/admin/"));
        request.getRequestDispatcher("/WEB-INF/views/admin/voucher/voucherForm.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. SAVE (Tạo mới hoặc Cập nhật voucher)
    // ─────────────────────────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String base = baseUrl(request, user);
        String idText = request.getParameter("id");
        boolean isEdit = idText != null && idText.matches("\\d+");
        Voucher voucher = isEdit ? voucherService.getVoucherById(Integer.parseInt(idText)) : new Voucher();

        if (isEdit && (voucher == null || (user.isManager() && !voucherService.belongsToCentre(voucher.getVoucherId(), user.getCentreId())))) {
            setFlash(request, "error", "Không tìm thấy hoặc bạn không có quyền thao tác voucher này.");
            response.sendRedirect(base);
            return;
        }

        if (!isEdit) {
            voucher.setCentreId(user.isManager() ? user.getCentreId() : null);
            voucher.setCreatedBy(user.getUserId());
        }

        // Đọc dữ liệu từ form
        String code = param(request, "code").toUpperCase();
        String name = param(request, "name");
        String desc = param(request, "description");
        String img = param(request, "imageUrl");
        String type = param(request, "type");
        String status = param(request, "status");
        String rawVal = param(request, "discountValue");
        String rawMin = param(request, "minimumOrderValue");
        String rawMax = param(request, "maximumDiscount");
        String rawStart = param(request, "startDate");
        String rawEnd = param(request, "endDate");
        String rawLimit = param(request, "usageLimit");

        // Giữ lại raw values cho FE nếu có lỗi nhập liệu
        Map<String, String> raw = new HashMap<>();
        raw.put("discountValue", rawVal);
        raw.put("minimumOrderValue", rawMin);
        raw.put("maximumDiscount", rawMax);
        raw.put("startDate", rawStart);
        raw.put("endDate", rawEnd);
        raw.put("usageLimit", rawLimit);

        Map<String, String> errors = new LinkedHashMap<>();

        // Khóa mã nếu voucher đã từng được sử dụng
        if (isEdit && voucher.getUsedCount() > 0) {
            code = voucher.getVoucherCode();
        }
        voucher.setVoucherCode(code);
        voucher.setVoucherName(name);
        voucher.setDescription(desc);
        voucher.setImageUrl(img.isEmpty() ? null : img);
        voucher.setDiscountType(type);
        voucher.setStatus(status);

        // 1. Validate thông tin cơ bản
        if (!code.matches("[A-Z0-9_-]{3,50}")) {
            errors.put("code", "Mã gồm 3-50 ký tự chữ hoa, số, gạch ngang hoặc gạch dưới.");
        } else if (voucherService.isVoucherCodeExists(code, voucher.getVoucherId())) {
            errors.put("code", "Mã voucher đã tồn tại.");
        }
        if (name.isEmpty() || name.length() > 150) {
            errors.put("name", "Tên voucher không được trống và tối đa 150 ký tự.");
        }
        if (desc.length() > 500) {
            errors.put("description", "Mô tả tối đa 500 ký tự.");
        }
        if (!"FIXED".equals(type) && !"PERCENT".equals(type)) {
            errors.put("type", "Hãy chọn loại giảm giá.");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            errors.put("status", "Hãy chọn trạng thái.");
        }

        // 2. Validate giá trị giảm
        try {
            BigDecimal val = new BigDecimal(rawVal.replace(",", ""));
            voucher.setDiscountValue(val);
            if (val.signum() <= 0) {
                errors.put("discountValue", "Giá trị giảm phải lớn hơn 0.");
            } else if ("PERCENT".equals(type) && val.compareTo(BigDecimal.valueOf(50)) > 0) {
                errors.put("discountValue", "Giảm theo phần trăm tối đa 50%.");
            }
        } catch (Exception e) {
            errors.put("discountValue", "Hãy nhập giá trị giảm hợp lệ.");
        }

        // 3. Validate đơn tối thiểu
        try {
            BigDecimal min = rawMin.isEmpty() ? BigDecimal.ZERO : new BigDecimal(rawMin.replace(",", ""));
            voucher.setMinimumOrderValue(min);
            if (min.signum() < 0) {
                errors.put("minimumOrderValue", "Giá trị đơn tối thiểu không hợp lệ.");
            }
        } catch (Exception e) {
            errors.put("minimumOrderValue", "Hãy nhập giá trị đơn tối thiểu hợp lệ.");
        }

        // 4. Validate mức giảm tối đa
        voucher.setMaximumDiscount(null);
        if (!rawMax.isEmpty()) {
            try {
                BigDecimal max = new BigDecimal(rawMax.replace(",", ""));
                voucher.setMaximumDiscount(max);
                if (max.signum() <= 0) {
                    errors.put("maximumDiscount", "Mức giảm tối đa phải lớn hơn 0.");
                }
            } catch (Exception e) {
                errors.put("maximumDiscount", "Hãy nhập mức giảm tối đa hợp lệ.");
            }
        }

        // 5. Validate thời gian hiệu lực
        LocalDateTime start = null, end = null;
        try {
            start = LocalDateTime.parse(rawStart);
            voucher.setStartDate(start);
        } catch (Exception e) {
            errors.put("startDate", "Hãy chọn ngày bắt đầu hợp lệ.");
        }
        try {
            end = LocalDateTime.parse(rawEnd);
            voucher.setEndDate(end);
        } catch (Exception e) {
            errors.put("endDate", "Hãy chọn ngày kết thúc hợp lệ.");
        }
        if (start != null && end != null && !end.isAfter(start)) {
            errors.put("endDate", "Ngày kết thúc phải sau ngày bắt đầu.");
        }

        // 6. Validate giới hạn lượt dùng
        voucher.setUsageLimit(null);
        if (!rawLimit.isEmpty()) {
            try {
                int limit = Integer.parseInt(rawLimit.trim());
                voucher.setUsageLimit(limit);
                if (limit < voucher.getUsedCount()) {
                    errors.put("usageLimit", "Giới hạn không được nhỏ hơn số lượt đã dùng (" + voucher.getUsedCount() + ").");
                }
            } catch (Exception e) {
                errors.put("usageLimit", "Giới hạn lượt dùng phải là số nguyên dương.");
            }
        }

        // Nếu có lỗi -> render lại form
        if (!errors.isEmpty()) {
            renderForm(request, response, voucher, errors, raw, base, user);
            return;
        }

        // Lưu vào DB
        try {
            if (voucher.getVoucherId() == 0) {
                voucherService.createVoucher(voucher);
                setFlash(request, "success", "Đã tạo voucher " + voucher.getVoucherCode() + " thành công.");
            } else {
                voucherService.updateVoucher(voucher);
                setFlash(request, "success", "Đã cập nhật voucher " + voucher.getVoucherCode() + " thành công.");
            }
        } catch (Exception e) {
            setFlash(request, "error", "Không lưu được voucher: " + e.getMessage());
        }
        response.sendRedirect(base);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TOGGLE STATUS (Bật / Tắt hoạt động voucher)
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleStatus(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        String base = baseUrl(request, user);
        String back = param(request, "back").replaceAll("[^A-Za-z0-9%&=._~+-]", "");
        String target = base + (back.isEmpty() ? "" : "?" + back);

        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String status = param(request, "newStatus");

            if (user.isManager() && !voucherService.belongsToCentre(id, user.getCentreId())) {
                setFlash(request, "error", "Bạn không có quyền thao tác voucher này.");
                response.sendRedirect(target);
                return;
            }

            voucherService.changeVoucherStatus(id, status);
            setFlash(request, "success", "ACTIVE".equals(status) ? "Đã kích hoạt voucher thành công." : "Đã tạm ngưng voucher.");
        } catch (Exception e) {
            setFlash(request, "error", "Không đổi được trạng thái, vui lòng thử lại.");
        }
        response.sendRedirect(target);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
