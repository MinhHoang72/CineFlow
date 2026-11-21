/**
 * Script quản lý Nhân viên (Staff) - CineFlow
 */
function openCreateStaffModal() {
    var modal = document.getElementById("createStaffModal");
    if (modal) modal.classList.add("active");
}

function closeCreateStaffModal() {
    var modal = document.getElementById("createStaffModal");
    if (modal) modal.classList.remove("active");
}

function openEditStaffModal(id, code, fullName, phone, email, role) {
    document.getElementById("editStaffUserId").value = id;
    document.getElementById("editStaffCode").textContent = code;
    document.getElementById("editStaffFullName").value = fullName;
    document.getElementById("editStaffPhone").value = phone;
    document.getElementById("editStaffEmail").value = email;
    document.getElementById("editStaffRole").value = role;

    var modal = document.getElementById("editStaffModal");
    if (modal) modal.classList.add("active");
}

function closeEditStaffModal() {
    var modal = document.getElementById("editStaffModal");
    if (modal) modal.classList.remove("active");
}

document.querySelectorAll(".cf-modal-backdrop").forEach(function(modal) {
    modal.addEventListener("click", function(event) {
        if (event.target === modal) {
            modal.classList.remove("active");
        }
    });
});

document.addEventListener("keydown", function(event) {
    if (event.key === "Escape") {
        closeCreateStaffModal();
        closeEditStaffModal();
    }
});

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
    var userId = selectEl.getAttribute("data-userid");
    var staffName = selectEl.getAttribute("data-name") || "nhân viên";
    var newStatus = selectEl.value;
    var previousStatus = selectEl.getAttribute("data-previous-status");

    var badge = document.getElementById("badge-status-" + userId);
    var row = selectEl.closest(".staff-row");

    selectEl.disabled = true;

    var formData = new URLSearchParams();
    formData.append("action", "status");
    formData.append("ajax", "1");
    formData.append("userId", userId);
    formData.append("status", newStatus);

    fetch(window.location.pathname, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With": "XMLHttpRequest"
        },
        body: formData.toString()
    })
    .then(function(response) {
        return response.json().then(function(data) {
            if (!response.ok || !data.success) {
                throw new Error(data.message || "Cập nhật thất bại.");
            }
            return data;
        });
    })
    .then(function() {
        selectEl.setAttribute("data-previous-status", newStatus);
        selectEl.className = "cf-status-select val-" + newStatus.toLowerCase();

        if (badge) {
            if (newStatus === "ACTIVE") {
                badge.className = "cf-badge cf-badge-success";
                badge.textContent = "Hoạt động";
            } else if (newStatus === "INACTIVE") {
                badge.className = "cf-badge cf-badge-warning";
                badge.textContent = "Tạm ngưng";
            } else {
                badge.className = "cf-badge cf-badge-danger";
                badge.textContent = "Đã khóa";
            }
        }

        if (row) {
            row.classList.remove("st-active", "st-inactive", "st-locked");
            row.classList.add("st-" + newStatus.toLowerCase());
            row.setAttribute("data-status", newStatus);
        }

        if (staffPagination) {
            staffPagination.refreshRows();
        }

        updateStaffKPI();
        showToast("Đã cập nhật trạng thái của " + staffName + ".", "success");
    })
    .catch(function(error) {
        selectEl.value = previousStatus;
        showToast(error.message || "Cập nhật trạng thái thất bại.", "error");
    })
    .finally(function() {
        selectEl.disabled = false;
    });
}

function updateStaffKPI() {
    var rows = document.querySelectorAll(".staff-row");
    var active = 0;
    var inactive = 0;

    rows.forEach(function(row) {
        var status = row.getAttribute("data-status");
        if (status === "ACTIVE") {
            active++;
        } else {
            inactive++;
        }
    });

    var elActive = document.getElementById("kpiActiveStaff");
    var elInactive = document.getElementById("kpiInactiveStaff");
    if (elActive) elActive.textContent = active;
    if (elInactive) elInactive.textContent = inactive;
}

function showToast(message, type) {
    var container = document.getElementById("cf-toast-container");
    if (!container) return;

    var toast = document.createElement("div");
    toast.className = "cf-toast" + (type === "error" ? " cf-toast-error" : "");
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(function() {
        toast.classList.add("cf-toast-fadeout");
        setTimeout(function() {
            toast.remove();
        }, 400);
    }, 2500);
}
