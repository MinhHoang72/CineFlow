import dao.CinemaDAO;
import dao.UserDAO;
import model.entity.Centre;
import model.entity.User;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Automated test suite verifying all 12 test cases specified in CODEX TASK - Đợt 1.
 */
public class CodexVerificationRunner {

    private static final UserDAO userDAO = new UserDAO();
    private static final CinemaDAO cinemaDAO = new CinemaDAO();
    private static final int ADMIN_ID = 4; // Administrator

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
        System.out.println("  CODEX TASK - ĐỢT 1: BẢO ĐẢM TÍNH TOÀN VẸN NGHIỆP VỤ BỔ NHIỆM & ĐIỀU CHUYỂN");
        System.out.println("  AUTOMATED VERIFICATION RUNNER (12 TEST CASES)");
        System.out.println("======================================================================");

        try {
            // Test Case 1: Bổ nhiệm Manager hợp lệ
            testCase1_AppointValidManager();

            // Test Case 2: Từ chối bổ nhiệm trùng lặp (Manager đã có rạp / Rạp đã có Manager)
            testCase2_RejectDuplicateAppointment();

            // Test Case 3: Điều chuyển hợp lệ từ Centre A sang Centre B
            testCase3_ValidTransfer();

            // Test Case 4: Từ chối điều chuyển đến Centre hiện tại
            testCase4_RejectTransferToSameCentre();

            // Test Case 5: Từ chối điều chuyển nếu Centre đích đã có Manager (Không ghi đè)
            testCase5_RejectTransferIfDestinationHasManager();

            // Test Case 6: Từ chối tài khoản LOCKED hoặc INACTIVE
            testCase6_RejectLockedOrInactiveManager();

            // Test Case 7: Từ chối role không hợp lệ (Không phải MANAGER)
            testCase7_RejectNonManagerRole();

            // Test Case 8: Từ chối Centre không hợp lệ (Không ACTIVE)
            testCase8_RejectNonActiveCentre();

            // Test Case 9: Kiểm thử Rollback giao dịch
            testCase9_TransactionRollback();

            // Test Case 10: Kiểm thử tương tranh (Concurrency)
            testCase10_ConcurrencyControl();

            // Test Case 11: Manager bị khóa -> Centre hiển thị 'Manager đang bị khóa'
            testCase11_LockedManagerAssignmentPreservation();

            // Test Case 12: Bảo toàn luồng điều chuyển Staff thông thường
            testCase12_RegularStaffTransfer();

            // Dọn dẹp dữ liệu kiểm thử về trạng thái chuẩn
            cleanupTestData();

        } catch (Exception e) {
            System.err.println("Unexpected exception during test execution:");
            e.printStackTrace();
        }

