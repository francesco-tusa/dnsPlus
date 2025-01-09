package experiments.cache.asynchronous.tasks;

import experiments.PubSubTaskDelegator;
import java.time.Duration;
import subscribing.AsynchronousSubscriber;

/**
 *
 * @author uceeftu
 */
public class SubscriberNopTask extends SubscriberTask {
    
    public SubscriberNopTask(String subscriberName, PubSubTaskDelegator taskRunner) {
        super(subscriberName, taskRunner, null);
        setName(subscriberName + ":nop");
    }
    
    public SubscriberNopTask(AsynchronousSubscriber subscriber, PubSubTaskDelegator taskRunner) {
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