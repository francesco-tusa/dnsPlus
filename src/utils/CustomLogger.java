package utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CustomLogger {

    private static final Map<String, Logger> loggers = new HashMap<>();
    private static Level globalLevel = Level.INFO; 
    private static FileHandler fileHandler = null;
    private static String logFilePath = ""; 
    private static final int LOG_FILE_LIMIT_BYTES = 95 * 1024 * 1024; // 95 MB
    private static final int LOG_FILE_COUNT = 100; // Keep up to 100 rotated files

    static {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            handler.close();
            rootLogger.removeHandler(handler);
        }
        
        rootLogger.setLevel(Level.ALL); 
        
        try {
            Handler consoleHandler = new LoggerConsoleHandler(System.out, new CustomFormatter());
            consoleHandler.setLevel(Level.INFO); 
            rootLogger.addHandler(consoleHandler);
        } catch (Exception e) {
            System.err.println("Failed to create console handler: " + e.getMessage());
        }
        
        silenceSystemLoggers();
    }
    
    private static void silenceSystemLoggers() {
        String[] noisyPackages = { "java.awt", "javax.swing", "sun.awt", "sun.lwawt", "org.graphstream" };
        for (String pkg : noisyPackages) {
            Logger.getLogger(pkg).setLevel(Level.INFO);
        }
    }

    public static Logger getLogger(String className) {
        return loggers.computeIfAbsent(className, k -> {
            Logger logger = Logger.getLogger(k);
            logger.setLevel(globalLevel); 
            logger.setUseParentHandlers(true); 
            return logger;
        });
    }

    public static synchronized void setGlobalLogLevel(Level newLevel, String simulationTimestamp) {
        Logger rootLogger = Logger.getLogger("");
        Logger selfLogger = getLogger(CustomLogger.class.getName());
        selfLogger.info("--- Setting Global Log Level to: " + newLevel.getName() + " ---");

        globalLevel = newLevel;

        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof LoggerConsoleHandler) {
                handler.setLevel(Level.INFO);
            }
        }

        // Remove old file handler
        if (fileHandler != null) {
            rootLogger.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
        }

        // Configure Rotating File Handler
        try {
            // Create directory: output/logs/<RunID>/
            String logDir = "output/logs/" + simulationTimestamp;
            new File(logDir).mkdirs();
            
            // Pattern: simulation_log_%g.log (%g is the rotation generation number)
            // Note: FileHandler requires the directory to exist.
            String pattern = logDir + "/simulation_log_%g.log";
            
            fileHandler = new FileHandler(pattern, LOG_FILE_LIMIT_BYTES, LOG_FILE_COUNT, true);
            fileHandler.setFormatter(new SimpleFileFormatter());
            fileHandler.setLevel(globalLevel);
            rootLogger.addHandler(fileHandler);
            
            logFilePath = logDir + "/simulation_log_0.log"; // Approximate current file
            selfLogger.info("--- Log file initialized at: " + logDir + " (Max " + (LOG_FILE_LIMIT_BYTES/1024/1024) + "MB per file) ---");
            
        } catch (IOException | SecurityException e) {
            selfLogger.log(Level.SEVERE, "Failed to create log file handler", e);
        }

        for (Logger logger : loggers.values()) {
            logger.setLevel(globalLevel);
        }
        silenceSystemLoggers();
    }
    
    public static String getLogFilePath() { return logFilePath; }
    public static Level getLevel() { return globalLevel; }
}