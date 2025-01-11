package experiments.cache.asynchronous.tasks;

import java.time.Duration;
import subscribing.ReceivingSubscriber;
import experiments.PubSubRunTasksExecutor;

/**
 *
 * @author uceeftu
 */
public class SubscriberNopTask extends SubscriberTask {
    
    public SubscriberNopTask(String subscriberName, PubSubRunTasksExecutor taskRunner) {
        super(subscriberName, taskRunner, null, 0);
        setName(subscriberName + ":nop");
    }
    
    public SubscriberNopTask(ReceivingSubscriber subscriber, PubSubRunTasksExecutor taskRunner) {
        super(subscriber, taskRunner, null);
        setName(subscriber.getName() + ":nop");
    }

    
    @Override
    public void subscriptionMeasurementPerformed(Duration replyDuration) {
        setAsynchronousProcessingDuration(Duration.ZERO);
        getTaskRunner().setTaskResponseReceived();
    }
    
    @Override
    public void run() {
        registerWithMeasurementProducer();
        // no operations
        setDuration(Duration.ZERO);
        getTaskRunner().setTaskRequestCompleted();
    }
}