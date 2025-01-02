package broker.tree.binarybalanced.cache.asynchronous;

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

/**
 *
 * @author uceeftu
 */
public class SubscriptionProcessor implements Runnable, AsynchronousMeasurementProducer {
    
    private static final Logger logger = CustomLogger.getLogger(SubscriptionProcessor.class.getName());
    private AsynchronousBrokerWithBinaryBalancedTreeAndCache broker;
    private BlockingQueue<Subscription> subscriptionQueue;
    private BlockingQueue<Publication> resultQueue;
    
    private List<AsynchronousMeasurementListener> measurementListeners;
    
    
    public SubscriptionProcessor(AsynchronousBrokerWithBinaryBalancedTreeAndCache b) {
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
        logger.log(Level.FINER, "Getting result from subscriptionQueue");
        return resultQueue.take();
    }
    
    public void addMatchResult(Publication p) {
        logger.log(Level.FINE, "Adding matching publication to the result queue: {0}", p.getServiceName());
        resultQueue.offer(p); // Store the result in the result queue
    }

    @Override
    public void addMeasurementListener(AsynchronousMeasurementListener l) {
        measurementListeners.add(l);
    }
    

    @Override
    public void run() {
        
        Duration duration = Duration.ZERO;
        
        while (true) {
            try {
                logger.log(Level.FINER, "Taking a subscription off the queue");
                Subscription s = subscriptionQueue.take();
                if (s != null) {
                    if (s instanceof PoisonPillSubscription) {
                        logger.log(Level.WARNING, "Thread {0} is being stopped", Thread.currentThread().getName());
                        resultQueue.offer(new PoisonPillPublication());
                        break;
                    }
                    logger.log(Level.FINE, "Subscription for {0} taken off the queue", s.getServiceName());

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
    
    private void sendMeasurement(Duration d) {
        logger.log(Level.WARNING, "Sending Measurement to listeners");
        for (AsynchronousMeasurementListener l : measurementListeners) {
            l.asynchronousMeasurementPerformed(d);
        } 
    }
}
