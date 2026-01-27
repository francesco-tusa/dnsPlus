package utils;

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.StreamHandler;
import java.util.logging.LogRecord;

/**
 * A custom ConsoleHandler that directs output to a specified stream
 * (e.g., System.out or System.err) and can have its level set.
 */
final class LoggerConsoleHandler extends StreamHandler {

    public LoggerConsoleHandler(OutputStream out, Formatter formatter) {
        super(out, formatter);
    }

    @Override
    public void publish(LogRecord record) {
        // Ensure this handler's level is respected
        if (isLoggable(record)) {
            super.publish(record);
            flush(); // Flush to ensure immediate output
        }
    }

    @Override
    public void close() {
        flush();
    }
}