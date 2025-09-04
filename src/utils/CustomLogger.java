package utils;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Formatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A centralized custom logger for the project.
 * Supports both a global log level for the simulator and specific
 * log levels for other parts of the application like experiments.
 */
public final class CustomLogger {

    private static final Map<String, Logger> loggers = new HashMap<>();
    private static Level globalLevel = Level.INFO; // Default global level for the simulator

    /**
     * Sets the global logging level for all registered loggers.
     * This is primarily used by the simulator package.
     * @param newLevel The new level to apply.
     */
    public static void setGlobalLogLevel(Level newLevel) {
        System.out.println("--- Setting Global Log Level to: " + newLevel.getName() + " ---");
        globalLevel = newLevel;
        // Update the level on all existing loggers and their handlers
        for (Logger logger : loggers.values()) {
            logger.setLevel(globalLevel);
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(globalLevel);
            }
        }
    }
    
    private static Logger configureLogger(Logger logger, Level level) {
        // Remove any existing handlers to prevent duplicates
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        logger.setUseParentHandlers(false);
        LoggerConsoleHandler handler = new LoggerConsoleHandler(System.out, new CustomFormatter());
        handler.setLevel(level);
        logger.addHandler(handler);
        logger.setLevel(level);
        return logger; 
    }
    
    private static Logger getInstance(String className, Level level) {
        return loggers.computeIfAbsent(className, k -> configureLogger(Logger.getLogger(k), level));
    }    
    
    /**
     * Gets a logger and sets it to a specific level.
     * This is used by the experiments package for its specific logging needs.
     * @param className The name of the class for the logger.
     * @param level The desired log level.
     * @return A configured Logger instance.
     */
    public static Logger getLogger(String className, Level level) {
        // This version creates/retrieves a logger and ensures it's set to the desired level,
        // bypassing the global setting for this specific instance.
        Logger logger = getInstance(className, level);
        logger.setLevel(level);
        for(Handler handler : logger.getHandlers()) {
            handler.setLevel(level);
        }
        return logger;
    }
    
    /**
     * Gets a logger that uses the current global log level.
     * This is used by the simulator package.
     * @param className The name of the class for the logger.
     * @return A configured Logger instance.
     */
    public static Logger getLogger(String className) {
        return getInstance(className, globalLevel);
    }
    
    public static Level getLevel() {
        return globalLevel;
    }
}


final class LoggerConsoleHandler extends ConsoleHandler {
    public LoggerConsoleHandler(OutputStream out, Formatter formatter) {
        super();
        setOutputStream(out);
        setFormatter(formatter);
    }
}

final class CustomFormatter extends Formatter {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        String levelColor = getLevelColor(record.getLevel());
        String date = dateFormat.format(new Date(record.getMillis()));
        String className = record.getLoggerName();
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf('.') + 1);
        }
        
        return String.format("%s%s [%-7s] [%-30s] %s%s%n",
                levelColor,
                date,
                record.getLevel().getName(),
                className,
                formatMessage(record),
                "\u001B[0m"); // Reset color
    }
    
    private String getLevelColor(Level level) {
        return switch (level.getName()) {
            case "SEVERE" -> "\u001B[31m";
            case "WARNING" -> "\u001B[33m";
            case "INFO" -> "\u001B[32m";
            case "CONFIG" -> "\u001B[36m";
            case "FINE", "FINER", "FINEST" -> "\u001B[37m";
            default -> "\u001B[0m";
        }; 
    }
}