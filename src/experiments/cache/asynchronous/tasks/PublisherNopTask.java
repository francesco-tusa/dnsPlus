package experiments.cache.asynchronous.tasks;

import experiments.PubSubTaskDelegator;
import java.time.Duration;
import java.util.ArrayList;
import publishing.Publisher;

/**
 *
 * @author uceeftu
 */

// used essentially to send a Poison Pill Publication
public class PublisherNopTask extends PublisherTask {

    public PublisherNopTask(String publisherName, PubSubTaskDelegator taskRunner) {
        super(publisherName, taskRunner, new ArrayList<>());
        setName(publisherName + ":nop");
    }
    
    public PublisherNopTask(Publisher publisher, PubSubTaskDelegator taskRunner) {
        super(publisher, taskRunner, new ArrayList<>());
        setName(publisher.getName() + ":nop");
    }

    @Override
    public void publicationMeasurementPerformed(Duration replyDuration) {
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
