package experiments.cache.asynchronous;

import encryption.HEPS;
import broker.tree.binarybalanced.cache.asynchronous.AsynchronousBrokerWithBinaryBalancedTreeAndCache;
import experiments.ExperimentTask;
import experiments.inputdata.DBFactory;
import experiments.inputdata.DomainDB;
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

/**
 *
 * @author f.tusa
 */
public final class AsynchronousDNSWithCacheRun implements AsynchronousExperimentRun {

    private static final Logger logger = CustomLogger.getLogger(AsynchronousDNSWithCacheRun.class.getName(), Level.WARNING);
    private AsynchronousSubscriber subscriber;
    private AsynchronousPublisher publisher;
    private AsynchronousMeasurementProducerBroker broker;
    private DomainDB domainDB;
    
    private List<AsynchronousExperimentTask> tasks;
    private String name;
    
    private CountDownLatch requestsLatch;
    private CountDownLatch repliesLatch;

    public AsynchronousDNSWithCacheRun(String fileName) {
        broker = new AsynchronousBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        subscriber = new AsynchronousSubscriber("Subscriber1", broker);
        publisher = new AsynchronousPublisher("Publisher1", broker);
        domainDB = DBFactory.getDomainsDB(fileName);
        tasks = new ArrayList<>();
        name = this.getClass().getSimpleName();
    }

    @Override
    public void setUp() {
        broker.startProcessing();
        publisher.init();
        subscriber.init();
    }
    
    @Override
    public void addTask(AsynchronousExperimentTask task) {
        tasks.add(task);
    }

    @Override
    public void start() {
        requestsLatch = new CountDownLatch(tasks.size());
        repliesLatch = new CountDownLatch(tasks.size());
        for (ExperimentTask task : tasks) {
            Thread t = new Thread(task, task.getName());
            t.start();
        }       
    }

    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Cleaning Up Run");
        publisher.publish(new PoisonPillPublication());
        subscriber.subscribe(new PoisonPillSubscription());
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
    public List<AsynchronousExperimentTask> getTasks() {
        return tasks;
    }


    public static void main(String[] args) {
        String home = System.getProperty("user.home");
        AsynchronousDNSWithCacheRun dnsPlus = new AsynchronousDNSWithCacheRun(home + "/websites.txt");

        dnsPlus.setUp();
        
        ConcurrentDNSWithCacheRunPublisherTask publisherTask = dnsPlus.new ConcurrentDNSWithCacheRunPublisherTask();
        ConcurrentDNSWithCacheRunSubscriberTask subscriberTask = dnsPlus.new ConcurrentDNSWithCacheRunSubscriberTask();
        
        dnsPlus.addTask(publisherTask);
        dnsPlus.addTask(subscriberTask);
        
        dnsPlus.start();
        
        dnsPlus.waitForRequestsCompletion();
        
        dnsPlus.cleanUp();
        
        dnsPlus.waitForRepliesCompletion();
        
        for (AsynchronousExperimentTask t : dnsPlus.getTasks()) {
            System.out.println("Task " + t.getName() + " request took " + t.getDuration() + " ms");
            System.out.println("Task " + t.getName() + " reply took " + t.getReplyDuration() + " ms");
        }
    }

    

    final class ConcurrentDNSWithCacheRunPublisherTask extends AsynchronousExperimentTask implements AsynchronousMeasurementListener {
        private static final Logger logger = CustomLogger.getLogger(ConcurrentDNSWithCacheRunPublisherTask.class.getName());

        public ConcurrentDNSWithCacheRunPublisherTask() {
            setName(this.getClass().getSimpleName());
            registerWithMeasurementProducer();
        }
        
        @Override
        public void registerWithMeasurementProducer() {
            broker.addPublicationMeasurementListener(this);
        }

        @Override
        public void asynchronousMeasurementPerformed(Duration replyDuration) {
            logger.log(Level.WARNING, "Received measurement {0}", replyDuration.toMillis());
            setReplyDuration(replyDuration);
            repliesLatch.countDown();
        }
        

        @Override
        public void run() {
            List<String> randomEntries = domainDB.getRandomEntries(1000);
            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                    -> {
                publisher.generateAndPublishAll(randomEntries);
                return null;
            });
            setRequestDuration(result.getDuration());
            logger.log(Level.WARNING, "Publisher task request completed");
            requestsLatch.countDown();
        }
    }
    
    final class ConcurrentDNSWithCacheRunSubscriberTask extends AsynchronousExperimentTask implements AsynchronousMeasurementListener {

        public ConcurrentDNSWithCacheRunSubscriberTask() {
            setName(this.getClass().getSimpleName());
            registerWithMeasurementProducer();
        }
        
        @Override
        public void registerWithMeasurementProducer() {
            broker.addSubscriptionMeasurementListener(this);
        }
        
        @Override
        public void asynchronousMeasurementPerformed(Duration replyDuration) {
            logger.log(Level.WARNING, "Received measurement {0}", replyDuration.toMillis());
            setReplyDuration(replyDuration);
            repliesLatch.countDown();
        }

        @Override
        public void run() {
            List<String> randomEntries = domainDB.getRandomEntries(1000);
            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                    -> {
                subscriber.generateAndSubscribeToAll(randomEntries);
                return null;
            });
            setRequestDuration(result.getDuration());
            logger.log(Level.WARNING, "Subscriber task request completed");
            requestsLatch.countDown();
        }
    }
}