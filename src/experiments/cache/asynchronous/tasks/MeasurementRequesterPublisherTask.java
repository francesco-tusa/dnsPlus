package experiments.cache.asynchronous.tasks;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.MeasurementPillPublication;
import publishing.Publisher;
import utils.CustomLogger;
import experiments.PubSubRunTasksExecutor;

/**
 *
 * @author uceeftu
 */
public class MeasurementRequesterPublisherTask extends PublisherTask implements MeasurementRequesterTask {

    private static final Logger logger = CustomLogger.getLogger(MeasurementRequesterPublisherTask.class.getName());

    public MeasurementRequesterPublisherTask(String publisherName, PubSubRunTasksExecutor taskRunner, List<String> domains) {
        super(publisherName, taskRunner, domains);
    }
    
    public MeasurementRequesterPublisherTask(String publisherName, PubSubRunTasksExecutor taskRunner, String domainsFile, int nPublications) {
        super(publisherName, taskRunner, domainsFile, nPublications);
    }
    
    public MeasurementRequesterPublisherTask(Publisher publisher, PubSubRunTasksExecutor taskRunner, List<String> domains) {
        super(publisher, taskRunner, domains);
    }
    
    public MeasurementRequesterPublisherTask(Publisher publisher, PubSubRunTasksExecutor taskRunner, String domainsFile, int nPublications) {
        super(publisher, taskRunner, domainsFile, nPublications);
    }

    @Override
    public void requestMeasurement() {
        Publisher publisher = getPublisher();
        logger.log(Level.INFO, "Publisher {0} is requesting measurements to the broker", publisher.getName());
        String name = publisher.getName();
        publisher.publish(new MeasurementPillPublication());
        logger.log(Level.FINE, "Publisher {0} sent a measurement pill publication", name);
    }

}
