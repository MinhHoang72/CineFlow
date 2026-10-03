package utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(10));
    }

    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    public static void main(String[] args) {

        String password = "123456";

        String hashedPassword = hashPassword(password);

        System.out.println("================================");
        System.out.println("Original password: " + password);
        System.out.println("Hashed password: " + hashedPassword);
        System.out.println("Check correct password: "
                + checkPassword("123456", hashedPassword));
        System.out.println("Check wrong password: "
                + checkPassword("999999", hashedPassword));
        System.out.println("================================");
    }
}