package experiments.measurement;

/**
 *
 * @author uceeftu
 */
public interface MeasurementProducerBroker {
    
    // add a listener for publication measurement events
    void addPublicationMeasurementListener(AsynchronousMeasurementListener listener);
    
    // add a listener for subscription measurement events
    void addSubscriptionMeasurementListener(AsynchronousMeasurementListener listener);
}
