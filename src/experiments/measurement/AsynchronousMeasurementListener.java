package experiments.measurement;

import java.util.EventListener;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousMeasurementListener extends EventListener {
    void registerWithMeasurementProducer();
}