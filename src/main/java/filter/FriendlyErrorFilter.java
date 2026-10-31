package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;

/** Chuyển các phản hồi lỗi về giao diện lỗi CineFlow. */
public class FriendlyErrorFilter implements Filter {

    private static final String FORWARDED = FriendlyErrorFilter.class.getName() + ".FORWARDED";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (req.getAttribute(FORWARDED) != null) {
            chain.doFilter(request, response);
            return;
        }

        StatusCaptureResponse captured = new StatusCaptureResponse(resp);
        chain.doFilter(request, captured);

        int status = captured.getStatus();
        if (status >= 400 && !resp.isCommitted()) {
            req.setAttribute(FORWARDED, Boolean.TRUE);
            req.setAttribute("jakarta.servlet.error.status_code", status);
            RequestDispatcher dispatcher = req.getRequestDispatcher("/WEB-INF/views/error/error.jsp");
            dispatcher.forward(req, resp);
        }
    }

    private static final class StatusCaptureResponse extends HttpServletResponseWrapper {
        private int status = HttpServletResponse.SC_OK;

        private StatusCaptureResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int status) {
            this.status = status;
            super.setStatus(status);
        }

        @Override
        public void sendError(int status) throws IOException {
            this.status = status;
            super.setStatus(status);
        }

        @Override
        public void sendError(int status, String message) throws IOException {
            this.status = status;
            super.setStatus(status);
        }

        @Override
        public int getStatus() {
            return status;
        }
    }
}
