package broker.tree.binarybalanced.cache.asynchronous;

import broker.AsynchronousCachingBroker;
import experiments.cache.asynchronous.AsynchronousTask;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.PoisonPillPublication;
import publishing.Publication;
import subscribing.PoisonPillSubscription;
import subscribing.Subscription;
import utils.CustomLogger;
import utils.ExecutionTimeLogger;
import experiments.measurement.AsynchronousSubscriptionMeasurementListener;
import experiments.measurement.AsynchronousSubscriptionMeasurementProducer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import subscribing.MeasurementPillSubscription;
import utils.ExecutionTimeResult;

/**
 *
 * @author uceeftu
 */
public class SubscriptionProcessor implements Runnable, AsynchronousSubscriptionMeasurementProducer {
    
    private static final Logger logger = CustomLogger.getLogger(SubscriptionProcessor.class.getName());
    private AsynchronousCachingBroker broker;
    private BlockingQueue<Subscription> subscriptionQueue;
    private BlockingQueue<Publication> resultQueue;

    private Map<String, AsynchronousTask> subscriptionTasks = Collections.synchronizedMap(new HashMap<>());
    private int poisonPills;
    private int measurementPills;
    
    private List<AsynchronousSubscriptionMeasurementListener> measurementListeners;
    
    
    public SubscriptionProcessor(AsynchronousCachingBroker b) {
        broker = b;
        subscriptionQueue = new LinkedBlockingQueue<>();
        resultQueue = new LinkedBlockingQueue<>();
        measurementListeners = new ArrayList<>();
        poisonPills = measurementPills = 0;
    }
    
    public void startProcessing() {
        Thread subscriptionProcessor = new Thread(this, "subscriptionProcessor");
        subscriptionProcessor.start();
    }
    
    public void addSubscription(Subscription s) {
        logger.log(Level.FINE, "Adding subscription to the queue: {0}", s.getServiceName());
        if (!subscriptionQueue.offer(s)) {
            logger.log(Level.WARNING, "Error adding subscription to the queue");
        }
    }
    
    public Publication getMatchResult() throws InterruptedException {
        logger.log(Level.FINEST, "Getting result from subscriptionQueue");
        return resultQueue.take();
    }
    
    public void addMatchResult(Publication p) {
        logger.log(Level.FINEST, "Adding matching publication to the result queue: {0}", p.getServiceName());
        resultQueue.offer(p); // Store the result in the result queue
    }
    
    @Override
    public void addSubscriptionTask(AsynchronousTask subscriberTask) {
        subscriptionTasks.putIfAbsent(subscriberTask.getName(), subscriberTask);
    }

    @Override
    public void removeSubscriptionTask(String subscriberTaskName) {
        subscriptionTasks.remove(subscriberTaskName);
    }
    
    public int getPublicationsQueueSize() {
        return resultQueue.size();
    }
    

    @Override
    public void addSubscriptionMeasurementListener(AsynchronousSubscriptionMeasurementListener l) {
        measurementListeners.add(l);
    }


    @Override
    public void run() {
        
        Duration duration = Duration.ZERO;
        
        logger.log(Level.FINE, "Started subscription processor thread");
        while (true) {
            try {
                logger.log(Level.FINEST, "Taking a subscription off the queue");
                Subscription s = subscriptionQueue.take();
                if (s != null) {
                    if (s instanceof PoisonPillSubscription) {
                        poisonPills++;
                        if (stopProcessing()) {
                            logger.log(Level.WARNING, "Thread {0} is being stopped", Thread.currentThread().getName());
                            resultQueue.offer(new PoisonPillPublication(""));
                            break;
                        }
                    } else if (s instanceof MeasurementPillSubscription) {
                        logger.log(Level.FINE, "Requested Measurements");
                        measurementPills++;
                        if (stopProcessing()) {
                            sendMeasurement(duration);
                            duration = Duration.ZERO;
                            measurementListeners.clear();
                            measurementPills = 0;
                        }
                    } else {
                        logger.log(Level.FINEST, "Subscription for {0} taken off the queue", s.getServiceName());

                        ExecutionTimeResult<Void> result = 
                            ExecutionTimeLogger.measure(
                                "cacheLookUp",
                                () -> { broker.cacheLookUp(s);
                                        return null; }
                            );

                        duration = duration.plus(result.getDuration());
                    }

                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
                break;
            }
        }
        
        // sending duration to listeners
        sendMeasurement(duration);
    }
    
    private boolean stopProcessing() {
        logger.log(Level.FINER, "map size: {0}", subscriptionTasks.size());
        logger.log(Level.FINER, "poison pills counter: {0}", poisonPills);
        logger.log(Level.FINER, "measurement pills counter: {0}", measurementPills);
        
        return subscriptionTasks.size() == poisonPills || 
               subscriptionTasks.size() == measurementPills;  
    }
    
    private void sendMeasurement(Duration d) {
        logger.log(Level.FINE, "Sending Measurement to listeners");
        for (AsynchronousSubscriptionMeasurementListener l : measurementListeners) {
            l.subscriptionMeasurementPerformed(d);
        } 
    }
}
