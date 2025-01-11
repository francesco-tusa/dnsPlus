package experiments.cache.asynchronous.tasks;

import broker.Broker;
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
import experiments.PubSubRunTasksExecutor;

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
    private PubSubRunTasksExecutor taskRunner;
    private String domainsFile;
    
    private int numberOfPublications;
    private List<String> publicationDomains;
    

    private PublisherTask(Publisher publisher, PubSubRunTasksExecutor taskRunner) {
        this.publisher = publisher; 
        this.taskRunner = taskRunner;
        this.publisher.setBroker((Broker) taskRunner.getBroker());
    }
    
    public PublisherTask(Publisher publisher, PubSubRunTasksExecutor taskRunner, String domainsFile, int nPublications) {
        this(publisher, taskRunner);
        this.domainsFile = domainsFile;
        numberOfPublications = nPublications;
        publicationDomains = DBFactory.getDomainsDB(domainsFile).getRandomEntries(nPublications);
        setName(generateTaskDescription());
    }
    
    public PublisherTask(Publisher publisher, PubSubRunTasksExecutor taskRunner, List<String> domains) {
        this(publisher, taskRunner);
        numberOfPublications = domains.size();
        publicationDomains = domains;
        setName(generateTaskDescription());
    }
    
    public PublisherTask(String publisherName, PubSubRunTasksExecutor taskRunner, List<String> domains) {
        this(new Publisher(publisherName), taskRunner, domains);
    }
    
    public PublisherTask(String publisherName, PubSubRunTasksExecutor taskRunner, String domainsFile, int nPublications) {
        this(new Publisher(publisherName), taskRunner, domainsFile, nPublications);
    }

    protected Publisher getPublisher() {
        return publisher;
    }

    protected PubSubRunTasksExecutor getTaskRunner() {
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
        setAsynchronousProcessingDuration(replyDuration);
        taskRunner.setTaskResponseReceived();
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
        taskRunner.setTaskRequestCompleted();
    }

    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Publisher {0} is cleaning up run", publisher.getName());
        String name = publisher.getName();
        publisher.publish(new PoisonPillPublication(name));
        logger.log(Level.FINE, "Publisher {0} sent a poison pill publication", name);
    }
}
