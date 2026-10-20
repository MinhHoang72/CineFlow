package controller;

import dao.FnbDAO;
import dao.OperationsReportDAO;
import dao.SalesReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.OccupancyRowDTO;
import model.OptionDTO;
import model.SalesRowDTO;
import model.User;

@WebServlet("/report/*")
public class ReportHubServlet extends HttpServlet {

    private static final int PAGE_SIZE = 15;
    private static final int MAX_DAYS = 366;
    private final SalesReportDAO sales = new SalesReportDAO();
    private final OperationsReportDAO operations = new OperationsReportDAO();
    private final FnbDAO fnb = new FnbDAO();

    private String crumb(User user, String base) {
        String label = fnb.centreLabel(user.getCentreId());
        return label == null ? base : base + " · " + label;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/report/sales", StandardCharsets.UTF_8));
            return;
        }
        boolean admin = "ADMIN".equals(user.getRole());
        boolean manager = "MANAGER".equals(user.getRole());
        if (!admin && !manager) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin và Manager mới xem được báo cáo.");
            return;
        }
        if (manager && user.getCentreId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa được gán Centre.");
            return;
        }

        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if ("/".equals(path)) {
            response.sendRedirect(request.getContextPath() + (admin ? "/report/chain" : "/report/sales"));
            return;
        }

        LocalDate today = LocalDate.now();
        boolean forward = "/operations".equals(path);
        LocalDate to = parseDate(request.getParameter("to"), forward ? today.plusDays(7) : today);
        LocalDate from = parseDate(request.getParameter("from"), forward ? today.minusDays(7) : to.minusDays(29));
        if (from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        if (from.plusDays(MAX_DAYS).isBefore(to)) {
            from = to.minusDays(MAX_DAYS - 1);
        }
        LocalDate toExclusive = to.plusDays(1);

        Integer centreId = user.getCentreId();
        List<OptionDTO> centres = null;
        if (admin) {
            centres = fnb.centres();
            centreId = null;
            String centreText = request.getParameter("centreId");
            if (centreText != null && centreText.matches("[0-9]+") && centreText.length() < 10) {
                int wanted = Integer.parseInt(centreText);
                for (OptionDTO option : centres) {
                    if (option.getId() == wanted) {
                        centreId = wanted;
                    }
                }
            }
        }

        String filters = "from=" + from + "&to=" + to + (admin ? "&centreId=" + (centreId == null ? "" : centreId) : "");
        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.setAttribute("isAdmin", admin);
        request.setAttribute("centres", centres);
        request.setAttribute("selectedCentre", centreId);
        request.setAttribute("filters", filters);
        request.setAttribute("pageBreadcrumb", admin ? "Báo cáo" : crumb(user, "Báo cáo"));

        switch (path) {
            case "/sales":
                showSales(request, response, from, toExclusive, centreId);
                break;
            case "/sales/export":
                exportSales(response, from, toExclusive, centreId);
                break;
            case "/operations":
                showOperations(request, response, from, toExclusive, centreId, filters);
                break;
            case "/chain":
                if (!admin) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Dashboard chuỗi chỉ dành cho Admin.");
                    return;
                }
                showChain(request, response, from, toExclusive);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private LocalDate parseDate(String text, LocalDate fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private void showSales(HttpServletRequest request, HttpServletResponse response,
            LocalDate from, LocalDate toExclusive, Integer centreId) throws ServletException, IOException {
        request.setAttribute("summary", sales.summary(from, toExclusive, centreId));
        request.setAttribute("days", sales.byDay(from, toExclusive, centreId));
        request.setAttribute("movies", sales.byMovie(from, toExclusive, centreId, 10));
        request.setAttribute("centreRows", sales.byCentre(from, toExclusive, centreId));
        request.setAttribute("fnbTop", sales.topFnb(from, toExclusive, centreId, 10));
        request.setAttribute("pageTitle", "Doanh thu & bán hàng");
        request.getRequestDispatcher("/WEB-INF/views/report/sales.jsp").forward(request, response);
    }

    private void exportSales(HttpServletResponse response, LocalDate from, LocalDate toExclusive, Integer centreId)
            throws IOException {
        List<SalesRowDTO> days = sales.byDay(from, toExclusive, centreId);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"doanh-thu_" + from + "_" + toExclusive.minusDays(1) + ".csv\"");
        PrintWriter out = response.getWriter();
        out.print('﻿');
        out.print("Ngày,Đơn đặt vé,Số vé,Doanh thu vé,Doanh thu bắp nước,Tổng\r\n");
        for (SalesRowDTO row : days) {
            out.print(row.getLabel() + "," + row.getBookings() + "," + row.getTickets() + ","
                    + row.getTicketRevenue().toPlainString() + "," + row.getFnbRevenue().toPlainString() + ","
                    + row.getTotal().toPlainString() + "\r\n");
        }
    }

    private void showOperations(HttpServletRequest request, HttpServletResponse response,
            LocalDate from, LocalDate toExclusive, Integer centreId, String filters) throws ServletException, IOException {
        int page = 1;
        try {
            page = Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException exception) {
            page = 1;
        }
        request.setAttribute("summary", operations.summary(from, toExclusive, centreId));
        request.setAttribute("result", operations.showtimes(from, toExclusive, centreId, page, PAGE_SIZE));
        request.setAttribute("scans", operations.scansByStaff(from, toExclusive, centreId));
        request.setAttribute("pagerBase", "/report/operations?" + filters + "&page=");
        request.setAttribute("pageTitle", "Chỉ số vận hành");
        request.getRequestDispatcher("/WEB-INF/views/report/operations.jsp").forward(request, response);
    }

    private void showChain(HttpServletRequest request, HttpServletResponse response,
            LocalDate from, LocalDate toExclusive) throws ServletException, IOException {
        List<SalesRowDTO> days = sales.byDay(from, toExclusive, null);
        if (days.size() > 31) {
            days = days.subList(days.size() - 31, days.size());
        }
        Map<String, Double> occupancy = new HashMap<>();
        for (OccupancyRowDTO row : operations.occupancyByCentre(from, toExclusive)) {
            occupancy.put(row.getLabel(), row.getOccupancy());
        }
        request.setAttribute("summary", sales.summary(from, toExclusive, null));
        request.setAttribute("ops", operations.summary(from, toExclusive, null));
        request.setAttribute("days", days);
        request.setAttribute("centreRows", sales.byCentre(from, toExclusive, null));
        request.setAttribute("occupancy", occupancy);
        request.setAttribute("movies", sales.byMovie(from, toExclusive, null, 5));
        request.setAttribute("fnbTop", sales.topFnb(from, toExclusive, null, 5));
        request.setAttribute("activeCentres", operations.activeCentres());
        request.setAttribute("customers", operations.activeCustomers());
        request.setAttribute("pageTitle", "Dashboard chuỗi");
        request.getRequestDispatcher("/WEB-INF/views/report/chain.jsp").forward(request, response);
    }
}
