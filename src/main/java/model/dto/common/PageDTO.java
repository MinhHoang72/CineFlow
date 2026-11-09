package model.dto.common;

import java.util.List;

public class PageDTO<T> {
    private final List<T> items;
    private final int page;
    private final int size;
    private final int total;

    public PageDTO(List<T> items, int page, int size, int total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotal() {
        return total;
    }

    public int getTotalPages() {
        return Math.max(1, (total + size - 1) / size); 
    }

    public int getFrom() {
        return total == 0 ? 0 : (page - 1) * size + 1;
    }

    public int getTo() {
        return Math.min(total, page * size);
    }

    public boolean hasPrev() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < getTotalPages();
    }

    /**
     * Tự động sinh mã HTML thanh điều hướng phân trang dùng chung cho toàn bộ dự án.
     * @param base đường dẫn gốc phân trang (VD: /admin/centres?search=...&page=)
     * @return chuỗi HTML thanh phân trang hoàn chỉnh
     */
    public String renderPager(String base) {
        int totalPages = getTotalPages();
        if (totalPages <= 1) {
            return "";
        }
        if (base == null) {
            base = "?page=";
        }

        StringBuilder html = new StringBuilder("<div class=\"cf-pagination-controls\">");

        // Nút trang trước
        if (page > 1) {
            html.append("<a href=\"").append(base).append(page - 1).append("\" class=\"cf-page-btn\" title=\"Trang trước\">«</a>");
        } else {
            html.append("<span class=\"cf-page-btn disabled\" title=\"Trang trước\">«</span>");
        }

        int last = 0;
        for (int i = 1; i <= totalPages; i++) {
            if (i == 1 || i == totalPages || Math.abs(i - page) <= 2) {
                if (last != 0 && i - last > 1) {
                    html.append("<span class=\"cf-page-ellipsis\">...</span>");
                }
                //trang hiện tại đang đưnsg 
                if (i == page) {
                    html.append("<span class=\"cf-page-btn active\">").append(i).append("</span>");
                } else {
                    // nút ấn ở đây 
                    html.append("<a href=\"").append(base).append(i).append("\" class=\"cf-page-btn\">").append(i).append("</a>");
                }
                last = i;
            }
        }
 
        // Nút trang sau
        if (page < totalPages) {
            html.append("<a href=\"").append(base).append(page + 1).append("\" class=\"cf-page-btn\" title=\"Trang sau\">»</a>");
        } else {
            html.append("<span class=\"cf-page-btn disabled\" title=\"Trang sau\">»</span>");
        }

        return html.append("</div>").toString();
    }
}
