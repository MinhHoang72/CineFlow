package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cung cấp kết nối JDBC tới cơ sở dữ liệu Microsoft SQL Server của hệ thống CineFlow.
 */
public class DBContext implements AutoCloseable {

    /**
     * Ket noi rieng cua cac DAO ke thua DBContext (BranchPricingDAO, SurchargeDAO, TimeSlotPricingDAO).
     * Ket noi mo o constructor duoc ghi lai theo request va dong boi filter.DBConnectionFilter khi request
     * ket thuc; dung DAO trong try-with-resources thi ket noi dong ngay.
     */
    protected Connection connection;

    private static final ThreadLocal<List<Connection>> REQUEST_CONNECTIONS = ThreadLocal.withInitial(ArrayList::new);

    public DBContext() {
        try { connection = getConnection(); }
        catch (SQLException e) { throw new IllegalStateException("Unable to connect to database", e); }
        REQUEST_CONNECTIONS.get().add(connection);
    }

    @Override
    public void close() {
        closeQuietly(connection);
        REQUEST_CONNECTIONS.get().remove(connection);
    }

    /** Dong moi ket noi DAO da mo trong request hien tai. */
    public static void closeRequestConnections() {
        for (Connection c : REQUEST_CONNECTIONS.get()) closeQuietly(c);
        REQUEST_CONNECTIONS.remove();
    }

    private static void closeQuietly(Connection c) {
        try {
            if (c != null && !c.isClosed()) c.close();
        } catch (SQLException ignored) {
            // ket noi da hong: khong con gi de dong
        }
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
