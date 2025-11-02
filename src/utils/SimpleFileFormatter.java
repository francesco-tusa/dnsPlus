package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A simple formatter for writing plain text logs to a file, without ANSI color codes.
 */
public class SimpleFileFormatter extends Formatter {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        String date = dateFormat.format(new Date(record.getMillis()));
        
        // Extract the class name
        String className = record.getLoggerName();
        try {
            className = Class.forName(className).getSimpleName();
        } catch (ClassNotFoundException e) {
            // Keep the full logger name if class not found
        }
        
        // Format: [Date] [LEVEL] [ClassName] Message
        return String.format("%s [%-7s] [%-30s] %s%n",
                date,
                record.getLevel().getName(),
                className,
                formatMessage(record)
                );
    }
}