package utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Tiện ích gửi thư điện tử (Email) thông báo OTP qua giao thức SMTP Gmail.
 */
public class EmailUtils {

    // Địa chỉ email hệ thống sử dụng để gửi OTP
    private static final String FROM_EMAIL = "duongquyetthai@gmail.com";

    // Mật khẩu ứng dụng (App Password) của Gmail
    private static final String APP_PASSWORD = "egwocrjeufohujlz";

    /**
     * Gửi mã OTP xác thực đặt lại mật khẩu cho người dùng.
     *
     * @param toEmail địa chỉ email nhận mã
     * @param otp mã OTP gồm 6 chữ số
     * @throws Exception khi xảy ra lỗi trong quá trình kết nối hoặc gửi email
     */
    public static void sendResetPasswordOtpEmail(String toEmail, String otp) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("CineFlow - Mã xác thực đặt lại mật khẩu");

        message.setContent(
                "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='margin:0; padding:0; background-color:#f4f4f4; font-family:Arial,sans-serif;'>"
                + "<div style='max-width:600px; margin:40px auto; background:white; "
                + "border-radius:12px; overflow:hidden; box-shadow:0 4px 15px rgba(0,0,0,0.1);'>"
                + "<div style='background:#e50914; padding:25px; text-align:center;'>"
                + "<h1 style='margin:0; color:white; font-size:28px;'>CineFlow</h1>"
                + "<p style='margin:8px 0 0; color:white; font-size:14px;'>Hệ thống Quản lý Rạp chiếu phim Hiện đại</p>"
                + "</div>"
                + "<div style='padding:35px;'>"
                + "<h2 style='color:#222; margin-top:0;'>Đặt lại mật khẩu tài khoản</h2>"
                + "<p style='color:#555; font-size:15px; line-height:1.6;'>"
                + "Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản CineFlow của bạn.<br>"
                + "Mã xác thực OTP của bạn là:"
                + "</p>"
                + "<div style='background:#f8f8f8; border:2px dashed #e50914; "
                + "border-radius:10px; padding:20px; text-align:center; margin:25px 0;'>"
                + "<div style='font-size:36px; font-weight:bold; letter-spacing:8px; color:#e50914;'>"
                + otp
                + "</div>"
                + "</div>"
                + "<p style='color:#777; font-size:14px; line-height:1.6;'>"
                + "Mã xác thực này có hiệu lực trong vòng <strong>15 phút</strong>."
                + "</p>"
                + "<div style='background:#fff4f4; border-left:4px solid #e50914; padding:12px 15px; margin-top:25px;'>"
                + "<p style='margin:0; color:#555; font-size:13px; line-height:1.5;'>"
                + "<strong>Lưu ý bảo mật:</strong> Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email và không cung cấp mã cho người khác."
                + "</p>"
                + "</div>"
                + "<p style='margin-top:30px; color:#999; font-size:12px;'>"
                + "Đây là email tự động từ hệ thống CineFlow. Vui lòng không trả lời thư này."
                + "</p>"
                + "</div>"
                + "<div style='background:#f7f7f7; padding:18px; text-align:center;'>"
                + "<p style='margin:0; color:#999; font-size:12px;'>© CineFlow</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>",
                "text/html; charset=UTF-8"
        );

        Transport.send(message);
    }

    public static void sendAccountActivationOtpEmail(
            String toEmail,
            String otp,
            String fullName) throws Exception {

        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(
                properties,
                new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("CineFlow - Mã kích hoạt tài khoản");

        String recipientName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";

        message.setContent(
                "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='margin:0; padding:0; background-color:#0B0F17; font-family:Arial,sans-serif; color:#F8FAFC;'>"
                + "<div style='max-width:600px; margin:40px auto; background:#161B26; "
                + "border-radius:14px; overflow:hidden; border:1px solid rgba(255,255,255,0.08); "
                + "box-shadow:0 12px 40px rgba(0,0,0,0.6);'>"
                + "<div style='background:linear-gradient(135deg, #FF2A54 0%, #E11D48 100%); padding:28px; text-align:center;'>"
                + "<h1 style='margin:0; color:#ffffff; font-size:28px; font-weight:800; letter-spacing:1px;'>CineFlow</h1>"
                + "<p style='margin:6px 0 0; color:#ffffff; font-size:13px; opacity:0.9;'>Hệ thống Đặt vé Rạp chiếu phim Hiện đại</p>"
                + "</div>"
                + "<div style='padding:36px; color:#F8FAFC;'>"
                + "<h2 style='color:#ffffff; margin-top:0; font-size:20px;'>Chào mừng " + recipientName + " đến với CineFlow!</h2>"
                + "<p style='color:#94A3B8; font-size:14px; line-height:1.6;'>"
                + "Cảm ơn bạn đã đăng ký tài khoản. Để kích hoạt tài khoản và nhận vé điện tử (E-ticket) khi đặt vé xem phim, vui lòng nhập mã xác thực OTP 6 chữ số dưới đây:"
                + "</p>"
                + "<div style='background:#111622; border:2px dashed #FF2A54; "
                + "border-radius:10px; padding:22px; text-align:center; margin:26px 0;'>"
                + "<div style='font-size:38px; font-weight:bold; letter-spacing:10px; color:#FF2A54; font-family:monospace;'>"
                + otp
                + "</div>"
                + "</div>"
                + "<p style='color:#94A3B8; font-size:13px; line-height:1.6;'>"
                + "Mã xác thực có hiệu lực trong vòng <strong style='color:#F8FAFC;'>15 phút</strong>. "
                + "Tuyệt đối không chia sẻ mã này cho bất kỳ ai để bảo vệ quyền lợi tài khoản của bạn."
                + "</p>"
                + "<div style='background:rgba(255,42,84,0.08); border-left:4px solid #FF2A54; "
                + "padding:12px 16px; margin-top:24px; border-radius:4px;'>"
                + "<p style='margin:0; color:#CBD5E1; font-size:12px; line-height:1.5;'>"
                + "<strong>Lưu ý:</strong> Nếu bạn không thực hiện yêu cầu đăng ký tại CineFlow, vui lòng bỏ qua email này."
                + "</p>"
                + "</div>"
                + "<p style='margin-top:28px; color:#64748B; font-size:11px;'>"
                + "Đây là email tự động từ hệ thống CineFlow. Vui lòng không phản hồi thư này."
                + "</p>"
                + "</div>"
                + "<div style='background:#111622; padding:16px; text-align:center; border-top:1px solid rgba(255,255,255,0.05);'>"
                + "<p style='margin:0; color:#64748B; font-size:12px;'>© 2026 CineFlow - Cinema Modern Experience</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>",
                "text/html; charset=UTF-8"
        );

        Transport.send(message);
    }
}

