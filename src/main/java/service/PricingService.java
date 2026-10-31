package service;

import dao.CentrePricingDAO;
import dao.SurchargeDAO;
import dao.TimeSlotPricingDAO;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PricingService {
    /** VND input accepts plain digits or Vietnamese thousands separators. */
    public static double parseVnd(String value) {
        String text = value == null ? "" : value.trim();
        if (!text.matches("(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+)"))
            throw new IllegalArgumentException("Số tiền phải là số nguyên VNĐ không âm.");
        java.math.BigDecimal amount = new java.math.BigDecimal(text.replace(".", ""));
        if (amount.compareTo(new java.math.BigDecimal("9999999999")) > 0)
            throw new IllegalArgumentException("Số tiền vượt giới hạn cho phép.");
        return amount.doubleValue();
    }


    private CentrePricingDAO centrePricingDAO;
    private TimeSlotPricingDAO timeSlotPricingDAO;
    private SurchargeDAO surchargeDAO;


    public PricingService() {

        centrePricingDAO =
                new CentrePricingDAO();

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
                centrePricingDAO
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