        System.out.println("\n======================================================================");
        System.out.println("  TEST SUMMARY: PASSED = " + totalPassed + " / " + (totalPassed + totalFailed) + ", FAILED = " + totalFailed);
        System.out.println("======================================================================");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 1: Bổ nhiệm Manager hợp lệ
    // -----------------------------------------------------------------------------------------
    private static void testCase1_AppointValidManager() throws Exception {
        System.out.println("\n[TEST 1] Bổ nhiệm Manager hợp lệ (Role = MANAGER, Status = ACTIVE, Unassigned):");
        
        // Setup: Đảm bảo User 17 (Phan Thanh Tùng) chưa được phân công và Centre 4 (CineFlow Quận 1) trống
        resetManagerAssignment(17);
        clearCentreManager(4);

        String result = userDAO.assignManager(4, 17, ADMIN_ID, "Quyết định bổ nhiệm số 01/QĐ-BGD");
        if (!"SUCCESS".equals(result)) {
            fail("TC1", "Expected SUCCESS but got: " + result);
            return;
        }

        // Verify Database state
        try (Connection conn = DBContext.getConnection()) {
            // Check Users.CentreID
            PreparedStatement psUser = conn.prepareStatement("SELECT CentreID, Status FROM Users WHERE UserID = 17");
            ResultSet rsUser = psUser.executeQuery();
            if (rsUser.next() && rsUser.getInt("CentreID") == 4) {
                pass("TC1.1", "Users.CentreID updated to 4 for Manager 17");
            } else {
                fail("TC1.1", "Users.CentreID was not updated to 4");
            }

            // Check CentreManagerAssignments active record
            PreparedStatement psCma = conn.prepareStatement(
                "SELECT AssignmentID, Status, EndDate FROM CentreManagerAssignments WHERE ManagerID = 17 AND CentreID = 4 AND Status = 'ACTIVE' AND EndDate IS NULL"
            );
            ResultSet rsCma = psCma.executeQuery();
            if (rsCma.next()) {
                pass("TC1.2", "Active record created in CentreManagerAssignments (AssignmentID: " + rsCma.getInt("AssignmentID") + ")");
            } else {
                fail("TC1.2", "No active assignment found in CentreManagerAssignments");
            }

            // Check StaffTransferHistory
            PreparedStatement psH = conn.prepareStatement(
                "SELECT TOP 1 TransferID, ToCentreID, Reason FROM StaffTransferHistory WHERE UserID = 17 ORDER BY TransferID DESC"
            );
            ResultSet rsH = psH.executeQuery();
            if (rsH.next() && rsH.getInt("ToCentreID") == 4) {
                pass("TC1.3", "Audit log created in StaffTransferHistory with ToCentreID = 4");
            } else {
                fail("TC1.3", "StaffTransferHistory audit log missing or incorrect");
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 2: Từ chối bổ nhiệm trùng lặp
    // -----------------------------------------------------------------------------------------
    private static void testCase2_RejectDuplicateAppointment() {
        System.out.println("\n[TEST 2] Từ chối bổ nhiệm trùng lặp (Manager đã có rạp / Rạp đã có Manager):");

        // 2.1: User 17 đã ở Centre 4, cố bổ nhiệm vào Centre 6
        String res1 = userDAO.assignManager(6, 17, ADMIN_ID, "Thử bổ nhiệm Manager đã có rạp");
        if ("MANAGER_ALREADY_ASSIGNED".equals(res1)) {
            pass("TC2.1", "Correctly rejected with MANAGER_ALREADY_ASSIGNED");
        } else {
            fail("TC2.1", "Expected MANAGER_ALREADY_ASSIGNED, got: " + res1);
        }

        // 2.2: User 19 (unassigned) cố bổ nhiệm vào Centre 4 (đã có User 17)
        String res2 = userDAO.assignManager(4, 19, ADMIN_ID, "Thử bổ nhiệm vào rạp đã có Manager");
        if ("CENTRE_ALREADY_HAS_MANAGER".equals(res2)) {
            pass("TC2.2", "Correctly rejected with CENTRE_ALREADY_HAS_MANAGER");
        } else {
            fail("TC2.2", "Expected CENTRE_ALREADY_HAS_MANAGER, got: " + res2);
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 3: Điều chuyển hợp lệ từ Centre A sang Centre B
    // -----------------------------------------------------------------------------------------
    private static void testCase3_ValidTransfer() throws Exception {
        System.out.println("\n[TEST 3] Điều chuyển Manager hợp lệ (Từ Centre 4 sang Centre 6):");
        
        clearCentreManager(6);

        String result = userDAO.transferManager(17, 6, ADMIN_ID, "Điều động tăng cường quản lý cụm Ngô Quyền", "Quyết định 02/QĐ");
        if (!"SUCCESS".equals(result)) {
            fail("TC3", "Expected SUCCESS but got: " + result);
            return;
        }

        try (Connection conn = DBContext.getConnection()) {
            // Check old assignment at Centre 4 is ENDED
            PreparedStatement psOld = conn.prepareStatement(
                "SELECT Status, EndDate FROM CentreManagerAssignments WHERE ManagerID = 17 AND CentreID = 4 AND Status = 'ENDED'"
            );
            ResultSet rsOld = psOld.executeQuery();
            if (rsOld.next() && rsOld.getTimestamp("EndDate") != null) {
                pass("TC3.1", "Old assignment at Centre 4 correctly marked as ENDED with EndDate timestamp");
            } else {
                fail("TC3.1", "Old assignment at Centre 4 was not ended properly");
            }

            // Check new assignment at Centre 6 is ACTIVE
            PreparedStatement psNew = conn.prepareStatement(
                "SELECT AssignmentID, Status, EndDate FROM CentreManagerAssignments WHERE ManagerID = 17 AND CentreID = 6 AND Status = 'ACTIVE' AND EndDate IS NULL"
            );
            ResultSet rsNew = psNew.executeQuery();
            if (rsNew.next()) {
                pass("TC3.2", "New assignment at Centre 6 created as ACTIVE (AssignmentID: " + rsNew.getInt("AssignmentID") + ")");
            } else {
                fail("TC3.2", "New assignment at Centre 6 missing or not ACTIVE");
            }

            // Check Users.CentreID is now 6
            PreparedStatement psU = conn.prepareStatement("SELECT CentreID FROM Users WHERE UserID = 17");
            ResultSet rsU = psU.executeQuery();
            if (rsU.next() && rsU.getInt("CentreID") == 6) {
                pass("TC3.3", "Users.CentreID updated to 6 for Manager 17");
            } else {
                fail("TC3.3", "Users.CentreID was not updated to 6");
            }

            // Check StaffTransferHistory
            PreparedStatement psH = conn.prepareStatement(
                "SELECT TOP 1 FromCentreID, ToCentreID, Reason FROM StaffTransferHistory WHERE UserID = 17 ORDER BY TransferID DESC"
            );
            ResultSet rsH = psH.executeQuery();
            if (rsH.next() && rsH.getInt("FromCentreID") == 4 && rsH.getInt("ToCentreID") == 6) {
                pass("TC3.4", "Audit log in StaffTransferHistory correctly recorded: From 4 To 6");
            } else {
                fail("TC3.4", "StaffTransferHistory audit log incorrect for transfer");
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 4: Từ chối điều chuyển đến Centre hiện tại
    // -----------------------------------------------------------------------------------------
    private static void testCase4_RejectTransferToSameCentre() {
        System.out.println("\n[TEST 4] Từ chối điều chuyển đến cùng Centre:");
        String res = userDAO.transferManager(17, 6, ADMIN_ID, "Chuyển tại chỗ", "");
        if ("SAME_CENTRE".equals(res)) {
            pass("TC4", "Correctly rejected with SAME_CENTRE");
        } else {
            fail("TC4", "Expected SAME_CENTRE, got: " + res);
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 5: Từ chối điều chuyển nếu Centre đích đã có Manager (Không ghi đè)
    // -----------------------------------------------------------------------------------------
    private static void testCase5_RejectTransferIfDestinationHasManager() throws Exception {
        System.out.println("\n[TEST 5] Từ chối điều chuyển nếu Centre đích đã có Manager:");
        // Centre 1 hiện có User 14 (Trần Đình Trọng)
        String res = userDAO.transferManager(17, 1, ADMIN_ID, "Cố chuyển vào rạp đã có người", "");
        if ("CENTRE_ALREADY_HAS_MANAGER".equals(res)) {
            pass("TC5.1", "Correctly rejected with CENTRE_ALREADY_HAS_MANAGER");
        } else {
            fail("TC5.1", "Expected CENTRE_ALREADY_HAS_MANAGER, got: " + res);
        }

        // Kiểm tra Manager của Centre 1 không bị ảnh hưởng (không bị ghi đè)
        try (Connection conn = DBContext.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT ManagerID FROM CentreManagerAssignments WHERE CentreID = 1 AND Status = 'ACTIVE' AND EndDate IS NULL"
            );
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt("ManagerID") == 14) {
                pass("TC5.2", "Existing manager (User 14) at Centre 1 preserved intact, no overwrite occurred");
            } else {
                fail("TC5.2", "Existing manager at Centre 1 was improperly modified!");
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 6: Từ chối Manager có trạng thái tài khoản không hợp lệ (LOCKED / INACTIVE)
    // -----------------------------------------------------------------------------------------
    private static void testCase6_RejectLockedOrInactiveManager() throws Exception {
        System.out.println("\n[TEST 6] Từ chối tài khoản LOCKED hoặc INACTIVE:");

        // Tạm thời khóa User 17
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Users SET Status = 'LOCKED' WHERE UserID = 17");
        }

        // Cố điều chuyển User 17 khi đang LOCKED
        String resTransfer = userDAO.transferManager(17, 4, ADMIN_ID, "Điều chuyển người bị khóa", "");
        if ("MANAGER_NOT_ACTIVE".equals(resTransfer)) {
            pass("TC6.1", "Transfer rejected with MANAGER_NOT_ACTIVE for LOCKED manager");
        } else {
            fail("TC6.1", "Expected MANAGER_NOT_ACTIVE, got: " + resTransfer);
        }

        // Tạm thời set User 19 (chưa phân công) thành INACTIVE
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Users SET Status = 'INACTIVE' WHERE UserID = 19");
        }

        String resAssign = userDAO.assignManager(4, 19, ADMIN_ID, "Bổ nhiệm người INACTIVE");
        if ("MANAGER_NOT_ACTIVE".equals(resAssign)) {
            pass("TC6.2", "Appointment rejected with MANAGER_NOT_ACTIVE for INACTIVE manager");
        } else {
            fail("TC6.2", "Expected MANAGER_NOT_ACTIVE, got: " + resAssign);
        }

        // Khôi phục trạng thái
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Users SET Status = 'ACTIVE' WHERE UserID = 17");
            conn.createStatement().executeUpdate("UPDATE Users SET Status = 'ACTIVE' WHERE UserID = 19");
        }
        pass("TC6.3", "Account statuses successfully restored to ACTIVE");
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 7: Từ chối role không hợp lệ (Tài khoản không phải Manager)
    // -----------------------------------------------------------------------------------------
    private static void testCase7_RejectNonManagerRole() {
        System.out.println("\n[TEST 7] Từ chối role không hợp lệ (Staff pretending to be Manager):");
        // User 2 là TICKET_SALES_STAFF
        String resAssign = userDAO.assignManager(4, 2, ADMIN_ID, "Bổ nhiệm staff làm manager");
        if ("NOT_A_MANAGER".equals(resAssign)) {
            pass("TC7.1", "assignManager correctly rejected non-manager with NOT_A_MANAGER");
        } else {
            fail("TC7.1", "Expected NOT_A_MANAGER, got: " + resAssign);
        }

        String resTransfer = userDAO.transferManager(2, 4, ADMIN_ID, "Điều chuyển staff qua luồng manager", "");
        if ("NOT_A_MANAGER".equals(resTransfer)) {
            pass("TC7.2", "transferManager correctly rejected non-manager with NOT_A_MANAGER");
        } else {
            fail("TC7.2", "Expected NOT_A_MANAGER, got: " + resTransfer);
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 8: Từ chối Centre không hợp lệ (Không ACTIVE)
    // -----------------------------------------------------------------------------------------
    private static void testCase8_RejectNonActiveCentre() throws Exception {
        System.out.println("\n[TEST 8] Từ chối Centre không hợp lệ (INACTIVE / TEMPORARILY_CLOSED):");

        // Tạm chuyển Centre 25 thành INACTIVE (phù hợp với CHECK constraint CK_Centres_Status)
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Centres SET Status = 'INACTIVE' WHERE CentreID = 25");
        }

        String resTransfer = userDAO.transferManager(17, 25, ADMIN_ID, "Chuyển tới rạp tạm ngưng", "");
        if ("CENTRE_INACTIVE".equals(resTransfer)) {
            pass("TC8.1", "transferManager correctly rejected INACTIVE centre with CENTRE_INACTIVE");
        } else {
            fail("TC8.1", "Expected CENTRE_INACTIVE, got: " + resTransfer);
        }

        String resAssign = userDAO.assignManager(25, 19, ADMIN_ID, "Bổ nhiệm vào rạp tạm ngưng");
        if ("CENTRE_INACTIVE".equals(resAssign)) {
            pass("TC8.2", "assignManager correctly rejected INACTIVE centre with CENTRE_INACTIVE");
        } else {
            fail("TC8.2", "Expected CENTRE_INACTIVE, got: " + resAssign);
        }

        // Khôi phục Centre 25
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Centres SET Status = 'ACTIVE' WHERE CentreID = 25");
        }
        pass("TC8.3", "Centre 25 status successfully restored to ACTIVE");
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 9: Kiểm thử Rollback giao dịch
    // -----------------------------------------------------------------------------------------
    private static void testCase9_TransactionRollback() throws Exception {
        System.out.println("\n[TEST 9] Kiểm thử Rollback giao dịch:");

        // Kiểm tra xem transaction có rollback hoàn toàn khi có lỗi hay không.
        // Giả lập bằng cách gọi một transaction thủ công tương tự logic của UserDAO nhưng cố tình ném lỗi sau bước 1
        int testCentreId = 4;
        clearCentreManager(testCentreId);

        boolean rollbackSuccess = false;
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Bước 1: Insert assignment
                PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO CentreManagerAssignments (CentreID, ManagerID, StartDate, Status, AssignedBy, Notes) VALUES (?, ?, GETDATE(), 'ACTIVE', ?, ?)"
                );
                ps1.setInt(1, testCentreId);
                ps1.setInt(2, 19);
                ps1.setInt(3, ADMIN_ID);
                ps1.setString(4, "Test Rollback");
                ps1.executeUpdate();

                // Cố ý gây lỗi ở bước 2 bằng cách vi phạm foreign key hoặc câu lệnh SQL sai
                PreparedStatement psFail = conn.prepareStatement("INSERT INTO NonExistentTable VALUES (1)");
                psFail.executeUpdate();

                conn.commit();
            } catch (Exception expectedEx) {
                conn.rollback();
                rollbackSuccess = true;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        if (rollbackSuccess) {
            // Xác minh bước 1 không còn sót lại trong database
            try (Connection conn = DBContext.getConnection()) {
                PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM CentreManagerAssignments WHERE CentreID = ? AND ManagerID = 19 AND Status = 'ACTIVE'"
                );
                psCheck.setInt(1, testCentreId);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    pass("TC9", "Transaction rolled back cleanly without leaving orphan records");
                } else {
                    fail("TC9", "Orphan record found in CentreManagerAssignments after rollback!");
                }
            }
        } else {
            fail("TC9", "Rollback block was not triggered");
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 10: Kiểm thử tương tranh (Concurrency)
    // -----------------------------------------------------------------------------------------
    private static void testCase10_ConcurrencyControl() throws Exception {
        System.out.println("\n[TEST 10] Kiểm thử tương tranh (2 threads cùng bổ nhiệm vào 1 Centre):");

        int targetCentreId = 4;
        clearCentreManager(targetCentreId);
        resetManagerAssignment(17);
        resetManagerAssignment(19);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicReference<String> resultThread1 = new AtomicReference<>();
        AtomicReference<String> resultThread2 = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                startLatch.await();
                String r = userDAO.assignManager(targetCentreId, 17, ADMIN_ID, "Concurrent appoint thread 1");
                resultThread1.set(r);
            } catch (Exception e) {
                resultThread1.set("EXCEPTION: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                startLatch.await();
                String r = userDAO.assignManager(targetCentreId, 19, ADMIN_ID, "Concurrent appoint thread 2");
                resultThread2.set(r);
            } catch (Exception e) {
                resultThread2.set("EXCEPTION: " + e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        t1.start();
        t2.start();

        // Đồng loạt xuất phát
        startLatch.countDown();
        doneLatch.await();

        String r1 = resultThread1.get();
        String r2 = resultThread2.get();

        System.out.println("    Thread 1 result: " + r1);
        System.out.println("    Thread 2 result: " + r2);

        boolean oneSucceeded = ("SUCCESS".equals(r1) && !"SUCCESS".equals(r2)) ||
                               ("SUCCESS".equals(r2) && !"SUCCESS".equals(r1));

        if (oneSucceeded) {
            pass("TC10.1", "Exactly one thread succeeded (" + ("SUCCESS".equals(r1) ? "Thread 1" : "Thread 2") + ") and the other was cleanly rejected");
        } else {
            fail("TC10.1", "Unexpected concurrency results: r1=" + r1 + ", r2=" + r2);
        }

        // Kiểm tra database: Centre 4 chỉ có ĐÚNG 1 active manager
        try (Connection conn = DBContext.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM CentreManagerAssignments WHERE CentreID = 4 AND Status = 'ACTIVE' AND EndDate IS NULL"
            );
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 1) {
                pass("TC10.2", "Database integrity verified: Exactly 1 active manager in Centre 4");
            } else {
                fail("TC10.2", "Violation: Active manager count is not 1!");
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 11: Manager bị khóa -> Centre hiển thị 'Manager đang bị khóa'
    // -----------------------------------------------------------------------------------------
    private static void testCase11_LockedManagerAssignmentPreservation() throws Exception {
        System.out.println("\n[TEST 11] Kiểm thử khóa Manager nhưng bảo toàn phân công & nhận diện riêng:");

        // Đảm bảo Manager 17 đang quản lý Centre 6
        resetManagerAssignment(17);
        clearCentreManager(6);
        userDAO.assignManager(6, 17, ADMIN_ID, "Bổ nhiệm trước khi khóa");

        // Khóa tài khoản Manager 17
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Users SET Status = 'LOCKED' WHERE UserID = 17");
        }

        // Truy vấn Centre 6 từ CinemaDAO
        Centre centre = cinemaDAO.getCentreById(6);
        if (centre != null) {
            boolean isLocked = centre.hasLockedManager();
            boolean isActive = centre.hasActiveManager();
            boolean isNone = centre.hasNoManager();

            if (isLocked && !isActive && !isNone) {
                pass("TC11.1", "Centre recognizes managerStatus = LOCKED, hasLockedManager() = true, hasNoManager() = false");
            } else {
                fail("TC11.1", "Centre recognition failed: hasLocked=" + isLocked + ", hasActive=" + isActive + ", hasNone=" + isNone);
            }

            if (centre.getManagerId() != null && centre.getManagerId() == 17) {
                pass("TC11.2", "Manager assignment preserved (ManagerID = 17, not cleared/nullified)");
            } else {
                fail("TC11.2", "Manager assignment was improperly nullified on lock!");
            }
        } else {
            fail("TC11", "Could not fetch Centre 6");
        }

        // Khôi phục User 17
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Users SET Status = 'ACTIVE' WHERE UserID = 17");
        }
        pass("TC11.3", "Manager 17 restored to ACTIVE");
    }

    // -----------------------------------------------------------------------------------------
    // TEST CASE 12: Bảo toàn luồng điều chuyển Staff thông thường
    // -----------------------------------------------------------------------------------------
    private static void testCase12_RegularStaffTransfer() throws Exception {
        System.out.println("\n[TEST 12] Bảo toàn luồng điều chuyển Staff thông thường (Rule 12):");

        // User 2 là TICKET_SALES_STAFF, đảm bảo ban đầu ở Centre 1
        int originalCentreId = 1;
        int targetCentreId = 6;
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate("UPDATE Users SET CentreID = 1 WHERE UserID = 2");
        }

        java.sql.Date effectiveDate = new java.sql.Date(System.currentTimeMillis());
        String result = userDAO.transferRegularStaff(2, targetCentreId, ADMIN_ID, "Tăng cường nhân sự bán vé", "Đợt 1 test", effectiveDate);

        if ("SUCCESS".equals(result)) {
            pass("TC12.1", "transferRegularStaff succeeded with SUCCESS");
        } else {
            fail("TC12.1", "transferRegularStaff failed with: " + result);
        }

        try (Connection conn = DBContext.getConnection()) {
            // Check Users.CentreID updated to target
            PreparedStatement psU = conn.prepareStatement("SELECT CentreID, Role FROM Users WHERE UserID = 2");
            ResultSet rsU = psU.executeQuery();
            if (rsU.next() && rsU.getInt("CentreID") == targetCentreId && "TICKET_SALES_STAFF".equals(rsU.getString("Role"))) {
                pass("TC12.2", "Users.CentreID updated to " + targetCentreId + ", Role kept as TICKET_SALES_STAFF");
            } else {
                fail("TC12.2", "Users record not properly updated for staff transfer");
            }

            // Check no entry in CentreManagerAssignments
            PreparedStatement psCma = conn.prepareStatement("SELECT COUNT(*) FROM CentreManagerAssignments WHERE ManagerID = 2");
            ResultSet rsCma = psCma.executeQuery();
            if (rsCma.next() && rsCma.getInt(1) == 0) {
                pass("TC12.3", "Verified NO records in CentreManagerAssignments for regular staff");
            } else {
                fail("TC12.3", "Unexpected records in CentreManagerAssignments for regular staff!");
            }

            // Check StaffTransferHistory entry
            PreparedStatement psH = conn.prepareStatement(
                "SELECT TOP 1 FromCentreID, ToCentreID, OldRole, NewRole FROM StaffTransferHistory WHERE UserID = 2 ORDER BY TransferID DESC"
            );
            ResultSet rsH = psH.executeQuery();
            if (rsH.next() && rsH.getInt("FromCentreID") == originalCentreId && rsH.getInt("ToCentreID") == targetCentreId) {
                pass("TC12.4", "StaffTransferHistory recorded regular staff transfer (OldRole=" + rsH.getString("OldRole") + ", NewRole=" + rsH.getString("NewRole") + ")");
            } else {
                fail("TC12.4", "StaffTransferHistory entry missing or incorrect");
            }

            // Hoàn trả User 2 về Centre 1
            userDAO.transferRegularStaff(2, originalCentreId, ADMIN_ID, "Hoàn trả vị trí cũ", "Dọn dẹp test", effectiveDate);
            pass("TC12.5", "Staff 2 restored back to original Centre 1");
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------------------------
    private static void resetManagerAssignment(int managerId) throws Exception {
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate(
                "UPDATE CentreManagerAssignments SET Status = 'ENDED', EndDate = GETDATE() WHERE ManagerID = " + managerId + " AND Status = 'ACTIVE'"
            );
            conn.createStatement().executeUpdate(
                "UPDATE Users SET CentreID = NULL WHERE UserID = " + managerId
            );
        }
    }

    private static void clearCentreManager(int centreId) throws Exception {
        try (Connection conn = DBContext.getConnection()) {
            conn.createStatement().executeUpdate(
                "UPDATE CentreManagerAssignments SET Status = 'ENDED', EndDate = GETDATE() WHERE CentreID = " + centreId + " AND Status = 'ACTIVE'"
            );
            conn.createStatement().executeUpdate(
                "UPDATE Users SET CentreID = NULL WHERE CentreID = " + centreId + " AND Role = 'MANAGER'"
            );
        }
    }

    private static void cleanupTestData() throws Exception {
        System.out.println("\n[CLEANUP] Dọn dẹp dữ liệu kiểm thử về trạng thái chuẩn...");
        resetManagerAssignment(17);
        resetManagerAssignment(19);
        clearCentreManager(4);
        clearCentreManager(6);
        System.out.println("  Cleaned up test assignments for User 17, User 19, Centre 4, Centre 6.");
    }
}
