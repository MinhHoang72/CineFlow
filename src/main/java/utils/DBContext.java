package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/**
 * Cung cấp kết nối JDBC tới cơ sở dữ liệu Microsoft SQL Server của hệ thống CineFlow.
 */
public class DBContext implements AutoCloseable {

    /** Ket noi that cua thread/request hien tai, mo khi DAO dung lan dau. */
    private static final ThreadLocal<Connection> REQUEST_CONNECTION = new ThreadLocal<>();

    /** Moi loi goi tren proxy chuyen sang ket noi that cua request hien tai (mo lai neu chua co / da dong). */
    private static final Connection REQUEST_CONNECTION_PROXY = (Connection) Proxy.newProxyInstance(
            DBContext.class.getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    closeRequestConnections();
                    return null;
                }
                Connection real = REQUEST_CONNECTION.get();
                if (real == null || real.isClosed()) {
                    real = getConnection();
                    REQUEST_CONNECTION.set(real);
                }
                try {
                    return method.invoke(real, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });

    /**
     * Ket noi cho cac DAO ke thua DBContext (CentrePricingDAO, SurchargeDAO, TimeSlotPricingDAO).
     * Moi request dung ket noi rieng, filter.DBConnectionFilter dong no khi request ket thuc, nen DAO
     * tao moi moi request hay giu lau trong servlet deu khong ro ket noi va khong dung ket noi da dong.
     */
    protected final Connection connection = REQUEST_CONNECTION_PROXY;

    @Override
    public void close() {
        closeRequestConnections();
    }

    /** Dong ket noi DAO cua request hien tai. */
    public static void closeRequestConnections() {
        Connection real = REQUEST_CONNECTION.get();
        REQUEST_CONNECTION.remove();
        try {
            if (real != null && !real.isClosed()) real.close();
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
