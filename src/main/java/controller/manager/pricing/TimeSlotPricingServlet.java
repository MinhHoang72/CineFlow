package controller.manager.pricing;
import jakarta.servlet.annotation.WebServlet;
/** Shares the same centre-scoped, versioned policy workflow as the other surcharge tabs. */
@WebServlet("/pricing/time-slots")
public class TimeSlotPricingServlet extends SurchargePricingServlet {}
