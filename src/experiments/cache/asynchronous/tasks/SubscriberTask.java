package experiments.cache.asynchronous.tasks;

import broker.AsynchronousBroker;
import experiments.cache.asynchronous.AsynchronousTask;
import experiments.inputdata.DBFactory;
import experiments.measurement.AsynchronousSubscriptionMeasurementListener;
import java.time.Duration;
import java.util.List;
import utils.CustomLogger;
import java.util.logging.Logger;
import java.util.logging.Level;
import subscribing.ReceivingSubscriber;
import subscribing.PoisonPillSubscription;
import utils.ExecutionTimeResult;
import utils.ExecutionTimeLogger;
import experiments.PubSubRunTasksExecutor;

/**
 *
 * @author uceeftu
 */
public class SubscriberTask extends AsynchronousTask implements AsynchronousSubscriptionMeasurementListener {
    private static final Logger logger = CustomLogger.getLogger(SubscriberTask.class.getName());
    
    private ReceivingSubscriber subscriber;
    private PubSubRunTasksExecutor taskRunner;
    private String domainsFile;
    
    private int numberOfSubscriptions;
    private List<String> subscriptionDomains;
    
    
    private SubscriberTask(ReceivingSubscriber subscriber, PubSubRunTasksExecutor taskRunner) {
        this.subscriber = subscriber;
        this.taskRunner = taskRunner;
        subscriber.setBroker((AsynchronousBroker)taskRunner.getBroker());
        subscriber.receivePublications();
    }
    
    public SubscriberTask(ReceivingSubscriber subscriber, PubSubRunTasksExecutor taskRunner, String domainsFile, int nSubscriptions) {
        this(subscriber, taskRunner);
        this.domainsFile = domainsFile;
        numberOfSubscriptions = nSubscriptions;
        subscriptionDomains = DBFactory.getDomainsDB(domainsFile).getRandomEntries(nSubscriptions);
        setName(generateTaskDescription());
    }
    
    public SubscriberTask(ReceivingSubscriber subscriber, PubSubRunTasksExecutor taskRunner, List<String> domains) {
        this(subscriber, taskRunner);
        numberOfSubscriptions = domains.size();
        subscriptionDomains = domains;
        setName(generateTaskDescription());
    }
    
    public SubscriberTask(String subscriberName, PubSubRunTasksExecutor taskRunner, List<String> domains) {
        this(new ReceivingSubscriber(subscriberName), taskRunner, domains);
    }
    
    public SubscriberTask(String subscriberName, PubSubRunTasksExecutor taskRunner, String domainsFile, int nSubscriptions) {
        this(new ReceivingSubscriber(subscriberName), taskRunner, domainsFile, nSubscriptions);
    }
    
    protected ReceivingSubscriber getSubscriber() {
        return subscriber;
    }

    protected PubSubRunTasksExecutor getTaskRunner() {
        return taskRunner;
    }
    
    private String generateTaskDescription() {
        return subscriber.getName() + ":subscribe:" + numberOfSubscriptions;
    } 
    
    
    @Override
    public void registerWithMeasurementProducer() {
        taskRunner.getBroker().addSubscriptionMeasurementListener(this);
        taskRunner.getBroker().addSubscriptionTask(this);
    }

    @Override
    public void subscriptionMeasurementPerformed(Duration replyDuration) {
        logger.log(Level.INFO, "Subscriber {0} received measurement {1}", new Object[]{subscriber.getName(), replyDuration.toMillis()});
        setAsynchronousProcessingDuration(replyDuration);
        taskRunner.setTaskResponseReceived();
        // remove task from broker when a measurement is received
        taskRunner.getBroker().removeSubscriptionTask(getName());
    }

    @Override
    public void run() {
        registerWithMeasurementProducer();
        
        ExecutionTimeResult<Void> result = 
            ExecutionTimeLogger.measure(
                "generateAndSubscribeToAll",
                () ->   { subscriber.generateAndSubscribeToAll(subscriptionDomains);
                          return null; }
            );
        
        //TODO: should use a map <calledMethod, duration> to allow
        // storing results of requests consisting of multiple method calls
        setDuration(result.getDuration()); 
        logger.log(Level.WARNING, "Subscriber {0} completed their request task operation: {1}", 
                                  new Object[]{subscriber.getName(), result.getCalledMethod()});
        taskRunner.setTaskRequestCompleted();
    }

    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Subscriber {0} is cleaning up run", subscriber.getName());
        String name = subscriber.getName();
        subscriber.subscribe(new PoisonPillSubscription(name));
        logger.log(Level.FINE, "Subscriber {0} sent a poison pill subscription", name);
    }
}
