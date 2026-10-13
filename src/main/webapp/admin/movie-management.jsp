<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Movie" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/movie-form.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/admin/sidebar.jsp" />

    <div class="cf-manager-main">
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <!-- PAGE HEADER -->
            <div class="cf-page-header">
                <div>
                    <p class="cf-breadcrumb">Quản trị nội dung / <%= editing ? "Chỉnh sửa phim" : "Thêm mới" %></p>
                    <h1><%= editing ? "Chỉnh sửa thông tin phim" : "Thêm phim mới vào hệ thống" %></h1>
                    <p><%= editing ? "Cập nhật dữ liệu phim, áp phích và tình trạng phân phối chiếu." : "Nhập đầy đủ thông tin chi tiết để thêm phim mới vào cơ sở dữ liệu CineFlow." %></p>
                </div>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-secondary">
                        Danh mục phim
                    </a>
                    <% if (editing) { %>
                        <a href="${pageContext.request.contextPath}/admin/distribution?movieId=<%= movie.getMovieId() %>" class="cf-btn cf-btn-primary">
                        Phân phối rạp chiếu
                        </a>
                    <% } %>
                </div>
            </div>

            <!-- MAIN FORM -->
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
                                <input class="cf-form-control" name="genre" value="<%= editing ? esc(movie.getGenre()) : "" %>" placeholder="Ví dụ: Hành động, Tình cảm">
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
                                    <option value="COMING_SOON" <%= "COMING_SOON".equals(status) ? "selected" : "" %>>Sắp chiếu (COMING_SOON)</option>
                                    <option value="NOW_SHOWING" <%= "NOW_SHOWING".equals(status) ? "selected" : "" %>>Đang chiếu (NOW_SHOWING)</option>
                                    <option value="ENDED" <%= "ENDED".equals(status) ? "selected" : "" %>>Đã ngừng chiếu (ENDED)</option>
                                    <option value="ARCHIVED" <%= "ARCHIVED".equals(status) ? "selected" : "" %>>Lưu trữ kho phim (ARCHIVED)</option>
                                </select>
                                <p style="margin: 6px 0 0; font-size: 12px; color: var(--cf-muted); line-height: 1.4;">
                                    💡 <strong>Vòng đời phim:</strong> Chọn "Đã ngừng chiếu" khi phim hết đợt chiếu hoặc "Lưu trữ" để ẩn khỏi hệ thống bán vé mà vẫn bảo toàn đầy đủ dữ liệu lịch sử đặt vé và báo cáo doanh thu.
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
                                <label class="cf-form-label">Ngày khởi chiếu</label>
                                <input class="cf-form-control" name="releaseDate" type="date" value="<%= editing && movie.getReleaseDate() != null ? movie.getReleaseDate() : "" %>">
                            </div>

                            <div class="cf-form-group">
                                <label class="cf-form-label">Ngày kết thúc dự kiến</label>
                                <input class="cf-form-control" name="endDate" type="date" value="<%= editing && movie.getEndDate() != null ? movie.getEndDate() : "" %>">
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
                                        <label class="cf-file-picker-btn">
                                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                                            <span>Chọn tệp ảnh...</span>
                                            <input type="file" name="posterFile" id="posterFileInput" accept="image/png,image/jpeg,image/webp" style="display: none;">
                                        </label>
                                        <span class="cf-file-name-hint" id="posterFileName"></span>

                                        <div class="cf-field-group-compact">
                                            <label>Hoặc nhập URL ảnh:</label>
                                            <input type="text" name="posterUrl" id="posterUrlInput" value="<%= esc(posterUrl) %>" placeholder="https://..." class="cf-input-sm">
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- TRAILER GIỚI THIỆU -->
                        <div class="cf-side-card" style="margin-top: 18px;">
                            <div class="cf-side-header">
                                <span class="cf-side-title">
                                    <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"></polygon><rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect></svg>
                                    Trailer giới thiệu
                                </span>
                            </div>
                            <div class="cf-side-body">
                                <div class="cf-trailer-compact-wrap">
                                    <div class="cf-trailer-file-row">
                                        <label class="cf-file-picker-btn">
                                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                                            <span>Tải video trailer...</span>
                                            <input type="file" name="trailerFile" id="trailerFileInput" accept="video/mp4,video/webm" style="display: none;">
                                        </label>
                                        <span class="cf-file-name-hint" id="trailerFileName"></span>
                                    </div>

                                    <div class="cf-field-group-compact" style="margin-top: 6px;">
                                        <label>Hoặc đường dẫn YouTube / Video URL:</label>
                                        <input type="text" name="trailerUrl" value="<%= esc(trailerUrl) %>" placeholder="https://youtube.com/..." class="cf-input-sm">
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

<!-- CLIENT JAVASCRIPT CONTROLLER -->
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

    function updatePosterPreview(src) {
        if (!src) {
            frame.innerHTML = '<div class="cf-poster-placeholder" id="posterPlaceholder">' +
                '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>' +
                '<span>Chưa có ảnh</span></div>';
            return;
        }
        frame.innerHTML = '<img src="' + src + '" alt="Poster preview" id="posterPreviewImg" onerror="handlePosterError()">';
    }

    window.handlePosterError = function() {
        frame.innerHTML = '<div class="cf-poster-placeholder" style="color: var(--cf-danger);">' +
            '<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>' +
            '<span>Lỗi tải ảnh</span></div>';
    };

    if (urlInput) {
        urlInput.addEventListener('input', function() {
            const val = this.value.trim();
            if (val) updatePosterPreview(val);
            else if (!fileInput || !fileInput.files.length) updatePosterPreview('');
        });
    }

    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                if (posterFileName) {
                    posterFileName.style.display = 'block';
                    posterFileName.textContent = file.name;
                }
                const reader = new FileReader();
                reader.onload = function(evt) {
                    updatePosterPreview(evt.target.result);
                };
                reader.readAsDataURL(file);
            } else {
                if (posterFileName) posterFileName.style.display = 'none';
            }
        });
    }

    if (trailerFileInput) {
        trailerFileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file && trailerFileName) {
                trailerFileName.style.display = 'block';
                trailerFileName.textContent = file.name;
            } else if (trailerFileName) {
                trailerFileName.style.display = 'none';
            }
        });
    }

    if (movieForm && btnSubmitMovie) {
        movieForm.addEventListener('submit', function() {
            btnSubmitMovie.classList.add('is-submitting');
            btnSubmitMovie.textContent = 'Đang lưu phim...';
        });
    }
});
</script>
</body>
</html>
