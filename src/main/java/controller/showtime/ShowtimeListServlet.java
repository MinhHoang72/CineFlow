package controller.showtime;

import dao.HallDAO;
import dao.MovieDAO;
import dao.ShowtimeDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/showtimes")
public class ShowtimeListServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        int centreId = service.ShowtimeService.requireCentreId(request);

        ShowtimeDAO showtimeDAO =
                new ShowtimeDAO();

        MovieDAO movieDAO =
                new MovieDAO();

        HallDAO hallDAO =
                new HallDAO();

        request.setAttribute(
                "showtimes",
                showtimeDAO.getByCentre(centreId)
        );

        request.setAttribute(
                "movies",
                movieDAO.getAvailableMoviesByCentre(
                        centreId
                )
        );

        request.setAttribute(
                "halls",
                hallDAO.getAvailableHallsByCentre(
                        centreId
                )
        );

        request.getRequestDispatcher(
                "/WEB-INF/views/showtime/showtimeList.jsp"
        ).forward(
                request,
                response
        );
    }
}