import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Automated test suite verifying the database schema enhancements requested:
 * 1. FnbItems: CentreID INT NULL (FK to Centres)
 * 2. FnbCombos: CentreID INT NULL (FK to Centres)
 * 3. Vouchers: CentreID INT NULL, CreatedBy INT NULL (FK to Users), PerCustomerLimit INT NOT NULL DEFAULT 1
 * 4. 3 new tables: VoucherDays, VoucherMovies, VoucherCentres
 * 5. Users table: STAFF and MANAGER must have CentreID (CK_Users_Staff_Manager_Centre)
 */
public class SchemaEnhancementVerificationRunner {

    private static int totalPassed = 0;
    private static int totalFailed = 0;

    private static void pass(String testName, String detail) {
        totalPassed++;
        System.out.println("  [PASS] " + testName + " -> " + detail);
    }

    private static void fail(String testName, String detail) {
        totalFailed++;
        System.err.println("  [FAIL] " + testName + " -> " + detail);
    }

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  VERIFICATION RUNNER: DATABASE SCHEMA ENHANCEMENTS");
        System.out.println("======================================================================");

        try {
            test1_FnbItemsCentreId();
            test2_FnbCombosCentreId();
            test3_VouchersColumnsAndConstraints();
            test4_NewVoucherTables();
            test5_StaffAndManagerCentreIdConstraint();
        } catch (Exception e) {
            System.err.println("Unexpected error during test execution:");
            e.printStackTrace();
        }

        System.out.println("\n======================================================================");
        System.out.println("  SUMMARY: PASSED = " + totalPassed + " / " + (totalPassed + totalFailed) + ", FAILED = " + totalFailed);
        System.out.println("======================================================================");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    // 1. FnbItems CentreID (FK to Centres)
    private static void test1_FnbItemsCentreId() throws Exception {
        System.out.println("\n[TEST 1] Kiểm tra FnbItems.CentreID (INT NULL, FK tới Centres):");
        try (Connection conn = DBContext.getConnection()) {
            PreparedStatement psCol = conn.prepareStatement(
                "SELECT IS_NULLABLE, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'FnbItems' AND COLUMN_NAME = 'CentreID'"
            );
            ResultSet rsCol = psCol.executeQuery();
            if (rsCol.next() && "int".equalsIgnoreCase(rsCol.getString("DATA_TYPE")) && "YES".equalsIgnoreCase(rsCol.getString("IS_NULLABLE"))) {
                pass("T1.1", "Column CentreID exists in FnbItems as INT NULL");
            } else {
                fail("T1.1", "Column CentreID missing or incorrect in FnbItems");
            }

            // Check Foreign Key
            PreparedStatement psFk = conn.prepareStatement(
                "SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('FnbItems') AND referenced_object_id = OBJECT_ID('Centres')"
            );
            ResultSet rsFk = psFk.executeQuery();
            if (rsFk.next()) {
                pass("T1.2", "Foreign Key constraint referencing Centres exists on FnbItems (" + rsFk.getString("name") + ")");
            } else {
                fail("T1.2", "Foreign Key referencing Centres missing on FnbItems");
            }
        }
    }

    // 2. FnbCombos CentreID (FK to Centres)
    private static void test2_FnbCombosCentreId() throws Exception {
        System.out.println("\n[TEST 2] Kiểm tra FnbCombos.CentreID (INT NULL, FK tới Centres):");
        try (Connection conn = DBContext.getConnection()) {
            PreparedStatement psCol = conn.prepareStatement(
                "SELECT IS_NULLABLE, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'FnbCombos' AND COLUMN_NAME = 'CentreID'"
            );
            ResultSet rsCol = psCol.executeQuery();
            if (rsCol.next() && "int".equalsIgnoreCase(rsCol.getString("DATA_TYPE")) && "YES".equalsIgnoreCase(rsCol.getString("IS_NULLABLE"))) {
                pass("T2.1", "Column CentreID exists in FnbCombos as INT NULL");
            } else {
                fail("T2.1", "Column CentreID missing or incorrect in FnbCombos");
            }

            // Check Foreign Key
            PreparedStatement psFk = conn.prepareStatement(
                "SELECT name FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('FnbCombos') AND referenced_object_id = OBJECT_ID('Centres')"
            );
            ResultSet rsFk = psFk.executeQuery();
            if (rsFk.next()) {
                pass("T2.2", "Foreign Key constraint referencing Centres exists on FnbCombos (" + rsFk.getString("name") + ")");
            } else {
                fail("T2.2", "Foreign Key referencing Centres missing on FnbCombos");
            }
        }
    }

