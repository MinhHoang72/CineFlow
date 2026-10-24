package controller.manager.pricing.timeslot;

import dao.TimeSlotPricingDAO;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.entity.TimeSlotPricing;

@WebServlet("/pricing/time-slots/update")
public class UpdateTimeSlotPricingServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            int id =
                Integer.parseInt(
                    request.getParameter("id")
                );

            int centreId = service.ShowtimeService.requireCentreId(request);

            String slotName =
                request.getParameter(
                    "slotName"
                );

            LocalTime startTime =
                LocalTime.parse(
                    request.getParameter(
                        "startTime"
                    )
                );

            LocalTime endTime =
                LocalTime.parse(
                    request.getParameter(
                        "endTime"
                    )
                );

            double surcharge =
                Double.parseDouble(
                    request.getParameter(
                        "surcharge"
                    )
                );

            int managerId = service.ShowtimeService.requireUserId(request);

            if (!endTime.isAfter(startTime)) {

                setError(
                    request,
                    "End time must be later than start time."
                );

                redirect(
                    request,
                    response
                );

                return;
            }

            if (surcharge < 0) {

                setError(
                    request,
                    "Surcharge cannot be negative."
                );

                redirect(
                    request,
                    response
                );

                return;
            }

            TimeSlotPricingDAO dao =
                    new TimeSlotPricingDAO();

            boolean overlap =
                dao.hasOverlap(
                    centreId,
                    Time.valueOf(startTime),
                    Time.valueOf(endTime),

                    // Quan trọng:
                    // loại chính rule đang edit
                    id
                );

            if (overlap) {

                setError(
                    request,
                    "This time slot overlaps with another pricing rule."
                );

                redirect(
                    request,
                    response
                );

                return;
            }

            TimeSlotPricing rule =
                    new TimeSlotPricing();

            rule.setCentreId(
                    centreId
            );

            rule.setSlotName(
                    slotName
            );

            rule.setStartTime(
                    startTime
            );

            rule.setEndTime(
                    endTime
            );

            rule.setSurcharge(
                    surcharge
            );

            rule.setUpdatedBy(
                    managerId
            );

            boolean success =
                dao.updateRule(
                    id,
                    rule
                );

            if (success) {

                request.getSession()
                    .setAttribute(
                        "message",
                        "Time-slot pricing updated successfully."
                    );

            } else {

                setError(
                    request,
                    "Unable to update pricing rule."
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            setError(
                request,
                "Invalid input."
            );
        }

        redirect(
            request,
            response
        );
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
            + "/pricing/time-slots"
        );
    }
}