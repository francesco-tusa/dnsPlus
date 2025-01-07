package experiments.measurement;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousBrokerMeasurementListener extends AsynchronousMeasurementListener {
    void asynchronousMeasurementPerformed(BrokerStats brokerStats);
}