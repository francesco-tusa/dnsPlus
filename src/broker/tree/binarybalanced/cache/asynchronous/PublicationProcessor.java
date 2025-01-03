package broker.tree.binarybalanced.cache.asynchronous;

import experiments.measurement.AsynchronousMeasurementProducer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import publishing.Publication;
import subscribing.Subscription;
import java.util.logging.Logger;
import java.util.logging.Level;
import publishing.PoisonPillPublication;
import subscribing.PoisonPillSubscription;
import utils.CustomLogger;
import utils.ExecutionTimeLogger;
import experiments.measurement.AsynchronousMeasurementListener;

/**
 *
 * @author uceeftu
 */
public class PublicationProcessor implements Runnable, AsynchronousMeasurementProducer{
    
    private static final Logger logger = CustomLogger.getLogger(PublicationProcessor.class.getName());
    private AsynchronousBrokerWithBinaryBalancedTreeAndCache broker;
    private BlockingQueue<Publication> publicationQueue;
    private BlockingQueue<Subscription> resultQueue;
    
    private List<AsynchronousMeasurementListener> measurementListeners;
    
    
    public PublicationProcessor(AsynchronousBrokerWithBinaryBalancedTreeAndCache b) {
        broker = b;
        publicationQueue = new LinkedBlockingQueue<>();
        resultQueue = new LinkedBlockingQueue<>();
        measurementListeners = new ArrayList<>();
    }
    
    public void startProcessing() {
        Thread publicationProcessor = new Thread(this, "publicationProcessor");
        publicationProcessor.start();
    }
    
    public void addPublication(Publication p) {
        logger.log(Level.FINE, "Adding publication to the queue: {0}", p.getServiceName());
        if (!publicationQueue.offer(p)) {
            logger.log(Level.WARNING, "Error adding publication to the queue");
        }
    }
    
    public Subscription getMatchResult() throws InterruptedException {
        logger.log(Level.FINEST, "Getting result from publicationQueue");
        return resultQueue.take();
    }
    
    public void addMatchResult(Subscription s) {
        logger.log(Level.FINEST, "Adding matching subscription to the result queue: {0}", s.getServiceName());
        resultQueue.offer(s);
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
                logger.log(Level.FINEST, "Taking a publication off the queue");
                Publication p = publicationQueue.take();
                if (p != null) {
                    if (p instanceof PoisonPillPublication) {
                        logger.log(Level.WARNING, "Thread {0} is being stopped", Thread.currentThread().getName());
                        resultQueue.offer(new PoisonPillSubscription());
                        break;
                    }
                    logger.log(Level.FINEST, "Publication for {0} taken off the queue", p.getServiceName());
                    
                    ExecutionTimeLogger.ExecutionResult<Void> result = ExecutionTimeLogger.measureExecutionTime(()
                            -> {
                        broker.matchPublication(p);
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
        logger.log(Level.FINE, "Sending Measurement to listeners");
        for (AsynchronousMeasurementListener l : measurementListeners) {
            l.asynchronousMeasurementPerformed(d);
        } 
    }
}
