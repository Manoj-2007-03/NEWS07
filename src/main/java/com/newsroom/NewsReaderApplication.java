package com.newsroom;

import com.newsroom.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application entry point for the NEWSROOM Editorial Desktop Reader.
 * 
 * Configures system rendering properties, initiates Look and Feel ergonomics,
 * and launches the MainFrame onto the Swing Event Dispatch Thread (EDT).
 */
public class NewsReaderApplication {

    public static void main(String[] args) {
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
