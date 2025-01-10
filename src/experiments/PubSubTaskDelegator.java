package experiments;

import experiments.measurement.MeasurementProducerBroker;

/**
 *
 * @author uceeftu
 */
public interface PubSubTaskDelegator {
    
    MeasurementProducerBroker getBroker();
    void taskRequestCompleted();
    void taskResponseReceived();
    //int getNumberOfPublications();
    //int getNumberOfSubscriptions();
}
