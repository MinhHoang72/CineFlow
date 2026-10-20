package controller.checkin;

import dao.CheckInDAO;
import dao.FnbDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;
import model.dto.CheckInResultDTO;
import model.entity.User;

@WebServlet("/checkin/*")
public class TicketCheckInServlet extends HttpServlet {

    private static final int HISTORY_SIZE = 20;
    private final CheckInDAO dao = new CheckInDAO();
    private final FnbDAO centres = new FnbDAO();

    private String crumb(User user, String base) {
        String label = centres.centreLabel(user.getCentreId());
        return label == null ? base : base + " · " + label;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = loginUser(request, response);
        if (user == null || !canEnter(user, response)) {
            return;
        }
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if (!"/".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        show(request, response, user, null, "");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = loginUser(request, response);
        if (user == null || !canEnter(user, response)) {
            return;
        }
        if (!"TICKET_CHECKER_STAFF".equals(user.getRole())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ nhân viên soát vé của Centre mới được soát vé.");
            return;
        }
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute("checkinCsrf");
        if (expected == null || !expected.equals(request.getParameter("_csrf"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên làm việc không hợp lệ, hãy tải lại trang.");
            return;
        }
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if (!"/verify".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String code = request.getParameter("code") == null ? "" : request.getParameter("code").trim();
        if (code.isEmpty() || code.length() > 500) {
            show(request, response, user,
                    new CheckInResultDTO(false, code.isEmpty() ? "Hãy nhập hoặc quét mã vé." : "Mã vé không hợp lệ.", null), code);
            return;
        }
        CheckInResultDTO result;
        try {
            result = dao.checkIn(code, user.getUserId(), user.getCentreId());
        } catch (SQLException exception) {
            exception.printStackTrace();
            result = new CheckInResultDTO(false, "Lỗi hệ thống, vui lòng thử lại.", null);
        }
        show(request, response, user, result, result.isSuccess() ? "" : code);
    }

    private User loginUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/checkin", StandardCharsets.UTF_8));
        }
        return user;
    }

    private boolean canEnter(User user, HttpServletResponse response) throws IOException {
        boolean staff = "TICKET_CHECKER_STAFF".equals(user.getRole());
        boolean manager = "MANAGER".equals(user.getRole());
        if (!staff && !manager) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ nhân viên soát vé và quản lý Centre mới vào được trang soát vé.");
            return false;
        }
        if (user.getCentreId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản chưa được gán Centre.");
            return false;
        }
        return true;
    }

    private void show(HttpServletRequest request, HttpServletResponse response, User user,
            CheckInResultDTO result, String code) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute("checkinCsrf");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("checkinCsrf", token);
        }
        request.setAttribute("csrf", token);
        request.setAttribute("result", result);
        request.setAttribute("code", code);
        request.setAttribute("canVerify", "TICKET_CHECKER_STAFF".equals(user.getRole()));
        request.setAttribute("logs", dao.recent(user.getCentreId(), HISTORY_SIZE));
        request.setAttribute("pageTitle", "Soát vé");
        request.setAttribute("pageBreadcrumb", crumb(user, "Vận hành"));
        request.getRequestDispatcher("/WEB-INF/views/checkin/ticketCheckIn.jsp").forward(request, response);
    }
}
