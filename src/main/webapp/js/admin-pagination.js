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
    const searchBoxWrap = options.searchBoxWrapId ? document.getElementById(options.searchBoxWrapId) : null;
    const suggestionsDropdown = options.suggestionsDropdownId ? document.getElementById(options.suggestionsDropdownId) : null;
    const suggestionsList = options.suggestionsListId ? document.getElementById(options.suggestionsListId) : null;
    const suggestionsCount = options.suggestionsCountId ? document.getElementById(options.suggestionsCountId) : null;
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

    // Cache movie metadata for fast fuzzy lookup
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
        const thumbImg = row.querySelector('.movie-thumb img');
        const posterUrl = thumbImg ? thumbImg.src : '';
        const editLink = row.querySelector('.cf-btn-edit');
        const editUrl = editLink ? editLink.href : '';

        return {
            row,
            movieId: id,
            code,
            title,
            genre,
            director,
            status,
            ageRating,
            releaseDate,
            duration,
            posterUrl,
            editUrl
        };
    });

    let currentPage = 1;
    let pageSize = pageSizeSelect ? (parseInt(pageSizeSelect.value, 10) || 12) : 12;
    let currentStatus = 'ALL';
    let currentSearch = '';
    let focusedSuggestionIndex = -1;

    function getStatusText(st) {
        switch (st) {
            case 'NOW_SHOWING': return 'Đang chiếu';
            case 'COMING_SOON': return 'Sắp khởi chiếu';
            case 'ENDED': return 'Đã kết thúc';
            case 'ARCHIVED': return 'Lưu trữ';
            default: return st;
        }
    }

    function getBadgeClass(st) {
        switch (st) {
            case 'NOW_SHOWING': return 'cf-badge-success';
            case 'COMING_SOON': return 'cf-badge-info';
            case 'ENDED': return 'cf-badge-warning';
            default: return 'cf-badge-muted';
        }
    }

    function render() {
        const matchedMovies = [];

        movieDataList.forEach(item => {
            // Lọc theo Status Pill
            if (currentStatus !== 'ALL' && item.status !== currentStatus) {
                item.row.style.display = 'none';
                return;
            }

            // Lọc theo Fuzzy Search
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

        // Sắp xếp các dòng hiển thị theo điểm số tương đồng
        if (currentSearch.trim()) {
            matchedMovies.sort((a, b) => b.score - a.score);
        }

        const visibleRows = matchedMovies.map(m => m.item.row);
        const totalFiltered = visibleRows.length;
        const actualPageSize = pageSize === 0 ? totalFiltered : pageSize;
        const totalPages = Math.ceil(totalFiltered / actualPageSize) || 1;

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        const startIndex = (currentPage - 1) * actualPageSize;
        const endIndex = pageSize === 0 ? totalFiltered : Math.min(startIndex + actualPageSize, totalFiltered);

        // Ẩn tất cả dòng và hiển thị các dòng thuộc trang hiện tại
        tableRows.forEach(r => { r.style.display = 'none'; });
        for (let i = startIndex; i < endIndex; i++) {
            if (visibleRows[i]) {
                visibleRows[i].style.display = '';
            }
        }

        // Xử lý empty state
        if (totalFiltered === 0) {
            if (emptyState) emptyState.style.display = 'block';
            if (tableWrapper) tableWrapper.style.display = 'none';
        } else {
            if (emptyState) emptyState.style.display = 'none';
            if (tableWrapper) tableWrapper.style.display = '';
        }

        // Hiển thị thông tin phân trang
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

    // --- AUTOCOMPLETE SUGGESTIONS POPUP (ĐỀ XUẤT TỨC THÌ) ---
    function updateSuggestions(query) {
        if (!suggestionsDropdown || !suggestionsList) return;

        const q = (query || '').trim();
        if (!q) {
            closeSuggestions();
            return;
        }

        const scored = [];
        movieDataList.forEach(item => {
            const res = computeMovieMatch(q, item);
            if (res.isMatch) {
                scored.push({ item, score: res.score });
            }
        });

        scored.sort((a, b) => b.score - a.score);
        focusedSuggestionIndex = -1;

        if (scored.length === 0) {
            suggestionsList.innerHTML = `
                <div class="cf-suggestions-empty">
                    🔍 Không tìm thấy phim phù hợp với từ khóa "<strong>${escapeHtml(q)}</strong>"<br>
                    <small style="color: var(--cf-muted); margin-top: 4px; display: inline-block;">Thử gõ không dấu hoặc kiểm tra lại tên phim.</small>
                </div>
            `;
            if (suggestionsCount) suggestionsCount.textContent = '0 gợi ý';
            suggestionsDropdown.style.display = 'block';
            return;
        }

        const topSuggestions = scored.slice(0, 8);
        if (suggestionsCount) {
            suggestionsCount.textContent = `${scored.length} phim phù hợp`;
        }

        suggestionsList.innerHTML = topSuggestions.map((s, idx) => {
            const m = s.item;
            const highlightedTitle = highlightMatch(m.title, q);
            const thumbHtml = m.posterUrl
                ? `<img src="${escapeHtml(m.posterUrl)}" alt="" onerror="this.parentElement.innerHTML='🎬';">`
                : '🎬';

            return `
                <div class="cf-suggestion-item" data-index="${idx}" data-movie-id="${escapeHtml(m.movieId)}" data-title="${escapeHtml(m.title)}">
                    <div class="cf-sugg-thumb">${thumbHtml}</div>
                    <div class="cf-sugg-info">
                        <div class="cf-sugg-title">${highlightedTitle}</div>
                        <div class="cf-sugg-meta">
                            <span class="cf-sugg-code">${escapeHtml(m.code)}</span>
                            <span>${escapeHtml(m.genre || 'Chưa cập nhật')}</span>
                            ${m.duration ? `<span>${escapeHtml(m.duration)} phút</span>` : ''}
                        </div>
                    </div>
                    <div class="cf-sugg-badge">
                        <span class="cf-badge ${getBadgeClass(m.status)}">${getStatusText(m.status)}</span>
                    </div>
                </div>
            `;
        }).join('');

        // Gắn sự kiện click cho từng gợi ý
        suggestionsList.querySelectorAll('.cf-suggestion-item').forEach(el => {
            el.addEventListener('click', () => {
                const chosenTitle = el.dataset.title;
                if (searchInput) {
                    searchInput.value = chosenTitle;
                    currentSearch = chosenTitle;
                    if (clearButton) clearButton.style.display = 'inline-flex';
                    if (resetFiltersBtn) resetFiltersBtn.style.display = 'grid';
                    currentPage = 1;
                    render();
                }
                closeSuggestions();
            });
        });

        suggestionsDropdown.style.display = 'block';
    }

    function closeSuggestions() {
        if (suggestionsDropdown) {
            suggestionsDropdown.style.display = 'none';
        }
        focusedSuggestionIndex = -1;
    }

    function highlightFocusedSuggestion() {
        if (!suggestionsList) return;
        const items = suggestionsList.querySelectorAll('.cf-suggestion-item');
        items.forEach((item, idx) => {
            if (idx === focusedSuggestionIndex) {
                item.classList.add('is-focused');
                item.scrollIntoView({ block: 'nearest' });
            } else {
                item.classList.remove('is-focused');
            }
        });
    }

    // --- SỰ KIỆN TÌM KIẾM TỨC THÌ (SEARCH-AS-YOU-TYPE) ---
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const val = e.target.value;
            currentSearch = val;
            currentPage = 1;

            if (clearButton) {
                clearButton.style.display = val.trim() ? 'inline-flex' : 'none';
            }
            if (resetFiltersBtn) {
                resetFiltersBtn.style.display = val.trim() ? 'grid' : 'none';
            }

            // 1. Lọc bảng tức thì trên từng ký tự
            render();

            // 2. Mở dropdown đề xuất tự động
            updateSuggestions(val);
        });

        searchInput.addEventListener('focus', () => {
            if (searchInput.value.trim()) {
                updateSuggestions(searchInput.value);
            }
        });

        // Điều khiển phím mũi tên và Enter
        searchInput.addEventListener('keydown', (e) => {
            if (!suggestionsDropdown || suggestionsDropdown.style.display === 'none') {
                return;
            }
            const items = suggestionsList.querySelectorAll('.cf-suggestion-item');
            if (items.length === 0) return;

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                focusedSuggestionIndex = (focusedSuggestionIndex + 1) % items.length;
                highlightFocusedSuggestion();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                focusedSuggestionIndex = (focusedSuggestionIndex - 1 + items.length) % items.length;
                highlightFocusedSuggestion();
            } else if (e.key === 'Enter') {
                if (focusedSuggestionIndex >= 0 && focusedSuggestionIndex < items.length) {
                    e.preventDefault();
                    items[focusedSuggestionIndex].click();
                } else {
                    closeSuggestions();
                }
            } else if (e.key === 'Escape') {
                closeSuggestions();
            }
        });
    }

    if (clearButton) {
        clearButton.addEventListener('click', () => {
            if (searchInput) {
                searchInput.value = '';
                searchInput.focus();
            }
            currentSearch = '';
            clearButton.style.display = 'none';
            if (resetFiltersBtn) resetFiltersBtn.style.display = 'none';
            closeSuggestions();
            currentPage = 1;
            render();
        });
    }

    // Đóng dropdown khi click ra ngoài
    document.addEventListener('click', (e) => {
        if (searchBoxWrap && !searchBoxWrap.contains(e.target)) {
            closeSuggestions();
        }
    });

    // Lọc theo Status Pills
    statusPills.forEach(pill => {
        pill.addEventListener('click', () => {
            statusPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            currentStatus = pill.dataset.status || 'ALL';
            currentPage = 1;
            render();
            if (searchInput && searchInput.value.trim()) {
                updateSuggestions(searchInput.value);
            }
        });
    });

    // Nút đặt lại bộ lọc
    if (resetFiltersBtn) {
        resetFiltersBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            currentSearch = '';
            if (clearButton) clearButton.style.display = 'none';
            resetFiltersBtn.style.display = 'none';
            closeSuggestions();
            currentPage = 1;
            render();
        });
    }

    if (emptyResetBtn) {
        emptyResetBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            currentSearch = '';
            if (clearButton) clearButton.style.display = 'none';
            if (resetFiltersBtn) resetFiltersBtn.style.display = 'none';
            closeSuggestions();
            currentPage = 1;
            render();
        });
    }

    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', () => {
            const val = pageSizeSelect.value;
            pageSize = val === 'ALL' ? 0 : parseInt(val, 10);
            currentPage = 1;
            render();
        });
    }

    // Khởi tạo ban đầu
    render();
}
