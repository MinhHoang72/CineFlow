package controller;

import dao.FnbDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import model.ComboLine;
import model.FnbCombo;
import model.FnbItem;
import model.FnbRowDTO;
import model.OptionDTO;
import model.PageDTO;
import model.User;
import utils.MediaStorage;

@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024)
@WebServlet("/fnb/*")
public class FnbServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000000");
    private final FnbDAO dao = new FnbDAO();

    private String crumb(User user, String base) {
        String label = "ADMIN".equals(user.getRole()) ? null : dao.centreLabel(user.getCentreId());
        return label == null ? base : base + " · " + label;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = loginUser(request, response);
        if (user == null || !canEnter(user, response)) {
            return;
        }
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        switch (path) {
            case "/":
                showList(request, response, user);
                break;
            case "/item":
                showItemForm(request, response, user);
                break;
            case "/combo":
                showComboForm(request, response, user);
                break;
            case "/stock":
                showStock(request, response, user);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = loginUser(request, response);
        if (user == null || !canEnter(user, response)) {
            return;
        }
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            try {
                request.getParts();
            } catch (IllegalStateException exception) {
                flash(request, "error", "Ảnh quá lớn, tối đa 2 MB.");
                response.sendRedirect(request.getContextPath() + "/fnb");
                return;
            }
        }
        if (!validCsrf(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc không hợp lệ, hãy tải lại trang.");
            return;
        }
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        // Các action kho: chỉ MANAGER được POST
        if ("/stock/toggle".equals(path) || "/stock/quantity".equals(path)) {
            if (!"MANAGER".equals(user.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Chỉ Manager của Centre mới được cập nhật kho.");
                return;
            }
            switch (path) {
                case "/stock/toggle":
                    toggleStock(request, response, user);
                    break;
                case "/stock/quantity":
                    updateQuantity(request, response, user);
                    break;
            }
            return;
        }
        // Các action thực đơn (item/combo): chỉ MANAGER được POST
        if (!"MANAGER".equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Manager của Centre mới được thay đổi thực đơn.");
            return;
        }
        switch (path) {
            case "/item/save":
                saveItem(request, response, user);
                break;
            case "/combo/save":
                saveCombo(request, response, user);
                break;
            case "/status":
                changeStatus(request, response, user);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String imageExtension(Part part) throws IOException {
        byte[] head;
        try (InputStream input = part.getInputStream()) {
            head = input.readNBytes(12);
        }
        if (head.length >= 4 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G') {
            return ".png";
        }
        if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return ".webp";
        }
        return null;
    }

    private Part uploadedImage(HttpServletRequest request, Map<String, String> errors) throws IOException, ServletException {
        Part part;
        try {
            part = request.getPart("image");
        } catch (IllegalStateException exception) {
            errors.put("image", "Ảnh tối đa 2 MB.");
            return null;
        }
        if (part == null || part.getSize() == 0) {
            return null;
        }
        if (imageExtension(part) == null) {
            errors.put("image", "Chỉ nhận ảnh JPG, PNG hoặc WEBP hợp lệ.");
            return null;
        }
        return part;
    }

    private String storeImage(HttpServletRequest request, Part part) throws IOException {
        String fileName = UUID.randomUUID() + imageExtension(part);
        Path directory = MediaStorage.uploadDirectory(getServletContext(), "fnb");
        part.write(directory.resolve(fileName).toString());
        return request.getContextPath() + "/uploads/fnb/" + fileName;
    }

    private void deleteImage(HttpServletRequest request, String url) {
        String prefix = request.getContextPath() + "/uploads/fnb/";
        if (url == null || !url.startsWith(prefix)) {
            return;
        }
        String fileName = url.substring(prefix.length());
        if (!fileName.matches("[0-9a-f-]{36}\\.(png|jpg|webp)")) {
            return;
        }
        try {
            Files.deleteIfExists(MediaStorage.uploadDirectory(getServletContext(), "fnb").resolve(fileName));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private User loginUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/fnb", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
        }
        return user;
    }

    private boolean canEnter(User user, HttpServletResponse response) throws IOException {
        String role = user.getRole();
        boolean admin = "ADMIN".equals(role);
        boolean manager = "MANAGER".equals(role);
        boolean staff = "TICKET_SALES_STAFF".equals(role);
        if (!admin && !manager && !staff) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem thực đơn.");
            return false;
        }
        if (!admin && user.getCentreId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa được gán Centre.");
            return false;
        }
        return true;
    }

    private String csrfToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("fnbCsrf");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("fnbCsrf", token);
        }
        return token;
    }

    private boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("fnbCsrf");
        String actual = request.getParameter("_csrf");
        return expected != null && expected.equals(actual);
    }

    private void flash(HttpServletRequest request, String type, String message) {
        HttpSession session = request.getSession();
        session.setAttribute("fnbFlashType", type);
        session.setAttribute("fnbFlashMessage", message);
    }

    private void showFlash(HttpServletRequest request) {
        HttpSession session = request.getSession();
        request.setAttribute("flashType", session.getAttribute("fnbFlashType"));
        request.setAttribute("flashMessage", session.getAttribute("fnbFlashMessage"));
        session.removeAttribute("fnbFlashType");
        session.removeAttribute("fnbFlashMessage");
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        boolean admin = "ADMIN".equals(user.getRole());
        boolean manager = "MANAGER".equals(user.getRole());

        String q = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
        if (q.length() > 100) {
            q = q.substring(0, 100);
        }

        String category = request.getParameter("category") == null ? "" : request.getParameter("category").trim();
        if (!"COMBO".equals(category) && !(category.matches("[0-9]+") && category.length() < 10)) {
            category = "";
        }

        String status = request.getParameter("status") == null ? "" : request.getParameter("status").trim();
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            status = "";
        }

        String sort = request.getParameter("sort") == null ? "" : request.getParameter("sort").trim();
        if (!"name".equals(sort) && !"price_asc".equals(sort) && !"price_desc".equals(sort) && !"newest".equals(sort)) {
            sort = "";
        }

        BigDecimal minPrice = null;
        String minText = request.getParameter("minPrice");
        if (minText != null && !minText.trim().isEmpty()) {
            try {
                minPrice = new BigDecimal(minText.trim());
                if (minPrice.signum() < 0) {
                    minPrice = null;
                }
            } catch (NumberFormatException exception) {
                minPrice = null;
            }
        }
        BigDecimal maxPrice = null;
        String maxText = request.getParameter("maxPrice");
        if (maxText != null && !maxText.trim().isEmpty()) {
            try {
                maxPrice = new BigDecimal(maxText.trim());
                if (maxPrice.signum() < 0) {
                    maxPrice = null;
                }
            } catch (NumberFormatException exception) {
                maxPrice = null;
            }
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal swap = minPrice;
            minPrice = maxPrice;
            maxPrice = swap;
        }

        int page = 1;
        try {
            page = Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException exception) {
            page = 1;
        }
        if (page < 1) {
            page = 1;
        }

        Integer selectedCentre = user.getCentreId();
        List<OptionDTO> centres = null;
        if (admin) {
            centres = dao.centres();
            selectedCentre = null;
            String centreText = request.getParameter("centreId");
            if (centreText != null && centreText.matches("[0-9]+") && centreText.length() < 10) {
                int wanted = Integer.parseInt(centreText);
                for (OptionDTO option : centres) {
                    if (option.getId() == wanted) {
                        selectedCentre = wanted;
                    }
                }
            }
        }

        PageDTO<FnbRowDTO> result = dao.search(q, category, status, minPrice, maxPrice, sort,
                selectedCentre, page, PAGE_SIZE);

        String minValue = minPrice == null ? "" : minPrice.stripTrailingZeros().toPlainString();
        String maxValue = maxPrice == null ? "" : maxPrice.stripTrailingZeros().toPlainString();
        String filters = "q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&category=" + URLEncoder.encode(category, StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8)
                + "&minPrice=" + URLEncoder.encode(minValue, StandardCharsets.UTF_8)
                + "&maxPrice=" + URLEncoder.encode(maxValue, StandardCharsets.UTF_8)
                + "&sort=" + URLEncoder.encode(sort, StandardCharsets.UTF_8)
                + (admin ? "&centreId=" + (selectedCentre == null ? "" : selectedCentre) : "");

        request.setAttribute("result", result);
        request.setAttribute("q", q);
        request.setAttribute("category", category);
        request.setAttribute("status", status);
        request.setAttribute("sort", sort);
        request.setAttribute("minPrice", minValue);
        request.setAttribute("maxPrice", maxValue);
        request.setAttribute("categories", dao.categories());
        request.setAttribute("centres", centres);
        request.setAttribute("selectedCentre", selectedCentre);
        request.setAttribute("canEdit", manager);
        request.setAttribute("isAdmin", admin);
        request.setAttribute("pagerBase", "/fnb?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + page);
        request.setAttribute("csrf", csrfToken(request));
        request.setAttribute("pageTitle", "Món & Combo");
        request.setAttribute("pageBreadcrumb", crumb(user, "Bắp nước"));
        showFlash(request);
        request.getRequestDispatcher("/WEB-INF/views/fnb/list.jsp").forward(request, response);
    }

    private void showItemForm(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        if (!"MANAGER".equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager của Centre mới được sửa thực đơn.");
            return;
        }
        int centreId = user.getCentreId();
        FnbItem item = new FnbItem();
        String idText = request.getParameter("id");
        if (idText != null && idText.matches("[0-9]+") && idText.length() < 10) {
            item = dao.findItem(Integer.parseInt(idText));
            if (item == null) {
                flash(request, "error", "Không tìm thấy món.");
                response.sendRedirect(request.getContextPath() + "/fnb");
                return;
            }
            if (item.getCentreId() != centreId) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Món này thuộc Centre khác.");
                return;
            }
        }
        request.setAttribute("item", item);
        request.setAttribute("categories", dao.categories());
        request.setAttribute("csrf", csrfToken(request));
        request.setAttribute("pageTitle", item.getFnbItemId() == 0 ? "Thêm món" : "Sửa món");
        request.setAttribute("pageBreadcrumb", crumb(user, "Bắp nước"));
        request.getRequestDispatcher("/WEB-INF/views/fnb/item-form.jsp").forward(request, response);
    }

    private void saveItem(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        int centreId = user.getCentreId();
        FnbItem item = new FnbItem();
        item.setCentreId(centreId);
        String idText = request.getParameter("id");
        if (idText != null && idText.matches("[0-9]+") && idText.length() < 10) {
            item = dao.findItem(Integer.parseInt(idText));
            if (item == null) {
                flash(request, "error", "Không tìm thấy món.");
                response.sendRedirect(request.getContextPath() + "/fnb");
                return;
            }
            if (item.getCentreId() != centreId) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Món này thuộc Centre khác.");
                return;
            }
        }

        Map<String, String> errors = new LinkedHashMap<>();
        String name = request.getParameter("name") == null ? "" : request.getParameter("name").trim();
        String description = request.getParameter("description") == null ? "" : request.getParameter("description").trim();
        String status = request.getParameter("status") == null ? "" : request.getParameter("status").trim();
        String priceText = request.getParameter("price") == null ? "" : request.getParameter("price").trim();
        String categoryText = request.getParameter("categoryId") == null ? "" : request.getParameter("categoryId").trim();

        item.setItemName(name);
        item.setDescription(description);
        item.setStatus(status);
        Part imagePart = uploadedImage(request, errors);

        if (name.isEmpty()) {
            errors.put("name", "Tên món là bắt buộc.");
        } else if (name.length() > 100) {
            errors.put("name", "Tên món tối đa 100 ký tự.");
        }
        if (description.length() > 500) {
            errors.put("description", "Mô tả tối đa 500 ký tự.");
        }

        List<OptionDTO> categories = dao.categories();
        int categoryId = 0;
        if (categoryText.matches("[0-9]+") && categoryText.length() < 10) {
            categoryId = Integer.parseInt(categoryText);
        }
        boolean categoryOk = false;
        for (OptionDTO option : categories) {
            if (option.getId() == categoryId) {
                categoryOk = true;
            }
        }
        item.setCategoryId(categoryId);
        if (!categoryOk) {
            errors.put("categoryId", "Hãy chọn danh mục.");
        }

        try {
            BigDecimal price = new BigDecimal(priceText.replace(",", ""));
            item.setPrice(price);
            if (price.signum() <= 0 || price.compareTo(MAX_PRICE) > 0 || price.scale() > 2) {
                errors.put("price", "Giá phải lớn hơn 0 và tối đa 10.000.000 đ.");
            }
        } catch (NumberFormatException exception) {
            errors.put("price", "Hãy nhập giá hợp lệ.");
        }

        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            errors.put("status", "Hãy chọn trạng thái.");
        } else if ("INACTIVE".equals(status) && item.getFnbItemId() > 0
                && dao.countActiveCombosUsing(item.getFnbItemId()) > 0) {
            errors.put("status", "Món đang nằm trong combo đang bán, hãy gỡ khỏi combo trước khi lưu trữ.");
        }

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("item", item);
            request.setAttribute("categories", categories);
            request.setAttribute("csrf", csrfToken(request));
            request.setAttribute("pageTitle", item.getFnbItemId() == 0 ? "Thêm món" : "Sửa món");
            request.setAttribute("pageBreadcrumb", crumb(user, "Bắp nước"));
            request.getRequestDispatcher("/WEB-INF/views/fnb/item-form.jsp").forward(request, response);
            return;
        }

        String oldImage = item.getImageUrl();
        String newImage = oldImage;
        if (imagePart != null) {
            newImage = storeImage(request, imagePart);
        } else if ("1".equals(request.getParameter("removeImage"))) {
            newImage = null;
        }
        item.setImageUrl(newImage);
        try {
            if (item.getFnbItemId() == 0) {
                dao.insertItem(item);
                flash(request, "success", "Đã thêm món \"" + item.getItemName() + "\".");
            } else {
                dao.updateItem(item);
                flash(request, "success", "Đã cập nhật món \"" + item.getItemName() + "\".");
            }
            if (oldImage != null && !oldImage.equals(newImage)) {
                deleteImage(request, oldImage);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            if (newImage != null && !newImage.equals(oldImage)) {
                deleteImage(request, newImage);
            }
            flash(request, "error", "Không lưu được món, vui lòng thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/fnb");
    }

    private void showComboForm(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        if (!"MANAGER".equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager của Centre mới được sửa thực đơn.");
            return;
        }
        int centreId = user.getCentreId();
        FnbCombo combo = new FnbCombo();
        String idText = request.getParameter("id");
        if (idText != null && idText.matches("[0-9]+") && idText.length() < 10) {
            combo = dao.findCombo(Integer.parseInt(idText));
            if (combo == null) {
                flash(request, "error", "Không tìm thấy combo.");
                response.sendRedirect(request.getContextPath() + "/fnb");
                return;
            }
            if (combo.getCentreId() != centreId) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Combo này thuộc Centre khác.");
                return;
            }
        }
        request.setAttribute("combo", combo);
        request.setAttribute("items", dao.activeItems(centreId));
        request.setAttribute("csrf", csrfToken(request));
        request.setAttribute("pageTitle", combo.getComboId() == 0 ? "Tạo combo" : "Sửa combo");
        request.setAttribute("pageBreadcrumb", crumb(user, "Bắp nước"));
        request.getRequestDispatcher("/WEB-INF/views/fnb/combo-form.jsp").forward(request, response);
    }

    private void saveCombo(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        int centreId = user.getCentreId();
        FnbCombo combo = new FnbCombo();
        combo.setCentreId(centreId);
        String idText = request.getParameter("id");
        if (idText != null && idText.matches("[0-9]+") && idText.length() < 10) {
            combo = dao.findCombo(Integer.parseInt(idText));
            if (combo == null) {
                flash(request, "error", "Không tìm thấy combo.");
                response.sendRedirect(request.getContextPath() + "/fnb");
                return;
            }
            if (combo.getCentreId() != centreId) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Combo này thuộc Centre khác.");
                return;
            }
        }

        Map<String, String> errors = new LinkedHashMap<>();
        String name = request.getParameter("name") == null ? "" : request.getParameter("name").trim();
        String description = request.getParameter("description") == null ? "" : request.getParameter("description").trim();
        String status = request.getParameter("status") == null ? "" : request.getParameter("status").trim();
        String priceText = request.getParameter("price") == null ? "" : request.getParameter("price").trim();

        combo.setComboName(name);
        combo.setDescription(description);
        combo.setStatus(status);
        Part imagePart = uploadedImage(request, errors);

        if (name.isEmpty()) {
            errors.put("name", "Tên combo là bắt buộc.");
        } else if (name.length() > 100) {
            errors.put("name", "Tên combo tối đa 100 ký tự.");
        }
        if (description.length() > 500) {
            errors.put("description", "Mô tả tối đa 500 ký tự.");
        }
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            errors.put("status", "Hãy chọn trạng thái.");
        }
        try {
            BigDecimal price = new BigDecimal(priceText.replace(",", ""));
            combo.setPrice(price);
            if (price.signum() <= 0 || price.compareTo(MAX_PRICE) > 0 || price.scale() > 2) {
                errors.put("price", "Giá phải lớn hơn 0 và tối đa 10.000.000 đ.");
            }
        } catch (NumberFormatException exception) {
            errors.put("price", "Hãy nhập giá hợp lệ.");
        }

        List<FnbItem> items = dao.activeItems(centreId);
        Map<Integer, Integer> merged = new LinkedHashMap<>();
        String[] itemIds = request.getParameterValues("lineItem");
        String[] quantities = request.getParameterValues("lineQty");
        if (itemIds != null && quantities != null) {
            for (int i = 0; i < itemIds.length && i < quantities.length; i++) {
                String itemText = itemIds[i].trim();
                String quantityText = quantities[i].trim();
                if (itemText.isEmpty() && quantityText.isEmpty()) {
                    continue;
                }
                boolean itemOk = false;
                int itemId = 0;
                if (itemText.matches("[0-9]+") && itemText.length() < 10) {
                    itemId = Integer.parseInt(itemText);
                    for (FnbItem option : items) {
                        if (option.getFnbItemId() == itemId) {
                            itemOk = true;
                        }
                    }
                }
                if (!itemOk) {
                    errors.put("lines", "Mỗi dòng cần chọn một món đang bán của Centre.");
                    break;
                }
                if (!quantityText.matches("[0-9]+") || quantityText.length() > 3
                        || Integer.parseInt(quantityText) < 1 || Integer.parseInt(quantityText) > 99) {
                    errors.put("lines", "Số lượng mỗi món phải từ 1 đến 99.");
                    break;
                }
                merged.merge(itemId, Integer.parseInt(quantityText), Integer::sum);
            }
        }
        combo.getLines().clear();
        for (Map.Entry<Integer, Integer> entry : merged.entrySet()) {
            combo.getLines().add(new ComboLine(entry.getKey(), Math.min(99, entry.getValue())));
        }
        if (combo.getLines().isEmpty() && !errors.containsKey("lines")) {
            errors.put("lines", "Combo cần ít nhất một món.");
        }

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("combo", combo);
            request.setAttribute("items", items);
            request.setAttribute("csrf", csrfToken(request));
            request.setAttribute("pageTitle", combo.getComboId() == 0 ? "Tạo combo" : "Sửa combo");
            request.setAttribute("pageBreadcrumb", crumb(user, "Bắp nước"));
            request.getRequestDispatcher("/WEB-INF/views/fnb/combo-form.jsp").forward(request, response);
            return;
        }

        boolean isNew = combo.getComboId() == 0;
        String oldImage = combo.getImageUrl();
        String newImage = oldImage;
        if (imagePart != null) {
            newImage = storeImage(request, imagePart);
        } else if ("1".equals(request.getParameter("removeImage"))) {
            newImage = null;
        }
        combo.setImageUrl(newImage);
        try {
            dao.saveCombo(combo);
            flash(request, "success", (isNew ? "Đã tạo combo \"" : "Đã cập nhật combo \"") + combo.getComboName() + "\".");
            if (oldImage != null && !oldImage.equals(newImage)) {
                deleteImage(request, oldImage);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            if (newImage != null && !newImage.equals(oldImage)) {
                deleteImage(request, newImage);
            }
            flash(request, "error", "Không lưu được combo, vui lòng thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/fnb");
    }

    private void changeStatus(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        int centreId = user.getCentreId();
        boolean combo = "COMBO".equals(request.getParameter("kind"));
        String idText = request.getParameter("id");
        String status = request.getParameter("newStatus") == null ? "" : request.getParameter("newStatus").trim();
        String back = request.getParameter("back") == null ? "" : request.getParameter("back").replaceAll("[^A-Za-z0-9%&=._~+-]", "");
        String target = request.getContextPath() + "/fnb" + (back.isEmpty() ? "" : "?" + back);

        if (idText == null || !idText.matches("[0-9]+") || idText.length() >= 10
                || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))) {
            flash(request, "error", "Yêu cầu không hợp lệ.");
            response.sendRedirect(target);
            return;
        }
        int id = Integer.parseInt(idText);

        boolean exists;
        int owner = 0;
        if (combo) {
            FnbCombo found = dao.findCombo(id);
            exists = found != null;
            if (exists) {
                owner = found.getCentreId();
            }
        } else {
            FnbItem found = dao.findItem(id);
            exists = found != null;
            if (exists) {
                owner = found.getCentreId();
            }
        }
        if (!exists) {
            flash(request, "error", "Không tìm thấy món hoặc combo.");
            response.sendRedirect(target);
            return;
        }
        if (owner != centreId) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Món hoặc combo này thuộc Centre khác.");
            return;
        }
        if (!combo && "INACTIVE".equals(status) && dao.countActiveCombosUsing(id) > 0) {
            flash(request, "error", "Món đang nằm trong combo đang bán, hãy gỡ khỏi combo trước khi lưu trữ.");
            response.sendRedirect(target);
            return;
        }
        try {
            dao.setStatus(combo, id, status);
            flash(request, "success", (combo ? "Combo " : "Món ") + ("ACTIVE".equals(status) ? "đã được khôi phục." : "đã được lưu trữ."));
        } catch (SQLException exception) {
            exception.printStackTrace();
            flash(request, "error", "Không đổi được trạng thái, vui lòng thử lại.");
        }
        response.sendRedirect(target);
    }

    /**
     * Hiển thị trang Kho theo Centre.
     * Quyền: MANAGER (xem + sửa), TICKET_SALES_STAFF (chỉ xem).
     * ADMIN không được truy cập (không gắn với centre cụ thể).
     */
    private void showStock(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        boolean manager = "MANAGER".equals(user.getRole());
        boolean staff = "TICKET_SALES_STAFF".equals(user.getRole());
        if (!manager && !staff) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Trang kho chỉ dành cho Manager và nhân viên bán vé của chi nhánh.");
            return;
        }
        request.setAttribute("rows", dao.stock(user.getCentreId()));
        request.setAttribute("canEdit", manager);   // chỉ Manager mới được cập nhật kho
        request.setAttribute("csrf", csrfToken(request));
        request.setAttribute("pageTitle", "Kho theo Centre");
        request.setAttribute("pageBreadcrumb", crumb(user, "Bắp nước"));
        showFlash(request);
        request.getRequestDispatcher("/WEB-INF/views/fnb/stock.jsp").forward(request, response);
    }

    /**
     * Toggle IsAvailable thủ công (Manager override).
     * Không thay đổi StockQuantity.
     * Dùng khi Manager muốn tắt món dù còn hàng (hàng lỗi, hết đột xuất).
     */
    private void toggleStock(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        int centreId = user.getCentreId();
        String itemText = request.getParameter("itemId");
        String available = request.getParameter("available");
        String target = request.getContextPath() + "/fnb/stock";
        if (itemText == null || !itemText.matches("[0-9]+") || itemText.length() >= 10
                || (!"true".equals(available) && !"false".equals(available))) {
            flash(request, "error", "Yêu cầu không hợp lệ.");
            response.sendRedirect(target);
            return;
        }
        int itemId = Integer.parseInt(itemText);
        if (!dao.isStockableItem(itemId, centreId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Món này không thuộc kho của Centre bạn.");
            return;
        }
        try {
            dao.setAvailability(centreId, itemId, Boolean.parseBoolean(available));
            flash(request, "success", "Đã cập nhật tình trạng kho.");
        } catch (SQLException exception) {
            exception.printStackTrace();
            flash(request, "error", "Không cập nhật được kho, vui lòng thử lại.");
        }
        response.sendRedirect(target);
    }

    /**
     * Cập nhật số lượng tồn kho (Manager nhập tay).
     * Khi quantity = 0 → hệ thống tự đặt IsAvailable = false.
     * Khi quantity > 0 → tự đặt IsAvailable = true.
     */
    private void updateQuantity(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        int centreId = user.getCentreId();
        String itemText = request.getParameter("itemId");
        String qtyText = request.getParameter("quantity") == null ? "" : request.getParameter("quantity").trim();
        String target = request.getContextPath() + "/fnb/stock";
        if (itemText == null || !itemText.matches("[0-9]+") || itemText.length() >= 10) {
            flash(request, "error", "Yêu cầu không hợp lệ.");
            response.sendRedirect(target);
            return;
        }
        if (!qtyText.matches("[0-9]+") || qtyText.length() > 6) {
            flash(request, "error", "Số lượng phải là số nguyên không âm (tối đa 999999).");
            response.sendRedirect(target);
            return;
        }
        int itemId = Integer.parseInt(itemText);
        int quantity = Integer.parseInt(qtyText);
        if (!dao.isStockableItem(itemId, centreId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Món này không thuộc kho của Centre bạn.");
            return;
        }
        try {
            dao.updateStockQuantity(centreId, itemId, quantity);
            String msg = quantity == 0
                    ? "Đã cập nhật số lượng về 0 – món tự động chuyển sang Hết hàng."
                    : "Đã cập nhật số lượng: " + quantity + " → Còn hàng.";
            flash(request, "success", msg);
        } catch (SQLException exception) {
            exception.printStackTrace();
            flash(request, "error", "Không cập nhật được số lượng, vui lòng thử lại.");
        }
        response.sendRedirect(target);
    }
}
