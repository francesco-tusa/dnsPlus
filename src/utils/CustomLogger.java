package utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * A centralized custom logger for the project.
 * This version supports split logging:
 * - INFO (and higher) messages go to the console (System.out).
 * - All messages (based on globalLevel) go to a log file.
 */
public final class CustomLogger {

    private static final Map<String, Logger> loggers = new HashMap<>();
    private static Level globalLevel = Level.INFO; // Default global level
    private static FileHandler fileHandler = null; // Single file handler for all loggers
    private static String logFilePath = ""; // Store the path

    // Initialize the root logger once
    static {
        Logger rootLogger = Logger.getLogger("");
        // Remove all existing handlers to prevent duplicates
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            handler.close();
            rootLogger.removeHandler(handler);
        }
        
        // 1. Set root logger to capture ALL levels
        rootLogger.setLevel(Level.ALL); 
        
        // 2. Add our custom console handler
        try {
            Handler consoleHandler = new LoggerConsoleHandler(System.out, new CustomFormatter());
            // 3. Set CONSOLE handler to ONLY show INFO
            consoleHandler.setLevel(Level.INFO); 
            rootLogger.addHandler(consoleHandler);
        } catch (Exception e) {
            // This is a critical failure, use System.err as a last resort
            System.err.println("Failed to create console handler: " + e.getMessage());
        }
        
        // 4. Silence noisy third-party/system loggers that inherit root's ALL level
        silenceSystemLoggers();
    }
    
    /**
     * Explicitly sets the log level of GUI and system packages to INFO
     * to prevent them from flooding the logs when the global level is FINE/ALL.
     */
    private static void silenceSystemLoggers() {
        String[] noisyPackages = {
            "java.awt",
            "javax.swing",
            "sun.awt",
            "sun.lwawt",
            "org.graphstream"
        };

        for (String pkg : noisyPackages) {
            Logger.getLogger(pkg).setLevel(Level.INFO);
        }
    }

    /**
     * Gets a logger instance. It will be configured with the current global log level
     * and handlers.
     * @param className The name of the class for the logger.
     * @return A configured Logger instance.
     */
    public static Logger getLogger(String className) {
        return loggers.computeIfAbsent(className, k -> {
            Logger logger = Logger.getLogger(k);
            logger.setLevel(globalLevel); // Set to current global level
            logger.setUseParentHandlers(true); // Let the root logger handle output
            return logger;
        });
    }

    /**
     * Sets the global logging level and configures handlers.
     * @param newLevel The new level to apply.
     * @param simulationTimestamp A timestamp used to create a unique log file name.
     */
    public static synchronized void setGlobalLogLevel(Level newLevel, String simulationTimestamp) {
        Logger rootLogger = Logger.getLogger(""); // Get the root logger
        
        // Log the change using the logger itself *before* changing levels
        Logger selfLogger = getLogger(CustomLogger.class.getName());
        selfLogger.info("--- Setting Global Log Level to: " + newLevel.getName() + " ---");

        globalLevel = newLevel;

        // 1. Update console handler level (to be safe, though it's set in static)
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof LoggerConsoleHandler) {
                handler.setLevel(Level.INFO);
            }
        }

        // 2. Remove old file handler if it exists
        if (fileHandler != null) {
            rootLogger.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
            logFilePath = "";
        }

        // 3. Add a new File Handler
        try {
            String logDir = "output/logs/";
            new File(logDir).mkdirs();
            
            logFilePath = logDir + "simulation_log_" + simulationTimestamp + ".log";
            selfLogger.info("--- Log file for this run: " + logFilePath + " (Level: " + globalLevel.getName() + ") ---");
            
            fileHandler = new FileHandler(logFilePath, 0, 1, true);
            fileHandler.setFormatter(new SimpleFileFormatter());
            fileHandler.setLevel(globalLevel); // This will be INFO or FINE
            rootLogger.addHandler(fileHandler);
            
        } catch (IOException | SecurityException e) {
            selfLogger.log(Level.SEVERE, "Failed to create log file handler", e);
        }

        // 4. Update all existing loggers to use the new level
        // (so they don't filter out messages before they reach the handlers)
        for (Logger logger : loggers.values()) {
            logger.setLevel(globalLevel);
        }
        
        // 5. Re-enforce silence on system loggers (in case they were somehow reset or lazily loaded)
        silenceSystemLoggers();
    }
    
    /**
     * Gets the file path of the current log file.
     * @return The file path, or an empty string if no file logger is active.
     */
    public static String getLogFilePath() {
        return logFilePath;
    }
    
    public static Level getLevel() {
        return globalLevel;
    }
}