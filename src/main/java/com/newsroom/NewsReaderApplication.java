package com.newsroom;

import com.newsroom.ui.MainFrame;
import com.newsroom.web.NewsWebServer;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;

/**
 * Main application entry point for NEWSROOM Editorial Edition.
 * 
 * Supports dual-mode operation:
 * 1. Cloud Web Mode: Automatically triggered when deployed on Render, Docker, or with --web flag / $PORT.
 * 2. Desktop GUI Mode: Standard desktop Swing experience on Windows, macOS, and Linux workstations.
 */
public class NewsReaderApplication {

    public static void main(String[] args) {
        boolean forceWeb = false;
        for (String arg : args) {
            if ("--web".equalsIgnoreCase(arg) || "-w".equalsIgnoreCase(arg) || "--server".equalsIgnoreCase(arg)) {
                forceWeb = true;
                break;
            }
        }

        String portEnv = System.getenv("PORT");
        boolean isHeadless = GraphicsEnvironment.isHeadless();

        // If running in cloud environment (e.g. Render with $PORT), headless server, or web flag
        if (forceWeb || portEnv != null || isHeadless) {
            int port = 8080;
            if (portEnv != null && !portEnv.trim().isEmpty()) {
                try {
                    port = Integer.parseInt(portEnv.trim());
                } catch (NumberFormatException ignored) {}
            }
            System.out.println("Activating NEWSROOM Cloud Web Service on port " + port + "...");
            NewsWebServer.startServer(port);
            return;
        }

        // Configure high-DPI and subpixel font antialiasing before AWT initialization
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.opengl", "true");

        // Schedule GUI execution on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // Apply system look and feel for native window decorations
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Notice: Using default Swing Look and Feel. " + e.getMessage());
            }

            try {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Fatal error during application launch: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
