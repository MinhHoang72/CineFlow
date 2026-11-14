<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Movie" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }
%>
<%!
    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
%>
<%
    Movie movie = (Movie) request.getAttribute("movie");
    boolean editing = movie != null;
    String age = editing && movie.getAgeRating() != null ? movie.getAgeRating() : "P";
    String status = editing && movie.getStatus() != null ? movie.getStatus() : "COMING_SOON";
    String posterUrl = editing && movie.getPosterUrl() != null ? movie.getPosterUrl() : "";
    String trailerUrl = editing && movie.getTrailerUrl() != null ? movie.getTrailerUrl() : "";
    String errorParam = request.getParameter("error");
    if ("extend_end_date_required".equals(errorParam)) {
        status = "NOW_SHOWING";
    } else if ("reissue_dates_required".equals(errorParam)) {
        status = "COMING_SOON";
    }

    request.setAttribute("activeNav", "movies");
    request.setAttribute("pageTitle", editing ? "Chỉnh sửa phim: " + movie.getTitle() : "Tạo phim mới");
    request.setAttribute("pageBreadcrumb", editing ? "Chỉnh sửa phim" : "Thêm mới");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= editing ? "Chỉnh sửa: " + esc(movie.getTitle()) : "Thêm phim mới" %> | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/movie-form.css?v=<%= System.currentTimeMillis() %>">
    <style>
        /* CANH BAO DO TUONG PHAN CAO (HIGH CONTRAST RED WARNING) */
        .cf-alert.cf-alert-danger,
        .cf-alert.cf-alert-error,
        .cf-alert-danger,
        .cf-alert-error {
            background-color: #FEF2F2 !important;
            border: 2px solid #EF4444 !important;
            border-left: 6px solid #DC2626 !important;
            color: #991B1B !important;
            font-weight: 600 !important;
            font-size: 14px !important;
            line-height: 1.5 !important;
            padding: 14px 18px !important;
            border-radius: 8px !important;
            display: flex !important;
            align-items: center !important;
            gap: 12px !important;
            box-shadow: 0 4px 14px rgba(220, 38, 38, 0.12) !important;
        }
        .cf-alert.cf-alert-danger strong,
        .cf-alert.cf-alert-error strong,
        .cf-alert-danger strong {
            color: #7F1D1D !important;
            font-weight: 800 !important;
        }
        .cf-alert.cf-alert-danger span,
        .cf-alert.cf-alert-error span,
        .cf-alert-danger span {
            color: #DC2626 !important;
            font-weight: 900 !important;
            font-size: 20px !important;
            flex-shrink: 0 !important;
        }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- TIÊU ĐỀ TRANG -->
            <div class="cf-page-header">
                <div>
                    <h1><%= editing ? "Chỉnh sửa thông tin phim" : "Thêm phim mới" %></h1>
                </div>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-secondary">
                        Danh mục phim
                    </a>
                </div>
            </div>

            <!-- THÔNG BÁO LỖI VALIDATION NẾU CÓ -->
            <% if ("missing_title".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng nhập tên phim (không được để trống).
                </div>
            <% } else if ("invalid_duration".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Thời lượng phim không hợp lệ (phải là số nguyên từ 1 đến 600 phút).
                </div>
            <% } else if ("invalid_age_rating".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Độ tuổi khuyến nghị không thuộc danh mục hợp lệ (P, K, T13, T16, T18, C).
                </div>
            <% } else if ("invalid_status".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Trạng thái phát hành không hợp lệ.
                </div>
            <% } else if ("missing_dates".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng nhập đầy đủ <strong>Ngày khởi chiếu</strong> và <strong>Ngày kết thúc</strong>.
                </div>
            <% } else if ("title_too_long".equals(errorParam) || "invalid_title".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Tên phim không được vượt quá 200 ký tự.</div>
            <% } else if ("duplicate_title".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Tên phim đã tồn tại.</div>
            <% } else if ("text_too_long".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Một trường mô tả, đạo diễn, diễn viên hoặc ngôn ngữ vượt quá độ dài cho phép.</div>
            <% } else if ("invalid_date_format".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Định dạng ngày không hợp lệ.</div>
            <% } else if ("end_date_in_past".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Ngày kết thúc của phim đang chiếu không được ở trong quá khứ.</div>
            <% } else if ("end_date_not_future".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Ngày kết thúc của phim sắp chiếu phải nằm trong tương lai.</div>
            <% } else if ("invalid_date_range".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Khoảng thời gian phát hành không hợp lệ: Ngày kết thúc phải sau hoặc bằng ngày khởi chiếu.
                </div>
            <% } else if ("coming_soon_in_past".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi ngày phát hành:</strong> Không thể đặt trạng thái "Sắp khởi chiếu" khi ngày khởi chiếu ở trong quá khứ. Vui lòng chọn ngày tương lai hoặc đổi trạng thái sang "Đang chiếu".
                </div>
            <% } else if ("now_showing_in_future".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi ngày phát hành:</strong> Phim có ngày khởi chiếu ở tương lai chưa thể đặt trạng thái "Đang chiếu". Vui lòng chọn trạng thái "Sắp khởi chiếu".
                </div>
            <% } else if ("invalid_poster_file".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi tệp:</strong> Áp phích (poster) phải là tệp hình ảnh (.jpg, .jpeg, .png, .webp).
                </div>
            <% } else if ("invalid_trailer_file".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi tệp:</strong> Trailer phải là tệp video (.mp4, .webm, .ogg).
                </div>
            <% } else if ("poster_too_large".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Poster không được vượt quá 5 MB.</div>
            <% } else if ("trailer_too_large".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Trailer không được vượt quá 50 MB.</div>
            <% } else if ("invalid_media_url".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;"><span>⚠</span> Đường dẫn media không hợp lệ.</div>
            <% } else if ("reissue_dates_required".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi ngày phát hành:</strong> Phim đang ở trạng thái <%= editing && "ARCHIVED".equalsIgnoreCase(movie.getStatus()) ? "Lưu trữ (ARCHIVED)" : "Đã kết thúc (ENDED)" %>. Khi chuyển sang "Sắp khởi chiếu" (COMING SOON), bạn bắt buộc phải thiết lập lại <strong>Ngày khởi chiếu</strong> (trong tương lai) và <strong>Ngày kết thúc</strong> mới trước khi lưu thay đổi.
                </div>

            <% } else if ("extend_end_date_required".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Gia hạn đợt chiếu:</strong> Phim đã kết thúc đợt chiếu. Khi chuyển sang "Đang chiếu" (NOW SHOWING), bạn <strong>bắt buộc phải thiết lập lại Ngày kết thúc</strong> mới từ hôm nay trở đi.
                </div>
            <% } else if ("cannot_reopen_to_now_showing".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> Phim đã lưu trữ (ARCHIVED) không thể chuyển trực tiếp sang "Đang chiếu" (NOW SHOWING). Vui lòng chọn "Sắp khởi chiếu" và thiết lập lại ngày chiếu mới.
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> Thao tác lưu phim thất bại. Vui lòng kiểm tra lại thông tin.
                </div>
            <% } %>

            <!-- FORM THÔNG TIN PHIM -->
            <form id="movieForm" method="post" action="${pageContext.request.contextPath}/admin/movie" enctype="multipart/form-data">
                <input type="hidden" name="action" value="save">
                <% if (editing) { %>
                    <input type="hidden" name="movieId" value="<%= movie.getMovieId() %>">
                <% } %>

                <div class="cf-movie-form-layout">
                    <!-- ========================================================
                         CỘT TRÁI (CHÍNH): TOÀN BỘ THÔNG TIN PHIM TRONG 1 THẺ LIỀN MẠCH
                         ======================================================== -->
                    <div class="cf-form-main-card">
                        <!-- PHẦN 1: THÔNG TIN CƠ BẢN -->
                        <div class="cf-form-section-header">
                            <h3 class="cf-form-section-title">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"></rect><line x1="7" y1="2" x2="7" y2="22"></line><line x1="17" y1="2" x2="17" y2="22"></line><line x1="2" y1="12" x2="22" y2="12"></line><line x1="2" y1="7" x2="7" y2="7"></line><line x1="2" y1="17" x2="7" y2="17"></line><line x1="17" y1="17" x2="22" y2="17"></line><line x1="17" y1="7" x2="22" y2="7"></line></svg>
                                Thông tin cơ bản
                            </h3>
                        </div>

                        <div class="cf-fields-grid">
                            <div class="cf-form-group cf-field-span-2">
                                <label class="cf-form-label">Tên phim <span class="req">*</span></label>
                                <input class="cf-form-control" name="title" value="<%= editing ? esc(movie.getTitle()) : "" %>" required placeholder="Ví dụ: Đào, Phở và Piano">
                            </div>

                            <div class="cf-form-group">
                                <label class="cf-form-label">Thể loại phim</label>
                                <select class="cf-form-control" name="genre">
                                    <option value="">-- Chọn thể loại --</option>
                                    <% String[] genres = {"Hành Động", "Tình Cảm", "Tâm Lý", "Hài Hước", "Kinh Dị", "Hoạt Hình", "Gia Đình", "Phiêu Lưu", "Khoa Học Viễn Tưởng", "Lịch Sử", "Chiến Tranh", "Chính Kịch"};
                                       String currentGenre = editing && movie.getGenre() != null ? movie.getGenre() : "";
                                       for (String genreOption : genres) { %>
                                        <option value="<%= esc(genreOption) %>" <%= genreOption.equalsIgnoreCase(currentGenre) ? "selected" : "" %>><%= esc(genreOption) %></option>
                                    <% } %>
                                </select>
                            </div>

                            <div class="cf-form-group">
                                <label class="cf-form-label">Mã phim (ID)</label>
                                <input class="cf-form-control" value="<%= editing ? movie.getMovieId() : "Tự động tạo khi lưu" %>" readonly>
                            </div>

                            <div class="cf-form-group">
                                <label class="cf-form-label">Thời lượng (phút) <span class="req">*</span></label>
                                <input class="cf-form-control" name="duration" type="number" min="1" max="600" value="<%= editing ? movie.getDuration() : "120" %>" required>
                            </div>

                            <div class="cf-form-group">
                                <label class="cf-form-label">Độ tuổi khuyến nghị <span class="req">*</span></label>
                                <select class="cf-form-control" name="ageRating">
                                    <option value="P" <%= "P".equals(age) ? "selected" : "" %>>P - Phổ biến cho mọi lứa tuổi</option>
                                    <option value="K" <%= "K".equals(age) ? "selected" : "" %>>K - Khán giả dưới 13 tuổi có người giám hộ</option>
                                    <option value="T13" <%= "T13".equals(age) ? "selected" : "" %>>T13 - Khán giả từ 13 tuổi trở lên</option>
                                    <option value="T16" <%= "T16".equals(age) ? "selected" : "" %>>T16 - Khán giả từ 16 tuổi trở lên</option>
                                    <option value="T18" <%= "T18".equals(age) ? "selected" : "" %>>T18 - Khán giả từ 18 tuổi trở lên</option>
                                    <option value="C" <%= "C".equals(age) ? "selected" : "" %>>C - Cấm phổ biến</option>
                                </select>
                            </div>

                            <div class="cf-form-group cf-field-span-2">
                                <label class="cf-form-label">Trạng thái phát hành <span class="req">*</span></label>
                                <select class="cf-form-control" name="status">
                                    <option value="COMING_SOON" <%= "COMING_SOON".equals(status) ? "selected" : "" %>>Sắp khởi chiếu (COMING_SOON)</option>
                                    <option value="NOW_SHOWING" <%= "NOW_SHOWING".equals(status) ? "selected" : "" %> <%= (editing && "ARCHIVED".equalsIgnoreCase(movie.getStatus())) ? "disabled style='opacity: 0.5; cursor: not-allowed;' title='Phim đã lưu trữ, vui lòng chọn Sắp khởi chiếu để tái phát hành'" : "" %>>Đang chiếu (NOW_SHOWING)</option>
                                    <option value="ENDED" <%= "ENDED".equals(status) ? "selected" : "" %>>Đã kết thúc (ENDED)</option>
                                    <option value="ARCHIVED" <%= "ARCHIVED".equals(status) ? "selected" : "" %>>Lưu trữ (ARCHIVED)</option>
                                </select>
                                <p style="margin: 6px 0 0; font-size: 12px; color: #64748B; line-height: 1.4;">
                                    💡 <strong>Vòng đời phim:</strong> 
                                    <strong>Sắp khởi chiếu</strong> (chờ phát hành) → 
                                    <strong>Đang chiếu</strong> (mở bán vé) → 
                                    <strong>Đã kết thúc</strong> (dừng chiếu, không tạo suất chiếu mới) → 
                                    <strong>Lưu trữ</strong> (giữ hồ sơ quản lý bản quyền/lịch sử và cân nhắc tái phát hành).
                                </p>
                            </div>
                        </div>

                        <!-- ĐƯỜNG PHÂN CÁCH -->
                        <div class="cf-form-divider"></div>

                        <!-- PHẦN 2: LỊCH PHÁT HÀNH -->
                        <div class="cf-form-section-header">
                            <h3 class="cf-form-section-title">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                                Lịch phát hành
                            </h3>
                        </div>

                        <div class="cf-fields-grid">
                            <div class="cf-form-group">
                                <label class="cf-form-label">Ngày khởi chiếu <span class="req">*</span></label>
                                <input class="cf-form-control" name="releaseDate" type="date" value="<%= editing && movie.getReleaseDate() != null ? movie.getReleaseDate() : "" %>" required>
                            </div>

                            <div class="cf-form-group">
                                <label class="cf-form-label">Ngày kết thúc dự kiến <span class="req">*</span></label>
                                <input class="cf-form-control" name="endDate" type="date" value="<%= editing && movie.getEndDate() != null && !"extend_end_date_required".equals(errorParam) ? movie.getEndDate() : "" %>" required>
                            </div>
                        </div>

                        <!-- ĐƯỜNG PHÂN CÁCH -->
                        <div class="cf-form-divider"></div>

                        <!-- PHẦN 3: ĐỘI NGŨ & NGÔN NGỮ -->
                        <div class="cf-form-section-header">
                            <h3 class="cf-form-section-title">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                                Đội ngũ & Ngôn ngữ
                            </h3>
                        </div>

                        <div class="cf-fields-grid">
                            <div class="cf-form-group">
                                <label class="cf-form-label">Đạo diễn</label>
                                <input class="cf-form-control" name="director" value="<%= editing ? esc(movie.getDirector()) : "" %>" placeholder="Tên đạo diễn">
                            </div>

                            <div class="cf-form-group">
                                <label class="cf-form-label">Ngôn ngữ & Phụ đề</label>
                                <input class="cf-form-control" name="language" value="<%= editing ? esc(movie.getLanguage()) : "" %>" placeholder="Ví dụ: Tiếng Việt, Phụ đề Tiếng Anh">
                            </div>

                            <div class="cf-form-group cf-field-span-2">
                                <label class="cf-form-label">Diễn viên chính</label>
                                <input class="cf-form-control" name="cast" value="<%= editing ? esc(movie.getCast()) : "" %>" placeholder="Danh sách diễn viên, ngăn cách bởi dấu phẩy">
                            </div>
                        </div>

                        <!-- ĐƯỜNG PHÂN CÁCH -->
                        <div class="cf-form-divider"></div>

                        <!-- PHẦN 4: NỘI DUNG TÓM TẮT PHIM -->
                        <div class="cf-form-section-header">
                            <h3 class="cf-form-section-title">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                                Nội dung tóm tắt phim
                            </h3>
                        </div>

                        <div class="cf-form-group">
                            <textarea class="cf-form-textarea" name="description" rows="4" placeholder="Nhập tóm tắt kịch bản và câu chuyện của bộ phim..."><%= editing ? esc(movie.getDescription()) : "" %></textarea>
                        </div>
                    </div>

                    <!-- ========================================================
                         CỘT PHẢI (PHƯƠNG TIỆN): GỌN GÀNG, KHÔNG PHÙNG PHÌNH
                         ======================================================== -->
                    <div>
                        <!-- ÁP PHÍCH PHIM (POSTER) -->
                        <div class="cf-side-card">
                            <div class="cf-side-header">
                                <span class="cf-side-title">
                                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
                                    Áp phích phim (Poster)
                                </span>
                            </div>
                            <div class="cf-side-body">
                                <div class="cf-poster-compact-wrap">
                                    <div class="cf-poster-thumb-preview" id="posterPreviewFrame">
                                        <% if (!posterUrl.isBlank()) { %>
                                            <img src="<%= esc(posterUrl) %>" alt="Poster preview" id="posterPreviewImg" onerror="handlePosterError()">
                                        <% } else { %>
                                            <div class="cf-poster-placeholder" id="posterPlaceholder">
                                                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
                                                <span>Chưa có ảnh</span>
                                            </div>
                                        <% } %>
                                    </div>

                                    <div class="cf-poster-controls">
                                        <input type="hidden" name="posterUrl" id="posterUrlInput" value="<%= esc(posterUrl) %>">
                                        <label class="cf-file-picker-btn">
                                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2"></path><polyline points="7 9 12 4 17 9"></polyline><line x1="12" y1="4" x2="12" y2="16"></line></svg>
                                            <span>Tải ảnh poster...</span>
                                            <input type="file" name="posterFile" id="posterFileInput" accept="image/png,image/jpeg,image/webp" style="display: none;">
                                        </label>
                                        <div class="cf-file-name-hint" id="posterFileName"></div>
                                        <p class="cf-file-support-hint">Định dạng hỗ trợ: JPG, PNG, WEBP (Tỉ lệ 2:3, tối đa 5MB).</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- TRAILER PHIM -->
                        <div class="cf-side-card" style="margin-top: 18px;">
                            <div class="cf-side-header">
                                <span class="cf-side-title">
                                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"></polygon><rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect></svg>
                                    Trailer phim
                                </span>
                            </div>
                            <div class="cf-side-body">
                                <div class="cf-trailer-compact-wrap">
                                    <input type="hidden" name="trailerUrl" id="trailerUrlInput" value="<%= esc(trailerUrl) %>">

                                    <!-- Khung xem trước Trailer (16:9) -->
                                    <div class="cf-trailer-preview-frame" id="trailerPreviewFrame">
                                        <div class="cf-trailer-placeholder" id="trailerPlaceholder">
                                            <span>Xem trước trailer</span>
                                        </div>
                                    </div>

                                    <div class="cf-trailer-file-row">
                                        <label class="cf-file-picker-btn">
                                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2"></path><polyline points="7 9 12 4 17 9"></polyline><line x1="12" y1="4" x2="12" y2="16"></line></svg>
                                            <span>Tải video trailer...</span>
                                            <input type="file" name="trailerFile" id="trailerFileInput" accept="video/mp4,video/webm,video/ogg" style="display: none;">
                                        </label>
                                        <div class="cf-file-name-hint" id="trailerFileName"></div>
                                        <p class="cf-file-support-hint">Định dạng hỗ trợ: MP4, WebM, OGG.</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- THANH HÀNH ĐỘNG CỐ ĐỊNH (FIXED SAFE FOOTER) -->
                <div class="cf-form-sticky-bar">
                    <div class="cf-form-sticky-info">
                        <%= editing ? ("Chỉnh sửa phim: <strong>" + esc(movie.getTitle()) + "</strong> (Mã: " + movie.getMovieId() + ")") : "Tạo phim mới vào danh mục hệ thống" %>
                    </div>

                    <div class="cf-form-actions">
                        <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-secondary">
                            Hủy bỏ
                        </a>
                        <button type="submit" class="cf-btn cf-btn-primary" id="btnSubmitMovie">
                            <%= editing ? "Lưu thay đổi" : "Tạo phim mới" %>
                        </button>
                    </div>
                </div>
            </form>
        </main>
    </div>
