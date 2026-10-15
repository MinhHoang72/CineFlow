<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.Centre,java.util.List" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }

    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "Bạn không có quyền truy cập trang quản lý nhân sự.");
        return;
    }

    if (request.getAttribute("staff") == null) {
        response.sendRedirect(request.getContextPath() + "/staff");
        return;
    }

    List<User> staff = (List<User>) request.getAttribute("staff");
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");
    Centre assignedCentre = (Centre) request.getAttribute("assignedCentre");
    Integer selectedCentreId = (Integer) request.getAttribute("selectedCentreId");
    boolean isAdminRole = Boolean.TRUE.equals(request.getAttribute("isAdmin"));

    String currentCentreName = assignedCentre != null
        ? assignedCentre.getCentreName()
        : (selectedCentreId == null ? "Chưa bổ nhiệm trung tâm" : "Toàn hệ thống");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
    String searchParam = request.getParameter("search");
    String cFilterVal = request.getParameter("centreId");
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý nhân sự | CineFlow</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/staff.css">

    <style>
        :root {
            --staff-bg: #0c1017;
            --staff-panel: #151b25;
            --staff-panel-soft: #111720;
            --staff-border: #2a3341;
            --staff-text: #f1f5f9;
            --staff-muted: #94a3b8;
            --staff-accent: #ff2855;
            --staff-radius: 12px;
        }

        .cf-content { color: var(--staff-text); }

        .cf-page-header { margin-bottom: 26px; }

        .cf-page-header h1 {
            margin: 8px 0;
            color: #fff;
            letter-spacing: -0.6px;
        }

        .cf-page-header > div > p:last-child {
            color: #a5b4c8;
            line-height: 1.7;
        }

        .cf-card {
            overflow: hidden;
            border: 1px solid var(--staff-border);
            border-radius: 16px;
            background: var(--staff-panel);
            box-shadow: 0 12px 32px rgba(0, 0, 0, .12);
        }

        .staff-card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 20px;
            padding: 24px 28px;
            border-bottom: 1px solid var(--staff-border);
        }

        .staff-heading h2 {
            margin: 0 0 7px;
            color: #fff;
            font-size: 19px;
            font-weight: 750;
        }

        .staff-heading p {
            margin: 0;
            color: var(--staff-muted);
            font-size: 13px;
        }

        .staff-toolbar {
            display: flex;
            align-items: center;
            justify-content: flex-end;
            flex-wrap: wrap;
            gap: 10px;
        }

        .staff-search-form {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
            margin: 0;
        }

        .staff-search-box {
            position: relative;
            display: flex;
            align-items: center;
        }

        .staff-search-icon {
            position: absolute;
            left: 13px;
            color: #8492a6;
            font-size: 15px;
            pointer-events: none;
        }

        #staffFilterInput {
            box-sizing: border-box;
            width: 290px;
            height: 42px;
            padding: 0 14px 0 38px;
            border: 1px solid #303b4b;
            border-radius: 10px;
            outline: none;
            background: var(--staff-panel-soft);
            color: #f8fafc;
            font-family: inherit;
            font-size: 13px;
            transition: border-color .2s, box-shadow .2s, background .2s;
        }

        #staffFilterInput::placeholder {
            color: #8492a6;
            opacity: 1;
        }

        #staffFilterInput:hover { border-color: #465267; }

        #staffFilterInput:focus {
            border-color: var(--staff-accent);
            background: #141b26;
            box-shadow: 0 0 0 3px rgba(255, 40, 85, .12);
        }

        .staff-control-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 7px;
            height: 42px;
            padding: 0 16px;
            border: 1px solid #394456;
            border-radius: 10px;
            background: #202735;
            color: #e5edf7;
            font-family: inherit;
            font-size: 13px;
            font-weight: 650;
            text-decoration: none;
            white-space: nowrap;
            cursor: pointer;
            transition: background .2s, border-color .2s, transform .2s;
        }

        .staff-control-btn:hover {
            background: #2a3444;
            border-color: #59677d;
        }

        .staff-control-btn:active { transform: translateY(1px); }

        .staff-control-btn-primary {
            border-color: var(--staff-accent);
            background: var(--staff-accent);
            color: #fff;
            box-shadow: 0 5px 15px rgba(255, 40, 85, .16);
        }

        .staff-control-btn-primary:hover {
            border-color: #ff496e;
            background: #ff496e;
        }

        .staff-select-wrap {
            position: relative;
        }

        .staff-select-wrap::after {
            position: absolute;
            top: 50%;
            right: 13px;
            width: 7px;
            height: 7px;
            border-right: 1.5px solid #a7b3c5;
            border-bottom: 1.5px solid #a7b3c5;
            content: "";
            pointer-events: none;
            transform: translateY(-70%) rotate(45deg);
        }

        #staffStatusFilter,
        .status-select-wrap .cf-status-select {
            appearance: none;
            -webkit-appearance: none;
            box-sizing: border-box;
            height: 42px;
            padding: 0 34px 0 13px;
            border: 1px solid #303b4b;
            border-radius: 10px;
            outline: none;
            background: var(--staff-panel-soft);
            color: #e2e8f0;
            font-family: inherit;
            font-size: 13px;
            cursor: pointer;
            transition: border-color .2s, background .2s;
        }

        #staffStatusFilter { min-width: 166px; }

        #staffStatusFilter:hover,
        .status-select-wrap .cf-status-select:hover {
            border-color: #59677d;
            background-color: #19212d;
        }

        #staffStatusFilter:focus,
        .status-select-wrap .cf-status-select:focus {
            border-color: var(--staff-accent);
            box-shadow: 0 0 0 3px rgba(255, 40, 85, .10);
        }

        #staffStatusFilter option,
        .status-select-wrap .cf-status-select option {
            background: #151b25;
            color: #f1f5f9;
        }

        .staff-table-body { padding: 22px 28px 28px; }

        .cf-table-wrap {
            overflow-x: auto;
            border: 1px solid var(--staff-border);
            border-radius: 12px;
            background: #151b25;
        }

        .cf-table {
            width: 100%;
            border-collapse: separate;
            border-spacing: 0;
        }

        .cf-table thead th {
            padding: 15px 18px;
            border-bottom: 1px solid var(--staff-border);
            background: #10151d;
            color: #9eacc0;
            font-size: 11px;
            font-weight: 750;
            letter-spacing: .65px;
            text-align: left;
            text-transform: uppercase;
            white-space: nowrap;
        }

        .cf-table tbody td {
            padding: 17px 18px;
            border-bottom: 1px solid rgba(42, 51, 65, .78);
            color: #e2e8f0;
            font-size: 13px;
            vertical-align: middle;
        }

        .cf-table tbody tr:last-child td { border-bottom: 0; }

        .cf-table tbody tr {
            transition: background .16s;
        }

        .cf-table tbody tr:hover { background: #1a222e; }

        .cf-table td strong {
            display: block;
            margin-bottom: 4px;
            color: #f8fafc;
            font-size: 13px;
            font-weight: 700;
        }

        .cf-table td small {
            color: #94a3b8;
            font-size: 12px;
        }

        .cf-table td code:not(.cf-badge) {
            color: #d6deeb;
            font-size: 12px;
        }

        .cf-table .cf-badge {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-height: 26px;
            padding: 4px 11px;
            border-radius: 999px;
            font-size: 11px;
            font-weight: 750;
            white-space: nowrap;
        }

        .cf-table .cf-badge-info {
            border: 1px solid rgba(125, 211, 252, .2);
            background: rgba(56, 189, 248, .12);
            color: #a5e4ff;
        }

        .cf-table .cf-badge-success {
            background: rgba(52, 211, 153, .13);
            color: #6ee7b7;
        }

        .cf-table .cf-badge-warning {
            background: rgba(251, 191, 36, .13);
            color: #fcd675;
        }

        .cf-table .cf-badge-danger {
            background: rgba(248, 113, 113, .13);
            color: #fca5a5;
        }

        .staff-actions {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
        }

        .staff-edit-btn {
            min-height: 35px;
            padding: 0 13px;
            border: 1px solid #3b4657;
            border-radius: 8px;
            background: #242c39;
            color: #e5edf7;
            font-family: inherit;
            font-size: 12px;
            font-weight: 650;
            cursor: pointer;
            transition: .2s;
        }

        .staff-edit-btn:hover {
            border-color: #64748b;
            background: #303a4a;
        }

        .status-select-wrap {
            position: relative;
            display: inline-flex;
            align-items: center;
            gap: 7px;
        }

        .status-select-wrap::after {
            position: absolute;
            top: 50%;
            right: 11px;
            width: 6px;
            height: 6px;
            border-right: 1.5px solid currentColor;
            border-bottom: 1.5px solid currentColor;
            content: "";
            pointer-events: none;
            transform: translateY(-70%) rotate(45deg);
        }

        .status-select-wrap .cf-status-select {
            height: 35px;
            min-width: 112px;
            padding: 0 27px 0 10px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 650;
        }

        .status-select-wrap .val-active {
            border-color: rgba(52, 211, 153, .35);
            background: rgba(52, 211, 153, .08);
            color: #6ee7b7;
        }

        .status-select-wrap .val-inactive {
            border-color: rgba(251, 191, 36, .35);
            background: rgba(251, 191, 36, .08);
            color: #fcd675;
        }

        .status-select-wrap .val-locked {
            border-color: rgba(248, 113, 113, .35);
            background: rgba(248, 113, 113, .08);
            color: #fca5a5;
        }

        .status-indicator {
            color: #6ee7b7;
            font-size: 14px;
            font-weight: 800;
        }

        .cf-empty {
            padding: 36px 20px !important;
            color: #94a3b8 !important;
            text-align: center;
        }

        .cf-alert {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 20px;
            padding: 13px 16px;
            border: 1px solid transparent;
            border-radius: 10px;
            font-size: 13px;
        }

        .cf-alert-success {
            border-color: rgba(52, 211, 153, .25);
            background: rgba(52, 211, 153, .09);
            color: #6ee7b7;
        }

        .cf-alert-danger {
            border-color: rgba(248, 113, 113, .25);
            background: rgba(248, 113, 113, .09);
            color: #fca5a5;
        }

        .cf-modal-backdrop {
            position: fixed;
            inset: 0;
            z-index: 9999;
            display: none;
            align-items: center;
            justify-content: center;
            overflow-y: auto;
            padding: 20px;
            background: rgba(2, 6, 12, .78);
            backdrop-filter: blur(7px);
        }

        .cf-modal-backdrop.active { display: flex; }

        .cf-modal {
            width: 100%;
            max-width: 520px;
            overflow: hidden;
            border: 1px solid #303b4b;
            border-radius: 16px;
            background: #151b25;
            box-shadow: 0 24px 70px rgba(0, 0, 0, .45);
            animation: staffModalIn .2s ease-out;
        }

        @keyframes staffModalIn {
            from { opacity: 0; transform: translateY(8px) scale(.99); }
            to { opacity: 1; transform: translateY(0) scale(1); }
        }

        .cf-modal-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            padding: 21px 24px;
            border-bottom: 1px solid var(--staff-border);
            background: #151b25;
        }

        .cf-modal-header h3 {
            margin: 0;
            color: #f8fafc;
            font-size: 17px;
            font-weight: 750;
        }

        .cf-modal-close {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 34px;
            height: 34px;
            border: 1px solid #303b4b;
            border-radius: 9px;
            background: #202735;
            color: #cbd5e1;
            font-size: 23px;
            cursor: pointer;
        }

        .cf-modal-close:hover {
            background: #303a4a;
            color: #fff;
        }

        .cf-modal-body {
            display: flex;
            flex-direction: column;
            gap: 16px;
            padding: 24px;
            background: #151b25;
            color: #e2e8f0;
        }

        .cf-form-group {
            display: flex;
            flex-direction: column;
            gap: 7px;
        }

        .cf-form-group label {
            color: #dce5f1;
            font-size: 12px;
            font-weight: 700;
        }

        .cf-form-group input,
        .cf-form-group select {
            box-sizing: border-box;
            width: 100%;
            min-height: 42px;
            padding: 10px 12px;
            border: 1px solid #303b4b;
            border-radius: 9px;
            outline: none;
            background: #10151d;
            color: #f8fafc;
            font-family: inherit;
            font-size: 13px;
            transition: border-color .2s, box-shadow .2s;
        }

        .cf-form-group input::placeholder {
            color: #77859a;
            opacity: 1;
        }

        .cf-form-group input:focus,
        .cf-form-group select:focus {
            border-color: var(--staff-accent);
            box-shadow: 0 0 0 3px rgba(255, 40, 85, .12);
        }

        .cf-form-group select option {
            background: #151b25;
            color: #f8fafc;
        }

        .cf-modal-footer {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
            padding: 17px 24px;
            border-top: 1px solid var(--staff-border);
            background: #111720;
        }

        .cf-modal-footer .cf-btn {
            min-height: 39px;
            border-radius: 9px;
        }

        #cf-toast-container {
            position: fixed;
            top: 22px;
            right: 22px;
            z-index: 10000;
            display: flex;
            flex-direction: column;
            gap: 10px;
            width: min(370px, calc(100vw - 32px));
        }

        .cf-toast {
            padding: 14px 16px;
            border: 1px solid rgba(52, 211, 153, .28);
            border-radius: 11px;
            background: #17251f;
            color: #a7f3d0;
            box-shadow: 0 12px 35px rgba(0, 0, 0, .25);
            font-size: 13px;
            line-height: 1.5;
            animation: staffToastIn .2s ease-out;
        }

        .cf-toast-error {
            border-color: rgba(248, 113, 113, .3);
            background: #2b1b20;
            color: #fca5a5;
        }

        .cf-toast-fadeout {
            opacity: 0;
            transform: translateY(-5px);
            transition: opacity .4s, transform .4s;
        }

        @keyframes staffToastIn {
            from { opacity: 0; transform: translateY(-5px); }
            to { opacity: 1; transform: translateY(0); }
        }

        @media (max-width: 1100px) {
            .staff-card-header { align-items: flex-start; }
            .staff-toolbar { justify-content: flex-start; width: 100%; }
            #staffFilterInput { width: min(290px, 35vw); }
            .staff-table-body { padding: 16px; }
            .cf-table thead th,
            .cf-table tbody td { padding: 13px 14px; }
        }

        @media (max-width: 700px) {
            .staff-card-header { padding: 20px 16px; }
            .staff-toolbar { align-items: stretch; }
            .staff-search-form { width: 100%; }
            .staff-search-box { flex: 1; min-width: 0; }
            #staffFilterInput { width: 100%; min-width: 0; }
            #staffStatusFilter { flex: 1; min-width: 0; }
            .staff-control-btn-primary { flex: 1; }
            .staff-table-body { padding: 12px; }
            .cf-modal-body { padding: 20px 16px; }
            .cf-modal-header { padding: 18px 16px; }
            .cf-modal-footer { padding: 14px 16px; }
        }

        @media (prefers-reduced-motion: reduce) {
            .cf-modal,
            .cf-toast { animation: none; }
            * { scroll-behavior: auto !important; }
        }
    </style>
