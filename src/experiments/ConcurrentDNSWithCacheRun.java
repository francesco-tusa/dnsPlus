package experiments;

import experiments.measurement.TaskDurationMeasurementListener;
import encryption.HEPS;
import broker.tree.binarybalanced.ConcurrentBrokerWithBinaryBalancedTreeAndCache;
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
import experiments.measurement.MeasurableAsynchronousBroker;
import publishing.PoisonPillPublication;
import subscribing.PoisonPillSubscription;

/**
 *
 * @author f.tusa
 */
public final class ConcurrentDNSWithCacheRun implements ExperimentRun {

    private static final Logger logger = CustomLogger.getLogger(ConcurrentDNSWithCacheRun.class.getName(), Level.WARNING);
    private AsynchronousSubscriber subscriber;
    private AsynchronousPublisher publisher;
    private MeasurableAsynchronousBroker broker;
    private DomainDB domainDB;
    
    private List<ExperimentParallelTask> tasks;
    private String name;
    private CountDownLatch latch;

    public ConcurrentDNSWithCacheRun(String fileName) {
        broker = new ConcurrentBrokerWithBinaryBalancedTreeAndCache("Broker1", HEPS.getInstance());
        subscriber = new AsynchronousSubscriber("Subscriber1", broker);
        publisher = new AsynchronousPublisher("Publisher1", broker);
        domainDB = DBFactory.getDomainsDB(fileName);
        tasks = new ArrayList<>();
        name = this.getClass().getSimpleName();
    }

    @Override
    public void setUp() {
        publisher.init();
        subscriber.init();
    }
    
    @Override
    public void addTask(ExperimentParallelTask task) {
        tasks.add(task);
    }

    @Override
    public void start() {
        latch = new CountDownLatch(tasks.size());
        for (ExperimentParallelTask task : tasks) {
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
    public void waitForCompletion() {
        try {
            latch.await();
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
    public List<ExperimentParallelTask> getTasks() {
        return tasks;
    }


    public static void main(String[] args) {
        String home = System.getProperty("user.home");
        ConcurrentDNSWithCacheRun dnsPlus = new ConcurrentDNSWithCacheRun(home + "/websites.txt");

        dnsPlus.setUp();
        
        ConcurrentDNSWithCacheRunPublisherTask publisherTask = dnsPlus.new ConcurrentDNSWithCacheRunPublisherTask();
        ConcurrentDNSWithCacheRunSubscriberTask subscriberTask = dnsPlus.new ConcurrentDNSWithCacheRunSubscriberTask();
        
        dnsPlus.addTask(publisherTask);
        dnsPlus.addTask(subscriberTask);
        
        publisherTask.register();
        subscriberTask.register();
        
        dnsPlus.start();
        
        dnsPlus.waitForCompletion();
        
        dnsPlus.cleanUp();
        
//        for (ExperimentParallelTask t : dnsPlus.getTasks()) {
//            System.out.println("Task " + t.getName() + " took " + t.getDuration() + "ms");
//        }
    }

    

    final class ConcurrentDNSWithCacheRunPublisherTask extends ExperimentParallelTask implements TaskDurationMeasurementListener {
        private static final Logger logger = CustomLogger.getLogger(ConcurrentDNSWithCacheRunPublisherTask.class.getName());

        public ConcurrentDNSWithCacheRunPublisherTask() {
            name = this.getClass().getSimpleName();
            duration = Duration.ZERO;
        }
        
        @Override
        public void register() {
            broker.addPublicationMeasurementListener(this);
        }

        @Override
        public void measurementPerformed(Duration d) {
            logger.log(Level.WARNING, "Received measurement {0}", d.toMillis());
            duration = duration.plus(d);
        }
        

        @Override
        public void run() {
            List<String> randomEntries = domainDB.getRandomEntries(1000);
            publisher.generateAndPublishAll(randomEntries);
//            List<Publication> generatedPublications = publisher.generatePublications(randomEntries);
//            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
//                    -> {
//                publisher.publishAll(generatedPublications);
//                return null;
//            });
//            duration = duration.plus(result.getDuration());
            logger.log(Level.WARNING, "Publisher task completed");
            latch.countDown();
        }
    }
    
    final class ConcurrentDNSWithCacheRunSubscriberTask extends ExperimentParallelTask implements TaskDurationMeasurementListener {

        public ConcurrentDNSWithCacheRunSubscriberTask() {
            name = this.getClass().getSimpleName();
            duration = Duration.ZERO;
        }
        
        @Override
        public void register() {
            broker.addSubscriptionMeasurementListener(this);
        }
        
        @Override
        public void measurementPerformed(Duration d) {
            logger.log(Level.WARNING, "Received measurement {0}", d.toMillis());
            duration = duration.plus(d);
        }

        @Override
        public void run() {
            List<String> randomEntries = domainDB.getRandomEntries(1000);
            subscriber.generateAndSubscribeToAll(randomEntries);
//            List<Subscription> generatedSubscriptions = subscriber.generateSubscriptions(randomEntries);
//            ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
//                    -> {
//                subscriber.subscribeToAll(generatedSubscriptions);
//                return null;
//            });
//            duration = duration.plus(result.getDuration());
            logger.log(Level.WARNING, "Subscriber task completed");
            latch.countDown();
        }
    }
}