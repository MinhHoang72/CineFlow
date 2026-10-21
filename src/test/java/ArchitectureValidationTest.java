import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class ArchitectureValidationTest {

    private static final Path WEBAPP_DIR = Path.of("src/main/webapp");
    private static final Path DTO_DIR = Path.of("src/main/java/model/dto");

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  CINEFLOW ARCHITECTURE INTEGRITY VERIFICATION");
        System.out.println("=================================================");

        testNoRedundantDtoFilesInRoot();
        testDtoPackageStandardization();
        testCoreServicesExist();
        testNoRedundantJspInFragments();
        testRequiredFragmentJspfFilesExist();
        testWebXmlJspfMapping();
        testNoEmptyDummyForwarderJspInViews();

        System.out.println("\n[SUCCESS] ALL ARCHITECTURE VERIFICATION CHECKS PASSED!");
    }

    static void testNoRedundantDtoFilesInRoot() throws IOException {
        try (Stream<Path> stream = Files.list(DTO_DIR)) {
            List<Path> rootFiles = stream
                    .filter(Files::isRegularFile)
                    .toList();
            if (!rootFiles.isEmpty()) {
                throw new AssertionError("Redundant DTO files found in root model/dto: " + rootFiles);
            }
        }
        System.out.println("  [PASS] Zero redundant DTO files in model/dto root");
    }

    static void testDtoPackageStandardization() throws ClassNotFoundException {
        String[] dtos = {
            "model.dto.customer.CustomerManagementDTO",
            "model.dto.centre.CentreManagementDTO",
            "model.dto.movie.MovieDistributionDTO",
            "model.dto.showtime.ShowtimeDetailDTO",
            "model.dto.showtime.ShowtimeSeatMatrixDTO",
            "model.dto.booking.BookingDetailDTO",
            "model.dto.booking.BookingHistoryDTO",
            "model.dto.pricing.SeatConfigDTO",
            "model.dto.fnb.FnbRowDTO",
            "model.dto.fnb.FnbSaleDTO",
            "model.dto.fnb.StockRowDTO",
            "model.dto.checkin.CheckInResultDTO",
            "model.dto.checkin.CheckInLogDTO",
            "model.dto.report.SalesSummaryDTO",
            "model.dto.report.SalesRowDTO",
            "model.dto.report.OccupancyRowDTO",
            "model.dto.common.PageDTO",
            "model.dto.common.OptionDTO"
        };
        for (String dto : dtos) {
            Class.forName(dto);
        }
        System.out.println("  [PASS] All standardized DTOs exist in model.dto.<domain>.*");
    }

    static void testCoreServicesExist() throws ClassNotFoundException {
        String[] services = {
            "service.BookingService",
            "service.PaymentService",
            "service.PricingService",
            "service.ShowtimeService",
            "service.UserService",
            "service.VoucherService"
        };
        for (String svc : services) {
            Class.forName(svc);
        }
        System.out.println("  [PASS] All 6 Core Services exist in service.*");
    }

    static void testNoRedundantJspInFragments() throws IOException {
        Path fragmentsDir = WEBAPP_DIR.resolve("WEB-INF/fragments");
        try (Stream<Path> stream = Files.list(fragmentsDir)) {
            List<Path> jspFiles = stream
                    .filter(p -> p.toString().endsWith(".jsp"))
                    .toList();
            if (!jspFiles.isEmpty()) {
                throw new AssertionError("Redundant .jsp files found in WEB-INF/fragments: " + jspFiles);
            }
        }
        System.out.println("  [PASS] Zero redundant .jsp files in WEB-INF/fragments");
    }

    static void testRequiredFragmentJspfFilesExist() {
        Path fragmentsDir = WEBAPP_DIR.resolve("WEB-INF/fragments");
        String[] files = {
            "adminSidebar.jspf", "adminTopbar.jspf", "managerSidebar.jspf", "managerTopbar.jspf",
            "header.jspf", "footer.jspf", "pagination.jspf", "cinemaBegin.jspf", "cinemaEnd.jspf",
            "showtimePricingNav.jspf"
        };
        for (String f : files) {
            if (!Files.exists(fragmentsDir.resolve(f))) {
                throw new AssertionError("Missing fragment JSPF: " + f);
            }
        }
        System.out.println("  [PASS] All standard .jspf fragment files exist");
    }

    static void testWebXmlJspfMapping() throws IOException {
        Path webXml = WEBAPP_DIR.resolve("WEB-INF/web.xml");
        if (!Files.exists(webXml)) throw new AssertionError("web.xml missing");
        String content = Files.readString(webXml);
        if (!content.contains("*.jspf")) throw new AssertionError("web.xml must map *.jspf");
        if (!content.contains("<jsp-config>")) throw new AssertionError("web.xml must contain <jsp-config>");
        System.out.println("  [PASS] web.xml maps *.jspf and contains <jsp-config>");
    }

    static void testNoEmptyDummyForwarderJspInViews() throws IOException {
        try (Stream<Path> stream = Files.walk(WEBAPP_DIR.resolve("WEB-INF/views"))) {
            List<Path> tinyFiles = stream
                    .filter(p -> p.toString().endsWith(".jsp"))
                    .filter(p -> {
                        try {
                            List<String> lines = Files.readAllLines(p);
                            return lines.size() <= 4 && lines.stream().anyMatch(l -> l.contains("sendRedirect"));
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();
            if (!tinyFiles.isEmpty()) {
                throw new AssertionError("Found dummy forwarder JSPs in views: " + tinyFiles);
            }
        }
        System.out.println("  [PASS] No dummy forwarder JSPs in WEB-INF/views");
    }
}
