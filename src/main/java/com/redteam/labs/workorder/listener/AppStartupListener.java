package com.redteam.labs.workorder.listener;

import com.redteam.labs.workorder.util.DatabaseInit;

import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

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
            System.out.println("Database initialization completed successfully.");
        }
        catch (Exception e)
        {
            System.err.println("FATAL ERROR: Database initialization failed!");
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }

        try(InputStream is = getClass().getClassLoader().getResourceAsStream("app.properties"))
        {
            if (is == null) {
                System.err.println("WARNING: app.properties file not found in classpath");
                return;
            }
            
            Properties props = new Properties();
            props.load(is);
            System.out.println("Loading " + props.size() + " properties from app.properties:");
            for(Entry<Object, Object> prop : props.entrySet()) {
                String key = String.valueOf(prop.getKey());
                String value = String.valueOf(prop.getValue());
                System.setProperty(key, value);
                System.out.println("  " + key + " = " + value);
            }
            System.out.println("Properties loaded successfully.");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to load app.properties");
            e.printStackTrace();
            // Don't throw here as properties might not be critical
        }

        System.out.println("AppStartupListener initialization completed.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Clean-up code if needed on shutdown
    }
}