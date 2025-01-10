package experiments.cache.asynchronous.tasks;

import experiments.PubSubTaskDelegator;
import java.time.Duration;
import subscribing.ReceivingSubscriber;

/**
 *
 * @author uceeftu
 */
public class SubscriberNopTask extends SubscriberTask {
    
    public SubscriberNopTask(String subscriberName, PubSubTaskDelegator taskRunner) {
        super(subscriberName, taskRunner, null, 0);
        setName(subscriberName + ":nop");
    }
    
    public SubscriberNopTask(ReceivingSubscriber subscriber, PubSubTaskDelegator taskRunner) {
        super(subscriber, taskRunner, null);
        setName(subscriber.getName() + ":nop");
    }

    
    @Override
    public void subscriptionMeasurementPerformed(Duration replyDuration) {
        setReplyDuration(Duration.ZERO);
        getTaskRunner().taskResponseReceived();
    }
    
    @Override
    public void run() {
        registerWithMeasurementProducer();
        // no operations
        setDuration(Duration.ZERO);
        getTaskRunner().taskRequestCompleted();
    }
}