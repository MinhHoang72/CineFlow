package test;

import dao.UserDAO;
import model.User;
import utils.DBContext;
import utils.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestRegister {

    public static void main(String[] args) {

        System.out.println();
        System.out.println("==========================================");
        System.out.println("       CINEFLOW REGISTER TEST");
        System.out.println("==========================================");


        // =====================================================
        // TEST 1: DATABASE CONNECTION
        // =====================================================

        System.out.println();
        System.out.println("[TEST 1] DATABASE CONNECTION");

        try (Connection connection = DBContext.getConnection()) {

            System.out.println("Connection: SUCCESS");

            String sql = "SELECT DB_NAME() AS DatabaseName";

            try (PreparedStatement ps =
                         connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String databaseName =
                            rs.getString("DatabaseName");

                    System.out.println(
                            "Database: " + databaseName
                    );
                }
            }

        } catch (Exception e) {

            System.out.println("Connection: FAILED");

            e.printStackTrace();

            return;
        }


        // =====================================================
        // TEST 2: RAW SQL INSERT
        // =====================================================
        //
        // Test trực tiếp SQL Server.
        // INSERT xong sẽ ROLLBACK nên không tạo dữ liệu thật.
        //

        System.out.println();
        System.out.println("[TEST 2] RAW SQL INSERT TEST");


        String testAccount =
                "test_" + System.currentTimeMillis();

        String testEmail =
                testAccount + "@gmail.com";


        String rawSql =
                "INSERT INTO Users "
                + "(FullName, Account, Email, Password, Role, Status, LoginProvider) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";


        try (Connection connection =
                     DBContext.getConnection()) {

            // Bắt đầu transaction
            connection.setAutoCommit(false);


            try (PreparedStatement ps =
                         connection.prepareStatement(rawSql)) {

                ps.setString(1, "Test Customer");
                ps.setString(2, testAccount);
                ps.setString(3, testEmail);
                ps.setString(
                        4,
                        PasswordUtils.hashPassword("Test1234!")
                );
                ps.setString(5, "CUSTOMER");
                ps.setString(6, "ACTIVE");
                ps.setString(7, "LOCAL");


                int rows =
                        ps.executeUpdate();


                System.out.println(
                        "Rows inserted: " + rows
                );


                if (rows == 1) {

                    System.out.println(
                            "RAW SQL INSERT: SUCCESS"
                    );

                } else {

                    System.out.println(
                            "RAW SQL INSERT: FAILED"
                    );
                }


                // Không lưu test vào database
                connection.rollback();

                System.out.println(
                        "Test data rolled back."
                );
            }


        } catch (Exception e) {

            System.out.println(
                    "RAW SQL INSERT: FAILED"
            );

            e.printStackTrace();

            return;
        }


        // =====================================================
        // TEST 3: USERDAO REGISTER
        // =====================================================

        System.out.println();
        System.out.println("[TEST 3] USERDAO REGISTER");


        UserDAO userDAO =
                new UserDAO();


        // Tạo User test

        User user =
                new User();

        user.setFullName(
                "Test Customer"
        );

        user.setAccount(
                testAccount
        );

        user.setEmail(
                testEmail
        );

        user.setPassword(
                PasswordUtils.hashPassword(
                        "Test1234!"
                )
        );


        System.out.println(
                "Test Account: "
                + testAccount
        );

        System.out.println(
                "Test Email: "
                + testEmail
        );


        // Gọi UserDAO.register()

        boolean result =
                userDAO.register(user);


        System.out.println();

        System.out.println(
                "UserDAO.register() = "
                + result
        );


        if (result) {

            System.out.println(
                    "USERDAO REGISTER: SUCCESS"
            );

        } else {

            System.out.println(
                    "USERDAO REGISTER: FAILED"
            );
        }


        // =====================================================
        // TEST 4: CHECK USER IN DATABASE
        // =====================================================

        System.out.println();
        System.out.println(
                "[TEST 4] CHECK INSERTED USER"
        );


        String checkSql =
                "SELECT UserID, FullName, Account, "
                + "Email, Role, Status, LoginProvider "
                + "FROM Users "
                + "WHERE Account = ?";


        try (Connection connection =
                     DBContext.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(checkSql)) {


            ps.setString(
                    1,
                    testAccount
            );


            try (ResultSet rs =
                         ps.executeQuery()) {


                if (rs.next()) {

                    System.out.println(
                            "USER FOUND IN DATABASE!"
                    );

                    System.out.println(
                            "UserID = "
                            + rs.getInt("UserID")
                    );

                    System.out.println(
                            "FullName = "
                            + rs.getString("FullName")
                    );

                    System.out.println(
                            "Account = "
                            + rs.getString("Account")
                    );

                    System.out.println(
                            "Email = "
                            + rs.getString("Email")
                    );

                    System.out.println(
                            "Role = "
                            + rs.getString("Role")
                    );

                    System.out.println(
                            "Status = "
                            + rs.getString("Status")
                    );

                    System.out.println(
                            "LoginProvider = "
                            + rs.getString("LoginProvider")
                    );


                } else {

                    System.out.println(
                            "USER NOT FOUND IN DATABASE!"
                    );
                }
            }


        } catch (Exception e) {

            e.printStackTrace();
        }


        // =====================================================
        // TEST 5: DELETE TEST USER
        // =====================================================
        //
        // Chỉ xóa user test vừa tạo.
        //

        if (result) {

            System.out.println();
            System.out.println(
                    "[TEST 5] DELETE TEST USER"
            );


            String deleteSql =
                    "DELETE FROM Users "
                    + "WHERE Account = ?";


            try (Connection connection =
                         DBContext.getConnection();
                 PreparedStatement ps =
                         connection.prepareStatement(
                                 deleteSql
                         )) {


                ps.setString(
                        1,
                        testAccount
                );


                int deleted =
                        ps.executeUpdate();


                System.out.println(
                        "Deleted rows: "
                        + deleted
                );


                if (deleted == 1) {

                    System.out.println(
                            "TEST USER DELETED."
                    );

                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }


        // =====================================================
        // FINAL RESULT
        // =====================================================

        System.out.println();
        System.out.println("==========================================");
        System.out.println("             TEST FINISHED");
        System.out.println("==========================================");

    }
}