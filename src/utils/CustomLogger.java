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
 * - FINE (and lower) messages go to a log file if the level is set to FINE.
 */
public final class CustomLogger {

    private static final Map<String, Logger> loggers = new HashMap<>();
    private static Level globalLevel = Level.INFO; // Default global level
    private static FileHandler fileHandler = null; // Single file handler for all loggers

    /**
     * Sets the global logging level and configures handlers.
     * @param newLevel The new level to apply.
     * @param simulationTimestamp A timestamp used to create a unique log file name if needed.
     */
    public static synchronized void setGlobalLogLevel(Level newLevel, String simulationTimestamp) {
        System.out.println("--- Setting Global Log Level to: " + newLevel.getName() + " ---");
        globalLevel = newLevel;

        // Get the root logger to configure handlers
        Logger rootLogger = Logger.getLogger("");

        // 1. Remove all existing handlers to prevent duplicates
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            handler.close(); // Close existing file/stream handlers
            rootLogger.removeHandler(handler);
        }
        
        // 2. Add the Console Handler for INFO messages
        try {
            Handler consoleHandler = new LoggerConsoleHandler(System.out, new CustomFormatter());
            // The console handler ONLY logs INFO and above
            consoleHandler.setLevel(Level.INFO); 
            rootLogger.addHandler(consoleHandler);
        } catch (Exception e) {
            System.err.println("Failed to create console handler: " + e.getMessage());
        }

        // 3. Add a File Handler ONLY IF the requested level is FINE or lower (verbose mode)
        if (newLevel.intValue() <= Level.FINE.intValue()) {
            try {
                // Ensure output directory exists
                String logDir = "output/logs";
                new File(logDir).mkdirs();
                
                String logFilePath = logDir + "/simulation-" + simulationTimestamp + ".log";
                System.out.println("--- Verbose logging enabled. Writing FINE logs to: " + logFilePath + " ---");
                
                fileHandler = new FileHandler(logFilePath, 0, 1, true);
                fileHandler.setFormatter(new SimpleFileFormatter());
                // The file handler logs EVERYTHING (if the global level is set this low)
                fileHandler.setLevel(Level.ALL); 
                rootLogger.addHandler(fileHandler);
                
            } catch (IOException | SecurityException e) {
                System.err.println("Failed to create log file handler: " + e.getMessage());
            }
        }

        // 4. Set the root logger's level to the requested global level
        rootLogger.setLevel(globalLevel);

        // Update all existing loggers to use the new level
        for (Logger logger : loggers.values()) {
            logger.setLevel(globalLevel);
        }
    }
    
    /**
     * Gets a logger instance. It will be configured with the current global log level
     * and handlers.
     * @param className The name of the class for the logger.
     * @return A configured Logger instance.
     */
    public static Logger getLogger(String className) {
        // Use computeIfAbsent to create and configure the logger only if it doesn't exist
        return loggers.computeIfAbsent(className, k -> {
            Logger logger = Logger.getLogger(k);
            // We only need to set the level. Handlers are managed by the root logger.
            logger.setLevel(globalLevel); 
            // Prevent logs from being passed up to the root's handlers twice
            logger.setUseParentHandlers(true); 
            return logger;
        });
    }
    
    public static Level getLevel() {
        return globalLevel;
    }
}