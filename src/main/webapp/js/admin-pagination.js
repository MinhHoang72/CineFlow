/**
 * Bộ phân trang, tìm kiếm tiếng Việt không dấu và lọc trạng thái dùng chung cho các bảng quản trị CineFlow.
 * Được dùng ở: movieList.jsp, customerList.jsp, managerList.jsp, staffList.jsp.
 */

/**
 * Loại bỏ dấu tiếng Việt (ví dụ: "Đào, Phở và Piano" -> "Dao, Pho va Piano")
 * để hỗ trợ tìm kiếm linh hoạt dù người dùng gõ có dấu hay không dấu.
 *
 * @param {string} str chuỗi văn bản gốc
 * @returns {string} chuỗi đã bỏ dấu tiếng Việt
 */
function removeVietnameseDiacritics(str) {
    if (!str) return '';
    return str.normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .replace(/Đ/g, 'D');
}

/**
 * Hỗ trợ lấy phần tử DOM từ chuỗi ID hoặc trả về trực tiếp nếu đã là phần tử HTML.
 *
 * @param {string|HTMLElement|null} val mã ID hoặc phần tử DOM
 * @returns {HTMLElement|null} phần tử DOM tìm được
 */
function resolveEl(val) {
    return typeof val === 'string' ? document.getElementById(val) : (val || null);
}

/**
 * Khởi tạo bộ điều khiển tìm kiếm, lọc trạng thái và phân trang cho một bảng HTML.
 *
 * @param {Object} options cấu hình ID các phần tử DOM (tableBodyId, searchInputId, paginationId, ...)
 * @returns {Object|null} đối tượng điều khiển gồm { render, refreshRows, setPage, setStatus, setSearch }
 */
