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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Controller Quản lý Voucher (Voucher Management).
 * Thiết kế tinh gọn, chuẩn hóa tương tự CentreManagementServlet:
 * - Kiểm tra phiên và quyền tập trung qua authorizedUser().
 * - Phân tách 4 luồng nghiệp vụ rõ ràng: showList, showForm, save, toggleStatus.
 * - Các hàm chỉ gọi DAO lấy dữ liệu và trả về giá trị (Entity, PageDTO, boolean), không kiểm tra lồng ghép rườm rà.
 */
@WebServlet(name = "VoucherManagementServlet", urlPatterns = {"/admin/vouchers", "/admin/vouchers/*", "/manager/vouchers", "/manager/vouchers/*"})
public class VoucherManagementServlet extends HttpServlet {

    public static final int MAX_DISCOUNT_PERCENT = 50;
    private static final int PAGE_SIZE = 10;
    private final VoucherDAO voucherDAO = new VoucherDAO();

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

        // 2. Kiểm tra vai trò
        if (!isAdmin && !isManager) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên hoặc Quản lý chi nhánh mới có quyền truy cập.");
            return null;
        }
         //3.cố tình nhập sai role
        String uri = request.getRequestURI();
        if (uri.contains("/admin/vouchers") && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin mới truy cập được voucher toàn chuỗi.");
            return null;
        }
        if (uri.contains("/manager/vouchers") && !isManager && !isAdmin) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền truy cập.");
            return null;
        }

        return user;
    }

    private String baseUrl(HttpServletRequest request, User user) {
        //true false
        boolean isAdminPath = user.isAdmin() && request.getRequestURI().contains("/admin/");
        return request.getContextPath() + (isAdminPath ? "/admin/vouchers" : "/manager/vouchers");
    }

    private Integer scopeCentreId(HttpServletRequest request, User user) {
        return (user.isAdmin() && request.getRequestURI().contains("/admin/")) ? null : user.getCentreId();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP GET
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = authorizedUser(request, response);
        if (user == null) return;
          //sau url pattern
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
        String q = request.getParameter("q");
        String status = request.getParameter("status");
        String type = request.getParameter("type");

        int page = 1;
        try {
            String p = request.getParameter("page");
            if (p != null && !p.trim().isEmpty()) {
                page = Integer.parseInt(p.trim());
            }
        } catch (NumberFormatException ignored) {}
       //lấy url cho admin or manager
        String base = baseUrl(request, user);
        //trả về id cho user
        Integer scope = scopeCentreId(request, user);

        // Gọi DAO lấy danh sách phân trang
        PageDTO<Voucher> result = voucherDAO.search(q, status, type, scope, page, PAGE_SIZE);
//url k thể chứ dấu cachs và tiếng việt 

        String filters = "q=" + URLEncoder.encode(q != null ? q.trim() : "", StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status != null ? status.trim() : "", StandardCharsets.UTF_8)
                + "&type=" + URLEncoder.encode(type != null ? type.trim() : "", StandardCharsets.UTF_8);

        request.setAttribute("result", result);
        request.setAttribute("q", q != null ? q.trim() : "");
        request.setAttribute("status", status != null ? status.trim() : "");
        request.setAttribute("type", type != null ? type.trim() : "");
        request.setAttribute("pagerBase", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + result.getPage());
        request.setAttribute("pageTitle", "Quản lý Voucher");
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
//check có id hay k 
        String idText = request.getParameter("id");
        if (idText != null && !idText.trim().isEmpty()) {
            try {
                int id = Integer.parseInt(idText.trim());
                voucher = voucherDAO.getVoucherById(id);
                if (voucher == null) {
                    response.sendRedirect(base + "?error=invalid_id");
                    return;
                }
                if (user.isManager() && !voucherDAO.belongsToCentre(voucher.getVoucherId(), user.getCentreId())) {
                    response.sendRedirect(base + "?error=forbidden");
                    return;
                }
            } catch (Exception e) {
                response.sendRedirect(base + "?error=invalid_id");
                return;
            }
        }

        request.setAttribute("voucher", voucher);
        request.setAttribute("pageTitle", voucher.getVoucherId() == 0 ? "Thêm voucher" : "Sửa voucher");
        request.setAttribute("baseUrl", base);
        request.setAttribute("currentUser", user);
        request.setAttribute("isAdminScope", user.isAdmin() && request.getRequestURI().contains("/admin/"));
        request.setAttribute("maxDiscountPercent", MAX_DISCOUNT_PERCENT);

        request.getRequestDispatcher("/WEB-INF/views/admin/voucher/voucherForm.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. SAVE (Tạo mới hoặc Cập nhật voucher)
    // ─────────────────────────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        String base = baseUrl(request, user);
        String idText = request.getParameter("id");
        int voucherId = 0;
        try {
            if (idText != null && !idText.trim().isEmpty()) {
                voucherId = Integer.parseInt(idText.trim());
            }
        } catch (NumberFormatException ignored) {}

        boolean isEdit = voucherId > 0;
        Voucher voucher = isEdit ? voucherDAO.getVoucherById(voucherId) : new Voucher();

        if (isEdit && (voucher == null || (user.isManager() && !voucherDAO.belongsToCentre(voucher.getVoucherId(), user.getCentreId())))) {
            response.sendRedirect(base + "?error=forbidden");
            return;
        }

        // Đọc các giá trị từ form
        String code = request.getParameter("code");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String imageUrl = request.getParameter("imageUrl");
        String type = request.getParameter("type");
        String status = request.getParameter("status");
        String discountValue = request.getParameter("discountValue");
        String minimumOrderValue = request.getParameter("minimumOrderValue");
        String maximumDiscount = request.getParameter("maximumDiscount");
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String usageLimit = request.getParameter("usageLimit");

        String formUrl = base + "/form" + (isEdit ? "?id=" + voucherId + "&" : "?");

        // 1. Kiểm tra các trường bắt buộc
        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()
                || discountValue == null || discountValue.trim().isEmpty()
                || startDate == null || startDate.trim().isEmpty()
                || endDate == null || endDate.trim().isEmpty()) {
            response.sendRedirect(formUrl + "error=missing_fields");
            return;
        }

        code = code.trim().toUpperCase();

        // 2. Kiểm tra trùng mã voucher
        if (voucherDAO.isVoucherCodeExists(code, voucherId)) {
            response.sendRedirect(formUrl + "error=duplicate_code");
            return;
        }

      //giá trị giảm 
        BigDecimal discountVal;
        try {
            discountVal = new BigDecimal(discountValue.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            response.sendRedirect(formUrl + "error=invalid_discount");
            return;
        }
        if (discountVal.signum() <= 0) {
            response.sendRedirect(formUrl + "error=invalid_discount");
            return;
        }
        if ("PERCENT".equals(type) && discountVal.compareTo(BigDecimal.valueOf(MAX_DISCOUNT_PERCENT)) > 0) {
            response.sendRedirect(formUrl + "error=discount_over_limit");
            return;
        }

        // 4. Validate endDate phải sau startDate
        LocalDateTime start, end;
        try {
            start = LocalDateTime.parse(startDate.trim());
            end = LocalDateTime.parse(endDate.trim());
        } catch (Exception e) {
            response.sendRedirect(formUrl + "error=invalid_date");
            return;
        }
        if (!end.isAfter(start)) {
            response.sendRedirect(formUrl + "error=enddate_before_start");
            return;
        }

        // 5. Validate usageLimit không nhỏ hơn số lượt đã dùng
        Integer limit = null;
        if (usageLimit != null && !usageLimit.trim().isEmpty()) {
            try {
                limit = Integer.parseInt(usageLimit.trim());
                if (limit < 0 || (isEdit && limit < voucher.getUsedCount())) {
                    response.sendRedirect(formUrl + "error=invalid_limit");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendRedirect(formUrl + "error=invalid_limit");
                return;
            }
        }
//chỉ cho phép nếu chưa có lượt dùng 
        try {
            // Không đổi mã nếu voucher đã từng có người sử dụng
            if (!isEdit || voucher.getUsedCount() == 0) {
                voucher.setVoucherCode(code);
            }
            voucher.setVoucherName(name.trim());
            voucher.setDescription(description != null ? description.trim() : "");
            voucher.setImageUrl(imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl.trim() : null);
            voucher.setDiscountType(type != null ? type.trim() : "PERCENT");
            voucher.setStatus(status != null ? status.trim() : "ACTIVE");

            voucher.setDiscountValue(discountVal);
            voucher.setMinimumOrderValue(minimumOrderValue != null && !minimumOrderValue.trim().isEmpty()
                    ? new BigDecimal(minimumOrderValue.trim().replace(",", "")) : BigDecimal.ZERO);
            voucher.setMaximumDiscount(maximumDiscount != null && !maximumDiscount.trim().isEmpty()
                    ? new BigDecimal(maximumDiscount.trim().replace(",", "")) : null);

            voucher.setStartDate(start);
            voucher.setEndDate(end);
            voucher.setUsageLimit(limit);

            if (!isEdit) {
                voucher.setCentreId(user.isManager() ? user.getCentreId() : null);
                voucher.setCreatedBy(user.getUserId());
            }

            // Gọi DAO lưu dữ liệu (chỉ trả về true/false)
            boolean ok = isEdit ? voucherDAO.updateVoucher(voucher) : voucherDAO.addVoucher(voucher);
            response.sendRedirect(ok ? (base + (isEdit ? "?success=updated" : "?success=created"))
                                     : (formUrl + "error=failed"));
        } catch (Exception e) {
            response.sendRedirect(formUrl + "error=invalid_data");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TOGGLE STATUS (Bật / Tắt hoạt động voucher)
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleStatus(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        String base = baseUrl(request, user);
        String back = request.getParameter("back");
        String redirectBase = (back != null && !back.trim().isEmpty())
                ? (base + "?" + back.replaceAll("[^A-Za-z0-9%&=._~+-]", "") + "&")
                : (base + "?");

        try {
            int id = Integer.parseInt(request.getParameter("id").trim());
            String newStatus = request.getParameter("newStatus");

            if (user.isManager() && !voucherDAO.belongsToCentre(id, user.getCentreId())) {
                response.sendRedirect(redirectBase + "error=forbidden");
                return;
            }

            // Gọi DAO đổi trạng thái
            boolean ok = voucherDAO.toggleStatus(id, newStatus);
            response.sendRedirect(redirectBase + (ok ? "success=status_updated" : "error=failed"));
        } catch (Exception e) {
            response.sendRedirect(redirectBase + "error=invalid_id");
        }
    }
}
