package utils;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
public class ExecutionTimeResult<T> {
        private final String calledMethod;
        private final T result;
        private final Duration duration;

        public ExecutionTimeResult(String calledMethod, T result, Duration duration) {
            this.calledMethod = calledMethod;
            this.result = result;
            this.duration = duration;
        }

        public String getCalledMethod() {
            return calledMethod;
        }

        public T getResult() {
            return result;
        }

        public Duration getDuration() {
            return duration;
        }
    }