</head>

<body>
<div class="cf-manager-shell">
    <% if (isAdminRole) { %>
        <jsp:include page="/admin/sidebar.jsp"/>
    <% } else { %>
        <jsp:include page="/manager/sidebar.jsp"/>
    <% } %>

    <div class="cf-manager-main">
        <% if (isAdminRole) { %>
            <jsp:include page="/admin/topbar.jsp"/>
        <% } else { %>
            <jsp:include page="/manager/topbar.jsp"/>
        <% } %>

        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <p class="cf-breadcrumb">Nhân sự / Danh sách nhân viên</p>
                    <h1>Nhân sự <%= currentCentreName %></h1>
                    <p>Quản lý tài khoản, tạo nhân viên, cập nhật thông tin và kiểm soát trạng thái hoạt động tại trung tâm.</p>
                </div>
            </div>

            <% if ("created".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã tạo mới tài khoản nhân viên cho trung tâm.
                </div>
            <% } else if ("updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật thông tin nhân viên thành công.
                </div>
            <% } else if ("status_updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> Đã cập nhật trạng thái hoạt động của nhân viên.
                </div>
            <% } else if ("account_exists".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản đã tồn tại trong hệ thống.
                </div>
            <% } else if ("email_exists".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Địa chỉ Email này đã được sử dụng.
                </div>
            <% } else if ("missing_fields".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Vui lòng điền đầy đủ các thông tin bắt buộc.
                </div>
            <% } else if ("unassigned_manager".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Quản lý chưa được bổ nhiệm trung tâm nên không thể tạo nhân viên.
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Thao tác không thành công. Vui lòng kiểm tra lại thông tin.
                </div>
            <% } %>

            <% if (selectedCentreId == null && !isAdminRole) { %>
                <section class="cf-card" style="padding:40px 24px;text-align:center;margin-top:20px;">
                    <div style="font-size:40px;margin-bottom:14px;">🏛</div>
                    <h2 style="color:#fbbf24;margin-bottom:8px;">Tài khoản chưa được bổ nhiệm trung tâm</h2>
                    <p style="color:#94a3b8;max-width:540px;margin:0 auto 16px;font-size:14px;line-height:1.7;">
                        Tài khoản Quản lý của bạn hiện chưa được phân công phụ trách trung tâm chiếu phim nào.
                        Quản lý chỉ được tạo và quản lý nhân viên tại trung tâm đã được bổ nhiệm.
                    </p>
                    <p style="font-size:13px;color:#cbd5e1;">
                        Vui lòng liên hệ Quản trị viên hệ thống để được phân công trung tâm.
                    </p>
                </section>
            <% } else { %>

            <section class="cf-card" style="margin-top:20px;">
                <div class="staff-card-header">
                    <div class="staff-heading">
                        <h2>Danh sách nhân viên</h2>
                        <p id="staffCountLabel"><%= staff == null ? 0 : staff.size() %> tài khoản đang hiển thị.</p>
                    </div>

                    <div class="staff-toolbar">
                        <form class="staff-search-form" method="get"
                              action="${pageContext.request.contextPath}/staff">
                            <% if (cFilterVal != null) { %>
                                <input type="hidden" name="centreId" value="<%= cFilterVal %>">
                            <% } %>

                            <div class="staff-search-box">
                                <span class="staff-search-icon" aria-hidden="true">⌕</span>
                                <input type="search" name="search" id="staffFilterInput"
                                       value="<%= searchParam != null ? searchParam : "" %>"
                                       placeholder="Tìm mã NV, họ tên, tài khoản..."
                                       aria-label="Tìm nhân viên">
                            </div>

                            <button type="submit" class="staff-control-btn">Tìm kiếm</button>

                            <% if (searchParam != null && !searchParam.trim().isEmpty()) { %>
                                <a href="${pageContext.request.contextPath}/staff<%= cFilterVal != null ? "?centreId=" + cFilterVal : "" %>"
                                   class="staff-control-btn">Xóa lọc</a>
                            <% } %>
                        </form>

                        <div class="staff-select-wrap">
                            <select id="staffStatusFilter" aria-label="Lọc theo trạng thái">
                                <option value="">Tất cả trạng thái</option>
                                <option value="st-active">Đang hoạt động</option>
                                <option value="st-inactive">Tạm ngưng</option>
                                <option value="st-locked">Đã khóa</option>
                            </select>
                        </div>

                        <button type="button"
                                class="staff-control-btn staff-control-btn-primary"
                                onclick="openCreateStaffModal()">
                            <span aria-hidden="true"></span> Thêm nhân viên
                        </button>
                    </div>
                </div>

                <div class="staff-table-body">
                    <div class="cf-table-wrap">
                        <table class="cf-table">
                            <thead>
                            <tr>
                                <th>Mã nhân sự</th>
                                <th>Nhân viên</th>
                                <th>Tên đăng nhập</th>
                                <th>Số điện thoại</th>
                                <th>Trung tâm</th>
                                <th>Trạng thái</th>
                                <th>Thao tác</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% if (staff == null || staff.isEmpty()) { %>
                                <tr>
                                    <td colspan="7" class="cf-empty">
                                        Chưa có nhân viên phù hợp với từ khóa tìm kiếm.
                                    </td>
                                </tr>
                            <% } else {
                                for (User item : staff) {
                                    String st = item.getStatus() != null ? item.getStatus() : "INACTIVE";
                                    String staffName = item.getFullName() != null ? item.getFullName() : "";
                                    String staffEmail = item.getEmail() != null ? item.getEmail() : "";
                                    String staffAccount = item.getAccount() != null ? item.getAccount() : "";
                                    String staffPhone = item.getPhone() != null ? item.getPhone() : "";
                                    String employeeCode = item.getEmployeeCode() != null ? item.getEmployeeCode() : "";
                                    String searchVal = (employeeCode + " " + item.getUserId() + " nv" + item.getUserId()
                                        + " " + staffName + " " + staffEmail + " " + staffAccount + " " + staffPhone).toLowerCase();
                                    String stClass = "ACTIVE".equalsIgnoreCase(st) ? "st-active"
                                        : ("INACTIVE".equalsIgnoreCase(st) ? "st-inactive" : "st-locked");
                                    String itemCentreName = item.getCentreName() != null
                                        ? item.getCentreName() : currentCentreName;
                            %>
                                <tr class="staff-row <%= stClass %>" data-search="<%= searchVal %>">
                                    <td>
                                        <code class="cf-badge cf-badge-info"><%= employeeCode %></code>
                                    </td>
                                    <td>
                                        <strong><%= staffName %></strong>
                                        <small><%= staffEmail %></small>
                                    </td>
                                    <td><code><%= staffAccount %></code></td>
                                    <td><%= !staffPhone.isEmpty() ? staffPhone : "—" %></td>
                                    <td>🏛 <%= itemCentreName %></td>
                                    <td>
                                        <span id="badge-status-<%= item.getUserId() %>"
                                              class="cf-badge <%= "ACTIVE".equalsIgnoreCase(st) ? "cf-badge-success" : ("LOCKED".equalsIgnoreCase(st) ? "cf-badge-danger" : "cf-badge-warning") %>">
                                            <%= "ACTIVE".equalsIgnoreCase(st) ? "Đang hoạt động"
                                                : ("LOCKED".equalsIgnoreCase(st) ? "Đã khóa" : "Tạm ngưng") %>
                                        </span>
                                    </td>
                                    <td>
                                        <div class="staff-actions">
                                            <button type="button" class="staff-edit-btn"
                                                    onclick="openEditStaffModal(<%= item.getUserId() %>, '<%= employeeCode.replace("'", "\\'") %>', '<%= staffName.replace("'", "\\'") %>', '<%= staffPhone.replace("'", "\\'") %>', '<%= staffEmail.replace("'", "\\'") %>')"
                                                    title="Chỉnh sửa thông tin">
                                                Sửa
                                            </button>

                                            <div class="status-select-wrap">
                                                <select class="cf-status-select val-<%= st.toLowerCase() %>"
                                                        data-userid="<%= item.getUserId() %>"
                                                        data-name="<%= staffName.replace("\"", "&quot;") %>"
                                                        onchange="handleStaffStatusChange(this)"
                                                        title="Thay đổi trạng thái tài khoản">
                                                    <option value="ACTIVE" <%= "ACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>Hoạt động</option>
                                                    <option value="INACTIVE" <%= "INACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>Tạm ngưng</option>
                                                    <option value="LOCKED" <%= "LOCKED".equalsIgnoreCase(st) ? "selected" : "" %>>Khóa</option>
                                                </select>
                                                <span class="status-indicator"
                                                      id="status-ind-<%= item.getUserId() %>"
                                                      style="display:none;">✓</span>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            <%  }
                               } %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>
            <% } %>
        </main>
    </div>
