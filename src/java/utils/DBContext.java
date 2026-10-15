package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBContext {

    private static final String URL =
            "jdbc:sqlserver://localhost:1433;"
            + "databaseName=CineFlowDemoDB;"
            + "encrypt=true;"
            + "trustServerCertificate=true;";

    private static final String USER = "sa";

    private static final String PASSWORD = "123";

    protected Connection connection;

    public DBContext() {
        try {
            this.connection = getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQL Server JDBC driver is not available", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Test connection
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