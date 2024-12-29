package utils;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Formatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author f.tusa
 */
public final class CustomLogger {

    private static Map<String, Logger> loggers = new HashMap<>();
    private static Level level = Level.INFO;
    
    private static Logger configureLogger(Logger logger) {
        logger.setUseParentHandlers(false); // Disable parent handlers to avoid duplicate logs
        LoggerConsoleHandler handler = new LoggerConsoleHandler(System.out, new CustomFormatter());
        handler.setLevel(level);
        logger.addHandler(handler);
        logger.setLevel(level);

        return logger; 
    }
    
    private static Logger getInstance(String className) {
        if (loggers.containsKey(className)) {
            return loggers.get(className);
        } else {
            Logger logger = configureLogger(Logger.getLogger(className));
            loggers.put(className, logger);
            return logger;
        }
        
    }    
    
    public static Logger getLogger(String className, Level l) {
        level = l;
        return getInstance(className);
    }
    
    public static Logger getLogger(String className) {
        return getInstance(className);
    }
    
    public static Level getLevel() {
        return level;
    }
    
}



final class LoggerConsoleHandler extends ConsoleHandler {

    public LoggerConsoleHandler(OutputStream out, Formatter formatter) {
        super();
        setOutputStream(out);
        setFormatter(formatter);
    }

    public LoggerConsoleHandler(OutputStream out) {
        this(out, new CustomFormatter());
    }
}




final class CustomFormatter extends Formatter {
    //private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        return generateFormatString(record);
    }
    
    
    private String generateFormatString(LogRecord record) {
        String levelColor = getLevelColor(record.getLevel().getName());
        String date = dateFormat.format(new Date(record.getMillis()));
        String className = record.getSourceClassName();
        String methodName = record.getSourceMethodName();
        String threadName = Thread.currentThread().getName();
        
        if (CustomLogger.getLevel().intValue() < Level.INFO.intValue()) {
            return String.format("%s%s %s [%s][%s][%s]: %s%s%n",
                    levelColor,
                    date,
                    record.getLevel().getName(),
                    className,
                    threadName,
                    methodName,
                    formatMessage(record),
                    getLevelColor("RESET"));
        } else {
            return String.format("%s%s [%s][%s]: %s%s%n",
                    levelColor,
                    date,
                    //record.getLevel().getName(),
                    className,
                    //threadName,
                    methodName,
                    formatMessage(record),
                    getLevelColor("RESET"));
        }
    }
    

    private String getLevelColor(String levelName) {
        return switch (levelName) {
            case "SEVERE" -> "\u001B[31m"; // Red
            case "WARNING" -> "\u001B[33m"; // Yellow
            case "INFO" -> "\u001B[34m"; // Blue
            case "FINE" -> "\u001B[30m"; // Black
            case "FINER" -> "\u001B[90m"; // Light gray
            default -> "\u001B[0m"; // Reset
        }; 
    }
}