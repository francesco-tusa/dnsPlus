package experiments.cache.asynchronous.tasks;

import broker.Broker;
import experiments.PubSubTaskDelegator;
import experiments.cache.asynchronous.AsynchronousTask;
import experiments.inputdata.DBFactory;
import experiments.measurement.AsynchronousPublicationMeasurementListener;
import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.PoisonPillPublication;
import publishing.Publisher;
import utils.CustomLogger;
import utils.ExecutionTimeLogger;
import utils.ExecutionTimeResult;

/**
 *
 * @author uceeftu
 */
public class PublisherTask extends AsynchronousTask implements AsynchronousPublicationMeasurementListener {

    private static final Logger logger = CustomLogger.getLogger(PublisherTask.class.getName());

    // a normal publisher is used here (does not listen for subscriptions that matched
    // its publications) as this is the case in pub/sub scenarios
    // Asynchronous publisher should be used for debugging instead
    private Publisher publisher;
    private PubSubTaskDelegator taskRunner;
    private String domainsFile;
    private int numberOfPublications;
    

    public PublisherTask(Publisher publisher, PubSubTaskDelegator taskRunner, String domainsFile, int nPublications) {
        this.taskRunner = taskRunner;
        this.domainsFile = domainsFile;
        this.publisher = publisher; 
        this.publisher.setBroker((Broker) taskRunner.getBroker());
        numberOfPublications = nPublications;
        setName(generateTaskDescription());
    }
    
    public PublisherTask(Publisher publisher, PubSubTaskDelegator taskRunner, String domainsFile) {
        this(publisher, taskRunner, domainsFile, taskRunner.getNumberOfPublications());
    }
    
    public PublisherTask(String publisherName, PubSubTaskDelegator taskRunner, String domainsFile) {
        this(new Publisher(publisherName), taskRunner, domainsFile);
    }
    
    public PublisherTask(String publisherName, PubSubTaskDelegator taskRunner, String domainsFile, int nPublications) {
        this(new Publisher(publisherName), taskRunner, domainsFile, nPublications);
    }
    

    protected Publisher getPublisher() {
        return publisher;
    }

    protected PubSubTaskDelegator getTaskRunner() {
        return taskRunner;
    }
    
    private String generateTaskDescription() {
        return publisher.getName() + ":publish:" + numberOfPublications;
    }
    
    
    @Override
    public final void registerWithMeasurementProducer() {
        taskRunner.getBroker().addPublicationMeasurementListener(this);
        taskRunner.getBroker().addPublicationTask(this);
    }

    @Override
    public void publicationMeasurementPerformed(Duration replyDuration) {
        logger.log(Level.INFO, "Publisher {0} received measurement {1}", new Object[]{publisher.getName(), replyDuration.toMillis()});
        setReplyDuration(replyDuration);
        taskRunner.taskResponseReceived();
        // remove task from broker when a measurement is received
        taskRunner.getBroker().removePublicationTask(getName());
    }

    @Override
    public void run() {
        registerWithMeasurementProducer();
        List<String> randomEntries = DBFactory.getDomainsDB(domainsFile).getRandomEntries(numberOfPublications);
        
        ExecutionTimeResult<Void> result = 
            ExecutionTimeLogger.measure(
                "generateAndPublishAll",
                () -> { publisher.generateAndPublishAll(randomEntries);
                        return null; }
            );
        
        setDuration(result.getDuration());
        logger.log(Level.WARNING, "Publisher {0} completed their request task operation: {1}", 
                                  new Object[]{publisher.getName(), result.getCalledMethod()});
        taskRunner.taskRequestCompleted();
    }

    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Publisher {0} is cleaning up run", publisher.getName());
        String name = publisher.getName();
        publisher.publish(new PoisonPillPublication(name));
        logger.log(Level.FINE, "Publisher {0} sent a poison pill publication", name);
    }
}
