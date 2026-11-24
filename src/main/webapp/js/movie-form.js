/**
 * Điều khiển giao diện biểu mẫu thêm mới / chỉnh sửa phim (movieForm.jsp).
 * Chức năng chính:
 * 1. Xem trước ảnh Poster ngay khi người dùng chọn tệp từ máy tính.
 * 2. Xem trước video Trailer từ tệp video tải lên từ máy tính.
 * 3. Hộp thoại xác nhận khi kết thúc phim có suất chiếu và khóa nút khi gửi biểu mẫu.
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

    // Đọc dữ liệu ngữ cảnh do movieForm.jsp gắn lên thẻ <form>
    const originalStatus = movieForm ? (movieForm.dataset.originalStatus || '') : '';
    const hasUpcomingShowtimes = movieForm ? (movieForm.dataset.hasUpcomingShowtimes === 'true') : false;
    const upcomingCount = movieForm ? parseInt(movieForm.dataset.upcomingCount || '0', 10) : 0;
    const soldCount = movieForm ? parseInt(movieForm.dataset.soldCount || '0', 10) : 0;

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
    // 3. HỘP THOẠI XÁC NHẬN KHI KẾT THÚC PHIM CÓ SUẤT CHIẾU & KHÓA NÚT SUBMIT
    // ==============================================================
    if (movieForm && btnSubmit) {
        movieForm.addEventListener('submit', function (e) {
            const status = statusSelect ? statusSelect.value : '';

            // Rẽ nhánh xác nhận khi Admin kết thúc/lưu trữ phim đang còn suất chiếu tương lai
            if ((status === 'ENDED' || status === 'ARCHIVED') && originalStatus !== 'ENDED' && originalStatus !== 'ARCHIVED' && hasUpcomingShowtimes) {
                let confirmMsg = '';
                if (soldCount > 0) {
                    confirmMsg = '⛔ CẢNH BÁO NGUY HIỂM:\n\n'
                               + 'Phim này đang có ' + upcomingCount + ' suất chiếu sắp tới và ĐÃ BÁN ' + soldCount + ' VÉ cho khách hàng!\n\n'
                               + 'Kết thúc phim trước thời hạn sẽ tự động HỦY toàn bộ suất chiếu và vé đã bán của khách.\n'
                               + 'Bạn có chắc chắn 100% muốn tiếp tục không?';
                } else {
                    confirmMsg = '⚠️ LƯU Ý:\n\n'
                               + 'Phim này đang có ' + upcomingCount + ' suất chiếu sắp tới (chưa có khách nào đặt vé).\n\n'
                               + 'Bạn có chắc chắn muốn kết thúc phim không?';
                }
                if (!window.confirm(confirmMsg)) {
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
