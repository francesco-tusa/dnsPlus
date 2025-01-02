package experiments.measurement;

import java.time.Duration;
import java.util.EventListener;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousMeasurementListener extends EventListener {
    void asynchronousMeasurementPerformed(Duration duration);
    void registerWithMeasurementProducer();
}