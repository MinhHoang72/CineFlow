/**
 * Script giao diện Quản lý Tài khoản Quản lý chi nhánh (managerList.jsp).
 * Chức năng chính:
 * 1. Xác nhận trước khi đổi trạng thái tài khoản Manager (ACTIVE / INACTIVE / LOCKED).
 * 2. Đóng/mở Modal Tạo mới và Modal Chỉnh sửa thông tin Manager.
 * 3. Khởi tạo bộ tìm kiếm, lọc trạng thái và phân trang bảng Manager.
 */

/**
 * Tự động gửi biểu mẫu cập nhật trạng thái khi Admin chọn trạng thái mới trên dropdown.
 *
 * @param {HTMLSelectElement} selectEl phần tử dropdown trạng thái vừa thay đổi
 */
function handleManagerStatusChange(selectEl) {
    if (selectEl && selectEl.form) {
        selectEl.form.submit();
    }
}

// Khởi tạo bộ lọc, tìm kiếm và phân trang cho bảng Manager khi DOM tải xong
document.addEventListener('DOMContentLoaded', function () {
    var countLabel = document.getElementById('managerCountLabel');
    if (typeof initTablePagination === 'function') {
        initTablePagination({
            tableBodyId: 'managerTableBody',           // Phần <tbody> chứa các dòng Manager
            rowSelector: 'tr.manager-row',             // Chỉ lấy dòng dữ liệu Manager
            emptyRowId: 'managerTableEmptyRow',        // Dòng hiển thị khi không tìm thấy kết quả
            paginationId: 'managerPaginationControls', // Vùng vẽ nút chuyển trang
            infoDisplayId: 'managerPaginationInfo',    // Vùng hiện thống kê "Hiển thị x - y..."
            pageSizeSelectId: 'managerPageSizeSelect', // Dropdown chọn số dòng mỗi trang
            searchInputId: 'managerFilterInput',       // Ô tìm kiếm tên/email/tài khoản/rạp
            searchClearBtnId: 'managerSearchClearBtn', // Nút xóa nhanh ô tìm kiếm
            statusFilterSelectId: 'statusFilterSelect',// Dropdown lọc trạng thái (ALL/ACTIVE/INACTIVE/LOCKED)
            itemLabel: 'quản lý',
            defaultPageSize: 10,
            // Cập nhật số lượng tài khoản đang hiển thị sau mỗi lần lọc
            onPageChange: function (state) {
                if (countLabel) {
                    countLabel.textContent = state.totalFiltered + ' tài khoản đang hiển thị.';
                }
            }
        });
    }
});
