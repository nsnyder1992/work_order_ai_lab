package com.redteam.labs.workorder.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.IOException;

import com.redteam.labs.workorder.dao.UserDAO;
import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.util.AccountLockoutManager;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 2762906072275930532L;
    private final AccountLockoutManager lockoutManager = new AccountLockoutManager();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.length() < 1 || username.length() < 1) {
            req.setAttribute("error", "Username and password are required.");
            req.getRequestDispatcher("login.jsp").forward(req, resp);
            return;
        }
        
        if (lockoutManager.isLockedOut(username)) {
            long minutesLeft = lockoutManager.getRemainingLockoutMillis(username) / 60000;
            req.setAttribute("error", "Account locked. Try again in " + minutesLeft + " minutes.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        User user = UserDAO.authenticateUser(username, password);

        if (user != null) {
            lockoutManager.reset(username);
            HttpSession session = req.getSession();
            session.setAttribute("user", user);
            session.setAttribute("isAdmin", user.getRole().equals("admin"));
            session.setAttribute("userId", user.getId());
            resp.sendRedirect("jsp/main.jsp");  // Redirect to main page after login
        } else {
            lockoutManager.registerFailedAttempt(username);
            req.setAttribute("error", "Invalid username or password.");
            req.getRequestDispatcher("login.jsp").forward(req, resp);
        }
    }

}