    // 3. Vouchers: CentreID, CreatedBy, PerCustomerLimit
    private static void test3_VouchersColumnsAndConstraints() throws Exception {
        System.out.println("\n[TEST 3] Kiểm tra Vouchers: CentreID INT NULL, CreatedBy INT NULL, PerCustomerLimit DEFAULT 1:");
        try (Connection conn = DBContext.getConnection()) {
            // Check CentreID
            PreparedStatement ps1 = conn.prepareStatement(
                "SELECT IS_NULLABLE, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Vouchers' AND COLUMN_NAME = 'CentreID'"
            );
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next() && "int".equalsIgnoreCase(rs1.getString("DATA_TYPE")) && "YES".equalsIgnoreCase(rs1.getString("IS_NULLABLE"))) {
                pass("T3.1", "Vouchers.CentreID is INT NULL");
            } else {
                fail("T3.1", "Vouchers.CentreID missing or incorrect");
            }

            // Check CreatedBy
            PreparedStatement ps2 = conn.prepareStatement(
                "SELECT IS_NULLABLE, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Vouchers' AND COLUMN_NAME = 'CreatedBy'"
            );
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next() && "int".equalsIgnoreCase(rs2.getString("DATA_TYPE")) && "YES".equalsIgnoreCase(rs2.getString("IS_NULLABLE"))) {
                pass("T3.2", "Vouchers.CreatedBy is INT NULL");
            } else {
                fail("T3.2", "Vouchers.CreatedBy missing or incorrect");
            }

            // Check PerCustomerLimit
            PreparedStatement ps3 = conn.prepareStatement(
                "SELECT IS_NULLABLE, DATA_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Vouchers' AND COLUMN_NAME = 'PerCustomerLimit'"
            );
            ResultSet rs3 = ps3.executeQuery();
            if (rs3.next() && "int".equalsIgnoreCase(rs3.getString("DATA_TYPE")) && "NO".equalsIgnoreCase(rs3.getString("IS_NULLABLE"))) {
                pass("T3.3", "Vouchers.PerCustomerLimit is INT NOT NULL (Default: " + rs3.getString("COLUMN_DEFAULT") + ")");
            } else {
                fail("T3.3", "Vouchers.PerCustomerLimit missing or incorrect");
            }

            // Check FK to Centres and FK to Users
            PreparedStatement psFk = conn.prepareStatement(
                "SELECT COUNT(*) FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('Vouchers') AND referenced_object_id IN (OBJECT_ID('Centres'), OBJECT_ID('Users'))"
            );
            ResultSet rsFk = psFk.executeQuery();
            if (rsFk.next() && rsFk.getInt(1) >= 2) {
                pass("T3.4", "FK constraints to Centres and Users exist on Vouchers");
            } else {
                fail("T3.4", "FK constraints to Centres/Users missing on Vouchers");
            }

