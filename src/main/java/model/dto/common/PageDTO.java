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
}
