/**
 * Điều khiển bộ phân trang tương tác và tìm kiếm trực tiếp trong trang quản trị phim CineFlow.
 * Tính năng chính:
 * - Tìm kiếm tức thì theo từng ký tự (Search-as-you-type)
 * - Tự động nhận diện tiếng Việt có dấu / không dấu (Accent-insensitive)
 * - Hỗ trợ gõ sai ký tự, sai dấu (Typo tolerance & Levenshtein distance)
 * - Đề xuất tự động (Autocomplete Suggestions Dropdown) với highlight từ khóa
 * - Phân trang động & Lọc theo trạng thái phát hành
 */

function removeVietnameseDiacritics(str) {
    if (!str) return '';
    return str
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .replace(/Đ/g, 'D')
        .replace(/[\u02C6\u0306\u031B]/g, '');
}

function levenshteinDistance(s1, s2) {
    if (s1 === s2) return 0;
    const len1 = s1.length, len2 = s2.length;
    if (len1 === 0) return len2;
    if (len2 === 0) return len1;

    let prevRow = Array.from({ length: len2 + 1 }, (_, i) => i);
    let currRow = new Array(len2 + 1);

    for (let i = 1; i <= len1; i++) {
        currRow[0] = i;
        const c1 = s1[i - 1];
        for (let j = 1; j <= len2; j++) {
            const cost = c1 === s2[j - 1] ? 0 : 1;
            currRow[j] = Math.min(
                prevRow[j] + 1,
                currRow[j - 1] + 1,
                prevRow[j - 1] + cost
            );
        }
        prevRow = [...currRow];
    }
    return prevRow[len2];
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function highlightMatch(text, query) {
    if (!query || !text) return escapeHtml(text);
    const qNorm = removeVietnameseDiacritics(query).toLowerCase().trim();
    if (!qNorm) return escapeHtml(text);

    // Nếu query 1 ký tự, highlight tất cả vị trí xuất hiện ký tự đó
    if (qNorm.length === 1) {
        let res = '';
        for (let i = 0; i < text.length; i++) {
            const cNorm = removeVietnameseDiacritics(text[i]).toLowerCase();
            if (cNorm === qNorm) {
                res += '<mark>' + escapeHtml(text[i]) + '</mark>';
            } else {
                res += escapeHtml(text[i]);
            }
        }
        return res;
    }

    // Tìm vị trí chuỗi con
    const textNorm = removeVietnameseDiacritics(text).toLowerCase();
    const idx = textNorm.indexOf(qNorm);
    if (idx !== -1) {
        return escapeHtml(text.substring(0, idx)) +
            '<mark>' + escapeHtml(text.substring(idx, idx + qNorm.length)) + '</mark>' +
            escapeHtml(text.substring(idx + qNorm.length));
    }

    // Highlight từng từ
    const words = qNorm.split(/\s+/).filter(Boolean);
    let result = escapeHtml(text);
    for (const w of words) {
        if (w.length >= 2) {
            const regex = new RegExp('(' + w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ')', 'gi');
            result = result.replace(regex, '<mark>$1</mark>');
        }
    }
    return result;
}

function computeMovieMatch(rawQuery, movie) {
    if (!rawQuery) return { isMatch: true, score: 100 };

    const qNorm = removeVietnameseDiacritics(rawQuery).toLowerCase().trim();
    if (!qNorm) return { isMatch: true, score: 100 };

    const titleNorm = removeVietnameseDiacritics(movie.title || '').toLowerCase();
    const genreNorm = removeVietnameseDiacritics(movie.genre || '').toLowerCase();
    const directorNorm = removeVietnameseDiacritics(movie.director || '').toLowerCase();
    const codeNorm = removeVietnameseDiacritics(movie.code || '').toLowerCase();
    const idNorm = String(movie.movieId || '').toLowerCase();

    const titleWords = titleNorm.split(/[\s,./_–-]+/).filter(Boolean);
    const fullTextNorm = `${titleNorm} ${codeNorm} ${genreNorm} ${directorNorm} ${idNorm}`;
    const allWords = fullTextNorm.split(/[\s,./_–-]+/).filter(Boolean);

    // 1. Query 1 ký tự (VD: 'h' -> tìm thấy 'Hành Tinh Cát', 'Đất Rừng Phương Nam' vì chứa chữ 'h')
    if (qNorm.length === 1) {
        if (titleNorm.startsWith(qNorm)) {
            return { isMatch: true, score: 110 };
        }
        for (const tw of titleWords) {
            if (tw.startsWith(qNorm)) {
                return { isMatch: true, score: 95 };
            }
            if (tw.includes(qNorm)) {
                return { isMatch: true, score: 75 };
            }
        }
        if (fullTextNorm.includes(qNorm)) {
            return { isMatch: true, score: 50 };
        }
        return { isMatch: false, score: 0 };
    }

    let score = 0;

    // 2. Exact substring trong title
    if (titleNorm.includes(qNorm)) {
        score += 100;
        if (titleNorm.startsWith(qNorm)) score += 35;
    } else if (fullTextNorm.includes(qNorm)) {
        score += 70;
    }

    // 3. Compact match (không tính khoảng trắng, VD: 'hanhtinhcat' -> 'hanh tinh cat')
    const compactTarget = titleNorm.replace(/[\s\-_–]+/g, '');
    const compactQuery = qNorm.replace(/[\s\-_–]+/g, '');
    if (compactQuery.length >= 3 && compactTarget.includes(compactQuery)) {
        score += 85;
    }

    // 4. Acronym / Chữ cái đầu (VD: 'htc' -> 'Hành Tinh Cát', 'drpn' -> 'Đất Rừng Phương Nam')
    const initials = titleWords.map(w => w[0]).join('');
    if (qNorm.length >= 2 && initials.includes(qNorm)) {
        score += 80;
    }

    // 5. Từng từ với Levenshtein distance (hỗ trợ gõ sai dấu, sai 1-2 ký tự: 'hanh tih' -> 'hanh tinh', 'dât rung' -> 'dat rung')
    const qWords = qNorm.split(/\s+/).filter(Boolean);
    let allWordsMatched = true;
    let wordPoints = 0;

    for (const qw of qWords) {
        let matchedThisWord = false;
        // Kiểm tra tiền tố hoặc chứa
        for (const tw of allWords) {
            if (tw.startsWith(qw) || tw.includes(qw)) {
                matchedThisWord = true;
                wordPoints += (tw.startsWith(qw) ? 25 : 20);
                break;
            }
        }

        // Nếu chưa khớp, kiểm tra khoảng cách sai ký tự nhẹ
        if (!matchedThisWord) {
            const maxDist = qw.length <= 2 ? 0 : (qw.length <= 4 ? 1 : 2);
            if (maxDist > 0) {
                let best = 999;
                for (const tw of allWords) {
                    if (Math.abs(tw.length - qw.length) <= maxDist) {
                        const dist = levenshteinDistance(qw, tw);
                        if (dist <= maxDist && dist < best) {
                            best = dist;
                        }
                    }
                }
                if (best <= maxDist) {
                    matchedThisWord = true;
                    wordPoints += (15 - best * 5);
                }
            }
        }

        if (!matchedThisWord) {
            allWordsMatched = false;
            break;
        }
    }

    if (allWordsMatched && qWords.length > 0) {
        score += wordPoints + 30;
    }

    // 6. Subsequence match (các ký tự xuất hiện theo thứ tự)
    if (score === 0 && qNorm.length >= 3) {
        let qIdx = 0;
        for (let i = 0; i < titleNorm.length && qIdx < qNorm.length; i++) {
            if (titleNorm[i] === qNorm[qIdx]) qIdx++;
        }
        if (qIdx === qNorm.length) {
            score += 35;
        }
    }

    return {
        isMatch: score > 0,
        score: score
    };
}

function initAdminPagination(options) {
    const tableBody = document.getElementById(options.tableBodyId);
    const paginationContainer = document.getElementById(options.paginationId);
    const searchInput = document.getElementById(options.searchInputId);
    const clearButton = options.clearButtonId ? document.getElementById(options.clearButtonId) : null;
    const statusPills = document.querySelectorAll(options.statusPillSelector || '.cf-pill[data-status]');
    const resetFiltersBtn = options.resetButtonId ? document.getElementById(options.resetButtonId) : null;
    const emptyResetBtn = options.emptyResetButtonId ? document.getElementById(options.emptyResetButtonId) : null;
    const pageSizeSelect = document.getElementById(options.pageSizeSelectId);
    const infoDisplay = document.getElementById(options.infoDisplayId);
    const tableWrapper = document.getElementById(options.tableWrapperId);
    const emptyState = document.getElementById(options.emptyStateId);

    const tableRows = tableBody ? Array.from(tableBody.querySelectorAll('tr[data-movie-id]')) : [];
    const totalItemsCount = tableRows.length;

    // Lưu trữ metadata của phim để tìm kiếm tức thì
    const movieDataList = tableRows.map(row => {
        const id = row.dataset.movieId || '';
        const code = row.dataset.code || ('CF-' + id.padStart(4, '0'));
        const title = row.dataset.title || '';
        const genre = row.dataset.genre || '';
        const director = row.dataset.director || '';
        const status = row.dataset.status || '';
        const ageRating = row.dataset.ageRating || 'P';
        const releaseDate = row.dataset.releaseDate || '';
        const durationEl = row.querySelector('td:nth-child(4) strong');
        const duration = durationEl ? durationEl.textContent.trim() : '';
        const titleLink = row.querySelector('.movie-title-link');
        const originalTitle = titleLink ? titleLink.textContent.trim() : title;

        return {
            row,
            movieId: id,
            code,
            title,
            originalTitle,
            titleLink,
            genre,
            director,
            status,
            ageRating,
            releaseDate,
            duration
        };
    });

    let currentPage = 1;
    let pageSize = pageSizeSelect ? (parseInt(pageSizeSelect.value, 10) || 12) : 12;
    let currentStatus = 'ALL';
    let currentSearch = '';

    function render() {
        const matchedMovies = [];

        movieDataList.forEach(item => {
            // 1. Lọc theo trạng thái phim (Pills)
            if (currentStatus !== 'ALL' && item.status !== currentStatus) {
                item.row.style.display = 'none';
                return;
            }

            // 2. Lọc thông minh theo từ khóa (Fuzzy, Accent-insensitive, Typo-tolerant)
            const matchResult = computeMovieMatch(currentSearch, item);
            if (matchResult.isMatch) {
                matchedMovies.push({
                    item,
                    score: matchResult.score
                });
            } else {
                item.row.style.display = 'none';
            }
        });

        // Sắp xếp các dòng hiển thị: điểm số tương đồng cao nhất lên đầu
        if (currentSearch.trim()) {
            matchedMovies.sort((a, b) => b.score - a.score);
        }

        // Highlight ký tự hoặc từ khớp trực tiếp trong tên phim trên bảng
        movieDataList.forEach(item => {
            if (item.titleLink) {
                if (currentSearch.trim()) {
                    item.titleLink.innerHTML = highlightMatch(item.originalTitle, currentSearch);
                } else {
                    item.titleLink.textContent = item.originalTitle;
                }
            }
        });

        const visibleRows = matchedMovies.map(m => m.item.row);
        const totalFiltered = visibleRows.length;
        const actualPageSize = pageSize === 0 ? totalFiltered : pageSize;
        const totalPages = Math.ceil(totalFiltered / actualPageSize) || 1;

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        const startIndex = (currentPage - 1) * actualPageSize;
        const endIndex = pageSize === 0 ? totalFiltered : Math.min(startIndex + actualPageSize, totalFiltered);

        // Ẩn tất cả dòng và hiển thị các dòng trang hiện tại theo thứ tự điểm số
        tableRows.forEach(r => { r.style.display = 'none'; });
        for (let i = startIndex; i < endIndex; i++) {
            if (visibleRows[i]) {
                if (tableBody) tableBody.appendChild(visibleRows[i]);
                visibleRows[i].style.display = '';
            }
        }

        // Xử lý Empty State khi không có kết quả
        if (totalFiltered === 0) {
            if (emptyState) emptyState.style.display = 'block';
            if (tableWrapper) tableWrapper.style.display = 'none';
        } else {
            if (emptyState) emptyState.style.display = 'none';
            if (tableWrapper) tableWrapper.style.display = '';
        }

        // Cập nhật thông tin phân trang
        if (infoDisplay) {
            if (totalFiltered === 0) {
                infoDisplay.innerHTML = 'Không tìm thấy phim phù hợp với từ khóa <strong>"' + escapeHtml(currentSearch) + '"</strong>';
            } else {
                infoDisplay.innerHTML = `Hiển thị <strong>${startIndex + 1} - ${endIndex}</strong> trong tổng số <strong>${totalFiltered}</strong> phim` +
                    (totalFiltered !== totalItemsCount ? ` (lọc từ tổng số ${totalItemsCount} phim)` : '');
            }
        }

        renderPaginationControls(totalPages);
    }

    function renderPaginationControls(totalPages) {
        if (!paginationContainer) return;
        paginationContainer.innerHTML = '';
        if (totalPages <= 1) return;

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

        const pages = [];
        if (totalPages <= 7) {
            for (let i = 1; i <= totalPages; i++) pages.push(i);
        } else {
            pages.push(1);
            if (currentPage > 3) pages.push('...');
            const start = Math.max(2, currentPage - 1);
            const end = Math.min(totalPages - 1, currentPage + 1);
            for (let i = start; i <= end; i++) pages.push(i);
            if (currentPage < totalPages - 2) pages.push('...');
            pages.push(totalPages);
        }

        pages.forEach(p => {
            if (p === '...') {
                const span = document.createElement('span');
                span.style.padding = '0 6px';
                span.style.color = 'var(--cf-muted)';
                span.textContent = '...';
                paginationContainer.appendChild(span);
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

    // --- SỰ KIỆN TÌM KIẾM TỨC THÌ (SEARCH-AS-YOU-TYPE) ---
    // Gõ bất kỳ ký tự nào, danh sách bảng phim bên dưới tự động lọc theo ý người dùng
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentSearch = e.target.value;
            currentPage = 1;

            if (clearButton) {
                clearButton.style.display = currentSearch.trim() ? 'inline-flex' : 'none';
            }
            if (resetFiltersBtn) {
                resetFiltersBtn.style.display = currentSearch.trim() ? 'grid' : 'none';
            }

            render();
        });
    }

    const resetSearchAndFilters = () => {
        if (searchInput) {
            searchInput.value = '';
            searchInput.focus();
        }
        currentSearch = '';
        if (clearButton) clearButton.style.display = 'none';
        if (resetFiltersBtn) resetFiltersBtn.style.display = 'none';
        currentPage = 1;
        render();
    };

    if (clearButton) {
        clearButton.addEventListener('click', resetSearchAndFilters);
    }

    if (resetFiltersBtn) {
        resetFiltersBtn.addEventListener('click', resetSearchAndFilters);
    }

    if (emptyResetBtn) {
        emptyResetBtn.addEventListener('click', () => {
            statusPills.forEach(p => p.classList.remove('active'));
            const allPill = document.querySelector('.cf-pill[data-status="ALL"]');
            if (allPill) allPill.classList.add('active');
            currentStatus = 'ALL';
            resetSearchAndFilters();
        });
    }

    // Lọc theo Status Pills
    statusPills.forEach(pill => {
        pill.addEventListener('click', (e) => {
                if (pill.tagName === 'A') e.preventDefault();
            statusPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            currentStatus = pill.dataset.status || 'ALL';
            currentPage = 1;
            render();
        });
    });

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', () => {
            const val = pageSizeSelect.value;
            pageSize = val === 'ALL' ? 0 : parseInt(val, 10);
            currentPage = 1;
            render();
        });
    }

    // Khởi tạo hiển thị lần đầu
    render();
}


