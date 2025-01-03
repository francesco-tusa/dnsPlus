package experiments.cache.asynchronous;

import encryption.HEPS;
import broker.tree.binarybalanced.cache.asynchronous.AsynchronousBrokerWithBinaryBalancedTreeAndCache;
import experiments.Task;
import experiments.inputdata.DBFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.AsynchronousPublisher;
import subscribing.AsynchronousSubscriber;
import utils.CustomLogger;
import publishing.PoisonPillPublication;
import subscribing.PoisonPillSubscription;
import experiments.measurement.AsynchronousMeasurementListener;
import experiments.measurement.AsynchronousMeasurementProducerBroker;
import utils.ExecutionTimeLogger;
import experiments.inputdata.DomainsDB;

/**
 *
 * @author f.tusa
 */
public final class DNSWithCacheAsynchronousRun implements AsynchronousRunTasksExecutor {

    private static final Logger logger = CustomLogger.getLogger(DNSWithCacheAsynchronousRun.class.getName());
    //private AsynchronousSubscriber subscriber;
    //private AsynchronousPublisher publisher;
    private AsynchronousMeasurementProducerBroker broker;
    private DomainsDB domainsDB;
    
    private List<Task> tasks;
    private String name;
    
    private CountDownLatch requestsLatch;
    private CountDownLatch repliesLatch;

    private int numberOfPublications;
    private int numberOfSubscriptions;
    
    
    public DNSWithCacheAsynchronousRun(DomainsDB db, int numberOfPublications, int numberOfSubscriptions) {
        this(db, null, numberOfPublications, numberOfSubscriptions);
    }

    public DNSWithCacheAsynchronousRun(String fileName, int numberOfPublications, int numberOfSubscriptions) {
        this(null, fileName, numberOfPublications, numberOfSubscriptions);
    }

    private DNSWithCacheAsynchronousRun(DomainsDB db, String fileName, int nPublications, int nSubscriptions) {
        broker = new AsynchronousBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        //subscriber = new AsynchronousSubscriber("Subscriber1", broker);
        //publisher = new AsynchronousPublisher("Publisher1", broker);
        domainsDB = (db != null) ? db : DBFactory.getDomainsDB(fileName);
        tasks = new ArrayList<>();
        name = this.getClass().getSimpleName();
        numberOfPublications = nPublications;
        numberOfSubscriptions = nSubscriptions;
    }
    

    @Override
    public void setUp() {
        broker.startProcessing();
        //publisher.init();
        //subscriber.init();
    }
    
    @Override
    public void addTask(Task task) {
        tasks.add(task);
    }

    @Override
    public void start() {
        requestsLatch = new CountDownLatch(tasks.size());
        repliesLatch = new CountDownLatch(tasks.size());
        for (Task task : tasks) {
            Thread t = new Thread(task, task.getName());
            t.start();
        }       
    }

    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Cleaning Up Run");
        //publisher.publish(new PoisonPillPublication());
        //subscriber.subscribe(new PoisonPillSubscription());
    }
    
    @Override
    public void waitForRequestsCompletion() {
        try {
            requestsLatch.await();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void waitForRepliesCompletion() {
        try {
            repliesLatch.await();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Task> getTasks() {
        return tasks;
    }

    

    final class PublisherTask extends AsynchronousTask implements AsynchronousMeasurementListener {
        private static final Logger logger = CustomLogger.getLogger(PublisherTask.class.getName());
        private AsynchronousPublisher publisher;
        
        public PublisherTask(String publisherName) {
            publisher = new AsynchronousPublisher(publisherName, broker);
            publisher.init();
            setName(this.getClass().getSimpleName());
            registerWithMeasurementProducer();
        }
        
        @Override
        public void registerWithMeasurementProducer() {
            broker.addPublicationMeasurementListener(this);
        }

        @Override
        public void asynchronousMeasurementPerformed(Duration replyDuration) {
            logger.log(Level.INFO, "Received measurement {0}", replyDuration.toMillis());
            setReplyDuration(replyDuration);
            repliesLatch.countDown();
        }
        

        @Override
        public void run() {
            List<String> randomEntries = domainsDB.getRandomEntries(numberOfPublications);
            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                    -> {
                publisher.generateAndPublishAll(randomEntries);
                return null;
            });
            setDuration(result.getDuration());
            logger.log(Level.INFO, "Publisher task request completed");
            //publisher.publish(new PoisonPillPublication());
            requestsLatch.countDown();
        }
    }
    
    final class SubscriberTask extends AsynchronousTask implements AsynchronousMeasurementListener {
        private static final Logger logger = CustomLogger.getLogger(SubscriberTask.class.getName());
        private AsynchronousSubscriber subscriber;
        
        public SubscriberTask(String subscriberName) {
            subscriber = new AsynchronousSubscriber(subscriberName, broker);
            subscriber.init();
            setName(this.getClass().getSimpleName());
            registerWithMeasurementProducer();
        }
        
        @Override
        public void registerWithMeasurementProducer() {
            broker.addSubscriptionMeasurementListener(this);
        }
        
        @Override
        public void asynchronousMeasurementPerformed(Duration replyDuration) {
            logger.log(Level.INFO, "Received measurement {0}", replyDuration.toMillis());
            setReplyDuration(replyDuration);
            repliesLatch.countDown();
        }

        @Override
        public void run() {
            List<String> randomEntries = domainsDB.getRandomEntries(numberOfSubscriptions);
            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                    -> {
                subscriber.generateAndSubscribeToAll(randomEntries);
                return null;
            });
            setDuration(result.getDuration());
            logger.log(Level.INFO, "Subscriber {0} completed their request task", subscriber.getName());
            //subscriber.subscribe(new PoisonPillSubscription());
            requestsLatch.countDown();
        }
    }
}