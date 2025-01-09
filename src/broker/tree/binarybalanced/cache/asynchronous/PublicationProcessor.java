package broker.tree.binarybalanced.cache.asynchronous;

import broker.AsynchronousBroker;
import experiments.cache.asynchronous.AsynchronousTask;
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
import experiments.measurement.AsynchronousPublicationMeasurementListener;
import experiments.measurement.AsynchronousPublicationMeasurementProducer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import publishing.MeasurementPillPublication;
import utils.ExecutionTimeResult;

/**
 *
 * @author uceeftu
 */
public class PublicationProcessor implements Runnable, AsynchronousPublicationMeasurementProducer{
    
    private static final Logger logger = CustomLogger.getLogger(PublicationProcessor.class.getName());
    private AsynchronousBroker broker;
    private BlockingQueue<Publication> publicationQueue;
    private BlockingQueue<Subscription> resultQueue;
    
    private List<AsynchronousPublicationMeasurementListener> measurementListeners;
    
    private Map<String, AsynchronousTask> publicationTasks = Collections.synchronizedMap(new HashMap<>());
    private int poisonPills;
    private int measurementPills;
    
    
    public PublicationProcessor(AsynchronousBroker b) {
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
    public void addPublicationTask(AsynchronousTask publisherTask) {
        publicationTasks.putIfAbsent(publisherTask.getName(), publisherTask);
    }

    @Override
    public void removePublicationTask(String publisherTaskName) {
        publicationTasks.remove(publisherTaskName);
    }
    
    
    @Override
    public void addPublicationMeasurementListener(AsynchronousPublicationMeasurementListener l) {
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
                        poisonPills++;
                        if (stopProcessing()) {
                            logger.log(Level.WARNING, "Thread {0} is being stopped", Thread.currentThread().getName());
                            resultQueue.offer(new PoisonPillSubscription(""));
                            break;
                        }
                    } else if (p instanceof MeasurementPillPublication) {
                        logger.log(Level.FINE, "Requested Measurements");
                        measurementPills++;
                        if (stopProcessing()) {
                            sendMeasurement(duration);
                            duration = Duration.ZERO;
                            measurementListeners.clear();
                            measurementPills = 0;
                        }
                    } else {
                        logger.log(Level.FINEST, "Publication for {0} taken off the queue", p.getServiceName());

                        ExecutionTimeResult<Void> result = 
                            ExecutionTimeLogger.measure(
                                "matchPublication",    
                                () -> { broker.matchPublication(p);
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
        logger.log(Level.FINER, "map size: {0}", publicationTasks.size());
        logger.log(Level.FINER, "poison pills counter: {0}", poisonPills);
        logger.log(Level.FINER, "measurement pills counter: {0}", measurementPills);
        
        return publicationTasks.size() == poisonPills || 
               publicationTasks.size() == measurementPills;  
    }
    
    
    private void sendMeasurement(Duration d) {
        logger.log(Level.FINE, "Sending Measurement to listeners");
        for (AsynchronousPublicationMeasurementListener l : measurementListeners) {
            l.publicationMeasurementPerformed(d);
        } 
    }
}
