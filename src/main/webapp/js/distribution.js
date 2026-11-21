/**
 * Điều khiển giao diện màn hình Phân phối phim (movieDistribution.jsp).
 * Chức năng chính:
 * 1. Tự động ẩn thông báo lưu thành công sau 1.5 giây.
 * 2. Chọn / tìm kiếm phim qua dropdown và tải lại trang theo movieId được chọn.
 * 3. Đếm số lượng cụm rạp đang tích chọn và làm nổi bật dòng đã chọn.
 * 4. Lọc danh sách cụm rạp theo tên, thành phố và trạng thái hoạt động.
 * 5. Khóa nút "Lưu phân phối" khi submit để tránh bấm gửi trùng lặp.
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Tự ẩn thông báo thành công sau 1.5 giây
    const successAlert = document.getElementById('saveSuccessAlert');
    if (successAlert) {
        setTimeout(() => {
            successAlert.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
            successAlert.style.opacity = '0';
            successAlert.style.transform = 'translateY(-6px)';
            setTimeout(() => successAlert.remove(), 400);
        }, 1500);
    }

    // 2. Dropdown chọn & tìm kiếm phim (Bước 1)
    const picker = document.getElementById('moviePickerContainer');
    const inputBox = document.getElementById('movieInputBox');
    const movieSearch = document.getElementById('movieSearchInput');
    const btnClearMovie = document.getElementById('btnClearMovieSearch');
    const btnToggleMovie = document.getElementById('toggleMovieDropdown');
    const movieOptions = Array.from(document.querySelectorAll('.cf-movie-option'));
    const movieEmpty = document.getElementById('movieEmptyOption');

    /**
     * Đóng hoặc mở danh sách phim thả xuống.
     *
     * @param {boolean} open    true để mở dropdown, false để đóng
     * @param {boolean} restore nếu đóng mà chưa chọn phim mới thì khôi phục lại tên phim đang chọn
     */
    function setDropdownOpen(open, restore) {
        if (!picker) return;
        picker.classList.toggle('is-open', open);
        if (inputBox) inputBox.classList.toggle('is-focused', open);
        if (!open && restore && movieSearch) {
            movieSearch.value = movieSearch.dataset.selectedTitle || '';
            filterMovies('');
        }
    }

    /** Lọc các phim hiển thị trong dropdown theo tên hoặc mã phim. */
    function filterMovies(keyword) {
        const q = (keyword || '').trim().toLowerCase();
        let count = 0;
        movieOptions.forEach(opt => {
            const match = !q || (opt.dataset.title || '').includes(q) || (opt.dataset.code || '').includes(q);
            opt.style.display = match ? 'flex' : 'none';
            if (match) count++;
        });
        if (movieEmpty) movieEmpty.style.display = count === 0 ? 'block' : 'none';
        if (btnClearMovie) btnClearMovie.style.display = q ? 'flex' : 'none';
    }

    /**
     * Hàm toàn cục được gọi từ onclick trên từng dòng phim trong dropdown:
     * điều hướng trình duyệt tới /admin/distribution?movieId=... để nạp danh sách rạp của phim đó.
     *
     * @param {number} movieId mã phim Admin vừa chọn
     */
    window.selectMovie = function (movieId) {
        setDropdownOpen(false, false);
        const ctx = picker ? (picker.dataset.contextPath || '') : '';
        window.location.href = ctx + '/admin/distribution?movieId=' + movieId;
    };

    if (movieSearch) {
        movieSearch.addEventListener('focus', () => { setDropdownOpen(true); movieSearch.select(); });
        movieSearch.addEventListener('click', () => setDropdownOpen(true));
        movieSearch.addEventListener('input', () => { setDropdownOpen(true); filterMovies(movieSearch.value); });
        movieSearch.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') { setDropdownOpen(false, true); movieSearch.blur(); }
        });
    }

    if (btnToggleMovie) {
        btnToggleMovie.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = picker && picker.classList.contains('is-open');
            setDropdownOpen(!isOpen, isOpen);
            if (!isOpen && movieSearch) movieSearch.focus();
        });
    }

    if (btnClearMovie) {
        btnClearMovie.addEventListener('click', (e) => {
            e.stopPropagation();
            if (movieSearch) { movieSearch.value = ''; filterMovies(''); movieSearch.focus(); }
        });
    }

    // Bấm ra ngoài khung dropdown sẽ tự động đóng lại
    document.addEventListener('click', (e) => {
        if (picker && !picker.contains(e.target)) setDropdownOpen(false, true);
    });

    // 3. Checkbox chọn rạp & cập nhật bộ đếm "Đã chọn X/Y cụm rạp" (Bước 2)
    const tableBody = document.getElementById('centreTableBody');
    if (!tableBody) return;

    const rows = Array.from(tableBody.querySelectorAll('.cf-centre-row'));
    const searchInput = document.getElementById('centreSearchInput');
    const cityFilter = document.getElementById('cityFilterSelect');
    const statusFilter = document.getElementById('statusFilterSelect');
    const selectedCountNum = document.getElementById('selectedCountNum');
    const distributionForm = document.getElementById('distributionForm');

    /** Đếm lại số checkbox rạp đang được tích chọn và gắn class is-selected cho dòng tương ứng. */
    function updateCounter() {
        let count = 0;
        rows.forEach(row => {
            const cb = row.querySelector('.cf-centre-checkbox');
            const checked = !!(cb && cb.checked);
            row.classList.toggle('is-selected', checked);
            if (checked) count++;
        });
        if (selectedCountNum) selectedCountNum.textContent = count;
    }

    rows.forEach(row => {
        const cb = row.querySelector('.cf-centre-checkbox');
        if (cb) cb.addEventListener('change', updateCounter);
    });
    updateCounter();

    // 4. Lọc bảng cụm rạp theo từ khóa tìm kiếm, thành phố và trạng thái rạp
    function applyFilters() {
        const query = (searchInput ? searchInput.value : '').trim().toLowerCase();
        const city = cityFilter ? cityFilter.value : 'ALL';
        const status = statusFilter ? statusFilter.value : 'ALL';

        rows.forEach(row => {
            const matchText = !query || (row.dataset.name || '').includes(query) || (row.dataset.city || '').includes(query);
            const matchCity = city === 'ALL' || (row.dataset.cityRaw || '').toLowerCase() === city.toLowerCase();
            const matchStatus = status === 'ALL' || (row.dataset.status || '').toUpperCase() === status;
            row.style.display = (matchText && matchCity && matchStatus) ? '' : 'none';
        });
    }

    if (searchInput) searchInput.addEventListener('input', applyFilters);
    if (cityFilter) cityFilter.addEventListener('change', applyFilters);
    if (statusFilter) statusFilter.addEventListener('change', applyFilters);

    // 5. Khóa nút Lưu khi gửi form POST (bỏ tích rạp được phép lưu trực tiếp, không cảnh báo suất chiếu)
    if (distributionForm) {
        distributionForm.addEventListener('submit', function () {
            const btnSave = distributionForm.querySelector('.cf-btn-save');
            if (btnSave) {
                btnSave.disabled = true;
                btnSave.textContent = 'Đang lưu phân phối...';
            }
        });
    }
});
