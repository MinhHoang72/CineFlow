package service;

import dao.BranchPricingDAO;
import dao.SurchargeDAO;
import dao.TimeSlotPricingDAO;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PricingService {

    private BranchPricingDAO branchPricingDAO;
    private TimeSlotPricingDAO timeSlotPricingDAO;
    private SurchargeDAO surchargeDAO;


    public PricingService() {

        branchPricingDAO =
                new BranchPricingDAO();

        timeSlotPricingDAO =
                new TimeSlotPricingDAO();

        surchargeDAO =
                new SurchargeDAO();
    }


    /** Legacy callers use a 2D hall; pass hallType explicitly for actual bookings. */
    public double calculateTicketPrice(int centreId, LocalDateTime showtime, String seatType) {
        return calculateTicketPrice(centreId, showtime, seatType, "2D");
    }

    public double calculateTicketPrice(
            int centreId,
            LocalDateTime showtime,
            String seatType,
            String hallType) {

        Timestamp time =
                Timestamp.valueOf(
                        showtime
                );

        double basePrice =
                branchPricingDAO
                    .getBasePriceAt(
                        centreId,
                        time
                    );

        double timeSurcharge =
                timeSlotPricingDAO
                    .getSurchargeAt(
                        centreId,
                        time
                    );

        double seatSurcharge =
                surchargeDAO
                    .getSeatSurchargeAt(
                        seatType,
                        time
                    );

        return basePrice
                + timeSurcharge
                + seatSurcharge
                + surchargeDAO.getHallSurchargeAt(hallType, time);
    }
}