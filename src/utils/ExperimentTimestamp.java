package utils;

import java.time.Instant;

/**
 *
 * @author uceeftu
 */
public class ExperimentTimestamp {
    
    public static String getTimestamp() {
        Instant timestamp = Instant.now();
        return String.valueOf(timestamp.toEpochMilli());
    }
    
    public static String appendTimestamp(String string) {
        return string + "-" + getTimestamp();
    }
    
}
