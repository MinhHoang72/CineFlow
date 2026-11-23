/**
 * Script quản lý Nhân viên (Staff) - CineFlow
 */
var staffPagination = null;

document.addEventListener("DOMContentLoaded", function() {
    var countLabel = document.getElementById("staffCountLabel");
    var roleFilter = document.getElementById("staffRoleFilter");
    var searchInput = document.getElementById("staffFilterInput");
    var searchForm = document.getElementById("staffSearchForm");

    if (!document.getElementById("staffTableBody") || typeof initTablePagination !== "function") {
        return;
    }

    staffPagination = initTablePagination({
        tableBodyId: "staffTableBody",
        rowSelector: "tr.staff-row",
        emptyRowId: "staffTableEmptyRow",
        paginationId: "staffPaginationControls",
        infoDisplayId: "staffPaginationInfo",
        pageSizeSelectId: "staffPageSizeSelect",
        searchInputId: "staffFilterInput",
        searchClearBtnId: "staffSearchClearBtn",
        statusFilterSelectId: "staffStatusFilter",
        itemLabel: "nhân viên",
        defaultPageSize: 10,
        customFilter: function(row) {
            var roleValue = roleFilter ? roleFilter.value : "";
            if (roleValue && !row.classList.contains(roleValue)) {
                return false;
            }
            return true;
        },
        onPageChange: function(state) {
            if (countLabel) {
                countLabel.textContent = state.totalFiltered + " tài khoản đang hiển thị.";
            }
        }
    });

    if (roleFilter) {
        roleFilter.addEventListener("change", function() {
            staffPagination.setPage(1);
        });
    }

    if (searchForm) {
        searchForm.addEventListener("submit", function(event) {
            event.preventDefault();
            if (searchInput) {
                staffPagination.setSearch(searchInput.value);
            }
        });
    }
});

function handleStaffStatusChange(selectEl) {
    var newStatus = selectEl.value;
    var currentStatus = selectEl.getAttribute("data-current-status") || "ACTIVE";
    var staffName = selectEl.getAttribute("data-name") || "nhân viên";

    if (newStatus === currentStatus) {
        return;
    }

    var message = "";
    if (newStatus === "ACTIVE") {
        message = 'Kích hoạt lại tài khoản cho nhân viên "' + staffName + '" sang HOẠT ĐỘNG?';
    } else if (newStatus === "INACTIVE") {
        message = 'Chuyển tài khoản nhân viên "' + staffName + '" sang TẠM NGƯNG?';
    } else if (newStatus === "LOCKED") {
        message = 'CẢNH BÁO: Bạn có chắc chắn muốn KHÓA tài khoản nhân viên "' + staffName + '"?';
    }

    if (confirm(message)) {
        if (selectEl.form) {
            selectEl.form.submit();
        }
    } else {
        selectEl.value = currentStatus;
    }
}
