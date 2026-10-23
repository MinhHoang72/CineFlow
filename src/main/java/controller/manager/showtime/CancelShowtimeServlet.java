package controller.manager.showtime;

import dao.ShowtimeDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/showtimes/cancel")
public class CancelShowtimeServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        try {

            int showtimeId =
                    Integer.parseInt(
                            request.getParameter(
                                    "showtimeId"
                            )
                    );

            ShowtimeDAO dao =
                    new ShowtimeDAO();


            /*
             * Không cho cancel nếu
             * đã có booking active
             */
            if (dao.hasActiveBookings(
                    showtimeId)) {

                request.getSession()
                        .setAttribute(
                                "error",
                                "Cannot cancel showtime because it already has active bookings."
                        );

                redirect(request, response);

                return;
            }


            boolean success =
                    dao.cancelShowtime(
                            showtimeId
                    );


            if (success) {

                request.getSession()
                        .setAttribute(
                                "message",
                                "Showtime cancelled successfully."
                        );

            } else {

                request.getSession()
                        .setAttribute(
                                "error",
                                "Unable to cancel showtime."
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            request.getSession()
                    .setAttribute(
                            "error",
                            "Invalid showtime."
                    );
        }

        redirect(request, response);
    }


    private void redirect(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.sendRedirect(
                request.getContextPath()
                + "/showtimes"
        );
    }
}