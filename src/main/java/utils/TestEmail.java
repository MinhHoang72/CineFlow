package utils;

public class TestEmail {

    public static void main(String[] args) {

        try {

            String otp = "123456";

            EmailUtils.sendResetPasswordOtpEmail(
                    "quyetthaix2@gmail.com",
                    otp
            );

            System.out.println("================================");
            System.out.println("EMAIL SENT SUCCESSFULLY!");
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