</div>

<!-- MÃ JAVASCRIPT ĐIỀU KHIỂN PHÍA CLIENT -->
<script>

document.addEventListener('DOMContentLoaded', function() {
    const urlInput = document.getElementById('posterUrlInput');
    const fileInput = document.getElementById('posterFileInput');
    const frame = document.getElementById('posterPreviewFrame');
    const posterFileName = document.getElementById('posterFileName');

    const trailerFileInput = document.getElementById('trailerFileInput');
    const trailerFileName = document.getElementById('trailerFileName');

    const movieForm = document.getElementById('movieForm');
    const btnSubmitMovie = document.getElementById('btnSubmitMovie');

    const trailerUrlInput = document.getElementById('trailerUrlInput');
    const trailerFrame = document.getElementById('trailerPreviewFrame');

    const statusSelect = document.querySelector('select[name="status"]');
    const releaseDateInput = document.querySelector('input[name="releaseDate"]');
    const endDateInput = document.querySelector('input[name="endDate"]');

    // Trạng thái ban đầu lấy từ dữ liệu phim trên server.
    const originalStatus = '<%= editing && movie.getStatus() != null ? movie.getStatus() : "" %>';

    // 1. LẤY NGÀY HIỆN TẠI THEO MÚI GIỜ LOCAL

    function getLocalToday() {
        const now = new Date();
        const offset = now.getTimezoneOffset() * 60000;

        return new Date(now.getTime() - offset)
            .toISOString()
            .split('T')[0];
    }

    // 2. POSTER PREVIEW

    function updatePosterPreview(src) {
        if (!src) {
            frame.innerHTML =
                '<div class="cf-poster-placeholder" id="posterPlaceholder">' +
                '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5">' +
                '<rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>' +
                '<circle cx="8.5" cy="8.5" r="1.5"></circle>' +
                '<polyline points="21 15 16 10 5 21"></polyline>' +
                '</svg>' +
                '<span>Chưa có ảnh</span></div>';
            return;
        }

        const img = document.createElement('img');
        img.id = 'posterPreviewImg';
        img.alt = 'Poster preview';
        img.src = src;
        img.onerror = handlePosterError;

        frame.replaceChildren(img);
    }

    function handlePosterError() {
        frame.innerHTML =
            '<div class="cf-poster-placeholder" style="color: var(--cf-danger);">' +
            '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5">' +
            '<circle cx="12" cy="12" r="10"></circle>' +
            '<line x1="12" y1="8" x2="12" y2="12"></line>' +
            '<line x1="12" y1="16" x2="12.01" y2="16"></line>' +
            '</svg>' +
            '<span>Lỗi tải ảnh</span></div>';
    }

    window.handlePosterError = handlePosterError;

    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];

            if (file) {
                const sizeKB = Math.round(file.size / 1024);
                const sizeDisplay = sizeKB > 1024
                    ? (sizeKB / 1024).toFixed(1) + ' MB'
                    : sizeKB + ' KB';

                if (posterFileName) {
                    posterFileName.style.display = 'flex';

                    const span = document.createElement('span');
                    span.textContent = file.name + ' (' + sizeDisplay + ')';

                    posterFileName.replaceChildren(span);
                }

                const reader = new FileReader();

                reader.onload = function(evt) {
                    updatePosterPreview(evt.target.result);
                };

                reader.readAsDataURL(file);
            } else if (posterFileName) {
                posterFileName.style.display = 'none';
            }
        });
    }

    // 3. TRAILER PREVIEW

    function extractYouTubeId(url) {
        if (!url) return null;

        url = url.trim();

        const regExp =
            /^.*(?:youtu\.be\/|v\/|u\/\w\/|embed\/|shorts\/|watch\?v=|\&v=)([^#\&\?]*).*/;

        const match = url.match(regExp);

        return (match && match[1].length === 11)
            ? match[1]
            : null;
    }

    function isDirectVideo(url) {
        return /\.(mp4|webm|ogg|m4v)(\?.*)?$/i.test(url);
    }

    function updateTrailerPreview(src, isLocalFile) {
        if (!trailerFrame) return;

        if (!src) {
            trailerFrame.innerHTML =
                '<div class="cf-trailer-placeholder" id="trailerPlaceholder">' +
                '<span>Xem trước trailer</span></div>';
            return;
        }

        if (isLocalFile) {
            const video = document.createElement('video');
            video.controls = true;
            video.playsInline = true;
            video.preload = 'metadata';
            video.style.cssText =
                'width:100%;height:100%;background:#000;display:block;';

            const source = document.createElement('source');
            source.src = src;

            video.appendChild(source);
            trailerFrame.replaceChildren(video);
            return;
        }

        const ytId = extractYouTubeId(src);

        if (ytId) {
            const iframe = document.createElement('iframe');

            iframe.src =
                'https://www.youtube-nocookie.com/embed/' +
                encodeURIComponent(ytId) + '?rel=0&enablejsapi=1';

            iframe.title = 'Trailer YouTube Preview';
            iframe.allowFullscreen = true;
            iframe.setAttribute(
                'allow',
                'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share'
            );

            iframe.style.cssText =
                'width:100%;height:100%;border:none;display:block;';

            trailerFrame.replaceChildren(iframe);
            return;
        }

        if (isDirectVideo(src) || src.startsWith('/') || src.startsWith('http')) {
            const video = document.createElement('video');
            video.controls = true;
            video.playsInline = true;
            video.preload = 'metadata';
            video.style.cssText =
                'width:100%;height:100%;background:#000;display:block;';

            const source = document.createElement('source');
            source.src = src;

            video.appendChild(source);
            trailerFrame.replaceChildren(video);
            return;
        }

        trailerFrame.innerHTML =
            '<div class="cf-trailer-placeholder">' +
            '<span>Xem trước trailer</span></div>';
    }

    // Khởi tạo trailer đã lưu.
    if (trailerUrlInput && trailerUrlInput.value.trim()) {
        updateTrailerPreview(trailerUrlInput.value.trim(), false);
    }

    if (trailerFileInput) {
        trailerFileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];

            if (file) {
                const sizeMB = (file.size / (1024 * 1024)).toFixed(1);

                if (trailerFileName) {
                    trailerFileName.style.display = 'flex';

                    const span = document.createElement('span');
                    span.textContent = file.name + ' (' + sizeMB + ' MB)';

                    trailerFileName.replaceChildren(span);
                }

                const objectUrl = URL.createObjectURL(file);
                updateTrailerPreview(objectUrl, true);
            } else {
                if (trailerFileName) {
                    trailerFileName.style.display = 'none';
                }

                if (trailerUrlInput && trailerUrlInput.value.trim()) {
                    updateTrailerPreview(trailerUrlInput.value.trim(), false);
                } else {
                    updateTrailerPreview('', false);
                }
            }
        });
    }

    // 4. NGHIỆP VỤ CHUYỂN TRẠNG THÁI PHIM

    if (statusSelect && endDateInput) {

        statusSelect.addEventListener('change', function() {
            const selectedStatus = this.value;
            const todayStr = getLocalToday();

            // A. ADMIN CHỦ ĐỘNG KẾT THÚC PHIM

            if (selectedStatus === 'ENDED' ||
                selectedStatus === 'ARCHIVED') {

                /*
                 * Nếu chưa có ngày kết thúc hoặc ngày kết thúc
                 * nằm trong tương lai thì chốt ngày hôm nay.
                 *
                 * Nếu ngày kết thúc đã ở quá khứ thì giữ nguyên
                 * để bảo toàn lịch sử phát hành.
                 */

                if (!endDateInput.value ||
                    endDateInput.value > todayStr) {

                    endDateInput.value = todayStr;
                }
            }

            // B. TÁI PHÁT HÀNH PHIM ĐÃ ENDED / ARCHIVED

            else if (
                (originalStatus === 'ARCHIVED' ||
                 originalStatus === 'ENDED') &&
                selectedStatus === 'COMING_SOON'
            ) {
                // Ngày cũ không còn phù hợp với đợt phát hành mới.
                if (releaseDateInput &&
                    releaseDateInput.value <= todayStr) {

                    releaseDateInput.value = '';
                }

                if (endDateInput.value <= todayStr) {
                    endDateInput.value = '';
                }
            }

            // C. GIA HẠN PHIM ĐÃ ENDED

            else if (
                originalStatus === 'ENDED' &&
                selectedStatus === 'NOW_SHOWING'
            ) {
                // Bắt buộc nhập ngày kết thúc mới.
                endDateInput.value = '';
                endDateInput.focus();
            }
        });
    }
    // 5. VALIDATION TRƯỚC KHI SUBMIT

    if (movieForm && btnSubmitMovie) {

        movieForm.addEventListener('submit', function(e) {

            const status = statusSelect ? statusSelect.value : '';

            const relVal = releaseDateInput
                ? releaseDateInput.value
                : '';

            const endVal = endDateInput
                ? endDateInput.value
                : '';

            const todayStr = getLocalToday();

            // A. KHÔNG CHO PHIM ARCHIVED CHUYỂN THẲNG NOW_SHOWING

            if (originalStatus === 'ARCHIVED' &&
                status === 'NOW_SHOWING') {

                alert(
                    'Phim đang ở trạng thái Lưu trữ (ARCHIVED). ' +
                    'Không thể chuyển trực tiếp sang "Đang chiếu" ' +
                    '(NOW SHOWING). Vui lòng chọn "Sắp khởi chiếu" ' +
                    'và thiết lập lại ngày chiếu mới.'
                );

                statusSelect.focus();
                e.preventDefault();
                return;
            }

            // B. ENDED -> NOW_SHOWING

            if (originalStatus === 'ENDED' &&
                status === 'NOW_SHOWING') {

                if (!endVal || endVal < todayStr) {

                    alert(
                        'Phim đã kết thúc đợt chiếu. Khi chuyển sang ' +
                        '"Đang chiếu" (NOW SHOWING), bạn bắt buộc phải ' +
                        'thiết lập Ngày kết thúc mới từ hôm nay trở đi.'
                    );

                    endDateInput.focus();
                    e.preventDefault();
                    return;
                }
            }

            // C. ENDED / ARCHIVED -> COMING_SOON

            if (
                (originalStatus === 'ARCHIVED' ||
                 originalStatus === 'ENDED') &&
                status === 'COMING_SOON'
            ) {

                if (
                    !relVal ||
                    !endVal ||
                    relVal < todayStr ||
                    endVal < relVal ||
                    endVal <= todayStr
                ) {

                    alert(
                        'Phim đang ở trạng thái ' +
                        (originalStatus === 'ARCHIVED'
                            ? 'Lưu trữ (ARCHIVED)'
                            : 'Đã kết thúc (ENDED)') +
                        '. Khi chuyển sang "Sắp khởi chiếu" ' +
                        '(COMING SOON), bạn bắt buộc phải thiết lập lại ' +
                        'Ngày khởi chiếu (trong tương lai) và Ngày kết thúc mới.'
                    );

                    releaseDateInput.focus();
                    e.preventDefault();
                    return;
                }
            }

            // D. BẮT BUỘC NHẬP ĐỦ NGÀY

            if (!relVal || !endVal) {

                alert(
                    'Vui lòng nhập đầy đủ Ngày khởi chiếu và Ngày kết thúc.'
                );

                if (!relVal && releaseDateInput) {
                    releaseDateInput.focus();
                } else if (endDateInput) {
                    endDateInput.focus();
                }

                e.preventDefault();
                return;
            }

            // E. KIỂM TRA KHOẢNG NGÀY

            if (endVal < relVal) {

                alert(
                    'Ngày kết thúc phải lớn hơn hoặc bằng ngày khởi chiếu.'
                );

                endDateInput.focus();
                e.preventDefault();
                return;
            }

            // F. COMING_SOON KHÔNG ĐƯỢC CÓ NGÀY KHỞI CHIẾU QUÁ KHỨ

            if (status === 'COMING_SOON' &&
                relVal < todayStr) {

                alert(
                    'Phim có ngày khởi chiếu trong quá khứ không thể đặt ' +
                    'trạng thái "Sắp khởi chiếu". Vui lòng chọn ngày ' +
                    'tương lai hoặc đổi sang "Đang chiếu".'
                );

                releaseDateInput.focus();
                e.preventDefault();
                return;
            }

           // G. NOW_SHOWING KHÔNG ĐƯỢC CÓ NGÀY KHỞI CHIẾU TƯƠNG LAI

            if (status === 'NOW_SHOWING' &&
                relVal > todayStr) {

                alert(
                    'Phim có ngày khởi chiếu ở tương lai chưa thể đặt ' +
                    'trạng thái "Đang chiếu". Vui lòng chọn trạng thái ' +
                    '"Sắp khởi chiếu".'
                );

                releaseDateInput.focus();
                e.preventDefault();
                return;
            }
            
            // H. SUBMIT THÀNH CÔNG VỀ MẶT VALIDATION CLIENT
            
            btnSubmitMovie.classList.add('is-submitting');
            btnSubmitMovie.textContent = 'Đang lưu phim...';
        });
    }
});
</script>
</body>
</html>
