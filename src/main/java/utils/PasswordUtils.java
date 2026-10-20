package utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích mã hóa và kiểm tra mật khẩu sử dụng thuật toán BCrypt.
 */
public class PasswordUtils {

    /**
     * Băm mật khẩu dạng văn bản thuần sang chuỗi mã hóa BCrypt an toàn.
     *
     * @param password mật khẩu văn bản thuần
     * @return chuỗi mật khẩu đã được băm
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    /**
     * Kiểm tra tính khớp giữa mật khẩu nhập vào và chuỗi mật khẩu đã băm.
     *
     * @param password mật khẩu văn bản thuần do người dùng nhập
     * @param hashedPassword chuỗi băm lưu trữ trong cơ sở dữ liệu
     * @return true nếu mật khẩu chính xác, ngược lại false
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}