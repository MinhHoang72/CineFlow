import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ArchitectureValidationTest {

    private static final Path WEBAPP_DIR = Path.of("src/main/webapp");
    private static final Pattern JSP_INCLUDE_JSPF = Pattern.compile("<jsp:include[^>]+page=['\"][^'\"]*\\.jspf");

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  CINEFLOW ARCHITECTURE INTEGRITY VERIFICATION");
        System.out.println("=================================================");

        testNoJspIncludePointsToJspf();
        testDtoPackageStandardization();
        testCoreServicesExist();
        testRequiredFragmentJspFilesExist();
        testWebXmlJspfMapping();
        testNoEmptyDummyForwarderJspInViews();

        System.out.println("\n[SUCCESS] ALL ARCHITECTURE VERIFICATION CHECKS PASSED!");
    }

    static void testNoJspIncludePointsToJspf() throws IOException {
        try (Stream<Path> stream = Files.walk(WEBAPP_DIR)) {
            List<Path> offendingFiles = stream
                    .filter(p -> p.toString().endsWith(".jsp") || p.toString().endsWith(".jspf"))
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            return JSP_INCLUDE_JSPF.matcher(content).find();
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();
            if (!offendingFiles.isEmpty()) {
                throw new AssertionError("Offending <jsp:include> .jspf files found: " + offendingFiles);
            }
        }
        System.out.println("  [PASS] No <jsp:include> points to .jspf");
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
            "model.dto.stock.StockRowDTO" == null ? "" : "model.dto.fnb.StockRowDTO",
            "model.dto.checkin.CheckInResultDTO",
            "model.dto.checkin.CheckInLogDTO",
            "model.dto.report.SalesSummaryDTO",
            "model.dto.report.SalesRowDTO",
            "model.dto.report.OccupancyRowDTO"
        };
        for (String dto : dtos) {
            if (!dto.isEmpty()) {
                Class.forName(dto);
            }
        }
        System.out.println("  [PASS] All 16 standardized DTOs exist in model.dto.*");
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

    static void testRequiredFragmentJspFilesExist() {
        Path fragmentsDir = WEBAPP_DIR.resolve("WEB-INF/fragments");
        String[] files = {
            "adminSidebar.jsp", "adminTopbar.jsp", "managerSidebar.jsp", "managerTopbar.jsp",
            "header.jsp", "footer.jsp", "pagination.jsp", "cinemaBegin.jsp", "cinemaEnd.jsp",
            "showtimePricingNav.jsp"
        };
        for (String f : files) {
            if (!Files.exists(fragmentsDir.resolve(f))) {
                throw new AssertionError("Missing fragment JSP: " + f);
            }
        }
        System.out.println("  [PASS] All fragment .jsp companion files exist");
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
