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

    /** Lớp tiện ích chỉ có hàm static, không tạo đối tượng. */
    private DbUtil() {}

    /** Mot khoi cong viec dung chung mot ket noi (mot transaction). */
    @FunctionalInterface
    public interface TransactionWork<T, E extends Exception> {
        T run(Connection conn) throws SQLException, E;
    }

    /**
     * Chạy một khối công việc trong một transaction:
     * mở kết nối, tắt auto-commit, chạy work; thành công thì commit, có lỗi thì rollback rồi ném lại lỗi; cuối cùng đóng kết nối.
     * Service dùng hàm này để các bước ghi CSDL (ví dụ giữ ghế, xác nhận thanh toán) hoặc thành công hết, hoặc không bước nào được lưu.
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

    /**
     * Lỗi có phải do vi phạm UNIQUE / PRIMARY KEY của SQL Server không (mã 2601, 2627).
     * Dùng để nhận biết hai người cùng giữ một ghế.
     */
    public static boolean isDuplicateKey(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) return true;
        }
        return false;
    }

    /** Tạo chuỗi "?, ?, ?" với n dấu hỏi cho câu lệnh IN (n >= 1). */
    public static String placeholders(int n) {
        return String.join(", ", Collections.nCopies(n, "?"));
    }

    /**
     * Gán lần lượt các giá trị int vào PreparedStatement, bắt đầu từ vị trí start.
     * @return vị trí tham số kế tiếp
     */
    public static int setInts(PreparedStatement ps, int start, Collection<Integer> values) throws SQLException {
        int i = start;
        for (int v : values) ps.setInt(i++, v);
        return i;
    }
}
