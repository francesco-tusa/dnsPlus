package utils;

/**
 *
 * @author uceeftu
 */
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExecutionTimeLogger {

    private static final Logger logger = CustomLogger.getLogger(ExecutionTimeLogger.class.getName());

    //public static <T> ExecutionResult<T> measureExecutionTime(Supplier<T> method, String methodName) {
    public static <T> ExecutionResult<T> measureExecutionTime(Supplier<T> method) {
        Instant start = Instant.now();
        T result = method.get();
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        logger.log(Level.FINER, "{0} executed in {1} ms", new Object[]{"", timeElapsed.toMillis()});
        return new ExecutionResult<>(result, timeElapsed);
    }

    public static class ExecutionResult<T> {
        private final T result;
        private final Duration duration;

        public ExecutionResult(T result, Duration duration) {
            this.result = result;
            this.duration = duration;
        }

        public T getResult() {
            return result;
        }

        public Duration getDuration() {
            return duration;
        }
    }
}