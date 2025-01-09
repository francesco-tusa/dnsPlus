package experiments.measurement;

import experiments.cache.asynchronous.AsynchronousTask;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousSubscriptionMeasurementProducer {
    void addSubscriptionMeasurementListener(AsynchronousSubscriptionMeasurementListener l);
    void addSubscriptionTask(AsynchronousTask subscriberTask);
    void removeSubscriptionTask(String subscriberTaskName);
}