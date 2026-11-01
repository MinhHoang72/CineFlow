package service;

import dao.FnbDAO;
import model.dto.common.OptionDTO;
import model.dto.common.PageDTO;
import model.dto.fnb.FnbRowDTO;
import model.dto.fnb.StockRowDTO;
import model.entity.ComboLine;
import model.entity.FnbCombo;
import model.entity.FnbItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service: Xử lý toàn bộ logic nghiệp vụ liên quan đến F&B (Món lẻ, Combo, và Quản lý kho).
 * Tuân thủ mô hình 3 lớp Controller -> Service -> DAO.
 */
public class FnbService {

    public static final BigDecimal MAX_PRICE = new BigDecimal("10000000");
    private final FnbDAO dao;

    public FnbService() {
        this(new FnbDAO());
    }

    public FnbService(FnbDAO dao) {
        this.dao = dao;
    }

    public static class ValidationResult<T> {
        private final boolean valid;
        private final Map<String, String> errors;
        private final T entity;

        public ValidationResult(boolean valid, Map<String, String> errors, T entity) {
            this.valid = valid;
            this.errors = errors;
            this.entity = entity;
        }

        public boolean isValid() {
            return valid;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public T getEntity() {
            return entity;
        }
    }

    public enum StatusResult {
        SUCCESS,
        INVALID_REQUEST,
        NOT_FOUND,
        FORBIDDEN,
        ITEM_IN_ACTIVE_COMBO,
        FAILED
    }

    public record OperationResult(StatusResult status, String message) {
        public boolean isSuccess() {
            return status == StatusResult.SUCCESS;
        }
    }

    public List<OptionDTO> getCategories() {
        return dao.categories();
    }

    public List<OptionDTO> getCentres() {
        return dao.centres();
    }

    public String getCentreLabel(Integer centreId) {
        if (centreId == null) return null;
        return dao.centreLabel(centreId);
    }

    public PageDTO<FnbRowDTO> search(String q, String category, String status,
                                     BigDecimal minPrice, BigDecimal maxPrice,
                                     String sort, Integer centreId, int page, int size) {
        return dao.search(q, category, status, minPrice, maxPrice, sort, centreId, page, size);
    }

    public FnbItem findItem(int itemId) {
        if (itemId <= 0) return null;
        return dao.findItem(itemId);
    }

    public FnbCombo findCombo(int comboId) {
        if (comboId <= 0) return null;
        return dao.findCombo(comboId);
    }

    public List<FnbItem> getActiveItems(int centreId) {
        return dao.activeItems(centreId);
    }

    public int countActiveCombosUsing(int itemId) {
        return dao.countActiveCombosUsing(itemId);
    }

