/**
 * Script quản lý Nhân viên (Staff) - CineFlow
 */
var staffPagination = null;

document.addEventListener("DOMContentLoaded", function() {
    var countLabel = document.getElementById("staffCountLabel");
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
        onPageChange: function(state) {
            if (countLabel) {
                countLabel.textContent = state.totalFiltered + " tài khoản đang hiển thị.";
            }
        }
    });

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
    if (selectEl && selectEl.form) {
        selectEl.form.submit();
    }
}
