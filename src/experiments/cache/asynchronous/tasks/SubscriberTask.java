package experiments.cache.asynchronous.tasks;

import broker.AsynchronousBroker;
import experiments.PubSubTaskDelegator;
import experiments.cache.asynchronous.AsynchronousTask;
import experiments.inputdata.DBFactory;
import experiments.measurement.AsynchronousSubscriptionMeasurementListener;
import java.time.Duration;
import java.util.List;
import utils.CustomLogger;
import java.util.logging.Logger;
import java.util.logging.Level;
import subscribing.AsynchronousSubscriber;
import subscribing.PoisonPillSubscription;
import utils.ExecutionTimeResult;
import utils.ExecutionTimeLogger;

/**
 *
 * @author uceeftu
 */
public class SubscriberTask extends AsynchronousTask implements AsynchronousSubscriptionMeasurementListener {
    private static final Logger logger = CustomLogger.getLogger(SubscriberTask.class.getName());
    
    private AsynchronousSubscriber subscriber;
    private PubSubTaskDelegator taskRunner;
    private String domainsFile;
    private int numberOfSubscriptions;
    
    
    public SubscriberTask(AsynchronousSubscriber subscriber, PubSubTaskDelegator taskRunner, String domainsFile, int nSubscriptions) {
        this.taskRunner = taskRunner;
        this.domainsFile = domainsFile;
        this.subscriber = subscriber;
        this.subscriber.setBroker((AsynchronousBroker)taskRunner.getBroker());
        numberOfSubscriptions = nSubscriptions;
        setName(generateTaskDescription());
    }
    
    public SubscriberTask(AsynchronousSubscriber subscriber, PubSubTaskDelegator taskRunner, String domainsFile) {
        this(subscriber, taskRunner, domainsFile, taskRunner.getNumberOfSubscriptions());  
    }
    
    public SubscriberTask(String subscriberName, PubSubTaskDelegator taskRunner, String domainsFile) {
        this(new AsynchronousSubscriber(subscriberName), taskRunner, domainsFile);
    }
    
    public SubscriberTask(String subscriberName, PubSubTaskDelegator taskRunner, String domainsFile, int nSubscriptions) {
        this(new AsynchronousSubscriber(subscriberName), taskRunner, domainsFile, nSubscriptions);
    }

    protected AsynchronousSubscriber getSubscriber() {
        return subscriber;
    }

    protected PubSubTaskDelegator getTaskRunner() {
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
        setReplyDuration(replyDuration);
        taskRunner.taskResponseReceived();
        // remove task from broker when a measurement is received
        taskRunner.getBroker().removeSubscriptionTask(getName());
    }

    @Override
    public void run() {
        registerWithMeasurementProducer();
        List<String> randomEntries = DBFactory.getDomainsDB(domainsFile).getRandomEntries(numberOfSubscriptions);
        
        ExecutionTimeResult<Void> result = 
            ExecutionTimeLogger.measure(
                "generateAndSubscribeToAll",
                () ->   { subscriber.generateAndSubscribeToAll(randomEntries);
                          return null; }
            );
        
        //TODO: should use a map <calledMethod, duration> to allow
        // storing results of requests consisting of multiple method calls
        setDuration(result.getDuration()); 
        logger.log(Level.WARNING, "Subscriber {0} completed their request task operation: {1}", 
                                  new Object[]{subscriber.getName(), result.getCalledMethod()});
        taskRunner.taskRequestCompleted();
    }

    @Override
    public void cleanUp() {
        logger.log(Level.INFO, "Subscriber {0} is cleaning up run", subscriber.getName());
        String name = subscriber.getName();
        subscriber.subscribe(new PoisonPillSubscription(name));
        logger.log(Level.FINE, "Subscriber {0} sent a poison pill subscription", name);
    }
}