/**
 * Bộ điều khiển phân trang & tìm kiếm tái sử dụng cho các bảng trong trang quản trị CineFlow.
 * Sử dụng cho: Quản lý khách hàng, Quản lý manager, Bổ nhiệm & Điều chuyển, Nhật ký lịch sử,...
 */
function initTablePagination(options) {
    if (!options) return null;
    const tableBody = typeof options.tableBody === 'string'
        ? document.getElementById(options.tableBody)
        : (options.tableBody || (options.tableBodyId ? document.getElementById(options.tableBodyId) : null));

    if (!tableBody) return null;

    const rowSelector = options.rowSelector || 'tr:not(.empty-row)';
    let rows = Array.from(tableBody.querySelectorAll(rowSelector));

    const paginationContainer = typeof options.paginationId === 'string'
        ? document.getElementById(options.paginationId)
        : (options.paginationId || options.paginationControls || (options.paginationControlsId ? document.getElementById(options.paginationControlsId) : null));

    const infoDisplay = typeof options.infoDisplayId === 'string'
        ? document.getElementById(options.infoDisplayId)
        : (options.infoDisplayId || options.paginationInfo);

    const pageSizeSelect = typeof options.pageSizeSelectId === 'string'
        ? document.getElementById(options.pageSizeSelectId)
        : (options.pageSizeSelectId || options.pageSizeSelect);

    const searchInput = typeof options.searchInputId === 'string'
        ? document.getElementById(options.searchInputId)
        : (options.searchInputId || options.searchInput);

    const searchClearBtn = typeof options.searchClearBtnId === 'string'
        ? document.getElementById(options.searchClearBtnId)
        : (options.searchClearBtnId || options.searchClearBtn);

    const emptyRow = typeof options.emptyRowId === 'string' ? document.getElementById(options.emptyRowId) : options.emptyRow;
    const emptyState = typeof options.emptyStateId === 'string'
        ? document.getElementById(options.emptyStateId)
        : (options.emptyStateId || options.emptyState);

    const tableWrapper = typeof options.tableWrapperId === 'string'
        ? document.getElementById(options.tableWrapperId)
        : (options.tableWrapperId || options.tableWrapper);

    const statusPills = options.statusPillSelector
        ? Array.from(document.querySelectorAll(options.statusPillSelector))
        : [];

    const statusSelect = typeof options.statusFilterSelectId === 'string'
        ? document.getElementById(options.statusFilterSelectId)
        : (options.statusFilterSelectId || options.statusFilterSelect);

    const itemLabel = options.itemLabel || 'bản ghi';
    let currentPage = 1;
    let currentStatus = options.defaultStatus || 'ALL';
    let currentSearch = (searchInput ? searchInput.value : '').trim();

    let pageSize = 10;
    if (pageSizeSelect) {
        const val = pageSizeSelect.value;
        pageSize = val === 'ALL' ? 0 : (parseInt(val, 10) || 10);
    } else if (typeof options.defaultPageSize !== 'undefined') {
        pageSize = options.defaultPageSize === 'ALL' ? 0 : parseInt(options.defaultPageSize, 10);
    }

    function refreshRows() {
        rows = Array.from(tableBody.querySelectorAll(rowSelector));
        render();
    }

    function getFilteredRows() {
        const qNorm = removeVietnameseDiacritics(currentSearch).toLowerCase().trim();
        const searchWords = qNorm ? qNorm.split(/\s+/).filter(Boolean) : [];
        const statusUpper = currentStatus.toUpperCase();

        return rows.filter(row => {
            // Filter by status if applicable
            if (statusUpper !== 'ALL') {
                const rowStatus = (row.dataset.status || '').toUpperCase();
                if (rowStatus && rowStatus !== statusUpper) {
                    return false;
                }
            }

            // Custom filter callback
            if (typeof options.customFilter === 'function') {
                if (!options.customFilter(row)) {
                    return false;
                }
            }

            // Search filtering
            if (searchWords.length > 0) {
                const searchTarget = row.dataset.search 
                    ? removeVietnameseDiacritics(row.dataset.search).toLowerCase()
                    : removeVietnameseDiacritics(row.innerText || '').toLowerCase();
                
                const isMatch = searchWords.every(word => searchTarget.includes(word));
                if (!isMatch) return false;
            }

            return true;
        });
    }

    function renderPaginationControls(totalPages) {
        if (!paginationContainer) return;
        paginationContainer.innerHTML = '';
        if (totalPages <= 1) return;

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

        const pages = [];
        if (totalPages <= 7) {
            for (let i = 1; i <= totalPages; i++) pages.push(i);
        } else {
            pages.push(1);
            if (currentPage > 3) pages.push('...');
            const start = Math.max(2, currentPage - 1);
            const end = Math.min(totalPages - 1, currentPage + 1);
            for (let i = start; i <= end; i++) pages.push(i);
            if (currentPage < totalPages - 2) pages.push('...');
            pages.push(totalPages);
        }

        pages.forEach(p => {
            if (p === '...') {
                const span = document.createElement('span');
                span.style.padding = '0 6px';
                span.style.color = 'var(--cf-muted)';
                span.textContent = '...';
                paginationContainer.appendChild(span);
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

    function render() {
        const filtered = getFilteredRows();
        const totalItemsCount = rows.length;
        const totalFiltered = filtered.length;

        const totalPages = pageSize > 0 ? Math.ceil(totalFiltered / pageSize) : 1;
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        }
        if (currentPage < 1) currentPage = 1;

        const startIndex = pageSize > 0 ? (currentPage - 1) * pageSize : 0;
        const endIndex = pageSize > 0 ? Math.min(startIndex + pageSize, totalFiltered) : totalFiltered;

        // Set visibility of rows
        const visibleSet = new Set(filtered.slice(startIndex, endIndex));
        rows.forEach(row => {
            if (visibleSet.has(row)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });

        if (emptyRow) { emptyRow.style.display = totalFiltered === 0 ? '' : 'none'; }
        // Toggle empty state if configured
        if (emptyState) {
            if (totalFiltered === 0) {
                emptyState.style.display = 'block';
                if (tableWrapper) tableWrapper.style.display = 'none';
            } else {
                emptyState.style.display = 'none';
                if (tableWrapper) tableWrapper.style.display = '';
            }
        }

        // Info text
        if (infoDisplay) {
            if (totalFiltered === 0) {
                infoDisplay.innerHTML = 'Không tìm thấy ' + itemLabel + ' nào phù hợp';
            } else {
                infoDisplay.innerHTML = 'Hiển thị <strong>' + (startIndex + 1) + ' - ' + endIndex + '</strong> trong tổng số <strong>' + totalFiltered + '</strong> ' + itemLabel +
                    (totalFiltered !== totalItemsCount ? ' (lọc từ tổng số ' + totalItemsCount + ' ' + itemLabel + ')' : '');
            }
        }

        renderPaginationControls(totalPages);

        if (typeof options.onPageChange === 'function') {
            options.onPageChange({ currentPage, totalPages, totalFiltered, startIndex, endIndex });
        }
    }

    // Bind search input
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentSearch = e.target.value;
            currentPage = 1;
            if (searchClearBtn) {
                searchClearBtn.style.display = currentSearch ? 'grid' : 'none';
            }
            render();
        });
    }

    if (searchClearBtn) {
        searchClearBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            currentSearch = '';
            searchClearBtn.style.display = 'none';
            currentPage = 1;
            render();
            if (searchInput) searchInput.focus();
        });
        if (searchInput && searchInput.value) {
            searchClearBtn.style.display = 'grid';
        }
    }

    // Bind status pills
    if (statusPills.length > 0) {
        statusPills.forEach(pill => {
            pill.addEventListener('click', (e) => {
                if (pill.tagName === 'A') e.preventDefault();
                statusPills.forEach(p => p.classList.remove('active'));
                pill.classList.add('active');
                currentStatus = pill.dataset.status || pill.dataset.tab || 'ALL';
                if (statusSelect) {
                    statusSelect.value = currentStatus;
                }
                currentPage = 1;
                render();
            });
        });
    }

    // Bind status select
    if (statusSelect) {
        statusSelect.addEventListener('change', () => {
            currentStatus = statusSelect.value || 'ALL';
            if (statusPills.length > 0) {
                statusPills.forEach(p => {
                    const st = (p.dataset.status || p.dataset.tab || 'ALL').toUpperCase();
                    if (st === currentStatus.toUpperCase()) {
                        p.classList.add('active');
                    } else {
                        p.classList.remove('active');
                    }
                });
            }
            currentPage = 1;
            render();
        });
    }

    // Bind page size select
    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', () => {
            const val = pageSizeSelect.value;
            pageSize = val === 'ALL' ? 0 : parseInt(val, 10);
            currentPage = 1;
            render();
        });
    }

    // Set initial status in select if present
    if (statusSelect && currentStatus) {
        statusSelect.value = currentStatus;
    }

    // Initial render
    render();

    return {
        render,
        refreshRows,
        setPage: (p) => { currentPage = p; render(); },
        setStatus: (st) => { currentStatus = st; currentPage = 1; render(); },
        setSearch: (q) => {
            currentSearch = q;
            if (searchInput) searchInput.value = q;
            currentPage = 1;
            render();
        }
    };
}
