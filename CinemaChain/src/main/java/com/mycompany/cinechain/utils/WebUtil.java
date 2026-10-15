package com.mycompany.cinechain.utils;

import com.mycompany.cinechain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WebUtil {

    /** Duong dan trang dang nhap cua FE-01. Sua lai neu ban lam login dung URL khac. */
    public static final String LOGIN_PATH = "/login";

    /**
     * Tra ve user dang nhap; neu chua dang nhap thi redirect sang trang login va tra ve null.
     */
    public static User requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(User.SESSION_KEY);
        if (user != null) return user;

        String back = req.getRequestURI();
        if ("GET".equals(req.getMethod()) && req.getQueryString() != null) back += "?" + req.getQueryString();
        resp.sendRedirect(req.getContextPath() + LOGIN_PATH + "?redirect="
                + URLEncoder.encode(back, StandardCharsets.UTF_8));
        return null;
    }

    /** Thong bao hien 1 lan o trang ke tiep (sau redirect). type: success | danger | warning | info */
    public static void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("flashType", type);
        req.getSession().setAttribute("flashMessage", message);
    }

    public static Integer parseInt(String value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
