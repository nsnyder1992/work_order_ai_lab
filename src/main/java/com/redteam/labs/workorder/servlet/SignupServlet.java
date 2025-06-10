package com.redteam.labs.workorder.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.IOException;

import com.redteam.labs.workorder.dao.UserDAO;

@WebServlet("/signup")
public class SignupServlet extends HttpServlet {

    private static final long serialVersionUID = -5451031327690591331L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/signup.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.length() < 1 || password.length() < 1 ) {
            req.setAttribute("error", "Username and password are required.");
            req.getRequestDispatcher("/signup.jsp").forward(req, resp);
            return;
        }

        // Password validation
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$";
        if (!password.matches(passwordPattern)) {
            req.setAttribute("error", "<p>Password must include:</p><ul><li>8-16 characters</li>"
                    + " <li>1 lowercase</li><li>1 uppercase</li><li>1 number</li><li>1 special character</li></ul>");
            req.getRequestDispatcher("/signup.jsp").forward(req, resp);
            return;
        }
        
        boolean success = false;
        try
        {
            success = UserDAO.registerUser(username, password);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (success) {
            resp.sendRedirect("login.jsp");
        } else {
            req.setAttribute("error", "Username already exists or error occurred.");
            req.getRequestDispatcher("signup.jsp").forward(req, resp);
        }
    }

}