</div>

<!-- TẠO NHÂN VIÊN -->
<div class="cf-modal-backdrop" id="createStaffModal">
    <div class="cf-modal" role="dialog" aria-modal="true" aria-labelledby="createStaffTitle">
        <div class="cf-modal-header">
            <h3 id="createStaffTitle">Thêm nhân viên mới tại <%= currentCentreName %></h3>
            <button type="button" class="cf-modal-close" onclick="closeCreateStaffModal()" aria-label="Đóng">&times;</button>
        </div>

        <form method="post" action="${pageContext.request.contextPath}/staff">
            <input type="hidden" name="action" value="create">

            <div class="cf-modal-body">
                <p style="font-size:13px;color:#94a3b8;margin:0 0 4px;line-height:1.6;">
                    Tài khoản sẽ được gán vai trò <strong>STAFF</strong> và trực thuộc trung tâm
                    <strong><%= currentCentreName %></strong>.
                </p>

                <div class="cf-form-group">
                    <label for="createStaffFullName">Họ và tên nhân viên *</label>
                    <input type="text" id="createStaffFullName" name="fullName"
                           placeholder="Ví dụ: Nguyễn Văn B" required>
                </div>

                <div style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;">
                    <div class="cf-form-group">
                        <label for="createStaffAccount">Tên đăng nhập *</label>
                        <input type="text" id="createStaffAccount" name="account"
                               placeholder="staff_td_06" required>
                    </div>
                    <div class="cf-form-group">
                        <label for="createStaffPhone">Số điện thoại</label>
                        <input type="tel" id="createStaffPhone" name="phone"
                               placeholder="09xxxxxxxx">
                    </div>
                </div>

                <div class="cf-form-group">
                    <label for="createStaffEmail">Email *</label>
                    <input type="email" id="createStaffEmail" name="email"
                           placeholder="staff@cineflow.vn" required>
                </div>

                <div class="cf-form-group">
                    <label for="createStaffPassword">Mật khẩu ban đầu *</label>
                    <input type="password" id="createStaffPassword" name="password"
                           value="123456" required>
                    <small style="color:#94a3b8;font-size:12px;line-height:1.5;">
                        Mật khẩu khởi tạo mặc định; nên đổi sau lần đăng nhập đầu tiên.
                    </small>
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateStaffModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Tạo tài khoản</button>
            </div>
        </form>
    </div>
