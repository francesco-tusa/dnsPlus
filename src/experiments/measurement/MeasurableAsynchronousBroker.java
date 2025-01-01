package experiments.measurement;

import broker.AsynchronousBroker;

/**
 *
 * @author uceeftu
 */
public interface MeasurableAsynchronousBroker extends AsynchronousBroker {
    
    // add a listener for publication measurement events
    void addPublicationMeasurementListener(TaskDurationMeasurementListener listener);
    
    // add a listener for subscription measurement events
    void addSubscriptionMeasurementListener(TaskDurationMeasurementListener listener);
}
