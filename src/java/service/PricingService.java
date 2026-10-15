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


    public double calculateTicketPrice(
            int branchId,
            LocalDateTime showtime,
            String seatType) {

        Timestamp time =
                Timestamp.valueOf(
                        showtime
                );

        double basePrice =
                branchPricingDAO
                    .getBasePriceAt(
                        branchId,
                        time
                    );

        double timeSurcharge =
                timeSlotPricingDAO
                    .getSurchargeAt(
                        branchId,
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
                + seatSurcharge;
    }
}