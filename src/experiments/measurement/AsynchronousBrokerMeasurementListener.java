package experiments.measurement;

import experiments.outputdata.BrokerStats;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousBrokerMeasurementListener extends AsynchronousMeasurementListener {
    void asynchronousMeasurementPerformed(BrokerStats brokerStats);
}