package test;

import utils.EmailUtils;

public class TestEmail {

    public static void main(String[] args) {

        try {
            String recipient = args.length > 0
                    ? args[0]
                    : "quyetthaix2@gmail.com";
            String otp = args.length > 1 ? args[1] : "123456";

            if (recipient == null || recipient.trim().isEmpty()) {
                throw new IllegalArgumentException("Recipient email is required.");
            }

            EmailUtils.sendResetPasswordOtpEmail(
                    recipient.trim(),
                    otp
            );

            System.out.println("================================");
            System.out.println("EMAIL SENT SUCCESSFULLY!");
            System.out.println("To: " + recipient.trim());
            System.out.println("OTP: " + otp);
            System.out.println("================================");

        } catch (Exception e) {

            System.out.println("================================");
            System.out.println("EMAIL SEND FAILED!");
            System.out.println("================================");

            e.printStackTrace();
        }
    }
}
