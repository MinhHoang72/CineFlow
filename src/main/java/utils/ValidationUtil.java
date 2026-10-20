package utils;

import java.sql.Date;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lớp tiện ích kiểm tra tính hợp lệ của dữ liệu (Validation) trên toàn bộ hệ thống CineFlow.
 * Tập trung các quy tắc nghiệp vụ về định dạng email, số điện thoại, mật khẩu, tên tài khoản,
 * khoảng thời gian, tệp tin và các trường danh mục chuẩn.
 */
public final class ValidationUtil {

    // Regex kiểm tra định dạng email chuẩn RFC 5322 cơ bản
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    // Regex kiểm tra số điện thoại di động Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09 hoặc +84)
    private static final Pattern VN_PHONE_PATTERN = Pattern.compile(
            "^(0|\\+84)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$"
    );

    // Regex kiểm tra tên tài khoản: 3 - 30 ký tự, gồm chữ, số, gạch dưới, gạch ngang, dấu chấm
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]{3,30}$"
    );

    // Regex độ mạnh mật khẩu: tối thiểu 8 ký tự, có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt
    private static final Pattern PASSWORD_STRENGTH_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"
    );

    // Danh sách các nhà cung cấp email tạm thời (disposable / temp mail) cần chặn
    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = new HashSet<>(Arrays.asList(
            "tempmail.com", "10minutemail.com", "guerrillamail.com", "mailinator.com",
            "throwawaymail.com", "trashmail.com", "sharklasers.com", "yopmail.com",
            "getnada.com", "dispostable.com"
    ));

    // Bộ phân loại độ tuổi xem phim hợp lệ tại rạp
    private static final Set<String> VALID_AGE_RATINGS = new HashSet<>(Arrays.asList(
            "P", "K", "T13", "T16", "T18", "C"
    ));

    // Bộ trạng thái vòng đời phim hợp lệ
    private static final Set<String> VALID_MOVIE_STATUSES = new HashSet<>(Arrays.asList(
            "COMING_SOON", "NOW_SHOWING", "ENDED", "ARCHIVED"
    ));

    // Bộ trạng thái tài khoản người dùng hợp lệ
    private static final Set<String> VALID_USER_STATUSES = new HashSet<>(Arrays.asList(
            "ACTIVE", "INACTIVE", "LOCKED"
    ));

    // Bộ vai trò người dùng trong hệ thống
    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList(
            "ADMIN", "MANAGER", "STAFF", "CUSTOMER",
            "TICKET_SALES_STAFF", "TICKET_CHECKER_STAFF",
            "TICKET_SALES", "TICKET_CHECKER"
    ));

    private ValidationUtil() {
        // Lớp tiện ích không khởi tạo đối tượng
    }

    /**
     * Kiểm tra chuỗi có rỗng hoặc chỉ chứa khoảng trắng hay không.
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Kiểm tra định dạng địa chỉ email hợp lệ.
     */
    public static boolean isValidEmail(String email) {
        if (isBlank(email)) {
            return false;
        }
        String trimmed = email.trim();
        return trimmed.length() <= 100 && EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Kiểm tra xem địa chỉ email có thuộc các dịch vụ email rác, email tạm thời hay không.
     */
    public static boolean isDisposableEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase().trim();
        return DISPOSABLE_EMAIL_DOMAINS.contains(domain);
    }

    /**
     * Kiểm tra số điện thoại di động Việt Nam hợp lệ (có thể để trống nếu không bắt buộc).
     *
     * @param phone số điện thoại cần kiểm tra
     * @param required bắt buộc hay không
     */
    public static boolean isValidPhone(String phone, boolean required) {
        if (isBlank(phone)) {
            return !required;
        }
        String cleanPhone = phone.trim().replaceAll("[\\s.-]", "");
        return VN_PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Kiểm tra tên tài khoản (username) hợp lệ: 3-30 ký tự, không chứa ký tự đặc biệt nguy hiểm.
     */
    public static boolean isValidAccount(String account) {
        if (isBlank(account)) {
            return false;
        }
        return ACCOUNT_PATTERN.matcher(account.trim()).matches();
    }

    /**
     * Kiểm tra độ mạnh của mật khẩu (tối thiểu 8 ký tự, có đủ chữ hoa, thường, số và ký tự đặc biệt).
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_STRENGTH_PATTERN.matcher(password).matches();
    }

    /**
     * Kiểm tra họ và tên hợp lệ (độ dài 2 - 100 ký tự).
     */
    public static boolean isValidFullName(String fullName) {
        if (isBlank(fullName)) {
            return false;
        }
        String trimmed = fullName.trim();
        return trimmed.length() >= 2 && trimmed.length() <= 100;
    }

    /**
     * Kiểm tra khoảng thời gian hợp lệ: Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.
     */
    public static boolean isValidDateRange(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.before(startDate);
    }

    /**
     * Kiểm tra thời lượng phim (phút): từ 1 đến 600 phút.
     */
    public static boolean isValidDuration(int duration) {
        return duration > 0 && duration <= 600;
    }

    /**
     * Kiểm tra nhãn độ tuổi xem phim có thuộc danh mục hợp lệ hay không.
     */
    public static boolean isValidAgeRating(String ageRating) {
        if (isBlank(ageRating)) {
            return false;
        }
        return VALID_AGE_RATINGS.contains(ageRating.trim().toUpperCase());
    }

    /**
     * Kiểm tra trạng thái phát hành phim có hợp lệ hay không.
     */
    public static boolean isValidMovieStatus(String status) {
        if (isBlank(status)) {
            return false;
        }
        return VALID_MOVIE_STATUSES.contains(status.trim().toUpperCase());
    }

    /**
     * Kiểm tra tính nhất quán giữa trạng thái phát hành phim và ngày khởi chiếu.
     * - COMING_SOON: Ngày khởi chiếu không được trước ngày hôm nay.
     * - NOW_SHOWING: Ngày khởi chiếu không được sau ngày hôm nay.
     *
     * @param status trạng thái phim (COMING_SOON, NOW_SHOWING, ENDED, ARCHIVED)
     * @param releaseDate ngày khởi chiếu
     * @param today ngày hiện tại
     * @return null nếu hợp lệ, hoặc mã lỗi ("coming_soon_in_past", "now_showing_in_future")
     */
    public static String validateMovieStatusDateConsistency(String status, Date releaseDate, Date today) {
        if (isBlank(status) || releaseDate == null || today == null) {
            return null;
        }
        String st = status.trim().toUpperCase();
        if ("COMING_SOON".equals(st) && releaseDate.before(today)) {
            return "coming_soon_in_past";
        }
        if ("NOW_SHOWING".equals(st) && releaseDate.after(today)) {
            return "now_showing_in_future";
        }
        return null;
    }

    /**
     * Kiểm tra trạng thái tài khoản người dùng có hợp lệ hay không.
     */
    public static boolean isValidUserStatus(String status) {
        if (isBlank(status)) {
            return false;
        }
        return VALID_USER_STATUSES.contains(status.trim().toUpperCase());
    }

    /**
     * Kiểm tra vai trò người dùng có hợp lệ hay không.
     */
    public static boolean isValidRole(String role) {
        if (isBlank(role)) {
            return false;
        }
        return VALID_ROLES.contains(role.trim().toUpperCase());
    }

    /**
     * Kiểm tra vai trò có thuộc nhóm nhân sự rạp (STAFF, TICKET_SALES_STAFF, TICKET_CHECKER_STAFF) hay không.
     */
    public static boolean isStaffRole(String role) {
        if (isBlank(role)) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return "STAFF".equals(r) || "TICKET_SALES_STAFF".equals(r)
                || "TICKET_CHECKER_STAFF".equals(r) || "TICKET_SALES".equals(r)
                || "TICKET_CHECKER".equals(r);
    }

    /**
     * Kiểm tra định dạng đuôi mở rộng của tệp ảnh tải lên.
     */
    public static boolean isValidImageExtension(String filename) {
        if (isBlank(filename)) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp");
    }

    /**
     * Kiểm tra định dạng đuôi mở rộng của tệp video/trailer tải lên.
     */
    public static boolean isValidVideoExtension(String filename) {
        if (isBlank(filename)) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".ogg");
    }

    /**
     * Làm sạch ký tự HTML để phòng ngừa tấn công Cross-Site Scripting (XSS).
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
