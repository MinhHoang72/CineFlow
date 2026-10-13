package utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtils {

    // Email Gmail dùng để gửi OTP
    private static final String FROM_EMAIL = "duongquyetthai@gmail.com";

    // App Password của Gmail
    private static final String APP_PASSWORD = "egwocrjeufohujlz";

    public static void sendResetPasswordOtpEmail(
            String toEmail,
            String otp) throws Exception {

        Properties properties = new Properties();

        properties.put(
                "mail.smtp.host",
                "smtp.gmail.com"
        );

        properties.put(
                "mail.smtp.port",
                "587"
        );

        properties.put(
                "mail.smtp.auth",
                "true"
        );

        properties.put(
                "mail.smtp.starttls.enable",
                "true"
        );

        Session session = Session.getInstance(
                properties,
                new Authenticator() {

            @Override
            protected PasswordAuthentication
                    getPasswordAuthentication() {

                return new PasswordAuthentication(
                        FROM_EMAIL,
                        APP_PASSWORD
                );
            }
        }
        );

        Message message = new MimeMessage(session);

        message.setFrom(
                new InternetAddress(FROM_EMAIL)
        );

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
        );

        message.setSubject(
                "CineFlow - Password Reset OTP"
        );

        message.setContent(
                "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "</head>"
                + "<body style='margin:0; padding:0; background-color:#f4f4f4; font-family:Arial,sans-serif;'>"
                + "<div style='max-width:600px; margin:40px auto; background:white; "
                + "border-radius:12px; overflow:hidden; "
                + "box-shadow:0 4px 15px rgba(0,0,0,0.1);'>"
                + "<div style='background:#e50914; padding:25px; text-align:center;'>"
                + "<h1 style='margin:0; color:white; font-size:28px;'>CineFlow</h1>"
                + "<p style='margin:8px 0 0; color:white; font-size:14px;'>"
                + "Multi-Branch Cinema Management System"
                + "</p>"
                + "</div>"
                + "<div style='padding:35px;'>"
                + "<h2 style='color:#222; margin-top:0;'>Password Reset</h2>"
                + "<p style='color:#555; font-size:15px;'>"
                + "Hello,"
                + "</p>"
                + "<p style='color:#555; font-size:15px; line-height:1.6;'>"
                + "We received a request to reset your CineFlow account password."
                + "</p>"
                + "<p style='color:#555; font-size:15px;'>"
                + "Your verification code is:"
                + "</p>"
                + "<div style='background:#f8f8f8; border:2px dashed #e50914; "
                + "border-radius:10px; padding:20px; text-align:center; margin:25px 0;'>"
                + "<div style='font-size:36px; font-weight:bold; "
                + "letter-spacing:8px; color:#e50914;'>"
                + otp
                + "</div>"
                + "</div>"
                + "<p style='color:#777; font-size:14px; line-height:1.6;'>"
                + "This verification code will expire in "
                + "<strong>15 minutes</strong>."
                + "</p>"
                + "<div style='background:#fff4f4; border-left:4px solid #e50914; "
                + "padding:12px 15px; margin-top:25px;'>"
                + "<p style='margin:0; color:#555; font-size:13px; line-height:1.5;'>"
                + "<strong>Security notice:</strong><br>"
                + "If you did not request a password reset, "
                + "you can safely ignore this email."
                + "</p>"
                + "</div>"
                + "<p style='margin-top:30px; color:#999; font-size:12px;'>"
                + "This is an automated email from CineFlow. "
                + "Please do not reply to this message."
                + "</p>"
                + "</div>"
                + "<div style='background:#f7f7f7; padding:18px; text-align:center;'>"
                + "<p style='margin:0; color:#999; font-size:12px;'>"
                + "© CineFlow"
                + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>",
                "text/html; charset=UTF-8"
        );

        Transport.send(message);
    }
}
