package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cung cấp kết nối JDBC tới cơ sở dữ liệu Microsoft SQL Server của hệ thống CineFlow.
 */
public class DBContext {
    protected Connection connection;
    public DBContext() {
        try { connection = getConnection(); }
        catch (SQLException e) { throw new IllegalStateException("Unable to connect to database", e); }
    }

    private static final String URL =
            "jdbc:sqlserver://localhost:1433;"
            + "databaseName=CineFlowDB;"
            + "encrypt=true;"
            + "trustServerCertificate=true;";

    private static final String USER = "sa";
    private static final String PASSWORD = "123";

    /**
     * Mở và trả về một kết nối mới tới cơ sở dữ liệu.
     *
     * @return đối tượng Connection
     * @throws SQLException nếu kết nối thất bại
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC driver is not available", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Phương thức kiểm tra kết nối trực tiếp khi chạy độc lập
    public static void main(String[] args) {

        try (Connection connection = getConnection()) {

            if (connection != null) {
                System.out.println("================================");
                System.out.println("DATABASE CONNECTED SUCCESSFULLY!");
                System.out.println("Database: CineFlowDB");
                System.out.println("================================");
            }

        } catch (SQLException e) {

            System.out.println("================================");
            System.out.println("DATABASE CONNECTION FAILED!");
            System.out.println("================================");

            e.printStackTrace();
        }
    }
}