</div>

<!-- CHỈNH SỬA NHÂN VIÊN -->
<div class="cf-modal-backdrop" id="editStaffModal">
    <div class="cf-modal" role="dialog" aria-modal="true" aria-labelledby="editStaffTitle">
        <div class="cf-modal-header">
            <h3 id="editStaffTitle">Chỉnh sửa thông tin nhân viên</h3>
            <button type="button" class="cf-modal-close" onclick="closeEditStaffModal()" aria-label="Đóng">&times;</button>
        </div>

        <form method="post" action="${pageContext.request.contextPath}/staff">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="userId" id="editStaffUserId">

            <div class="cf-modal-body">
                <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 14px;border:1px solid #303b4b;border-radius:10px;background:#10151d;">
                    <span style="font-size:12px;color:#94a3b8;font-weight:600;">Mã nhân viên</span>
                    <code class="cf-badge cf-badge-info" id="editStaffCode">NV00000</code>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffFullName">Họ và tên nhân viên *</label>
                    <input type="text" name="fullName" id="editStaffFullName" required>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffPhone">Số điện thoại</label>
                    <input type="tel" name="phone" id="editStaffPhone">
                </div>

                <div class="cf-form-group">
                    <label for="editStaffEmail">Email *</label>
                    <input type="email" name="email" id="editStaffEmail" required>
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeEditStaffModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Lưu thay đổi</button>
            </div>
        </form>
    </div>
