package controller.manager.showtime;

import dao.HallDAO;
import dao.MovieDAO;
import dao.ShowtimeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import model.entity.Showtime;

@WebServlet("/showtimes/edit")
public class EditShowtimeServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println(
                "===== EDIT SHOWTIME ====="
        );

        System.out.println(
                "id param = "
                + request.getParameter("id")
        );

        try {

            int id =
                    Integer.parseInt(
                            request.getParameter("id")
                    );

            ShowtimeDAO showtimeDAO =
                    new ShowtimeDAO();

            Showtime showtime =
                    showtimeDAO.getById(id);

            System.out.println(
                    "Showtime = "
                    + (showtime == null
                    ? "NULL"
                    : showtime.getShowtimeId())
            );

            if (showtime != null) {

                System.out.println(
                        "Status = "
                        + showtime.getStatus()
                );
            }

            if (showtime == null) {

                response.getWriter().println(
                        "Showtime not found"
                );

                return;
            }

            int centreId = service.ShowtimeService.requireCentreId(request);

            MovieDAO movieDAO =
                    new MovieDAO();

            HallDAO hallDAO =
                    new HallDAO();

            request.setAttribute(
                    "showtime",
                    showtime
            );

            request.setAttribute(
                    "movies",
                    movieDAO
                            .getAvailableMoviesByCentre(
                                    centreId
                            )
            );

            request.setAttribute(
                    "halls",
                    hallDAO
                            .getAvailableHallsByCentre(
                                    centreId
                            )
            );

            request.getRequestDispatcher(
                    "/WEB-INF/views/manager/showtime/showtimeEdit.jsp"
            ).forward(
                    request,
                    response
            );

        } catch (Exception e) {

            e.printStackTrace();

            response.getWriter().println(
                    "ERROR: "
                    + e.getMessage()
            );
        }
    }
}