/**
 * Script giao diện Quản lý Tài khoản Khách hàng (customerList.jsp).
 * Chức năng chính:
 * 1. Khởi tạo bộ tìm kiếm không dấu, lọc trạng thái và phân trang trên bảng khách hàng.
 * 2. Tự động ẩn các hộp thông báo thành công / lỗi sau 3 giây.
 */
document.addEventListener('DOMContentLoaded', function () {
    // 1. Kết nối bộ phân trang dùng chung (admin-pagination.js) cho bảng khách hàng
    var countLabel = document.getElementById('customerCountLabel');
    if (typeof initTablePagination === 'function') {
        initTablePagination({
            tableBodyId: 'customerTableBody',           // Phần <tbody> chứa danh sách dòng khách hàng
            rowSelector: 'tr.customer-row',             // Chỉ lấy các dòng dữ liệu khách hàng (bỏ qua dòng trống)
            emptyRowId: 'customerTableEmptyRow',        // Dòng hiển thị khi lọc không có kết quả
            paginationId: 'customerPaginationControls', // Vùng vẽ các nút chuyển trang (‹ 1 2 3 ›)
            infoDisplayId: 'customerPaginationInfo',    // Vùng hiện chữ "Hiển thị 1 - 10 trong tổng số ..."
            pageSizeSelectId: 'customerPageSizeSelect', // Dropdown chọn số khách hàng mỗi trang
            searchInputId: 'customerSearchInput',       // Ô nhập từ khóa tìm kiếm
            searchClearBtnId: 'customerSearchClearBtn', // Nút ✕ xóa nhanh từ khóa
            statusFilterSelectId: 'statusFilterSelect', // Dropdown lọc theo trạng thái (ALL / ACTIVE / LOCKED)
            statusPillSelector: '.filter-tabs .filter-tab[data-status]',
            defaultStatus: 'ALL',
            itemLabel: 'khách hàng',
            defaultPageSize: 10,
            // Cập nhật lại dòng đếm tổng số tài khoản mỗi khi tìm kiếm hoặc đổi trang
            onPageChange: function (state) {
                if (countLabel) {
                    countLabel.textContent = state.totalFiltered + ' tài khoản đang hiển thị.';
                }
            }
        });
    }

    // 2. Tự động làm mờ và gỡ bỏ thông báo thành công / lỗi sau 3 giây
    document.querySelectorAll('.cf-alert-success, .cf-alert-danger').forEach(function (alert) {
        setTimeout(function () {
            alert.style.transition = 'opacity .4s ease, transform .4s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-8px)';
            setTimeout(function () {
                if (alert.parentNode) {
                    alert.parentNode.removeChild(alert);
                }
            }, 400);
        }, 3000);
    });
});
