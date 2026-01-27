package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A custom formatter for log messages, providing color-coded,
 * timestamped output.
 */
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
        
        // Format: [COLOR][TIME] [LEVEL] [CLASSNAME] [MESSAGE][RESET_COLOR]
        return String.format("%s%s [%-7s] [%-30s] %s%s%n",
                levelColor,
                date,
                record.getLevel().getName(),
                className,
                formatMessage(record),
                "\u001B[0m"); // Reset color
    }
    
    private String getLevelColor(Level level) {
        // ANSI escape codes for colors
        return switch (level.getName()) {
            case "SEVERE" -> "\u001B[31m";  // Red
            case "WARNING" -> "\u001B[33m"; // Yellow
            case "INFO" -> "\u001B[32m";   // Green
            case "CONFIG" -> "\u001B[36m";  // Cyan
            case "FINE", "FINER", "FINEST" -> "\u001B[37m"; // White/Light Gray
            default -> "\u001B[0m";       // Reset
        }; 
    }
}