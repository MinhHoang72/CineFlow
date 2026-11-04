import dao.MovieDAO;
import model.entity.Centre;
import model.entity.Movie;
import service.MovieService;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DistributionBusinessTestRunner {

    private static final MovieService movieService = new MovieService();
    private static final MovieDAO movieDAO = new MovieDAO();

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  VERIFICATION SUITE: MOVIE DISTRIBUTION BUSINESS RULES");
        System.out.println("======================================================================");

        int testMovieId = 0;
        try {
            // Bước 1: Tạo một bộ phim mới trong DB để kiểm thử
            Movie testMovie = new Movie();
            testMovie.setTitle("TEST DISTRIBUTION AUTO PHIM MOI " + System.currentTimeMillis());
            testMovie.setDuration(120);
            testMovie.setGenre("Hành Động");
            testMovie.setAgeRating("T16");
            testMovie.setStatus("COMING_SOON");
            testMovieId = movieDAO.insertMovie(testMovie);

            if (testMovieId <= 0) {
                throw new RuntimeException("Không tạo được phim test!");
            }
            System.out.println("[SETUP] Đã tạo phim test mới với ID: " + testMovieId);

            // TEST 1: Khi tạo phim mới, tất cả cụm rạp phải mặc định chưa phân phối (isDistributed == false)
            test1_NewMovieHasZeroDistribution(testMovieId);

            // TEST 2: Lấy danh sách rạp ACTIVE và INACTIVE trong hệ thống
            int activeCentreId1 = -1;
            int activeCentreId2 = -1;
            int inactiveCentreId = -1;

            List<Centre> initialCentres = movieService.getDistributionCentres(testMovieId);
            for (Centre c : initialCentres) {
                if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
                    if (activeCentreId1 == -1) activeCentreId1 = c.getCentreId();
                    else if (activeCentreId2 == -1) activeCentreId2 = c.getCentreId();
                } else {
                    if (inactiveCentreId == -1) inactiveCentreId = c.getCentreId();
                }
            }

            System.out.println("[INFO] Rạp hoạt động 1: " + activeCentreId1 + ", Rạp hoạt động 2: " + activeCentreId2
                    + ", Rạp không hoạt động/bảo trì: " + inactiveCentreId);

            // TEST 3: Bật phân phối cho 2 cụm rạp hoạt động -> Lưu và kiểm tra DB
            test3_SaveDistributionForActiveCentres(testMovieId, activeCentreId1, activeCentreId2);

            // TEST 4: Tắt riêng 1 cụm rạp (chỉ lưu activeCentreId1) -> activeCentreId2 phải chuyển sang false
            test4_ToggleOffIndividualCentre(testMovieId, activeCentreId1, activeCentreId2);

            // TEST 5: Thử phân phối cho cụm rạp đang bảo trì/tạm ngừng -> Không được phép lưu Active trong DB
            if (inactiveCentreId > 0) {
                test5_InactiveCentreCannotBeDistributed(testMovieId, activeCentreId1, inactiveCentreId);
            } else {
                System.out.println("  [SKIP] Hệ thống hiện không có cụm rạp INACTIVE, bỏ qua Test 5.");
            }

            // TEST 6: Mở lại cấu hình phân phối -> Trạng thái phải khớp chính xác 100% với DB
            test6_ReopenMatchesDatabase(testMovieId, activeCentreId1);

            System.out.println("\n[SUCCESS] TẤT CẢ 6 TEST CASE NGHIỆP VỤ PHÂN PHỐI PHIM ĐỀU ĐẠT CHUẨN!");
        } catch (Exception e) {
            System.err.println("\n[FAIL] XẢY RA LỖI KIỂM THỬ: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Dọn dẹp dữ liệu test
            if (testMovieId > 0) {
                cleanup(testMovieId);
            }
        }
    }

    private static void test1_NewMovieHasZeroDistribution(int movieId) {
        System.out.println("\n[TEST 1] Phim mới tạo -> Mặc định tất cả cụm rạp chưa được phân phối");
        List<Centre> centres = movieService.getDistributionCentres(movieId);
        int distributedCount = 0;
        for (Centre c : centres) {
            if (c.isDistributed()) {
                distributedCount++;
            }
        }
        if (distributedCount == 0) {
            System.out.println("  [PASS] 100% cụm rạp có isDistributed = false (Tổng số rạp: " + centres.size() + ")");
        } else {
            throw new AssertionError("Lỗi: Phim mới nhưng có " + distributedCount + " rạp bị đánh dấu là đã phân phối!");
        }
    }

    private static void test3_SaveDistributionForActiveCentres(int movieId, int id1, int id2) {
        System.out.println("\n[TEST 3] Lưu cấu hình phân phối cho 2 cụm rạp hoạt động (ID: " + id1 + ", " + id2 + ")");
        Set<Integer> set = new HashSet<>();
        set.add(id1);
        set.add(id2);
        boolean saved = movieService.saveMovieDistribution(movieId, set);
        if (!saved) {
            throw new AssertionError("Lưu phân phối thất bại!");
        }

        List<Centre> centres = movieService.getDistributionCentres(movieId);
        boolean id1Ok = false;
        boolean id2Ok = false;
        int totalDistributed = 0;

        for (Centre c : centres) {
            if (c.isDistributed()) {
                totalDistributed++;
                if (c.getCentreId() == id1) id1Ok = true;
                if (c.getCentreId() == id2) id2Ok = true;
            }
        }

        if (id1Ok && id2Ok && totalDistributed == 2) {
            System.out.println("  [PASS] Đã lưu chính xác 2 cụm rạp được chọn (isDistributed = true). Các rạp khác = false.");
        } else {
            throw new AssertionError("Lỗi: Dữ liệu phân phối sau khi lưu không khớp!");
        }
    }

    private static void test4_ToggleOffIndividualCentre(int movieId, int keepId, int turnOffId) {
        System.out.println("\n[TEST 4] Tắt riêng cụm rạp " + turnOffId + " -> Chỉ giữ lại cụm rạp " + keepId);
        Set<Integer> set = new HashSet<>();
        set.add(keepId);
        boolean saved = movieService.saveMovieDistribution(movieId, set);
        if (!saved) {
            throw new AssertionError("Lưu phân phối thất bại!");
        }

        List<Centre> centres = movieService.getDistributionCentres(movieId);
        for (Centre c : centres) {
            if (c.getCentreId() == keepId && !c.isDistributed()) {
                throw new AssertionError("Lỗi: Cụm rạp " + keepId + " bị mất phân phối!");
            }
            if (c.getCentreId() == turnOffId && c.isDistributed()) {
                throw new AssertionError("Lỗi: Cụm rạp " + turnOffId + " vẫn còn phân phối dù đã tắt!");
            }
        }
        System.out.println("  [PASS] Cụm rạp " + turnOffId + " đã chuyển sang OFF thành công, chỉ còn cụm rạp " + keepId + " là ON.");
    }

    private static void test5_InactiveCentreCannotBeDistributed(int movieId, int keepActiveId, int inactiveId) {
        System.out.println("\n[TEST 5] Kiểm tra ràng buộc rạp INACTIVE không được phép phân phối");
        Set<Integer> set = new HashSet<>();
        set.add(keepActiveId);
        set.add(inactiveId);
        movieService.saveMovieDistribution(movieId, set);

        List<Centre> centres = movieService.getDistributionCentres(movieId);
        for (Centre c : centres) {
            if (c.getCentreId() == inactiveId && c.isDistributed()) {
                throw new AssertionError("Lỗi: Cụm rạp bảo trì/ngừng hoạt động (ID " + inactiveId + ") lại được phân phối!");
            }
        }
        System.out.println("  [PASS] Rạp INACTIVE (ID " + inactiveId + ") bị chặn phân phối đúng như quy định.");
    }

    private static void test6_ReopenMatchesDatabase(int movieId, int expectedActiveId) {
        System.out.println("\n[TEST 6] Mở lại cấu hình phân phối -> Dữ liệu khớp tuyệt đối với CSDL");
        List<Centre> centres = movieService.getDistributionCentres(movieId);
        int onCount = 0;
        for (Centre c : centres) {
            if (c.isDistributed()) {
                onCount++;
                if (c.getCentreId() != expectedActiveId) {
                    throw new AssertionError("Lỗi: Cụm rạp không mong muốn có trạng thái ON: " + c.getCentreId());
                }
            }
        }
        if (onCount == 1) {
            System.out.println("  [PASS] Dữ liệu tải lại khớp 100% với trạng thái đã lưu.");
        } else {
            throw new AssertionError("Lỗi: Số lượng rạp ON không khớp: " + onCount);
        }
    }

    private static void cleanup(int movieId) {
        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CentreMovies WHERE MovieID = ?")) {
                ps.setInt(1, movieId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Movies WHERE MovieID = ?")) {
                ps.setInt(1, movieId);
                ps.executeUpdate();
            }
            System.out.println("[CLEANUP] Đã xóa dữ liệu test MovieID: " + movieId);
        } catch (Exception ignored) {
        }
    }
}
