package experiments.cache.asynchronous;

import broker.AsynchronousMeasurementProducerCachingBroker;
import encryption.HEPS;
import broker.tree.binarybalanced.cache.asynchronous.AsynchronousBrokerWithBinaryBalancedTreeAndCache;
import experiments.RunParallelTasksExecutor;
import experiments.Task;
import experiments.inputdata.DBFactory;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.Publisher;
import subscribing.AsynchronousSubscriber;
import utils.CustomLogger;
import utils.ExecutionTimeLogger;
import experiments.inputdata.DomainsDB;
import experiments.measurement.AsynchronousBrokerMeasurementListener;
import experiments.measurement.AsynchronousPublicationMeasurementListener;
import experiments.measurement.AsynchronousSubscriptionMeasurementListener;
import experiments.measurement.BrokerStats;
import publishing.PoisonPillPublication;
import subscribing.PoisonPillSubscription;

/**
 *
 * @author f.tusa
 */
public final class DNSWithCacheAsynchronousRun extends RunParallelTasksExecutor implements AsynchronousRunTasksExecutor {

    private static final Logger logger = CustomLogger.getLogger(DNSWithCacheAsynchronousRun.class.getName());
    private AsynchronousMeasurementProducerCachingBroker broker;
    private DomainsDB domainsDB;
    
    private CountDownLatch requestsLatch;
    private CountDownLatch repliesLatch;

    private int numberOfPublications;
    private int numberOfSubscriptions;
    
    private BrokerStatsCollector brokerStatsCollector;
    
    
    public DNSWithCacheAsynchronousRun(DomainsDB db, int numberOfPublications, int numberOfSubscriptions) {
        this(db, null, numberOfPublications, numberOfSubscriptions);
    }

    public DNSWithCacheAsynchronousRun(String fileName, int numberOfPublications, int numberOfSubscriptions) {
        this(null, fileName, numberOfPublications, numberOfSubscriptions);
    }

    private DNSWithCacheAsynchronousRun(DomainsDB db, String fileName, int nPublications, int nSubscriptions) {
        super(DNSWithCacheAsynchronousRun.class.getSimpleName());
        broker = new AsynchronousBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        domainsDB = (db != null) ? db : DBFactory.getDomainsDB(fileName);
        numberOfPublications = nPublications;
        numberOfSubscriptions = nSubscriptions;
        brokerStatsCollector = new BrokerStatsCollector();
    }
    

    @Override
    public void setUp() {
        broker.startProcessing();
    }

    @Override
    public void start() {
        List<Task> tasks = getTasks();
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
        List<Task> tasks = getTasks();
        for (Task task : tasks) {
            if (task instanceof AsynchronousTask asyncTask) {
                asyncTask.cleanUp();
            }
        }
    }

    @Override
    public void finalise() {
        broker.stopProcessing();
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
        
        broker.stopProcessing();
    }

    

    final class PublisherTask extends AsynchronousTask implements AsynchronousPublicationMeasurementListener {
        private static final Logger logger = CustomLogger.getLogger(PublisherTask.class.getName());
        // a normal publisher is used here (does not listen for subscriptions that matched
        // its publications) as this is the case in pub/sub scenarios
        // Asynchronous publisher might be used for debugging
        private Publisher publisher;
        
        public PublisherTask(String publisherName) {
            publisher = new Publisher(publisherName, broker);
            publisher.init();
            setName(publisher.getName() + "-" + this.getClass().getSimpleName());
            registerWithMeasurementProducer();
        }
        
        @Override
        public void registerWithMeasurementProducer() {
            broker.addPublicationMeasurementListener(this);
        }

        @Override
        public void publicationMeasurementPerformed(Duration replyDuration) {
            logger.log(Level.INFO, "Publisher {0} received measurement {1}", new Object[]{publisher.getName(), replyDuration.toMillis()});
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
            logger.log(Level.WARNING, "Publisher {0} completed their request task", publisher.getName());
            requestsLatch.countDown();
        }

        @Override
        public void cleanUp() {
            logger.log(Level.INFO, "Publisher {0} is cleaning up run", publisher.getName());
            String name = publisher.getName();
            publisher.publish(new PoisonPillPublication(name));
            logger.log(Level.FINE, "Publisher {0} sent a poison pill publication", name);
        }
        
        
    }
    
    final class SubscriberTask extends AsynchronousTask implements AsynchronousSubscriptionMeasurementListener {
        private static final Logger logger = CustomLogger.getLogger(SubscriberTask.class.getName());
        private AsynchronousSubscriber subscriber;
        
        public SubscriberTask(String subscriberName) {
            subscriber = new AsynchronousSubscriber(subscriberName, broker);
            subscriber.init();
            setName(subscriber.getName() + "-" + this.getClass().getSimpleName());
            registerWithMeasurementProducer();
        }
        
        @Override
        public void registerWithMeasurementProducer() {
            broker.addSubscriptionMeasurementListener(this);
        }
        
        @Override
        public void subscriptionMeasurementPerformed(Duration replyDuration) {
            logger.log(Level.INFO, "Subscriber {0} received measurement {1}", new Object[]{subscriber.getName(), replyDuration.toMillis()});
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
            logger.log(Level.WARNING, "Subscriber {0} completed their request task", subscriber.getName());
            requestsLatch.countDown();
        }

        @Override
        public void cleanUp() {
            logger.log(Level.INFO, "Subscriber {0} is cleaning up run", subscriber.getName());
            String name = subscriber.getName();
            subscriber.subscribe(new PoisonPillSubscription(name));
            logger.log(Level.FINE, "Subscriber {0} sent a poison pill subscription", name);
        }
    }
    
    final class BrokerStatsCollector implements AsynchronousBrokerMeasurementListener {

        private static final Logger logger = CustomLogger.getLogger(BrokerStatsCollector.class.getName());
        private BrokerStats brokerStats;

        public BrokerStatsCollector() {
            registerWithMeasurementProducer();
        }
        
        
        
        @Override
        public void asynchronousMeasurementPerformed(BrokerStats brokerStats) {
            this.brokerStats = brokerStats;
            logger.log(Level.SEVERE, brokerStats.toString());
        }

        @Override
        public void registerWithMeasurementProducer() {
            broker.addMeasurementListener(this);
        }
        
    }
}