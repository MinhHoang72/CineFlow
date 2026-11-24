package utils;

import java.security.SecureRandom;

/** Sinh ma ngau nhien cho don dat ve, don bap nuoc, ve (bo cac ky tu de nham 0/O, 1/I). */
public final class CodeUtil {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Lớp tiện ích chỉ có hàm static, không tạo đối tượng. */
    private CodeUtil() {}

    /**
     * Sinh mã ngẫu nhiên = tiền tố + chuỗi chữ hoa và số, ví dụ mã đơn "CF" + 10 ký tự, mã vé "TK" + 10 ký tự.
     */
    public static String randomCode(String prefix, int length) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}
