package broker.tree.binarybalanced;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import publishing.Publication;
import subscribing.Subscription;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public class SubscriptionProcessor implements Runnable {
    
    private static final Logger logger = CustomLogger.getLogger(SubscriptionProcessor.class.getName());
    private ConcurrentBrokerWithBinaryBalancedTreeAndCache broker;
    private BlockingQueue<Subscription> subscriptionQueue;
    private BlockingQueue<Publication> resultQueue;
    private boolean threadRunning;
    
    public SubscriptionProcessor(ConcurrentBrokerWithBinaryBalancedTreeAndCache b) {
        broker = b;
        subscriptionQueue = new LinkedBlockingQueue<>();
        resultQueue = new LinkedBlockingQueue<>();
        threadRunning = false;
    }
    
    public void startProcessing() {
        threadRunning = true;
        Thread subscriptionProcessor = new Thread(this, "subscriptionProcessor");
        subscriptionProcessor.start();
    }
    
    public void stopProcessing() {
        threadRunning = false;
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
    public void run() {
        while (threadRunning) {
            try {
                logger.log(Level.FINER, "Taking a subscription off the queue");
                Subscription s = subscriptionQueue.take();
                if (s != null) {
                    logger.log(Level.FINE, "Subscription for {0} taken off the queue", s.getServiceName());
                    broker.matchSubscription(s); 
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
                Thread.currentThread().interrupt();
            }
        }
    }
}
