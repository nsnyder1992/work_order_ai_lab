package com.redteam.labs.workorder.listener;

import com.redteam.labs.workorder.util.DatabaseInit;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("App starting up — initializing database...");
        try
        {
            DatabaseInit.initialize();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Clean-up code if needed on shutdown
    }
}