package experiments.measurement;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousMeasurementProducer {
    void addMeasurementListener(AsynchronousBrokerMeasurementListener l);  
}