package experiments.measurement;

import java.time.Duration;
import java.util.EventListener;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousSubscriptionMeasurementListener extends AsynchronousMeasurementListener {
    void subscriptionMeasurementPerformed(Duration duration);
    void registerWithMeasurementProducer();
}