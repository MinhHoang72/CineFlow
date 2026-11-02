package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

/**
 * Tien ich cho tang Service: chay nhieu thao tac DAO trong mot transaction, nhan biet loi trung khoa,
 * tao danh sach tham so "?, ?, ?" cho cau lenh IN.
 */
public final class DbUtil {

    private DbUtil() {}

    /** Mot khoi cong viec dung chung mot ket noi (mot transaction). */
    @FunctionalInterface
    public interface TransactionWork<T, E extends Exception> {
        T run(Connection conn) throws SQLException, E;
    }

    /**
     * Mo ket noi, chay work trong transaction: thanh cong thi commit, co loi thi rollback va nem lai loi.
     * Service dung de gom nhieu thao tac DAO thanh mot don vi (tat ca thanh cong hoac khong co gi thay doi).
     */
    public static <T, E extends Exception> T inTransaction(TransactionWork<T, E> work) throws SQLException, E {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (Throwable e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Loi vi pham UNIQUE / PRIMARY KEY cua SQL Server (2601, 2627). */
    public static boolean isDuplicateKey(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) return true;
        }
        return false;
    }

    /** "?, ?, ?" cho cau lenh IN (n >= 1). */
    public static String placeholders(int n) {
        return String.join(", ", Collections.nCopies(n, "?"));
    }

    /** Gan cac gia tri int vao PreparedStatement bat dau tu vi tri start. @return vi tri tham so ke tiep */
    public static int setInts(PreparedStatement ps, int start, Collection<Integer> values) throws SQLException {
        int i = start;
        for (int v : values) ps.setInt(i++, v);
        return i;
    }
}
