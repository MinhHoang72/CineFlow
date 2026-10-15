package controller.showtime;

import dao.MovieDAO;
import dao.ShowtimeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import model.Showtime;

@WebServlet("/showtimes/update")
public class UpdateShowtimeServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            int showtimeId =
                    Integer.parseInt(
                            request.getParameter(
                                    "showtimeId"
                            )
                    );

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

            ShowtimeDAO showtimeDAO =
                    new ShowtimeDAO();

            MovieDAO movieDAO =
                    new MovieDAO();


            Showtime oldShowtime =
                    showtimeDAO.getById(
                            showtimeId
                    );

            if (oldShowtime == null
                    || !"SCHEDULED".equals(
                            oldShowtime.getStatus()
                    )) {

                setError(
                        request,
                        "Showtime cannot be edited."
                );

                redirect(request, response);

                return;
            }


            if (!showtimeDAO
                    .isHallAvailable(hallId)) {

                setError(
                        request,
                        "Selected hall is not available."
                );

                redirect(request, response);

                return;
            }


            if (!showtimeDAO
                    .isMovieAvailableForHall(
                            movieId,
                            hallId
                    )) {

                setError(
                        request,
                        "This movie is not available at the selected branch."
                );

                redirect(request, response);

                return;
            }


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


            LocalDateTime endTime =
                    startTime.plusMinutes(
                            duration
                    );


            int cleaningBuffer =
                    oldShowtime
                            .getCleaningBufferMinutes();


            boolean conflict =
                    showtimeDAO
                            .hasScheduleConflict(
                                    hallId,
                                    startTime,
                                    endTime,
                                    cleaningBuffer,

                                    // rất quan trọng
                                    showtimeId
                            );


            if (conflict) {

                setError(
                        request,
                        "Schedule conflict with another showtime."
                );

                redirect(request, response);

                return;
            }


            Showtime updated =
                    new Showtime();

            updated.setShowtimeId(
                    showtimeId
            );

            updated.setMovieId(
                    movieId
            );

            updated.setHallId(
                    hallId
            );

            updated.setStartTime(
                    startTime
            );

            updated.setEndTime(
                    endTime
            );

            updated.setCleaningBufferMinutes(
                    cleaningBuffer
            );


            boolean success =
                    showtimeDAO
                            .updateShowtime(
                                    updated
                            );


            if (success) {

                request.getSession()
                        .setAttribute(
                                "message",
                                "Showtime updated successfully."
                        );

            } else {

                setError(
                        request,
                        "Unable to update showtime."
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
            String error) {

        request.getSession()
                .setAttribute(
                        "error",
                        error
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