package controller.pricing.timeslot;

import dao.TimeSlotPricingDAO;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.entity.TimeSlotPricing;

@WebServlet("/pricing/time-slots/save")
public class SaveTimeSlotPricingServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            int centreId = utils.CinemaContext.requireCentreId(request);

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

            int managerId = utils.CinemaContext.requireUserId(request);

            if (!endTime.isAfter(startTime)) {

                request.getSession()
                        .setAttribute(
                            "error",
                            "End time must be later than start time."
                        );

                redirect(request, response);

                return;
            }

            if (surcharge < 0) {

                request.getSession()
                        .setAttribute(
                            "error",
                            "Surcharge cannot be negative."
                        );

                redirect(request, response);

                return;
            }

            TimeSlotPricingDAO dao =
                    new TimeSlotPricingDAO();

            boolean overlap =
                    dao.hasOverlap(
                            centreId,
                            Time.valueOf(startTime),
                            Time.valueOf(endTime),
                            null
                    );
            System.out.println("Servlet BranchID = " + centreId);
System.out.println("Servlet Start = " + startTime);
System.out.println("Servlet End = " + endTime);
System.out.println("Servlet Overlap = " + overlap);

            if (overlap) {

                request.getSession()
                        .setAttribute(
                            "error",
                            "This time slot overlaps with an existing pricing rule."
                        );

                redirect(request, response);

                return;
            }

            TimeSlotPricing item =
                    new TimeSlotPricing();

            item.setBranchId(centreId);
            item.setSlotName(slotName);
            item.setStartTime(startTime);
            item.setEndTime(endTime);
            item.setSurcharge(surcharge);
            item.setUpdatedBy(managerId);

            boolean success =
                    dao.insert(item);

            if (success) {

                request.getSession()
                        .setAttribute(
                            "message",
                            "Time-slot pricing added successfully."
                        );

            } else {

                request.getSession()
                        .setAttribute(
                            "error",
                            "Unable to save time-slot pricing."
                        );
            }

        } catch (Exception e) {

            request.getSession()
                    .setAttribute(
                        "error",
                        "Invalid input."
                    );

            e.printStackTrace();
        }

        redirect(request, response);
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