function initTablePagination(options) {
    if (!options) return null;
    const tableBody = resolveEl(options.tableBody || options.tableBodyId);
    if (!tableBody) return null;

    // 1. Thu thập toàn bộ các dòng <tr> dữ liệu hiện có trong bảng
    const rowSelector = options.rowSelector || 'tr:not(.empty-row)';
    let rows = Array.from(tableBody.querySelectorAll(rowSelector));

    // 2. Lấy tham chiếu các điều khiển giao diện (ô tìm kiếm, bộ lọc, nút phân trang, trạng thái rỗng)
    const paginationBox = resolveEl(options.paginationId || options.paginationControls || options.paginationControlsId);
    const infoDisplay = resolveEl(options.infoDisplayId || options.paginationInfo);
    const pageSizeSelect = resolveEl(options.pageSizeSelectId || options.pageSizeSelect);
    const searchInput = resolveEl(options.searchInputId || options.searchInput);
    const searchClearBtn = resolveEl(options.searchClearBtnId || options.searchClearBtn || options.clearButtonId);
    const emptyRow = resolveEl(options.emptyRowId || options.emptyRow);
    const emptyState = resolveEl(options.emptyStateId || options.emptyState);
    const tableWrapper = resolveEl(options.tableWrapperId || options.tableWrapper);
    const statusSelect = resolveEl(options.statusFilterSelectId || options.statusFilterSelect);
    const statusPills = options.statusPillSelector
        ? Array.from(document.querySelectorAll(options.statusPillSelector))
        : [];

    // 3. Khởi tạo biến trạng thái hiện tại của bảng (trang hiện tại, trạng thái lọc, từ khóa, số dòng/trang)
    const itemLabel = options.itemLabel || 'bản ghi';
    let currentPage = 1;
    let currentStatus = options.defaultStatus || 'ALL';
    let currentSearch = (searchInput ? searchInput.value : '').trim();
    let pageSize = 10;

    if (pageSizeSelect) {
        pageSize = pageSizeSelect.value === 'ALL' ? 0 : (parseInt(pageSizeSelect.value, 10) || 10);
    } else if (options.defaultPageSize !== undefined) {
        pageSize = options.defaultPageSize === 'ALL' ? 0 : parseInt(options.defaultPageSize, 10);
    }

    /**
     * Lọc danh sách dòng <tr> thỏa mãn đồng thời: trạng thái, hàm lọc tùy biến (nếu có) và từ khóa tìm kiếm.
     *
     * @returns {HTMLTableRowElement[]} danh sách các dòng đạt điều kiện
     */
    function getFilteredRows() {
        const keywords = removeVietnameseDiacritics(currentSearch).toLowerCase().trim().split(/\s+/).filter(Boolean);
        const statusUpper = currentStatus.toUpperCase();

        return rows.filter(row => {
            // Lọc theo trạng thái (data-status trên thẻ <tr>)
            if (statusUpper !== 'ALL' && (row.dataset.status || '').toUpperCase() !== statusUpper) {
                return false;
            }
            // Gọi bộ lọc bổ sung nếu màn hình có truyền customFilter
            if (typeof options.customFilter === 'function' && !options.customFilter(row)) {
                return false;
            }
            // Lọc theo từ khóa: tách từng từ, yêu cầu mọi từ đều xuất hiện trong nội dung dòng (không phân biệt dấu)
            if (keywords.length > 0) {
                const rawText = row.dataset.search || row.innerText || '';
                const target = removeVietnameseDiacritics(rawText).toLowerCase();
                return keywords.every(word => target.includes(word));
            }
            return true;
        });
    }

    /** Tạo một nút bấm chuyển trang HTML. */
    function createPageBtn(label, onClick, disabled, active) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'cf-page-btn' + (active ? ' active' : '');
        btn.textContent = label;
        btn.disabled = !!disabled;
        btn.addEventListener('click', onClick);
        return btn;
    }

    /**
     * Vẽ thanh nút phân trang (‹ 1 ... trang hiện tại ... trang cuối ›).
     *
     * @param {number} totalPages tổng số trang tính được
     */
    function renderPagination(totalPages) {
        if (!paginationBox) return;
        paginationBox.innerHTML = '';
        if (totalPages <= 1) return;

        // Nút Trang trước (‹)
        paginationBox.appendChild(createPageBtn('‹', () => { currentPage--; render(); }, currentPage === 1));

        // Chỉ hiển thị trang đầu, trang cuối và các trang kề cận trang hiện tại (ẩn phần giữa bằng "...")
        const pages = [];
        for (let i = 1; i <= totalPages; i++) {
            if (i === 1 || i === totalPages || Math.abs(i - currentPage) <= 1) {
                pages.push(i);
            } else if (pages[pages.length - 1] !== '...') {
                pages.push('...');
            }
        }

        pages.forEach(p => {
            if (p === '...') {
                const span = document.createElement('span');
                span.style.cssText = 'padding:0 6px;color:var(--cf-muted);';
                span.textContent = '...';
                paginationBox.appendChild(span);
            } else {
                paginationBox.appendChild(createPageBtn(p, () => { currentPage = p; render(); }, false, p === currentPage));
            }
        });

        // Nút Trang sau (›)
        paginationBox.appendChild(createPageBtn('›', () => { currentPage++; render(); }, currentPage === totalPages));
    }

    /**
     * Cập nhật giao diện bảng: ẩn/hiện dòng theo trang hiện tại, cập nhật chữ thống kê và thanh phân trang.
     */
    function render() {
        const filtered = getFilteredRows();
        const total = filtered.length;
        const totalPages = pageSize > 0 ? Math.ceil(total / pageSize) : 1;
        currentPage = Math.max(1, Math.min(currentPage, totalPages || 1));

        // Cắt đúng lát dữ liệu thuộc trang hiện tại (pageSize = 0 nghĩa là hiển thị Tất cả)
        const start = pageSize > 0 ? (currentPage - 1) * pageSize : 0;
        const end = pageSize > 0 ? Math.min(start + pageSize, total) : total;
        const visibleSet = new Set(filtered.slice(start, end));

        rows.forEach(row => { row.style.display = visibleSet.has(row) ? '' : 'none'; });

        // Bật/tắt giao diện "Không tìm thấy dữ liệu" khi kết quả lọc bằng 0
        if (emptyRow) emptyRow.style.display = total === 0 ? '' : 'none';
        if (emptyState) {
            emptyState.style.display = total === 0 ? 'block' : 'none';
            if (tableWrapper) tableWrapper.style.display = total === 0 ? 'none' : '';
        }

        // Cập nhật dòng mô tả số bản ghi đang hiển thị
        if (infoDisplay) {
            infoDisplay.innerHTML = total === 0
                ? 'Không tìm thấy ' + itemLabel + ' nào phù hợp'
                : 'Hiển thị <strong>' + (start + 1) + ' - ' + end + '</strong> trong tổng số <strong>' + total + '</strong> ' + itemLabel +
                  (total !== rows.length ? ' (lọc từ ' + rows.length + ' ' + itemLabel + ')' : '');
        }

        renderPagination(totalPages);
        if (typeof options.onPageChange === 'function') {
            options.onPageChange({ currentPage, totalPages, totalFiltered: total, startIndex: start, endIndex: end });
        }
    }

    /** Xóa sạch từ khóa tìm kiếm và đưa bộ lọc trạng thái về "Tất cả" (ALL). */
    function resetAll() {
        currentSearch = '';
        currentStatus = 'ALL';
        currentPage = 1;
        if (searchInput) searchInput.value = '';
        if (searchClearBtn) searchClearBtn.style.display = 'none';
        if (statusSelect) statusSelect.value = 'ALL';
        statusPills.forEach(p => p.classList.toggle('active', (p.dataset.status || p.dataset.tab || 'ALL').toUpperCase() === 'ALL'));
        render();
    }

    // 4. Gắn sự kiện tương tác người dùng (gõ tìm kiếm, xóa nhanh, bấm tab trạng thái, đổi số dòng/trang)
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            currentSearch = searchInput.value;
            currentPage = 1;
            if (searchClearBtn) searchClearBtn.style.display = currentSearch ? 'grid' : 'none';
            render();
        });
    }

    if (searchClearBtn) {
        searchClearBtn.style.display = currentSearch ? 'grid' : 'none';
        searchClearBtn.addEventListener('click', () => {
            if (searchInput) { searchInput.value = ''; searchInput.focus(); }
            currentSearch = '';
            searchClearBtn.style.display = 'none';
            currentPage = 1;
            render();
        });
    }

    statusPills.forEach(pill => {
        pill.addEventListener('click', (e) => {
            if (pill.tagName === 'A') e.preventDefault();
            statusPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            currentStatus = pill.dataset.status || pill.dataset.tab || 'ALL';
            if (statusSelect) statusSelect.value = currentStatus;
            currentPage = 1;
            render();
        });
    });

    if (statusSelect) {
        statusSelect.value = currentStatus;
        statusSelect.addEventListener('change', () => {
            currentStatus = statusSelect.value || 'ALL';
            statusPills.forEach(p => {
                const st = (p.dataset.status || p.dataset.tab || 'ALL').toUpperCase();
                p.classList.toggle('active', st === currentStatus.toUpperCase());
            });
            currentPage = 1;
            render();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', () => {
            pageSize = pageSizeSelect.value === 'ALL' ? 0 : parseInt(pageSizeSelect.value, 10);
            currentPage = 1;
            render();
        });
    }

    [options.resetButtonId, options.emptyResetButtonId].forEach(id => {
        const btn = resolveEl(id);
        if (btn) btn.addEventListener('click', resetAll);
    });

    // Hiển thị lần đầu khi tải trang
    render();

    return {
        render,
        refreshRows: () => { rows = Array.from(tableBody.querySelectorAll(rowSelector)); render(); },
        setPage: (p) => { currentPage = p; render(); },
        setStatus: (st) => { currentStatus = st; currentPage = 1; render(); },
        setSearch: (q) => { currentSearch = q; if (searchInput) searchInput.value = q; currentPage = 1; render(); }
    };
}

/**
 * Hàm bao (wrapper) khởi tạo phân trang riêng cho trang Danh mục phim (movieList.jsp).
 *
 * @param {Object} options cấu hình ID các phần tử DOM trên movieList.jsp
 * @returns {Object|null} bộ điều khiển phân trang
 */
function initAdminPagination(options) {
    return initTablePagination(Object.assign({
        rowSelector: 'tr[data-movie-id]',
        itemLabel: 'phim'
    }, options));
}
