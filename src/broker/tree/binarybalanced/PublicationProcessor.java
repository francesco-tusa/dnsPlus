package broker.tree.binarybalanced;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import publishing.Publication;
import subscribing.Subscription;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.CustomLogger;

/**
 *
 * @author uceeftu
 */
public class PublicationProcessor implements Runnable {
    
    private static final Logger logger = CustomLogger.getLogger(PublicationProcessor.class.getName());
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
        logger.log(Level.FINE, "Adding publication to the queue: {0}", p.getServiceName());
        if (!publicationQueue.offer(p)) {
            logger.log(Level.WARNING, "Error adding publication to the queue");
        }
    }
    
    public Subscription getMatchResult() throws InterruptedException {
        logger.log(Level.FINER, "Getting result from publicationQueue");
        return resultQueue.take();
    }
    
    public void addMatchResult(Subscription s) {
        logger.log(Level.FINE, "Adding matching subscription to the result queue: {0}", s.getServiceName());
        resultQueue.offer(s);
    }

    @Override
    public void run() {
        while (threadRunning) {
            try {
                logger.log(Level.FINER, "Taking a publication off the queue");
                Publication p = publicationQueue.take();
                if (p != null) {
                    logger.log(Level.FINE, "Publication for {0} taken off the queue", p.getServiceName());
                    broker.matchPublication(p);
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Thread {0} was interrupted",  Thread.currentThread().getName());
                Thread.currentThread().interrupt();
            }
        }
    }
}
