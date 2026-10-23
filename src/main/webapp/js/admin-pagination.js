/**
 * Điều khiển bộ phân trang tương tác và lọc dữ liệu trực tiếp trong trang quản trị CineFlow.
 * Hỗ trợ lọc theo:
 * - Trạng thái phim (Tất cả, Đang chiếu, Sắp chiếu, Đã kết thúc, Lưu trữ)
 * - Từ khóa chung (Tên phim, Đạo diễn, Diễn viên, Thể loại...)
 * - Mã phim chuyên biệt (ID số hoặc CF-XXXX)
 * - Thể loại phim (Genre)
 * - Độ tuổi khán giả (Age Rating: P, K, T13, T16, T18)
 * - Ngày khởi chiếu (Mốc nhanh: Đã chiếu, Sắp chiếu, Tháng này, hoặc Chọn khoảng Từ ngày - Đến ngày)
 */
function initAdminPagination(options) {
    const tableBody = document.getElementById(options.tableBodyId);
    const paginationContainer = document.getElementById(options.paginationId);
    const searchInput = document.getElementById(options.searchInputId);
    const codeSearchInput = options.codeSearchInputId ? document.getElementById(options.codeSearchInputId) : null;
    const statusPills = document.querySelectorAll(options.statusPillSelector || '.filter-pill[data-status], .cf-pill[data-status]');
    const genreSelect = document.getElementById(options.genreSelectId);
    const ageSelect = options.ageSelectId ? document.getElementById(options.ageSelectId) : null;
    const releasePresetSelect = options.releasePresetSelectId ? document.getElementById(options.releasePresetSelectId) : null;
    const releaseFromInput = options.releaseFromInputId ? document.getElementById(options.releaseFromInputId) : null;
    const releaseToInput = options.releaseToInputId ? document.getElementById(options.releaseToInputId) : null;
    const resetFiltersBtn = options.resetButtonId ? document.getElementById(options.resetButtonId) : null;
    const pageSizeSelect = document.getElementById(options.pageSizeSelectId);
    const infoDisplay = document.getElementById(options.infoDisplayId);
    const tableWrapper = document.getElementById(options.tableWrapperId);
    const emptyState = document.getElementById(options.emptyStateId);

    const tableRows = tableBody ? Array.from(tableBody.querySelectorAll('tr[data-movie-id]')) : [];
    const totalItemsCount = tableRows.length;

    let currentPage = 1;
    let pageSize = pageSizeSelect ? (parseInt(pageSizeSelect.value, 10) || 12) : 12;
    let currentStatus = 'ALL';
    let currentSearch = '';
    let currentCode = '';
    let currentGenre = 'ALL';
    let currentAge = 'ALL';
    let currentReleasePreset = 'ALL';
    let currentReleaseFrom = '';
    let currentReleaseTo = '';

    function getTodayString() {
        const d = new Date();
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function getTomorrowString() {
        const d = new Date();
        d.setDate(d.getDate() + 1);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function getFirstDayOfMonth() {
        const d = new Date();
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        return `${year}-${month}-01`;
    }

    function getLastDayOfMonth() {
        const d = new Date();
        const year = d.getFullYear();
        const month = d.getMonth() + 1;
        const lastDay = new Date(year, month, 0).getDate();
        const monthStr = String(month).padStart(2, '0');
        const dayStr = String(lastDay).padStart(2, '0');
        return `${year}-${monthStr}-${dayStr}`;
    }

    function filterMatches(item) {
        const title = (item.dataset.title || '').toLowerCase();
        const genre = (item.dataset.genre || '').toLowerCase();
        const director = (item.dataset.director || '').toLowerCase();
        const status = item.dataset.status || '';
        const id = (item.dataset.movieId || '').trim();
        const formattedCode = (item.dataset.code || ('cf-' + id.padStart(4, '0'))).toLowerCase();
        const age = (item.dataset.ageRating || '').trim().toUpperCase();
        const releaseDateStr = (item.dataset.releaseDate || '').trim(); // YYYY-MM-DD

        // 1. So khớp trạng thái phát hành (Status Pills)
        if (currentStatus !== 'ALL' && status !== currentStatus) {
            return false;
        }

        // 2. So khớp thể loại phim
        if (currentGenre !== 'ALL' && !genre.includes(currentGenre.toLowerCase())) {
            return false;
        }

        // 3. So khớp độ tuổi khán giả (Age Rating)
        if (currentAge !== 'ALL' && age !== currentAge.toUpperCase()) {
            return false;
        }

        // 4. So khớp theo mã phim chuyên biệt
        if (currentCode) {
            const rawSearch = currentCode.toLowerCase().trim();
            const strippedCode = rawSearch.replace(/^cf-?0*/, '');
            const matchId = id === strippedCode || id === rawSearch;
            const matchFormatted = formattedCode.includes(rawSearch);
            const matchPrefix = id.startsWith(strippedCode);
            if (!matchId && !matchFormatted && !matchPrefix) {
                return false;
            }
        }

        // 5. So khớp theo ngày khởi chiếu
        if (currentReleasePreset === 'RELEASED') {
            const today = getTodayString();
            if (!releaseDateStr || releaseDateStr > today) return false;
        } else if (currentReleasePreset === 'UPCOMING') {
            const today = getTodayString();
            if (!releaseDateStr || releaseDateStr <= today) return false;
        } else if (currentReleasePreset === 'THIS_MONTH') {
            const thisMonth = getTodayString().substring(0, 7);
            if (!releaseDateStr || !releaseDateStr.startsWith(thisMonth)) return false;
        }

        // Khoảng ngày cụ thể (Từ ngày - Đến ngày)
        if (currentReleaseFrom) {
            if (!releaseDateStr || releaseDateStr < currentReleaseFrom) return false;
        }
        if (currentReleaseTo) {
            if (!releaseDateStr || releaseDateStr > currentReleaseTo) return false;
        }

        // 6. So khớp chuỗi tìm kiếm từ khóa chung (Tên, Thể loại, Đạo diễn, Mã phim)
        if (currentSearch) {
            const q = currentSearch.toLowerCase();
            const textMatch = title.includes(q) ||
                              genre.includes(q) ||
                              director.includes(q) ||
                              id.includes(q) ||
                              formattedCode.includes(q);
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
                infoDisplay.innerHTML = 'Không tìm thấy phim phù hợp với các tiêu chí lọc đã chọn';
            } else {
                infoDisplay.innerHTML = `Hiển thị <strong>${startIndex + 1} - ${endIndex}</strong> trong tổng số <strong>${totalFiltered}</strong> phim` +
                    (totalFiltered !== totalItemsCount ? ` (lọc từ tổng số ${totalItemsCount} phim)` : '');
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

    if (codeSearchInput) {
        let debounceTimer;
        codeSearchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                currentCode = e.target.value.trim();
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

    if (ageSelect) {
        ageSelect.addEventListener('change', (e) => {
            currentAge = e.target.value;
            currentPage = 1;
            render();
        });
    }

    if (releasePresetSelect) {
        releasePresetSelect.addEventListener('change', (e) => {
            currentReleasePreset = e.target.value;
            if (currentReleasePreset === 'ALL') {
                currentReleaseFrom = '';
                currentReleaseTo = '';
                if (releaseFromInput) releaseFromInput.value = '';
                if (releaseToInput) releaseToInput.value = '';
            } else if (currentReleasePreset === 'RELEASED') {
                currentReleaseFrom = '';
                currentReleaseTo = getTodayString();
                if (releaseFromInput) releaseFromInput.value = '';
                if (releaseToInput) releaseToInput.value = currentReleaseTo;
            } else if (currentReleasePreset === 'UPCOMING') {
                currentReleaseFrom = getTomorrowString();
                currentReleaseTo = '';
                if (releaseFromInput) releaseFromInput.value = currentReleaseFrom;
                if (releaseToInput) releaseToInput.value = '';
            } else if (currentReleasePreset === 'THIS_MONTH') {
                currentReleaseFrom = getFirstDayOfMonth();
                currentReleaseTo = getLastDayOfMonth();
                if (releaseFromInput) releaseFromInput.value = currentReleaseFrom;
                if (releaseToInput) releaseToInput.value = currentReleaseTo;
            }
            currentPage = 1;
            render();
        });
    }

    if (releaseFromInput) {
        releaseFromInput.addEventListener('change', (e) => {
            currentReleaseFrom = e.target.value;
            if (releasePresetSelect && currentReleasePreset !== 'CUSTOM') {
                releasePresetSelect.value = 'CUSTOM';
                currentReleasePreset = 'CUSTOM';
            }
            currentPage = 1;
            render();
        });
    }

    if (releaseToInput) {
        releaseToInput.addEventListener('change', (e) => {
            currentReleaseTo = e.target.value;
            if (releasePresetSelect && currentReleasePreset !== 'CUSTOM') {
                releasePresetSelect.value = 'CUSTOM';
                currentReleasePreset = 'CUSTOM';
            }
            currentPage = 1;
            render();
        });
    }

    if (resetFiltersBtn) {
        resetFiltersBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            if (codeSearchInput) codeSearchInput.value = '';
            if (genreSelect) genreSelect.value = 'ALL';
            if (ageSelect) ageSelect.value = 'ALL';
            if (releasePresetSelect) releasePresetSelect.value = 'ALL';
            if (releaseFromInput) releaseFromInput.value = '';
            if (releaseToInput) releaseToInput.value = '';

            statusPills.forEach(p => p.classList.remove('active'));
            const allPill = Array.from(statusPills).find(p => p.dataset.status === 'ALL');
            if (allPill) allPill.classList.add('active');

            currentSearch = '';
            currentCode = '';
            currentStatus = 'ALL';
            currentGenre = 'ALL';
            currentAge = 'ALL';
            currentReleasePreset = 'ALL';
            currentReleaseFrom = '';
            currentReleaseTo = '';
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
