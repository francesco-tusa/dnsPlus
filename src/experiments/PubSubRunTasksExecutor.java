package experiments;

import experiments.cache.asynchronous.AsynchronousRunTasksExecutor;
import experiments.measurement.MeasurementProducerBroker;

/**
 *
 * @author uceeftu
 */
public interface PubSubRunTasksExecutor extends AsynchronousRunTasksExecutor {
    
    MeasurementProducerBroker getBroker();
}
