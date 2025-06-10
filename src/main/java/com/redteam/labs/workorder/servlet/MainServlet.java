package com.redteam.labs.workorder.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.redteam.labs.workorder.dao.DocumentDAO;
import com.redteam.labs.workorder.dao.WorkOrderDAO;
import com.redteam.labs.workorder.model.User;
import com.redteam.labs.workorder.model.WorkOrder;

@WebServlet(urlPatterns = { "/jsp/main" }, initParams = { @WebInitParam(name = "uploadPath", value = "/uploads") })
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024)
public class MainServlet extends HttpServlet
{
    private static final long serialVersionUID = -2653774098247482321L;

    private String uploadPath;

    @Override
    public void init() throws ServletException
    {
        uploadPath = getServletContext().getRealPath(getInitParameter("uploadPath"));
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists())
            uploadDir.mkdirs();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        HttpSession session = req.getSession(false);
        User user = (User) session.getAttribute("user");
        if (user == null)
        {
            req.getRequestDispatcher("main.jsp").forward(req, resp);
            return;
        }

        List<WorkOrder> workOrders = user.getRole().equals("admin") ? WorkOrderDAO.getAllWorkOrders() :  WorkOrderDAO.getWorkOrdersByUserId(user.getId()); 
        session.setAttribute("isAdmin", user.getRole().equals("admin"));
        req.setAttribute("workOrders", workOrders);
        req.getRequestDispatcher("main.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null)
        {
            req.getRequestDispatcher("main.jsp").forward(req, resp);
            return;
        }

        // Ensure the request is multipart
        if (!req.getContentType().toLowerCase().startsWith("multipart/"))
        {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Form must be multipart");
            return;
        }

        // Extract form data
        String title = getFormField(req.getPart("title"));
        String description = getFormField(req.getPart("description"));
        String status = getFormField(req.getPart("status"));
        String solution = getFormField(req.getPart("solution"));
        String quoteStr = getFormField(req.getPart("quote"));
        String finalCostStr = getFormField(req.getPart("finalCost"));        
        

        // Validate required fields
        if (title == null || description == null)
        {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required fields");
            return;
        }

        // Create and save the WorkOrder
        WorkOrder order = new WorkOrder();
        order.setUserId(user.getId());
        order.setNumber("WO-" + System.currentTimeMillis()); // Generate a unique work order number
        order.setTitle(title);
        order.setDescription(description);
        order.setStatus(status != null ? status : "in progress");
        order.setCreatedAt(LocalDateTime.now());
        order.setSolution(solution != null ? solution : "");
        order.setQuote(quoteStr != null && !quoteStr.isEmpty() ? Integer.parseInt(quoteStr) : 0);
        order.setFinalCost(finalCostStr != null && !finalCostStr.isEmpty() ? Integer.parseInt(finalCostStr) : 0);

        boolean success = WorkOrderDAO.createWorkOrder(order);
        if (!success)
        {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create work order");
            return;
        }
        
        order = WorkOrderDAO.getWorkOrderByNumber(order.getNumber());
        
        // Handle files:
        Collection<Part> parts = req.getParts();
        for (Part part : parts)
        {
            if (part.getName().equals("uploadedFiles") && part.getSize() > 0)
            {
                String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();                
                DocumentDAO.saveDocument(order.getId(), uploadPath + "/" + user.getId(), fileName, "normal", part.getInputStream()); // Save document metadata if needed
            }
            
            if (part.getName().equals("complianceFiles") && part.getSize() > 0)
            {
                String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
                DocumentDAO.saveDocument(order.getId(), uploadPath + "/" + user.getId(), fileName, "compliance", part.getInputStream()); // Save document metadata if needed
            }
        }

        resp.sendRedirect("main?success=true");
    }

    @SuppressWarnings("resource")
    private String getFormField(Part part) throws IOException
    {
        InputStream is = part.getInputStream();
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n")).trim();
    }
}