</div>

<div id="cf-toast-container" aria-live="polite" aria-atomic="false"></div>

<script>
    function openCreateStaffModal() {
        document.getElementById('createStaffModal').classList.add('active');
    }

    function closeCreateStaffModal() {
        document.getElementById('createStaffModal').classList.remove('active');
    }

    function openEditStaffModal(id, code, fullName, phone, email) {
        document.getElementById('editStaffUserId').value = id;
        document.getElementById('editStaffCode').textContent = code;
        document.getElementById('editStaffFullName').value = fullName;
        document.getElementById('editStaffPhone').value = phone;
        document.getElementById('editStaffEmail').value = email;
        document.getElementById('editStaffModal').classList.add('active');
    }

    function closeEditStaffModal() {
        document.getElementById('editStaffModal').classList.remove('active');
    }

    document.querySelectorAll('.cf-modal-backdrop').forEach(function(modal) {
        modal.addEventListener('click', function(event) {
            if (event.target === modal) {
                modal.classList.remove('active');
            }
        });
    });

    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            closeCreateStaffModal();
            closeEditStaffModal();
        }
    });

    document.addEventListener('DOMContentLoaded', function() {
        var searchInput = document.getElementById('staffFilterInput');
        var statusFilter = document.getElementById('staffStatusFilter');
        var rows = document.querySelectorAll('.staff-row');
        var countLabel = document.getElementById('staffCountLabel');

        function filterRows() {
            var query = searchInput ? searchInput.value.toLowerCase().trim() : '';
            var statusVal = statusFilter ? statusFilter.value : '';
            var visible = 0;

            rows.forEach(function(row) {
                var searchVal = row.getAttribute('data-search') || '';
                var matchText = !query || searchVal.indexOf(query) !== -1;
                var matchStatus = !statusVal || row.classList.contains(statusVal);
                var showRow = matchText && matchStatus;

                row.style.display = showRow ? '' : 'none';
                if (showRow) visible++;
            });

            if (countLabel) {
                countLabel.textContent = visible + ' tài khoản đang hiển thị.';
            }
        }

        if (searchInput) searchInput.addEventListener('input', filterRows);
        if (statusFilter) statusFilter.addEventListener('change', filterRows);

        document.querySelectorAll('.cf-alert-success, .cf-alert-danger').forEach(function(alert) {
            setTimeout(function() {
                alert.style.transition = 'opacity .4s ease, transform .4s ease';
                alert.style.opacity = '0';
                alert.style.transform = 'translateY(-8px)';

                setTimeout(function() {
                    if (alert.parentNode) alert.parentNode.removeChild(alert);
                }, 400);
            }, 3000);
        });
    });

    function handleStaffStatusChange(selectEl) {
        var userId = selectEl.getAttribute('data-userid');
        var staffName = selectEl.getAttribute('data-name') || 'nhân viên';
        var newStatus = selectEl.value;
        var previousStatus = selectEl.getAttribute('data-previous-status') || '';
        var ind = document.getElementById('status-ind-' + userId);
        var badge = document.getElementById('badge-status-' + userId);
        var row = selectEl.closest('.staff-row');

        if (!previousStatus) {
            previousStatus = selectEl.querySelector('option[selected]') ?
                selectEl.querySelector('option[selected]').value : newStatus;
        }

        selectEl.disabled = true;

        if (ind) {
            ind.textContent = '…';
            ind.style.display = 'inline';
            ind.style.color = '#94a3b8';
        }

        var formData = new URLSearchParams();
        formData.append('action', 'status');
        formData.append('ajax', '1');
        formData.append('userId', userId);
        formData.append('status', newStatus);

        fetch('${pageContext.request.contextPath}/staff', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: formData.toString()
        })
        .then(function(response) {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(function(data) {
            if (!data.success) throw new Error(data.message || 'Lỗi xử lý');

            selectEl.setAttribute('data-previous-status', newStatus);
            selectEl.className = 'cf-status-select val-' + newStatus.toLowerCase();

            if (badge) {
                if (newStatus === 'ACTIVE') {
                    badge.className = 'cf-badge cf-badge-success';
                    badge.textContent = 'Đang hoạt động';
                } else if (newStatus === 'INACTIVE') {
                    badge.className = 'cf-badge cf-badge-warning';
                    badge.textContent = 'Tạm ngưng';
                } else {
                    badge.className = 'cf-badge cf-badge-danger';
                    badge.textContent = 'Đã khóa';
                }
            }

            if (row) {
                row.classList.remove('st-active', 'st-inactive', 'st-locked');
                row.classList.add('st-' + newStatus.toLowerCase());
            }

            if (ind) {
                ind.textContent = '✓';
                ind.style.color = '#6ee7b7';
                ind.style.display = 'inline';

                setTimeout(function() {
                    ind.style.display = 'none';
                }, 3000);
            }

            var statusLabel = newStatus === 'ACTIVE' ? 'Hoạt động'
                : (newStatus === 'INACTIVE' ? 'Tạm ngưng' : 'Đã khóa');

            showToast('Đã cập nhật trạng thái của ' + staffName + ' thành "' + statusLabel + '".', 'success');
        })
        .catch(function(error) {
            if (previousStatus) selectEl.value = previousStatus;
            showToast('Cập nhật trạng thái thất bại. Vui lòng thử lại.', 'error');
        })
        .finally(function() {
            selectEl.disabled = false;
            if (ind && ind.textContent === '…') ind.style.display = 'none';
        });
    }

    function showToast(message, type) {
        var container = document.getElementById('cf-toast-container');
        if (!container) return;

        var toast = document.createElement('div');
        toast.className = 'cf-toast' + (type === 'error' ? ' cf-toast-error' : '');
        toast.textContent = message;
        container.appendChild(toast);

        setTimeout(function() {
            toast.classList.add('cf-toast-fadeout');

            setTimeout(function() {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 400);
        }, 3000);
    }
</script>
</body>
</html>