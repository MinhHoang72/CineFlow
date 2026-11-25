/**
 * Điều khiển giao diện màn hình Phân phối phim (movieDistribution.jsp).
 * Chức năng chính:
 * 1. Tự động ẩn thông báo lưu thành công sau 1.5 giây.
 * 2. Đếm số lượng cụm rạp đang tích chọn và cập nhật nút "Phân phối tất cả".
 * 3. Lọc danh sách cụm rạp theo tên, thành phố và trạng thái hoạt động.
 * 4. Khóa nút "Lưu phân phối" khi submit để tránh bấm gửi trùng lặp.
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

    // 2. Checkbox chọn rạp & cập nhật bộ đếm "Đã chọn X/Y cụm rạp" (Bước 2)
    const tableBody = document.getElementById('centreTableBody');
    if (!tableBody) return;

    const rows = Array.from(tableBody.querySelectorAll('.cf-centre-row'));
    const searchInput = document.getElementById('centreSearchInput');
    const cityFilter = document.getElementById('cityFilterSelect');
    const statusFilter = document.getElementById('statusFilterSelect');
    const selectedCountNum = document.getElementById('selectedCountNum');
    const distributionForm = document.getElementById('distributionForm');
    const btnToggleAll = document.getElementById('btnToggleAllCentres');
    const btnToggleAllText = document.getElementById('btnToggleAllText');

    /** Đếm lại số checkbox rạp đang được tích chọn và cập nhật nút "Phân phối tất cả". */
    function updateCounter() {
        let count = 0;
        rows.forEach(row => {
            const cb = row.querySelector('.cf-centre-checkbox');
            const checked = !!(cb && cb.checked);
            if (checked) count++;
        });
        if (selectedCountNum) selectedCountNum.textContent = count;

        // Cập nhật trạng thái và màu sắc của nút "Phân phối tất cả"
        if (btnToggleAll && btnToggleAllText) {
            const isAllSelected = (rows.length > 0 && count === rows.length);
            if (isAllSelected) {
                btnToggleAll.classList.add('is-deselect');
                btnToggleAllText.textContent = 'Bỏ chọn tất cả';
                btnToggleAll.title = 'Bỏ chọn toàn bộ rạp';
            } else {
                btnToggleAll.classList.remove('is-deselect');
                btnToggleAllText.textContent = 'Phân phối tất cả';
                btnToggleAll.title = 'Bấm để phân phối tới toàn bộ cụm rạp';
            }
        }
    }

    // Sự kiện khi bấm trực tiếp vào nút "Phân phối tất cả"
    if (btnToggleAll) {
        btnToggleAll.addEventListener('click', function () {
            const checkboxes = rows.map(r => r.querySelector('.cf-centre-checkbox')).filter(Boolean);
            const isAllChecked = checkboxes.length > 0 && checkboxes.every(cb => cb.checked);
            const targetState = !isAllChecked;

            checkboxes.forEach(cb => {
                cb.checked = targetState;
            });

            updateCounter();
        });
    }

    rows.forEach(row => {
        const cb = row.querySelector('.cf-centre-checkbox');
        if (cb) cb.addEventListener('change', updateCounter);
    });
    updateCounter();

    // 3. Lọc bảng cụm rạp theo từ khóa tìm kiếm, thành phố và trạng thái rạp
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

    // 4. Khóa nút Lưu khi gửi form POST (bỏ tích rạp được phép lưu trực tiếp, không cảnh báo suất chiếu)
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
