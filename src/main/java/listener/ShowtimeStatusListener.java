package listener;

import dao.ShowtimeDAO;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Keeps persisted showtime states current while the web application is running. */
@WebListener
public class ShowtimeStatusListener implements ServletContextListener {
    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "cineflow-showtime-status");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                new ShowtimeDAO().synchronizeStatuses();
            } catch (Exception e) {
                // Keep the next run scheduled even if the database is temporarily unavailable.
                event.getServletContext().log("Unable to synchronize showtime statuses", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) executor.shutdownNow();
    }
}
