package com.redteam.labs.workorder.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.IOException;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 5746507108047218084L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false); // get existing session, don't create new
        if (session != null) {
            session.setAttribute("user", null); // clear user attribute
            session.invalidate();
        }
        resp.sendRedirect("login.jsp");
    }
}