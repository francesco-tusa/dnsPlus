package experiments.cache.asynchronous.tasks;

import experiments.PubSubTaskDelegator;
import java.util.logging.Level;
import java.util.logging.Logger;
import subscribing.AsynchronousSubscriber;
import subscribing.MeasurementPillSubscription;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public class MeasurementRequesterSubscriberTask extends SubscriberTask implements MeasurementRequesterTask {
    
    private static final Logger logger = CustomLogger.getLogger(MeasurementRequesterSubscriberTask.class.getName());
    
    public MeasurementRequesterSubscriberTask(String subscriberName, PubSubTaskDelegator taskRunner, String domainsFile) {
        super(subscriberName, taskRunner, domainsFile);
    }
    
    public MeasurementRequesterSubscriberTask(String subscriberName, PubSubTaskDelegator taskRunner, String domainsFile, int nSubscriptions) {
        super(subscriberName, taskRunner, domainsFile, nSubscriptions);
    }
    
    public MeasurementRequesterSubscriberTask(AsynchronousSubscriber subscriber, PubSubTaskDelegator taskRunner, String domainsFile) {
        super(subscriber, taskRunner, domainsFile);
    }
    
    public MeasurementRequesterSubscriberTask(AsynchronousSubscriber subscriber, PubSubTaskDelegator taskRunner, String domainsFile, int nSubscriptions) {
        super(subscriber, taskRunner, domainsFile, nSubscriptions);
    }
    
    @Override
    public void requestMeasurement() {
        AsynchronousSubscriber subscriber = getSubscriber();
        logger.log(Level.INFO, "Subscriber {0} is requesting measurements to the broker", subscriber.getName());
        String name = subscriber.getName();
        subscriber.subscribe(new MeasurementPillSubscription());
        logger.log(Level.FINE, "Subscriber {0} sent a measurement pill subscription", name);
    }
}