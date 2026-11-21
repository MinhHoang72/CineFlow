/**
 * Script giao diện Quản lý Tài khoản Quản lý chi nhánh (managerList.jsp).
 * Chức năng chính:
 * 1. Xác nhận trước khi đổi trạng thái tài khoản Manager (ACTIVE / INACTIVE / LOCKED).
 * 2. Đóng/mở Modal Tạo mới và Modal Chỉnh sửa thông tin Manager.
 * 3. Khởi tạo bộ tìm kiếm, lọc trạng thái và phân trang bảng Manager.
 */

/**
 * Hỏi xác nhận khi Admin thay đổi trạng thái của một tài khoản Manager trong dropdown.
 * - Nếu Admin chọn OK -> gửi form POST về ManagerManagementServlet (action="toggle_status").
 * - Nếu Admin chọn Hủy -> khôi phục lại giá trị trạng thái cũ trên dropdown.
 *
 * @param {HTMLSelectElement} selectEl phần tử dropdown trạng thái vừa thay đổi
 */
function handleManagerStatusChange(selectEl) {
    var newStatus = selectEl.value;
    var currentStatus = selectEl.getAttribute('data-current-status') || 'ACTIVE';
    var managerName = selectEl.getAttribute('data-name') || 'Quản lý';

    // Không xử lý nếu người dùng chọn lại đúng trạng thái hiện tại
    if (newStatus === currentStatus) {
        return;
    }

    var message = '';
    if (newStatus === 'ACTIVE') {
        message = 'Kích hoạt lại tài khoản cho Quản lý "' + managerName + '" sang HOẠT ĐỘNG?';
    } else if (newStatus === 'INACTIVE') {
        message = 'Chuyển tài khoản Quản lý "' + managerName + '" sang TẠM NGƯNG?\n\n(Dành cho trường hợp nghỉ phép dài hạn. Hệ thống sẽ tạm giải phóng rạp đang phụ trách).';
    } else if (newStatus === 'LOCKED') {
        message = 'CẢNH BÁO: Bạn có chắc chắn muốn KHÓA tài khoản Quản lý "' + managerName + '"?\n\n(Dành cho trường hợp thôi việc hoặc vi phạm kỷ luật. Tài khoản sẽ bị khóa hoàn toàn).';
    }

    if (confirm(message)) {
        selectEl.form.submit();
    } else {
        selectEl.value = currentStatus;
    }
}

/** Mở modal Tạo mới tài khoản Manager. */
function openCreateManagerModal() {
    var modal = document.getElementById('createManagerModal');
    if (modal) modal.classList.add('active');
}

/** Đóng modal Tạo mới tài khoản Manager. */
function closeCreateManagerModal() {
    var modal = document.getElementById('createManagerModal');
    if (modal) modal.classList.remove('active');
}

/**
 * Mở modal Chỉnh sửa và điền sẵn thông tin hiện tại của Manager vào các ô nhập liệu.
 *
 * @param {number} id       mã UserID của Manager
 * @param {string} code     mã nhân viên hiển thị (ví dụ: QL00005)
 * @param {string} fullName họ và tên hiện tại
 * @param {string} phone    số điện thoại hiện tại
 * @param {string} email    email liên hệ hiện tại
 */
function openEditManagerModal(id, code, fullName, phone, email) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editManagerCode').textContent = code;
    document.getElementById('editFullName').value = fullName;
    document.getElementById('editPhone').value = phone;
    document.getElementById('editEmail').value = email;
    var modal = document.getElementById('editManagerModal');
    if (modal) modal.classList.add('active');
}

/** Đóng modal Chỉnh sửa thông tin Manager. */
function closeEditManagerModal() {
    var modal = document.getElementById('editManagerModal');
    if (modal) modal.classList.remove('active');
}

// Đóng modal khi người dùng bấm ra vùng nền tối bên ngoài hộp thoại
document.querySelectorAll('.cf-modal-backdrop').forEach(function (modal) {
    modal.addEventListener('click', function (e) {
        if (e.target === this) {
            this.classList.remove('active');
        }
    });
});

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
