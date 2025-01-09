package experiments.measurement;

import experiments.cache.asynchronous.AsynchronousTask;

/**
 *
 * @author uceeftu
 */
public interface AsynchronousPublicationMeasurementProducer {
    void addPublicationMeasurementListener(AsynchronousPublicationMeasurementListener l);
    void addPublicationTask(AsynchronousTask publisherTask);
    void removePublicationTask(String publisherTaskName);
}