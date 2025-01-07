package broker.tree.binarybalanced.cache.asynchronous;

import broker.AsynchronousCachingBroker;
import experiments.measurement.AsynchronousMeasurementProducer;
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
import experiments.measurement.AsynchronousMeasurementListener;
import experiments.measurement.AsynchronousSubscriptionMeasurementListener;
import experiments.measurement.AsynchronousSubscriptionMeasurementProducer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import subscribing.AsynchronousSubscriber;

/**
 *
 * @author uceeftu
 */
public class SubscriptionProcessor implements Runnable, AsynchronousSubscriptionMeasurementProducer {
    
    private static final Logger logger = CustomLogger.getLogger(SubscriptionProcessor.class.getName());
    private AsynchronousCachingBroker broker;
    private BlockingQueue<Subscription> subscriptionQueue;
    private BlockingQueue<Publication> resultQueue;

    private Map<String, AsynchronousSubscriber> subscribers = Collections.synchronizedMap(new HashMap<>());
    
    private List<AsynchronousSubscriptionMeasurementListener> measurementListeners;
    
    
    public SubscriptionProcessor(AsynchronousCachingBroker b) {
        broker = b;
        subscriptionQueue = new LinkedBlockingQueue<>();
        resultQueue = new LinkedBlockingQueue<>();
        measurementListeners = new ArrayList<>();
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
    
    public void addSubscriber(AsynchronousSubscriber subscriber) {
        subscribers.putIfAbsent(subscriber.getName(), subscriber);
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
                    if (s instanceof PoisonPillSubscription poisonPill && stopProcessing(poisonPill)) {
                        logger.log(Level.WARNING, "Thread {0} is being stopped", Thread.currentThread().getName());
                        resultQueue.offer(new PoisonPillPublication(""));
                        break;
                    }
                    logger.log(Level.FINEST, "Subscription for {0} taken off the queue", s.getServiceName());

                    ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                            -> {
                        broker.cacheLookUp(s);
                        return null;
                    });
                    
                    duration = duration.plus(result.getDuration());

                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
                Thread.currentThread().interrupt();
            }
        }
        
        // sending duration to listeners
        sendMeasurement(duration);
    }
    
    private boolean stopProcessing(PoisonPillSubscription s) {
        subscribers.remove(s.getSubscriber());
        return subscribers.isEmpty();      
    }
    
    private void sendMeasurement(Duration d) {
        logger.log(Level.FINE, "Sending Measurement to listeners");
        for (AsynchronousSubscriptionMeasurementListener l : measurementListeners) {
            l.subscriptionMeasurementPerformed(d);
        } 
    }
}
