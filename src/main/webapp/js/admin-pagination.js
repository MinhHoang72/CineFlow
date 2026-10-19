/**
 * Điều khiển bộ phân trang tương tác và lọc dữ liệu trực tiếp trong trang quản trị CineFlow.
 */
function initAdminPagination(options) {
    const tableBody = document.getElementById(options.tableBodyId);
    const paginationContainer = document.getElementById(options.paginationId);
    const searchInput = document.getElementById(options.searchInputId);
    const statusPills = document.querySelectorAll(options.statusPillSelector || '.filter-pill[data-status], .cf-pill[data-status]');
    const genreSelect = document.getElementById(options.genreSelectId);
    const pageSizeSelect = document.getElementById(options.pageSizeSelectId);
    const infoDisplay = document.getElementById(options.infoDisplayId);
    const tableWrapper = document.getElementById(options.tableWrapperId);
    const emptyState = document.getElementById(options.emptyStateId);

    const tableRows = tableBody ? Array.from(tableBody.querySelectorAll('tr[data-movie-id]')) : [];
    const totalItemsCount = tableRows.length;

    let currentPage = 1;
    let pageSize = pageSizeSelect ? (parseInt(pageSizeSelect.value, 10) || 10) : 10;
    let currentStatus = 'ALL';
    let currentSearch = '';
    let currentGenre = 'ALL';

    function filterMatches(item) {
        const title = (item.dataset.title || '').toLowerCase();
        const genre = (item.dataset.genre || '').toLowerCase();
        const director = (item.dataset.director || '').toLowerCase();
        const status = item.dataset.status || '';
        const id = item.dataset.movieId || '';

        // So khớp trạng thái
        if (currentStatus !== 'ALL' && status !== currentStatus) {
            return false;
        }

        // So khớp thể loại
        if (currentGenre !== 'ALL' && !genre.includes(currentGenre.toLowerCase())) {
            return false;
        }

        // So khớp chuỗi tìm kiếm
        if (currentSearch) {
            const q = currentSearch.toLowerCase();
            const textMatch = title.includes(q) || genre.includes(q) || director.includes(q) || id.includes(q);
            if (!textMatch) {
                return false;
            }
        }

        return true;
    }

    function render() {
        const visibleRows = [];

        tableRows.forEach(row => {
            if (filterMatches(row)) {
                visibleRows.push(row);
            }
            row.style.display = 'none';
        });

        const totalFiltered = visibleRows.length;
        const actualPageSize = pageSize === 0 ? totalFiltered : pageSize;
        const totalPages = Math.ceil(totalFiltered / actualPageSize) || 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }

        const startIndex = (currentPage - 1) * actualPageSize;
        const endIndex = pageSize === 0 ? totalFiltered : Math.min(startIndex + actualPageSize, totalFiltered);

        // Hiển thị các dòng thuộc trang hiện tại
        for (let i = startIndex; i < endIndex; i++) {
            if (visibleRows[i]) {
                visibleRows[i].style.display = '';
            }
        }

        // Xử lý trạng thái danh sách rỗng
        if (totalFiltered === 0) {
            if (emptyState) {
                emptyState.style.display = 'block';
            }
            if (tableWrapper) {
                tableWrapper.style.display = 'none';
            }
        } else {
            if (emptyState) {
                emptyState.style.display = 'none';
            }
            if (tableWrapper) {
                tableWrapper.style.display = '';
            }
        }

        // Hiển thị thông tin thống kê số lượng dòng hiển thị
        if (infoDisplay) {
            if (totalFiltered === 0) {
                infoDisplay.innerHTML = 'Không tìm thấy kết quả phù hợp';
            } else {
                infoDisplay.innerHTML = `Hiển thị <strong>${startIndex + 1} - ${endIndex}</strong> trong tổng số <strong>${totalFiltered}</strong> phim` +
                    (totalFiltered !== totalItemsCount ? ` (lọc từ tổng ${totalItemsCount} phim)` : '');
            }
        }

        // Hiển thị các nút phân trang
        renderPaginationControls(totalPages);
    }

    function renderPaginationControls(totalPages) {
        if (!paginationContainer) {
            return;
        }
        paginationContainer.innerHTML = '';

        if (totalPages <= 1) {
            return;
        }

        // Nút trang trước (Prev)
        const prevBtn = document.createElement('button');
        prevBtn.type = 'button';
        prevBtn.className = 'cf-page-btn';
        prevBtn.innerHTML = '‹';
        prevBtn.title = 'Trang trước';
        prevBtn.disabled = currentPage === 1;
        prevBtn.addEventListener('click', () => {
            if (currentPage > 1) {
                currentPage--;
                render();
            }
        });
        paginationContainer.appendChild(prevBtn);

        // Tạo danh sách số trang kèm dấu ba chấm
        const pages = [];
        if (totalPages <= 7) {
            for (let i = 1; i <= totalPages; i++) {
                pages.push(i);
            }
        } else {
            pages.push(1);
            if (currentPage > 3) {
                pages.push('...');
            }
            const start = Math.max(2, currentPage - 1);
            const end = Math.min(totalPages - 1, currentPage + 1);
            for (let i = start; i <= end; i++) {
                pages.push(i);
            }
            if (currentPage < totalPages - 2) {
                pages.push('...');
            }
            pages.push(totalPages);
        }

        pages.forEach(p => {
            if (p === '...') {
                const ellipsis = document.createElement('span');
                ellipsis.style.padding = '0 6px';
                ellipsis.style.color = 'var(--cf-muted)';
                ellipsis.textContent = '...';
                paginationContainer.appendChild(ellipsis);
            } else {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'cf-page-btn' + (p === currentPage ? ' active' : '');
                btn.textContent = p;
                btn.addEventListener('click', () => {
                    currentPage = p;
                    render();
                });
                paginationContainer.appendChild(btn);
            }
        });

        // Nút trang tiếp theo (Next)
        const nextBtn = document.createElement('button');
        nextBtn.type = 'button';
        nextBtn.className = 'cf-page-btn';
        nextBtn.innerHTML = '›';
        nextBtn.title = 'Trang tiếp theo';
        nextBtn.disabled = currentPage === totalPages;
        nextBtn.addEventListener('click', () => {
            if (currentPage < totalPages) {
                currentPage++;
                render();
            }
        });
        paginationContainer.appendChild(nextBtn);
    }

    // Đăng ký các sự kiện lắng nghe tương tác
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                currentSearch = e.target.value.trim();
                currentPage = 1;
                render();
            }, 180);
        });
    }

    statusPills.forEach(pill => {
        pill.addEventListener('click', () => {
            statusPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            currentStatus = pill.dataset.status || 'ALL';
            currentPage = 1;
            render();
        });
    });

    if (genreSelect) {
        genreSelect.addEventListener('change', (e) => {
            currentGenre = e.target.value;
            currentPage = 1;
            render();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', (e) => {
            const val = pageSizeSelect.value;
            pageSize = val === 'ALL' ? 0 : parseInt(val, 10);
            currentPage = 1;
            render();
        });
    }

    // Khởi tạo hiển thị lần đầu
    render();
}
