package broker.tree.binarybalanced.cache.asynchronous;

import broker.tree.binarybalanced.cache.BrokerWithBinaryBalancedTreeAndCache;
import encryption.HEPS;
import publishing.Publication;
import subscribing.Subscription;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.CustomLogger;
import experiments.measurement.AsynchronousMeasurementListener;
import experiments.measurement.AsynchronousMeasurementProducerBroker;

public final class AsynchronousBrokerWithBinaryBalancedTreeAndCache extends BrokerWithBinaryBalancedTreeAndCache implements AsynchronousMeasurementProducerBroker {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousBrokerWithBinaryBalancedTreeAndCache.class.getName());
    private SubscriptionProcessor subscriptionProcessor;
    private PublicationProcessor publicationProcessor;
        
    public AsynchronousBrokerWithBinaryBalancedTreeAndCache(String name, HEPS heps) {
        super(name, heps);
        subscriptionProcessor = new SubscriptionProcessor(this);
        publicationProcessor = new PublicationProcessor(this);
    }

    public SubscriptionProcessor getSubscriptionProcessor() {
        return subscriptionProcessor;
    }

    public PublicationProcessor getPublicationProcessor() {
        return publicationProcessor;
    }

    public void setPublicationProcessor(PublicationProcessor publicationProcessor) {
        this.publicationProcessor = publicationProcessor;
    }

    @Override
    public void startProcessing() {
        subscriptionProcessor.startProcessing();
        publicationProcessor.startProcessing();
    }
    
    @Override
    public void addPublicationMeasurementListener(AsynchronousMeasurementListener listener) {
        publicationProcessor.addMeasurementListener(listener);
    }

    @Override
    public void addSubscriptionMeasurementListener(AsynchronousMeasurementListener listener) {
        subscriptionProcessor.addMeasurementListener(listener);
    }
    

    @Override
    public void processSubscription(Subscription s) {
        subscriptionProcessor.addSubscription(s);
    }

    @Override
    public Publication getSubscriptionResult() {
        try {
            return subscriptionProcessor.getMatchResult();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Subscription processor thread was interrupted: {0}", e.getMessage());
            return null;
        }
    }

    @Override
    public void processPublication(Publication p) {
        publicationProcessor.addPublication(p);
    }

    @Override
    public Subscription getPublicationResult() {
        try {
            return publicationProcessor.getMatchResult();
        } catch (InterruptedException e) {
            return null;
        }   
    }


    @Override
    public Subscription matchPublication(Publication p) 
    {
        Subscription matched  = super.matchPublication(p);
        
        if (matched != null) {
            logger.log(Level.FINEST, "Adding matching subscription to the queue: {0}", matched.getServiceName());
            publicationProcessor.addMatchResult(matched);
            logger.log(Level.FINEST, "Adding corresponding publication to the subscription processor''s results queue: {0}", p.getServiceName());
            subscriptionProcessor.addMatchResult(p);
        }
        
        return matched; // for debug
    }
    

    @Override
    public Publication cacheLookUp(Subscription s) 
    {   
        Publication matched = super.cacheLookUp(s);
        
        if (matched != null) {
            logger.log(Level.FINEST, "Adding matching publication from cache to the subscription processor''s results queue: {0}", s.getServiceName());
            subscriptionProcessor.addMatchResult(matched);
        }
        
        return matched; // for debug
    }
}
