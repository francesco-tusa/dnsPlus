package broker.tree.binarybalanced;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public class PublicationProcessor implements Runnable {
    
    private ConcurrentBrokerWithBinaryBalancedTreeAndCache broker;
    private BlockingQueue<Publication> publicationQueue;
    private BlockingQueue<Subscription> resultQueue;
    private boolean threadRunning;
    
    public PublicationProcessor(ConcurrentBrokerWithBinaryBalancedTreeAndCache b) {
        broker = b;
        publicationQueue = new LinkedBlockingQueue<>();
        resultQueue = new LinkedBlockingQueue<>();
        threadRunning = false;
    }
    
    public void startReceiving() {
        threadRunning = true;
        Thread publicationProcessor = new Thread(this, "publicationProcessor");
        publicationProcessor.start();
    }
    
    public void stopReceiving() {
        threadRunning = false;
    }
    
    public void addPublication(Publication p) {
        System.out.println("Adding publication to the queue: " + p.getServiceName());
        if (!publicationQueue.offer(p)) {
            System.out.println("Error adding publication to the queue");
        }
    }
    
    public Subscription getMatchResult() throws InterruptedException {
        System.out.println("Getting result from publicationQueue");
        return resultQueue.take();
    }
    
    public void addMatchResult(Subscription s) {
        System.out.println("Adding matching publication to the result queue: " + s.getServiceName());
    }

    @Override
    public void run() {
        while (threadRunning) {
            try {
                System.out.println("Taking a publication off the queue");
                Publication p = publicationQueue.take();
                if (p != null) {
                    System.out.println("Taking publication off queue: " + p.getServiceName());
                    broker.matchPublication(p);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
