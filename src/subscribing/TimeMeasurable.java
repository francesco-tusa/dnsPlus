package subscribing;

import java.time.Duration;

/**
 *
 * @author uceeftu
 */
interface TimeMeasurable {
    Duration getLastMeasurement();
}
