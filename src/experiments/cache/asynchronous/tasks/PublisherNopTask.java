package experiments.cache.asynchronous.tasks;

import java.time.Duration;
import java.util.ArrayList;
import publishing.Publisher;
import experiments.PubSubRunTasksExecutor;

/**
 *
 * @author uceeftu
 */

// used essentially to send a Poison Pill Publication
public class PublisherNopTask extends PublisherTask {

    public PublisherNopTask(String publisherName, PubSubRunTasksExecutor taskRunner) {
        super(publisherName, taskRunner, new ArrayList<>());
        setName(publisherName + ":nop");
    }
    
    public PublisherNopTask(Publisher publisher, PubSubRunTasksExecutor taskRunner) {
        super(publisher, taskRunner, new ArrayList<>());
        setName(publisher.getName() + ":nop");
    }

    @Override
    public void publicationMeasurementPerformed(Duration replyDuration) {
        setReplyDuration(Duration.ZERO);
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
