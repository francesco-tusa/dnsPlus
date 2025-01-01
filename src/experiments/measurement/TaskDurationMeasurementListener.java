package experiments.measurement;

import java.time.Duration;
import java.util.EventListener;

/**
 *
 * @author uceeftu
 */
public interface TaskDurationMeasurementListener extends EventListener {
    void measurementPerformed(Duration d);
    void register();
}