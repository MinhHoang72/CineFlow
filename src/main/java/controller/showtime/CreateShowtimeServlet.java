package controller.showtime;

import dao.MovieDAO;
import dao.ShowtimeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.time.LocalDateTime;

import model.entity.Showtime;

@WebServlet("/showtimes/create")
public class CreateShowtimeServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            int movieId =
                Integer.parseInt(
                    request.getParameter(
                        "movieId"
                    )
                );

            int hallId =
                Integer.parseInt(
                    request.getParameter(
                        "hallId"
                    )
                );

            LocalDateTime startTime =
                LocalDateTime.parse(
                    request.getParameter(
                        "startTime"
                    )
                );

            int cleaningBuffer = 15;

            /*
             * Demo manager ID.
             * Sau này lấy từ session.
             */
            int managerId = service.ShowtimeService.requireUserId(request);

            ShowtimeDAO showtimeDAO =
                    new ShowtimeDAO();

            MovieDAO movieDAO =
                    new MovieDAO();


            /*
             * 1. Check hall
             */
            if (!showtimeDAO
                    .isHallAvailable(hallId)) {

                setError(
                    request,
                    "Selected hall is not available."
                );

                redirect(request, response);

                return;
            }


            /*
             * 2. Check movie belongs
             * to centre
             */
            if (!showtimeDAO
                    .isMovieAvailableForHall(
                        movieId,
                        hallId
                    )) {

                setError(
                    request,
                    "This movie is not available at the selected centre."
                );

                redirect(request, response);

                return;
            }


            /*
             * 3. Get movie duration
             */
            int duration =
                    movieDAO.getMovieDuration(
                        movieId
                    );

            if (duration <= 0) {

                setError(
                    request,
                    "Invalid movie duration."
                );

                redirect(request, response);

                return;
            }


            /*
             * 4. Calculate EndTime
             */
            LocalDateTime endTime =
                    startTime.plusMinutes(
                        duration
                    );


            /*
             * 5. Check conflict
             */
            boolean conflict =
                showtimeDAO
                    .hasScheduleConflict(
                        hallId,
                        startTime,
                        endTime,
                        cleaningBuffer,
                        null
                    );

            if (conflict) {

                setError(
                    request,
                    "Schedule conflict: this hall is already occupied during the selected time."
                );

                redirect(request, response);

                return;
            }


            /*
             * 6. Create Showtime
             */
            Showtime showtime =
                    new Showtime();

            showtime.setMovieId(
                    movieId
            );

            showtime.setHallId(
                    hallId
            );

            showtime.setStartTime(
                    startTime
            );

            showtime.setEndTime(
                    endTime
            );

            showtime.setCleaningBufferMinutes(
                    cleaningBuffer
            );

            showtime.setCreatedBy(
                    managerId
            );


            boolean success =
                showtimeDAO.insertShowtime(
                    showtime
                );

            if (success) {

                request.getSession()
                    .setAttribute(
                        "message",
                        "Showtime created successfully."
                    );

            } else {

                setError(
                    request,
                    "Unable to create showtime."
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            setError(
                request,
                "Invalid showtime information."
            );
        }

        redirect(request, response);
    }


    private void setError(
            HttpServletRequest request,
            String message) {

        request.getSession()
            .setAttribute(
                "error",
                message
            );
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