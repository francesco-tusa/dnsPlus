package broker.tree.binarybalanced;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class SubscriptionProcessor implements Runnable {
    
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
        System.out.println("Adding subscription to the queue: " + s.getServiceName());
        if (!subscriptionQueue.offer(s)) {
            System.out.println("Error adding subscription to the queue");
        }
    }
    
    public Publication getMatchResult() throws InterruptedException {
        System.out.println("Getting result from subscriptionQueue");
        return resultQueue.take();
    }
    
    public void addMatchResult(Publication p) {
        resultQueue.offer(p); // Store the result in the result queue
    }

    @Override
    public void run() {
        while (threadRunning) {
            try {
                System.out.println("Taking a subscription off the queue");
                Subscription s = subscriptionQueue.take();
                if (s != null) {
                    System.out.println("Subscription for " + s.getServiceName() + " taken off the queue");
                    broker.matchSubscription(s); 
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