            // Check CHECK constraint PerCustomerLimit >= 1
            PreparedStatement psCk = conn.prepareStatement(
                "SELECT definition FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('Vouchers') AND name LIKE '%PerCustomerLimit%'"
            );
            ResultSet rsCk = psCk.executeQuery();
            if (rsCk.next()) {
                pass("T3.5", "CHECK constraint on PerCustomerLimit exists (" + rsCk.getString("definition") + ")");
            } else {
                fail("T3.5", "CHECK constraint on PerCustomerLimit missing");
            }
        }
    }

    // 4. 3 new tables: VoucherDays, VoucherMovies, VoucherCentres
    private static void test4_NewVoucherTables() throws Exception {
        System.out.println("\n[TEST 4] Kiểm tra 3 bảng mới: VoucherDays, VoucherMovies, VoucherCentres:");
        try (Connection conn = DBContext.getConnection()) {
            // Check table existence
            PreparedStatement psTbl = conn.prepareStatement(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME IN ('VoucherDays', 'VoucherMovies', 'VoucherCentres')"
            );
            ResultSet rsTbl = psTbl.executeQuery();
            int tblCount = 0;
            while (rsTbl.next()) {
                tblCount++;
            }
            if (tblCount == 3) {
                pass("T4.1", "All 3 tables (VoucherDays, VoucherMovies, VoucherCentres) exist in database");
            } else {
                fail("T4.1", "Found " + tblCount + " tables instead of 3");
            }

            // Test inserting and cascading delete into all 3 tables
            conn.setAutoCommit(false);
            try {
                // Insert a test voucher
                PreparedStatement psV = conn.prepareStatement(
                    "INSERT INTO Vouchers (VoucherCode, VoucherName, DiscountType, DiscountValue, MinimumOrderValue, StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status) "
                    + "VALUES ('TEST_VOUCHER_XYZ', N'Voucher Test', 'PERCENT', 15.0, 50000, GETDATE(), DATEADD(day, 30, GETDATE()), 100, 0, 2, 'ACTIVE')",
                    Statement.RETURN_GENERATED_KEYS
                );
                psV.executeUpdate();
                int testVoucherId = -1;
                try (ResultSet rs = psV.getGeneratedKeys()) {
                    if (rs.next()) testVoucherId = rs.getInt(1);
                }

                // Insert into VoucherDays
                PreparedStatement psDay = conn.prepareStatement("INSERT INTO VoucherDays (VoucherID, DayOfWeek) VALUES (?, 'MONDAY')");
                psDay.setInt(1, testVoucherId);
                psDay.executeUpdate();
                pass("T4.2", "Successfully inserted into VoucherDays (DayOfWeek = 'MONDAY')");

                // Insert into VoucherMovies
                PreparedStatement psMov = conn.prepareStatement("INSERT INTO VoucherMovies (VoucherID, MovieID) VALUES (?, 1)");
                psMov.setInt(1, testVoucherId);
                psMov.executeUpdate();
                pass("T4.3", "Successfully inserted into VoucherMovies (MovieID = 1)");

                // Insert into VoucherCentres
                PreparedStatement psCen = conn.prepareStatement("INSERT INTO VoucherCentres (VoucherID, CentreID) VALUES (?, 1)");
                psCen.setInt(1, testVoucherId);
                psCen.executeUpdate();
                pass("T4.4", "Successfully inserted into VoucherCentres (CentreID = 1)");

                // Test Cascade Delete
                PreparedStatement psDel = conn.prepareStatement("DELETE FROM Vouchers WHERE VoucherID = ?");
                psDel.setInt(1, testVoucherId);
                psDel.executeUpdate();

                PreparedStatement psCount = conn.prepareStatement(
                    "SELECT "
                    + "(SELECT COUNT(*) FROM VoucherDays WHERE VoucherID = ?) + "
                    + "(SELECT COUNT(*) FROM VoucherMovies WHERE VoucherID = ?) + "
                    + "(SELECT COUNT(*) FROM VoucherCentres WHERE VoucherID = ?)"
                );
                psCount.setInt(1, testVoucherId);
                psCount.setInt(2, testVoucherId);
                psCount.setInt(3, testVoucherId);
                ResultSet rsCount = psCount.executeQuery();
                if (rsCount.next() && rsCount.getInt(1) == 0) {
                    pass("T4.5", "CASCADE DELETE verified: All child records in 3 tables automatically removed when Voucher deleted");
                } else {
                    fail("T4.5", "Orphan records remained after Voucher deletion!");
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // 5. Users table: STAFF and MANAGER must have CentreID
    private static void test5_StaffAndManagerCentreIdConstraint() throws Exception {
        System.out.println("\n[TEST 5] Kiểm tra ràng buộc STAFF và MANAGER bắt buộc có CentreID:");
        try (Connection conn = DBContext.getConnection()) {
            // Check existing check constraint
            PreparedStatement psCk = conn.prepareStatement(
                "SELECT definition FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('Users') AND name = 'CK_Users_Staff_Manager_Centre'"
            );
            ResultSet rsCk = psCk.executeQuery();
            if (rsCk.next()) {
                pass("T5.1", "Constraint CK_Users_Staff_Manager_Centre exists: " + rsCk.getString("definition"));
            } else {
                fail("T5.1", "Constraint CK_Users_Staff_Manager_Centre missing on Users table");
            }

            // Test 5.2: Attempting to insert a MANAGER with CentreID = NULL must FAIL
            boolean managerNullRejected = false;
            try {
                conn.setAutoCommit(false);
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Users (FullName, Account, Email, Password, Role, Status, CentreID) "
                    + "VALUES (N'Manager Test', 'mgr_test_null', 'mgr_test_null@cineflow.vn', '123456', 'MANAGER', 'ACTIVE', NULL)"
                );
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException expected) {
                conn.rollback();
                managerNullRejected = true;
            } finally {
                conn.setAutoCommit(true);
            }
            if (managerNullRejected) {
                pass("T5.2", "Inserting MANAGER with CentreID = NULL was strictly rejected by database CHECK constraint");
            } else {
                fail("T5.2", "Inserting MANAGER with CentreID = NULL was unexpectedly allowed!");
            }

            // Test 5.3: Attempting to insert a STAFF with CentreID = NULL must FAIL
            boolean staffNullRejected = false;
            try {
                conn.setAutoCommit(false);
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Users (FullName, Account, Email, Password, Role, Status, CentreID) "
                    + "VALUES (N'Staff Test', 'staff_test_null', 'staff_test_null@cineflow.vn', '123456', 'TICKET_SALES_STAFF', 'ACTIVE', NULL)"
                );
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException expected) {
                conn.rollback();
                staffNullRejected = true;
            } finally {
                conn.setAutoCommit(true);
            }
            if (staffNullRejected) {
                pass("T5.3", "Inserting STAFF with CentreID = NULL was strictly rejected by database CHECK constraint");
            } else {
                fail("T5.3", "Inserting STAFF with CentreID = NULL was unexpectedly allowed!");
            }

            // Test 5.4: Inserting CUSTOMER with CentreID = NULL must SUCCEED
            boolean customerNullAllowed = false;
            int testCustId = -1;
            try {
                conn.setAutoCommit(false);
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Users (FullName, Account, Email, Password, Role, Status, CentreID) "
                    + "VALUES (N'Customer Test', 'cust_test_null', 'cust_test_null@cineflow.vn', '123456', 'CUSTOMER', 'ACTIVE', NULL)",
                    Statement.RETURN_GENERATED_KEYS
                );
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) testCustId = rs.getInt(1);
                }
                customerNullAllowed = (testCustId > 0);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                if (testCustId > 0) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("DELETE FROM Users WHERE UserID = " + testCustId);
                    }
                }
                conn.setAutoCommit(true);
            }
            if (customerNullAllowed) {
                pass("T5.4", "CUSTOMER with CentreID = NULL is properly allowed");
            } else {
                fail("T5.4", "CUSTOMER with CentreID = NULL failed unexpectedly");
            }
        }
    }
}
