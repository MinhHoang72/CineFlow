package com.mycompany.cinechain.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Ket noi SQL Server. Sua USER/PASS cho khop may cua ban.
 */
public class DBContext {

    private static final String URL =
            "jdbc:sqlserver://localhost:1433;databaseName=CineFlowDB;encrypt=true;trustServerCertificate=true";
    private static final String USER = "YOUR_USER";
    private static final String PASS = "YOUR_PASSWORD";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Thiếu SQL Server JDBC driver", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
