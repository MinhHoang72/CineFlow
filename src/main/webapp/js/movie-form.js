/**
 * Điều khiển giao diện biểu mẫu thêm mới / chỉnh sửa phim (movieForm.jsp).
 * Chức năng chính:
 * 1. Xem trước ảnh Poster ngay khi người dùng chọn tệp từ máy tính.
 * 2. Xem trước video Trailer từ tệp video tải lên từ máy tính.
 * 3. Tự động điều chỉnh Ngày khởi chiếu / Ngày kết thúc khi đổi trạng thái phim.
 * 4. Kiểm tra hợp lệ dữ liệu phía client và hiện hộp thoại xác nhận khi kết thúc phim có suất chiếu.
 */
document.addEventListener('DOMContentLoaded', function () {
    // Lấy tham chiếu các phần tử DOM trên biểu mẫu
    const posterInput = document.getElementById('posterFileInput');
    const posterFrame = document.getElementById('posterPreviewFrame');
    const posterFileName = document.getElementById('posterFileName');

    const trailerInput = document.getElementById('trailerFileInput');
    const trailerFileName = document.getElementById('trailerFileName');
    const trailerUrlInput = document.getElementById('trailerUrlInput');
    const trailerFrame = document.getElementById('trailerPreviewFrame');

    const movieForm = document.getElementById('movieForm');
    const btnSubmit = document.getElementById('btnSubmitMovie');
    const statusSelect = document.querySelector('select[name="status"]');
    const releaseInput = document.querySelector('input[name="releaseDate"]');
    const endInput = document.querySelector('input[name="endDate"]');

    // Đọc dữ liệu ngữ cảnh do movieForm.jsp gắn lên thẻ <form>
    const originalStatus = movieForm ? (movieForm.dataset.originalStatus || '') : '';
    const hasUpcomingShowtimes = movieForm ? (movieForm.dataset.hasUpcomingShowtimes === 'true') : false;

    /**
     * Lấy ngày hiện tại theo múi giờ cục bộ dưới định dạng chuẩn input[type=date] (yyyy-MM-dd).
     *
     * @returns {string} chuỗi ngày hôm nay (ví dụ: "2026-11-19")
     */
    function getLocalToday() {
        const now = new Date();
        return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().split('T')[0];
    }

    // ==============================================================
    // 1. XEM TRƯỚC ẢNH POSTER (Đọc tệp bằng FileReader)
    // ==============================================================
    if (posterInput && posterFrame) {
        posterInput.addEventListener('change', function () {
            const file = this.files[0];
            if (!file) {
                if (posterFileName) posterFileName.style.display = 'none';
                return;
            }
            // Hiển thị tên tệp và dung lượng ảnh đã chọn
            if (posterFileName) {
                const kb = Math.round(file.size / 1024);
                posterFileName.style.display = 'flex';
                posterFileName.textContent = file.name + ' (' + (kb > 1024 ? (kb / 1024).toFixed(1) + ' MB' : kb + ' KB') + ')';
            }
            // Chuyển tệp ảnh sang DataURL để hiển thị xem trước mà chưa cần upload lên Server
            const reader = new FileReader();
            reader.onload = (evt) => {
                const img = document.createElement('img');
                img.alt = 'Poster preview';
                img.src = evt.target.result;
                img.onerror = () => { posterFrame.innerHTML = '<div class="cf-poster-placeholder" style="color:#ef4444;"><span>Lỗi tải ảnh</span></div>'; };
                posterFrame.replaceChildren(img);
            };
            reader.readAsDataURL(file);
        });
    }

    // ==============================================================
    // 2. XEM TRƯỚC VIDEO TRAILER (Tệp video tải lên từ máy tính)
    // ==============================================================

    /**
     * Hiển thị khung xem trước Trailer bằng thẻ <video> từ tệp tải lên hoặc đường dẫn lưu trữ.
     *
     * @param {string} src đường dẫn video hoặc ObjectURL của tệp vừa chọn
     */
    function updateTrailerPreview(src) {
        if (!trailerFrame) return;
        if (!src) {
            trailerFrame.innerHTML = '<div class="cf-trailer-placeholder"><span>Xem trước trailer</span></div>';
            return;
        }
        const video = document.createElement('video');
        video.controls = true;
        video.playsInline = true;
        video.preload = 'metadata';
        video.style.cssText = 'width:100%;height:100%;background:#000;display:block;';
        video.src = src;
        trailerFrame.replaceChildren(video);
    }

    // Nếu phim đã có sẵn tệp Trailer (chế độ chỉnh sửa) -> tự động phát xem trước
    if (trailerUrlInput && trailerUrlInput.value.trim()) {
        updateTrailerPreview(trailerUrlInput.value.trim());
    }

    if (trailerInput) {
        trailerInput.addEventListener('change', function () {
            const file = this.files[0];
            if (file) {
                if (trailerFileName) {
                    trailerFileName.style.display = 'flex';
                    trailerFileName.textContent = file.name + ' (' + (file.size / (1024 * 1024)).toFixed(1) + ' MB)';
                }
                // Tạo ObjectURL tạm trong trình duyệt để phát thử video ngay lập tức
                updateTrailerPreview(URL.createObjectURL(file));
            } else {
                if (trailerFileName) trailerFileName.style.display = 'none';
                updateTrailerPreview(trailerUrlInput ? trailerUrlInput.value.trim() : '');
            }
        });
    }

    // ==============================================================
    // 3. TỰ ĐỘNG ĐIỀU CHỈNH NGÀY KHI ĐỔI TRẠNG THÁI PHIM
    // ==============================================================
    if (statusSelect && endInput) {
        statusSelect.addEventListener('change', function () {
            const nextStatus = this.value;
            const today = getLocalToday();
            const relVal = releaseInput ? releaseInput.value : '';

            if (nextStatus === 'ENDED' || nextStatus === 'ARCHIVED') {
                // Nếu phim chưa tới ngày khởi chiếu (COMING_SOON bị cấm/hủy sớm):
                // chốt EndDate = ReleaseDate để luôn đảm bảo EndDate >= ReleaseDate; ngược lại chốt = hôm nay.
                const targetEnd = (relVal && relVal > today) ? relVal : today;
                if (!endInput.value || endInput.value > targetEnd) endInput.value = targetEnd;
            } else if ((originalStatus === 'ARCHIVED' || originalStatus === 'ENDED') && nextStatus === 'COMING_SOON') {
                // Tái phát hành sang Sắp chiếu -> xóa các ngày cũ trong quá khứ để nhắc Admin nhập lịch mới
                if (releaseInput && releaseInput.value <= today) releaseInput.value = '';
                if (endInput.value <= today) endInput.value = '';
            } else if (originalStatus === 'ENDED' && nextStatus === 'NOW_SHOWING') {
                // Gia hạn phim đã kết thúc sang Đang chiếu -> yêu cầu nhập lại Ngày kết thúc mới
                endInput.value = '';
                endInput.focus();
            }
        });
    }

    // ==============================================================
    // 4. KIỂM TRA HỢP LỆ DỮ LIỆU & HỘP THOẠI XÁC NHẬN KHI SUBMIT
    // ==============================================================

    /** Báo lỗi, đưa con trỏ về ô nhập sai và chặn gửi biểu mẫu lên Server. */
    function fail(e, msg, el) {
        alert(msg);
        if (el) el.focus();
        e.preventDefault();
    }

    if (movieForm && btnSubmit) {
        movieForm.addEventListener('submit', function (e) {
            const status = statusSelect ? statusSelect.value : '';
            const relVal = releaseInput ? releaseInput.value : '';
            const endVal = endInput ? endInput.value : '';
            const today = getLocalToday();

            // Kiểm tra quy tắc vòng đời chuyển trạng thái (khớp với MovieService.java ở Backend)
            if (originalStatus === 'ARCHIVED' && status === 'NOW_SHOWING') {
                return fail(e, 'Phim đã lưu trữ không thể chuyển trực tiếp sang "Đang chiếu". Vui lòng chọn "Sắp khởi chiếu" và thiết lập lại ngày chiếu mới.', statusSelect);
            }
            if (originalStatus === 'ENDED' && status === 'NOW_SHOWING' && (!endVal || endVal < today)) {
                return fail(e, 'Khi chuyển phim đã kết thúc sang "Đang chiếu", bắt buộc phải thiết lập Ngày kết thúc mới từ hôm nay trở đi.', endInput);
            }
            if ((originalStatus === 'ARCHIVED' || originalStatus === 'ENDED') && status === 'COMING_SOON'
                    && (!relVal || !endVal || relVal < today || endVal < relVal || endVal <= today)) {
                return fail(e, 'Khi tái phát hành sang "Sắp khởi chiếu", bắt buộc nhập lại Ngày khởi chiếu (tương lai) và Ngày kết thúc mới.', releaseInput);
            }

            // Kiểm tra ngày khởi chiếu / kết thúc cơ bản
            if (!relVal || !endVal) {
                return fail(e, 'Vui lòng nhập đầy đủ Ngày khởi chiếu và Ngày kết thúc.', !relVal ? releaseInput : endInput);
            }
            if (endVal < relVal) {
                return fail(e, 'Ngày kết thúc phải lớn hơn hoặc bằng ngày khởi chiếu.', endInput);
            }
            if (status === 'COMING_SOON' && relVal < today) {
                return fail(e, 'Phim có ngày khởi chiếu trong quá khứ không thể đặt trạng thái "Sắp khởi chiếu".', releaseInput);
            }
            if (status === 'NOW_SHOWING' && relVal > today) {
                return fail(e, 'Phim có ngày khởi chiếu ở tương lai chưa thể đặt trạng thái "Đang chiếu".', releaseInput);
            }

            // Rẽ nhánh xác nhận khi Admin kết thúc/lưu trữ phim đang còn suất chiếu tương lai (hoặc vé đã bán)
            if ((status === 'ENDED' || status === 'ARCHIVED') && originalStatus !== 'ENDED' && originalStatus !== 'ARCHIVED' && hasUpcomingShowtimes) {
                if (!window.confirm('Kết thúc phim trước thời hạn sẽ hủy các suất chiếu và các vé đã bán. Bạn có chắc chắn muốn tiếp tục?')) {
                    e.preventDefault();
                    return;
                }
            }

            // Khóa giao diện nút Lưu để tránh người dùng bấm nhiều lần khi đang upload Poster/Trailer
            btnSubmit.classList.add('is-submitting');
            btnSubmit.textContent = 'Đang lưu phim...';
        });
    }
});
