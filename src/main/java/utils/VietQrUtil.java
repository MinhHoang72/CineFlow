package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Sinh noi dung ma VietQR (chuan EMVCo cua NAPAS) chuyen khoan vao tai khoan trong bank.properties,
 * dien san so tien va noi dung chuyen khoan. Quet duoc bang moi app ngan hang Viet Nam.
 */
public class VietQrUtil {

    private static final Properties CONFIG = loadConfig();

    private static Properties loadConfig() {
        Properties p = new Properties();
        try (InputStream in = VietQrUtil.class.getClassLoader().getResourceAsStream("bank.properties")) {
            if (in != null) p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được bank.properties", e);
        }
        return p;
    }

    public static String bankName() { return CONFIG.getProperty("bank.name", "").trim(); }
    public static String accountNo() { return CONFIG.getProperty("bank.accountNo", "").trim(); }
    public static String accountName() { return CONFIG.getProperty("bank.accountName", "").trim(); }
    private static String bin() { return CONFIG.getProperty("bank.bin", "").trim(); }

    public static boolean isConfigured() {
        return !bin().isEmpty() && !accountNo().isEmpty();
    }

    /** @param amount so tien VND (so nguyen), @param memo noi dung chuyen khoan (chu va so, khong dau) */
    public static String buildPayload(long amount, String memo) {
        String beneficiary = field("00", bin()) + field("01", accountNo());
        String merchantInfo = field("00", "A000000727") + field("01", beneficiary) + field("02", "QRIBFTTA");
        String payload = field("00", "01")
                + field("01", "12")
                + field("38", merchantInfo)
                + field("53", "704")
                + field("54", String.valueOf(amount))
                + field("58", "VN")
                + field("62", field("08", memo))
                + "6304";
        return payload + crc16(payload);
    }

    private static String field(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    /** CRC-16/CCITT-FALSE theo yeu cau cua EMVCo. */
    private static String crc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
