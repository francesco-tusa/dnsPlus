package experiments.measurement;

import broker.AsynchronousBroker;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousMeasurementProducerBroker extends AsynchronousBroker {
    
    // add a listener for publication measurement events
    void addPublicationMeasurementListener(AsynchronousMeasurementListener listener);
    
    // add a listener for subscription measurement events
    void addSubscriptionMeasurementListener(AsynchronousMeasurementListener listener);
}