    /**
     * Xác thực thông tin món F&B trước khi lưu.
     */
    public ValidationResult<FnbItem> validateItem(FnbItem item, String name, String description,
                                                  String status, String priceText, String categoryText) {
        Map<String, String> errors = new LinkedHashMap<>();
        name = name == null ? "" : name.trim();
        description = description == null ? "" : description.trim();
        status = status == null ? "" : status.trim();
        priceText = priceText == null ? "" : priceText.trim();
        categoryText = categoryText == null ? "" : categoryText.trim();

        item.setItemName(name);
        item.setDescription(description);
        item.setStatus(status);

        if (name.isEmpty()) {
            errors.put("name", "Tên món là bắt buộc.");
        } else if (name.length() > 100) {
            errors.put("name", "Tên món tối đa 100 ký tự.");
        }

        if (description.length() > 500) {
            errors.put("description", "Mô tả tối đa 500 ký tự.");
        }

        List<OptionDTO> categories = getCategories();
        int categoryId = 0;
        if (categoryText.matches("[0-9]+") && categoryText.length() < 10) {
            categoryId = Integer.parseInt(categoryText);
        }
        boolean categoryOk = false;
        for (OptionDTO option : categories) {
            if (option.getId() == categoryId) {
                categoryOk = true;
                break;
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
                && countActiveCombosUsing(item.getFnbItemId()) > 0) {
            errors.put("status", "Món đang nằm trong combo đang bán, hãy gỡ khỏi combo trước khi lưu trữ.");
        }

        return new ValidationResult<>(errors.isEmpty(), errors, item);
    }

    public void saveItem(FnbItem item) throws SQLException {
        if (item.getFnbItemId() == 0) {
            dao.insertItem(item);
        } else {
            dao.updateItem(item);
        }
    }

    /**
     * Xác thực thông tin Combo F&B trước khi lưu.
     */
    public ValidationResult<FnbCombo> validateCombo(FnbCombo combo, String name, String description,
                                                    String status, String priceText,
                                                    String[] itemIds, String[] quantities, int centreId) {
        Map<String, String> errors = new LinkedHashMap<>();
        name = name == null ? "" : name.trim();
        description = description == null ? "" : description.trim();
        status = status == null ? "" : status.trim();
        priceText = priceText == null ? "" : priceText.trim();

        combo.setComboName(name);
        combo.setDescription(description);
        combo.setStatus(status);

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

        List<FnbItem> items = getActiveItems(centreId);
        Map<Integer, Integer> merged = new LinkedHashMap<>();
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
                            break;
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

        return new ValidationResult<>(errors.isEmpty(), errors, combo);
    }

    public void saveCombo(FnbCombo combo) throws SQLException {
        dao.saveCombo(combo);
    }

    /**
     * Thay đổi trạng thái ACTIVE / INACTIVE cho Món hoặc Combo.
     */
    public OperationResult changeStatus(boolean isCombo, int id, String status, int centreId) {
        if (id <= 0 || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))) {
            return new OperationResult(StatusResult.INVALID_REQUEST, "Yêu cầu không hợp lệ.");
        }

        boolean exists;
        int owner = 0;
        if (isCombo) {
            FnbCombo found = findCombo(id);
            exists = found != null;
            if (exists) owner = found.getCentreId();
        } else {
            FnbItem found = findItem(id);
            exists = found != null;
            if (exists) owner = found.getCentreId();
        }

        if (!exists) {
            return new OperationResult(StatusResult.NOT_FOUND, "Không tìm thấy món hoặc combo.");
        }
        if (owner != centreId) {
            return new OperationResult(StatusResult.FORBIDDEN, "Món hoặc combo này thuộc Centre khác.");
        }
        if (!isCombo && "INACTIVE".equals(status) && countActiveCombosUsing(id) > 0) {
            return new OperationResult(StatusResult.ITEM_IN_ACTIVE_COMBO, "Món đang nằm trong combo đang bán, hãy gỡ khỏi combo trước khi lưu trữ.");
        }

        try {
            dao.setStatus(isCombo, id, status);
            String label = isCombo ? "Combo " : "Món ";
            String action = "ACTIVE".equals(status) ? "đã được khôi phục." : "đã được lưu trữ.";
            return new OperationResult(StatusResult.SUCCESS, label + action);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new OperationResult(StatusResult.FAILED, "Không đổi được trạng thái, vui lòng thử lại.");
        }
    }

    // ==============================================================
    // KHO HÀNG (STOCK)
    // ==============================================================

    public List<StockRowDTO> getStock(int centreId) {
        if (centreId <= 0) return List.of();
        return dao.stock(centreId);
    }

    public boolean isStockableItem(int itemId, int centreId) {
        if (itemId <= 0 || centreId <= 0) return false;
        return dao.isStockableItem(itemId, centreId);
    }

    public void setAvailability(int centreId, int itemId, boolean available) throws SQLException {
        dao.setAvailability(centreId, itemId, available);
    }

    public void updateStockQuantity(int centreId, int itemId, int quantity) throws SQLException {
        dao.updateStockQuantity(centreId, itemId, quantity);
    }
}
