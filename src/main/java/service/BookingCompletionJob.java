package service;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Mỗi phút chuyển các đơn CONFIRMED của suất đã chiếu xong sang COMPLETED. */
@WebListener
public class BookingCompletionJob implements ServletContextListener {

    private final BookingService bookingService = new BookingService();
    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "cineflow-booking-completion");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                bookingService.completeFinishedBookings();
            } catch (Exception e) {
                // Lỗi DB tạm thời không được làm dừng các lần chạy sau
                event.getServletContext().log("Unable to complete finished bookings", e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) executor.shutdownNow();
    }
}
