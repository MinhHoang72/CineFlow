import dao.CentreDAO;
import dao.MovieDAO;
import model.entity.Centre;
import model.entity.Movie;
import service.CinemaService;
import service.MovieService;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bộ kiểm thử tự động toàn diện kiểm chứng các quy tắc nghiệp vụ theo tài liệu:
 * "CineFlow – Các trạng thái liên quan đến phân phối phim"
 */
public class DistributionLifecycleTestRunner {

    private static final CentreDAO centreDAO = new CentreDAO();
    private static final MovieDAO movieDAO = new MovieDAO();
    private static final CinemaService cinemaService = new CinemaService();
    private static final MovieService movieService = new MovieService();

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  KIỂM THỬ VÒNG ĐỜI TRẠNG THÁI PHÂN PHỐI PHIM & CỤM RẠP CINEFLOW");
        System.out.println("======================================================================");

        int testCentreId = 0;
        int testMovieActiveId = 0;
        int testMovieExpiredId = 0;

        try {
            // SETUP DATA
            testCentreId = createTestCentre("CineFlow Test Centre - Lifecycle");
            testMovieActiveId = createTestMovie("Test Movie - Still Active", LocalDate.now().plusDays(30));
            testMovieExpiredId = createTestMovie("Test Movie - Expired", LocalDate.now().minusDays(5));

            System.out.println(String.format("[SETUP] Đã tạo rạp test ID: %d, Phim active ID: %d, Phim hết hạn ID: %d",
                    testCentreId, testMovieActiveId, testMovieExpiredId));

            // -------------------------------------------------------------
            // TEST 1: Kiểm tra luồng chuyển trạng thái cụm rạp hợp lệ & bất hợp lệ
            // -------------------------------------------------------------
            test1_StateTransitions(testCentreId);

            // -------------------------------------------------------------
            // TEST 2: Phim chưa phân phối -> Chỉ cho phép phân phối khi rạp ACTIVE
            // -------------------------------------------------------------
            test2_DistributionOnlyOnActiveTheatre(testCentreId, testMovieActiveId);

            // -------------------------------------------------------------
            // TEST 3: Rạp chuyển sang SUSPENDED -> Phân phối ACTIVE thành SUSPENDED
            // -------------------------------------------------------------
            test3_TheatreSuspendedMakesDistributionsSuspended(testCentreId, testMovieActiveId, testMovieExpiredId);

            // -------------------------------------------------------------
            // TEST 4: Admin hủy thủ công khi rạp SUSPENDED -> Phân phối thành CANCELLED
            // -------------------------------------------------------------
            test4_ManualCancelWhileSuspendedBecomesCancelled(testCentreId, testMovieActiveId);

            // -------------------------------------------------------------
            // TEST 5: Rạp SUSPENDED sang ACTIVE -> Chỉ khôi phục phân phối chưa quá endDate
            // -------------------------------------------------------------
            test5_SuspendedToActiveRestoresOnlyNonExpired(testCentreId, testMovieActiveId, testMovieExpiredId);

            // -------------------------------------------------------------
            // TEST 6: Rạp chuyển sang INACTIVE -> Tất cả phân phối thành CANCELLED
            // -------------------------------------------------------------
            test6_TheatreInactiveMakesAllDistributionsCancelled(testCentreId, testMovieActiveId);

            // -------------------------------------------------------------
            // TEST 7: Rạp INACTIVE sang ACTIVE -> CANCELLED giữ nguyên (không tự khôi phục)
            // -------------------------------------------------------------
            test7_InactiveToActiveKeepsCancelled(testCentreId, testMovieActiveId);

            System.out.println("\n======================================================================");
            System.out.println("  [SUCCESS] TẤT CẢ 7 TEST CASE ĐẶC TẢ NGHIỆP VỤ ĐỀU ĐẠT CHUẨN 100%!");
            System.out.println("======================================================================");

        } catch (Throwable t) {
            System.err.println("\n[FAIL] XẢY RA LỖI KIỂM THỬ: " + t.getMessage());
            t.printStackTrace();
        } finally {
            cleanup(testCentreId, testMovieActiveId, testMovieExpiredId);
        }
    }

    private static void test1_StateTransitions(int centreId) {
        System.out.println("\n[TEST 1] Kiểm tra luồng chuyển trạng thái hợp lệ và từ chối luồng sai");

        // Đảm bảo rạp đang ACTIVE
        setCentreStatusDirect(centreId, "ACTIVE");

        // ACTIVE -> SUSPENDED: Hợp lệ
        CinemaService.Result<Centre> r1 = cinemaService.changeCentreStatus(centreId, "SUSPENDED", "Tester", "Test chuyển tạm ngưng");
        assertCondition(r1.isSuccess(), "ACTIVE -> SUSPENDED phải thành công");
        assertCondition("SUSPENDED".equals(getCentreStatus(centreId)), "Trạng thái rạp phải là SUSPENDED");

        // SUSPENDED -> ACTIVE: Hợp lệ
        CinemaService.Result<Centre> r2 = cinemaService.changeCentreStatus(centreId, "ACTIVE", "Tester", "Test mở lại");
        assertCondition(r2.isSuccess(), "SUSPENDED -> ACTIVE phải thành công");
        assertCondition("ACTIVE".equals(getCentreStatus(centreId)), "Trạng thái rạp phải là ACTIVE");

        // ACTIVE -> INACTIVE: Hợp lệ
        CinemaService.Result<Centre> r3 = cinemaService.changeCentreStatus(centreId, "INACTIVE", "Tester", "Test ngừng hoạt động");
        assertCondition(r3.isSuccess(), "ACTIVE -> INACTIVE phải thành công");
        assertCondition("INACTIVE".equals(getCentreStatus(centreId)), "Trạng thái rạp phải là INACTIVE");

        // INACTIVE -> SUSPENDED: BẤT HỢP LỆ (Phải bị từ chối!)
        CinemaService.Result<Centre> r4 = cinemaService.changeCentreStatus(centreId, "SUSPENDED", "Tester", "Test sai luồng");
        assertCondition(!r4.isSuccess(), "INACTIVE -> SUSPENDED phải bị TỪ CHỐI!");
        assertCondition("INACTIVE".equals(getCentreStatus(centreId)), "Trạng thái rạp vẫn phải là INACTIVE");

        // INACTIVE -> ACTIVE: Hợp lệ
        CinemaService.Result<Centre> r5 = cinemaService.changeCentreStatus(centreId, "ACTIVE", "Tester", "Test kích hoạt lại");
        assertCondition(r5.isSuccess(), "INACTIVE -> ACTIVE phải thành công");
        assertCondition("ACTIVE".equals(getCentreStatus(centreId)), "Trạng thái rạp phải là ACTIVE");

        System.out.println("  [PASS] Luồng chuyển trạng thái hoạt động chính xác theo bảng đặc tả.");
    }

    private static void test2_DistributionOnlyOnActiveTheatre(int centreId, int movieId) {
        System.out.println("\n[TEST 2] Phim chưa phân phối -> Chỉ cho phép phân phối cho rạp ACTIVE");

        // Thử khi rạp SUSPENDED
        setCentreStatusDirect(centreId, "SUSPENDED");
        Set<Integer> set = new HashSet<>();
        set.add(centreId);
        movieDAO.saveMovieDistribution(movieId, set);
        String distStatus = getDistributionStatus(centreId, movieId);
        assertCondition(distStatus == null || !"ACTIVE".equals(distStatus), "Không được phân phối cho rạp SUSPENDED");

        // Thử khi rạp INACTIVE
        setCentreStatusDirect(centreId, "INACTIVE");
        movieDAO.saveMovieDistribution(movieId, set);
        distStatus = getDistributionStatus(centreId, movieId);
        assertCondition(distStatus == null || !"ACTIVE".equals(distStatus), "Không được phân phối cho rạp INACTIVE");

        // Thử khi rạp ACTIVE
        setCentreStatusDirect(centreId, "ACTIVE");
        movieDAO.saveMovieDistribution(movieId, set);
        distStatus = getDistributionStatus(centreId, movieId);
        assertCondition("ACTIVE".equals(distStatus), "Rạp ACTIVE phải được phân phối thành công (Status = ACTIVE)");

        System.out.println("  [PASS] Nghiệp vụ chỉ cho phép phân phối phim khi rạp ACTIVE.");
    }

    private static void test3_TheatreSuspendedMakesDistributionsSuspended(int centreId, int movieActiveId, int movieExpiredId) {
        System.out.println("\n[TEST 3] Rạp chuyển sang SUSPENDED -> Phân phối ACTIVE chuyển sang SUSPENDED");

        setCentreStatusDirect(centreId, "ACTIVE");
        Set<Integer> set = new HashSet<>();
        set.add(centreId);
        movieDAO.saveMovieDistribution(movieActiveId, set);
        movieDAO.saveMovieDistribution(movieExpiredId, set);

        assertCondition("ACTIVE".equals(getDistributionStatus(centreId, movieActiveId)), "movieActiveId phải ACTIVE");
        assertCondition("ACTIVE".equals(getDistributionStatus(centreId, movieExpiredId)), "movieExpiredId phải ACTIVE");

        // Chuyển rạp sang SUSPENDED
        cinemaService.changeCentreStatus(centreId, "SUSPENDED", "Admin", "Bảo trì định kỳ");

        assertCondition("SUSPENDED".equals(getDistributionStatus(centreId, movieActiveId)), "movieActiveId phân phối phải thành SUSPENDED");
        assertCondition("SUSPENDED".equals(getDistributionStatus(centreId, movieExpiredId)), "movieExpiredId phân phối phải thành SUSPENDED");

        System.out.println("  [PASS] Khi rạp SUSPENDED, các phân phối chuyển sang SUSPENDED (giữ cấu hình, tạm mất hiệu lực).");
    }

    private static void test4_ManualCancelWhileSuspendedBecomesCancelled(int centreId, int movieId) {
        System.out.println("\n[TEST 4] Admin hủy thủ công phân phối khi rạp đang SUSPENDED -> Status thành CANCELLED");

        // Hủy thủ công: gọi saveMovieDistribution với tập rỗng
        movieDAO.saveMovieDistribution(movieId, new HashSet<>());

        String status = getDistributionStatus(centreId, movieId);
        assertCondition("CANCELLED".equals(status), "Phân phối bị hủy thủ công phải có status = CANCELLED, thực tế: " + status);

        System.out.println("  [PASS] Hủy thủ công khi rạp SUSPENDED gán đúng trạng thái CANCELLED.");
    }

    private static void test5_SuspendedToActiveRestoresOnlyNonExpired(int centreId, int movieActiveId, int movieExpiredId) {
        System.out.println("\n[TEST 5] Rạp SUSPENDED sang ACTIVE -> Chỉ khôi phục phân phối chưa quá endDate");

        // Đảm bảo movieActiveId đang SUSPENDED (tạo lại phân phối SUSPENDED)
        setDistributionDirect(centreId, movieActiveId, "SUSPENDED", false);
        // movieExpiredId đang SUSPENDED
        setDistributionDirect(centreId, movieExpiredId, "SUSPENDED", false);

        // Khôi phục rạp về ACTIVE
        cinemaService.changeCentreStatus(centreId, "ACTIVE", "Admin", "Mở lại hoạt động rạp");

        String activeMovieDist = getDistributionStatus(centreId, movieActiveId);
        String expiredMovieDist = getDistributionStatus(centreId, movieExpiredId);

        assertCondition("ACTIVE".equals(activeMovieDist), "Phim chưa hết hạn chiếu phải được khôi phục về ACTIVE, thực tế: " + activeMovieDist);
        assertCondition(!"ACTIVE".equals(expiredMovieDist), "Phim ĐÃ HẾT HẠN chiếu (quá endDate) KHÔNG ĐƯỢC khôi phục về ACTIVE! Thực tế: " + expiredMovieDist);

        System.out.println("  [PASS] Rạp mở lại từ SUSPENDED chỉ khôi phục các phim chưa quá endDate.");
    }

    private static void test6_TheatreInactiveMakesAllDistributionsCancelled(int centreId, int movieId) {
        System.out.println("\n[TEST 6] Rạp chuyển sang INACTIVE -> Tất cả phân phối thành CANCELLED");

        setCentreStatusDirect(centreId, "ACTIVE");
        Set<Integer> set = new HashSet<>();
        set.add(centreId);
        movieDAO.saveMovieDistribution(movieId, set);
        assertCondition("ACTIVE".equals(getDistributionStatus(centreId, movieId)), "Phim phải ACTIVE trước");

        // Chuyển rạp sang INACTIVE
        cinemaService.changeCentreStatus(centreId, "INACTIVE", "Admin", "Đóng cửa rạp lâu dài");

        String distStatus = getDistributionStatus(centreId, movieId);
        assertCondition("CANCELLED".equals(distStatus), "Phân phối phải chuyển sang CANCELLED khi rạp INACTIVE, thực tế: " + distStatus);

        System.out.println("  [PASS] Khi rạp INACTIVE, toàn bộ phân phối chuyển sang CANCELLED.");
    }

    private static void test7_InactiveToActiveKeepsCancelled(int centreId, int movieId) {
        System.out.println("\n[TEST 7] Rạp INACTIVE sang ACTIVE -> CANCELLED giữ nguyên (Không tự khôi phục)");

        assertCondition("CANCELLED".equals(getDistributionStatus(centreId, movieId)), "Phân phối phải đang CANCELLED");

        // Kích hoạt lại rạp sang ACTIVE
        cinemaService.changeCentreStatus(centreId, "ACTIVE", "Admin", "Tái kích hoạt rạp");

        String distStatus = getDistributionStatus(centreId, movieId);
        assertCondition("CANCELLED".equals(distStatus), "CANCELLED phải giữ nguyên sau khi rạp mở lại từ INACTIVE, thực tế: " + distStatus);

        System.out.println("  [PASS] Rạp mở lại từ INACTIVE bảo toàn bản ghi CANCELLED không tự khôi phục nhầm.");
    }

    // --- HELPER METHODS ---

    private static void assertCondition(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("THẤT BẠI: " + msg);
        }
    }

    private static int createTestCentre(String name) throws Exception {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO Centres (CentreName, Address, City, Phone, Status, CreatedAt) VALUES (?, '123 Test St', 'Hà Nội', '0999888777', 'ACTIVE', GETDATE())",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new IllegalStateException("Không tạo được test centre");
    }

    private static int createTestMovie(String title, LocalDate endDate) throws Exception {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO Movies (Title, Duration, AgeRating, Status, EndDate, CreatedAt) VALUES (?, 120, 'P', 'NOW_SHOWING', ?, GETDATE())",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setDate(2, java.sql.Date.valueOf(endDate));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new IllegalStateException("Không tạo được test movie");
    }

    private static String getCentreStatus(int centreId) {
        Centre c = centreDAO.getCentreById(centreId);
        return c != null ? c.getStatus() : null;
    }

    private static void setCentreStatusDirect(int centreId, String status) {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE Centres SET Status = ? WHERE CentreID = ?")) {
            ps.setString(1, status);
            ps.setInt(2, centreId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getDistributionStatus(int centreId, int movieId) {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Status FROM CentreMovies WHERE CentreID = ? AND MovieID = ?")) {
            ps.setInt(1, centreId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setDistributionDirect(int centreId, int movieId, String status, boolean isActive) {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "MERGE CentreMovies AS target USING (SELECT ? AS CentreID, ? AS MovieID) AS source "
                             + "ON target.CentreID = source.CentreID AND target.MovieID = source.MovieID "
                             + "WHEN MATCHED THEN UPDATE SET Status = ?, IsActive = ?, UpdatedAt = GETDATE() "
                             + "WHEN NOT MATCHED THEN INSERT (CentreID, MovieID, Status, IsActive, CreatedAt) VALUES (source.CentreID, source.MovieID, ?, ?, GETDATE());")) {
            ps.setInt(1, centreId);
            ps.setInt(2, movieId);
            ps.setString(3, status);
            ps.setBoolean(4, isActive);
            ps.setString(5, status);
            ps.setBoolean(6, isActive);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cleanup(int centreId, int movie1, int movie2) {
        try (Connection conn = DBContext.getConnection()) {
            if (centreId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CentreMovies WHERE CentreID = ?")) {
                    ps.setInt(1, centreId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Centres WHERE CentreID = ?")) {
                    ps.setInt(1, centreId);
                    ps.executeUpdate();
                }
            }
            if (movie1 > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CentreMovies WHERE MovieID = ?")) {
                    ps.setInt(1, movie1);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Movies WHERE MovieID = ?")) {
                    ps.setInt(1, movie1);
                    ps.executeUpdate();
                }
            }
            if (movie2 > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CentreMovies WHERE MovieID = ?")) {
                    ps.setInt(1, movie2);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Movies WHERE MovieID = ?")) {
                    ps.setInt(1, movie2);
                    ps.executeUpdate();
                }
            }
            System.out.println("[CLEANUP] Đã xóa toàn bộ dữ liệu test thành công.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
