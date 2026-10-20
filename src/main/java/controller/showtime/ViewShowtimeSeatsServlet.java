package controller.showtime;

import dao.SeatDAO;
import dao.ShowtimeDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

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

            request.setAttribute(
                    "showtime",
                    showtimeDAO.getById(
                            showtimeId
                    )
            );

            request.setAttribute(
                    "seats",
                    seatDAO.getSeatAvailability(
                            showtimeId
                    )
            );

            request.getRequestDispatcher(
                    "/WEB-INF/views/showtime/showtimeSeatMatrix.jsp"
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