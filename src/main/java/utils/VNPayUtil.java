package utils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cong thanh toan VNPay (sandbox), API v2.1.0.
 * Cau hinh trong src/main/resources/vnpay.properties (vnp.tmnCode, vnp.hashSecret),
 * hoac bien moi truong VNP_TMN_CODE / VNP_HASH_SECRET (uu tien hon file).
 */
public final class VNPayUtil {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    private static final String TMN_CODE;
    private static final String HASH_SECRET;
    private static final String PAY_URL;
    private static final String RETURN_URL;

    static {
        Properties props = new Properties();
        try (InputStream in = VNPayUtil.class.getClassLoader().getResourceAsStream("vnpay.properties")) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            // khong co file cau hinh -> isConfigured() = false
        }
        TMN_CODE = setting("VNP_TMN_CODE", props, "vnp.tmnCode");
        HASH_SECRET = setting("VNP_HASH_SECRET", props, "vnp.hashSecret");
        String payUrl = setting("VNP_PAY_URL", props, "vnp.payUrl");
        PAY_URL = payUrl.isEmpty() ? DEFAULT_PAY_URL : payUrl;
        RETURN_URL = setting("VNP_RETURN_URL", props, "vnp.returnUrl");
    }

    private VNPayUtil() {}

    private static String setting(String env, Properties props, String key) {
        String value = System.getenv(env);
        if (value == null || value.isBlank()) value = props.getProperty(key, "");
        return value.trim();
    }

    public static boolean isConfigured() {
        return !TMN_CODE.isEmpty() && !HASH_SECRET.isEmpty();
    }

    /**
     * Tao URL chuyen khach sang trang thanh toan VNPay.
     * @param txnRef ma giao dich duy nhat (Payments.TransactionCode)
     * @param expireSeconds so giay con lai de thanh toan (= thoi gian giu ghe con lai)
     */
    public static String buildPayUrl(HttpServletRequest req, String txnRef, BigDecimal amount, String orderInfo,
            long expireSeconds) {
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", TMN_CODE);
        params.put("vnp_Amount", toVnpAmount(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl(req));
        params.put("vnp_IpAddr", clientIp(req));
        params.put("vnp_CreateDate", now.format(VNP_TIME));
        params.put("vnp_ExpireDate", now.plusSeconds(Math.max(expireSeconds, 60)).format(VNP_TIME));

        String query = encode(params);
        return PAY_URL + "?" + query + "&vnp_SecureHash=" + hmacSha512(HASH_SECRET, query);
    }

    /** Kiem tra chu ky du lieu VNPay gui ve (return URL / IPN). */
    public static boolean verify(Map<String, String> vnpParams) {
        String received = vnpParams.get("vnp_SecureHash");
        if (received == null || !isConfigured()) return false;
        Map<String, String> data = new TreeMap<>(vnpParams);
        data.remove("vnp_SecureHash");
        data.remove("vnp_SecureHashType");
        byte[] expected = hmacSha512(HASH_SECRET, encode(data)).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, received.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    /** Lay cac tham so vnp_* tu request. */
    public static Map<String, String> vnpParams(HttpServletRequest req) {
        Map<String, String> params = new TreeMap<>();
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            String value = e.getValue().length == 0 ? "" : e.getValue()[0];
            if (e.getKey().startsWith("vnp_") && !value.isEmpty()) params.put(e.getKey(), value);
        }
        return params;
    }

    /** Giao dich thanh cong: ma phan hoi 00 va trang thai giao dich 00. */
    public static boolean isSuccess(Map<String, String> vnpParams) {
        return "00".equals(vnpParams.get("vnp_ResponseCode")) && "00".equals(vnpParams.get("vnp_TransactionStatus"));
    }

    /** VNPay gui so tien x100. */
    public static String toVnpAmount(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** Ly do that bai theo vnp_ResponseCode, hien cho khach. */
    public static String failureMessage(String responseCode) {
        if (responseCode == null) return "Giao dịch không thành công.";
        return switch (responseCode) {
            case "24" -> "Bạn đã huỷ giao dịch.";
            case "11" -> "Đã hết thời gian chờ thanh toán.";
            case "51" -> "Tài khoản không đủ số dư.";
            case "65" -> "Tài khoản đã vượt hạn mức giao dịch trong ngày.";
            case "12" -> "Thẻ/tài khoản đang bị khoá.";
            case "13", "79" -> "Nhập sai mật khẩu xác thực (OTP) quá số lần quy định.";
            case "09" -> "Thẻ/tài khoản chưa đăng ký dịch vụ InternetBanking.";
            case "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì.";
            default -> "Giao dịch không thành công (mã " + responseCode + ").";
        };
    }

    private static String returnUrl(HttpServletRequest req) {
        if (!RETURN_URL.isEmpty()) return RETURN_URL;
        StringBuilder url = new StringBuilder(req.getScheme()).append("://").append(req.getServerName());
        int port = req.getServerPort();
        boolean defaultPort = ("http".equals(req.getScheme()) && port == 80) || ("https".equals(req.getScheme()) && port == 443);
        if (!defaultPort) url.append(':').append(port);
        return url.append(req.getContextPath()).append("/payment/vnpay-return").toString();
    }

    private static String clientIp(HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        // VNPay chi nhan IPv4; localhost IPv6 (0:0:0:0:0:0:0:1) -> 127.0.0.1
        return ip == null || ip.contains(":") ? "127.0.0.1" : ip;
    }

    /** key=value&... da sap xep theo key, gia tri ma hoa URL (US-ASCII) dung nhu code mau cua VNPay. */
    private static String encode(Map<String, String> sorted) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII)).append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
