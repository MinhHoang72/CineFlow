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

        int branchId = 1;

        ShowtimeDAO showtimeDAO =
                new ShowtimeDAO();

        MovieDAO movieDAO =
                new MovieDAO();

        HallDAO hallDAO =
                new HallDAO();

        request.setAttribute(
                "showtimes",
                showtimeDAO.getByBranch(branchId)
        );

        request.setAttribute(
                "movies",
                movieDAO.getAvailableMoviesByBranch(
                        branchId
                )
        );

        request.setAttribute(
                "halls",
                hallDAO.getAvailableHallsByBranch(
                        branchId
                )
        );

        request.getRequestDispatcher(
                "/showtime-list.jsp"
        ).forward(
                request,
                response
        );
    }
}