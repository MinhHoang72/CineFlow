package controller.manager.showtime;

import dao.SeatDAO;
import dao.ShowtimeDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;
import model.entity.Showtime;
import model.entity.SeatAvailability;
import service.ShowtimeSeatLayoutService;

@WebServlet("/showtimes/seats")
public class ViewShowtimeSeatsServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            int showtimeId =
                    Integer.parseInt(
                            request.getParameter("id")
                    );

            ShowtimeDAO showtimeDAO =
                    new ShowtimeDAO();

            SeatDAO seatDAO =
                    new SeatDAO();

            Showtime showtime = showtimeDAO.getById(showtimeId);
            request.setAttribute("showtime", showtime);
            if (showtime != null) {
                List<SeatAvailability> seats = seatDAO.getSeatAvailability(showtimeId);
                request.setAttribute("seats", seats);
                ShowtimeSeatLayoutService layoutService = new ShowtimeSeatLayoutService();
                java.util.Map<String,List<model.dto.showtime.ShowtimeSeatMatrixDTO>> rows = layoutService.buildRows(
                        seatDAO.getSeatConfigsByHall(showtime.getHallId()), seats);
                layoutService.attachPrices(rows,seatDAO.getShowtimeSeatPrices(showtimeId));
                request.setAttribute("seatRows", rows);
            }

            request.getRequestDispatcher(
                    "/WEB-INF/views/manager/showtime/showtimeSeatMatrix.jsp"
            ).forward(
                    request,
                    response
            );

        } catch (Exception e) {

            e.printStackTrace();

            response.sendRedirect(
                    request.getContextPath()
                    + "/showtimes"
            );
        }
    }
}
