package utils;

import java.security.SecureRandom;

/** Sinh ma ngau nhien cho don dat ve, don bap nuoc, ve (bo cac ky tu de nham 0/O, 1/I). */
public final class CodeUtil {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeUtil() {}

    public static String randomCode(String prefix, int length) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}
