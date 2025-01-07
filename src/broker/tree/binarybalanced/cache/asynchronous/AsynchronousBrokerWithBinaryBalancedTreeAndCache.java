package broker.tree.binarybalanced.cache.asynchronous;

import broker.AsynchronousMeasurementProducerCachingBroker;
import broker.tree.binarybalanced.cache.BrokerWithBinaryBalancedTreeAndCache;
import encryption.HEPS;
import publishing.Publication;
import subscribing.Subscription;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.CustomLogger;
import experiments.measurement.AsynchronousPublicationMeasurementListener;
import experiments.measurement.AsynchronousSubscriptionMeasurementListener;
import java.util.ArrayList;
import java.util.List;
import subscribing.AsynchronousSubscriber;
import experiments.measurement.AsynchronousBrokerMeasurementListener;
import experiments.measurement.BrokerStats;

public final class AsynchronousBrokerWithBinaryBalancedTreeAndCache extends BrokerWithBinaryBalancedTreeAndCache implements AsynchronousMeasurementProducerCachingBroker {
    
    private static final Logger logger = CustomLogger.getLogger(AsynchronousBrokerWithBinaryBalancedTreeAndCache.class.getName());
    private SubscriptionProcessor subscriptionProcessor;
    private PublicationProcessor publicationProcessor;
    private PublicationsDispatcher publicationsDispatcher;
    
    private List<AsynchronousBrokerMeasurementListener> listeners;
    private BrokerStats brokerStats;
        
    public AsynchronousBrokerWithBinaryBalancedTreeAndCache(String name, HEPS heps) {
        super(name, heps);
        subscriptionProcessor = new SubscriptionProcessor(this);
        publicationProcessor = new PublicationProcessor(this);
        publicationsDispatcher = new PublicationsDispatcher(this);
        listeners = new ArrayList<>();
        brokerStats = new BrokerStats();
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
    public void register(AsynchronousSubscriber subscriber) {
        logger.log(Level.INFO, "Registering sub {0}", subscriber.getName());
        publicationsDispatcher.addSubscriber(subscriber);
    }

    
    @Override
    public void startProcessing() {
        subscriptionProcessor.startProcessing();
        publicationProcessor.startProcessing();
        publicationsDispatcher.startDispatching();
    }
    

    @Override
    public void stopProcessing() {
        for (AsynchronousBrokerMeasurementListener l : listeners) {
            l.asynchronousMeasurementPerformed(brokerStats);
        }
    }
    
    
    @Override
    public void addPublicationMeasurementListener(AsynchronousPublicationMeasurementListener listener) {
        publicationProcessor.addPublicationMeasurementListener(listener);
    }

    @Override
    public void addSubscriptionMeasurementListener(AsynchronousSubscriptionMeasurementListener listener) {
        subscriptionProcessor.addSubscriptionMeasurementListener(listener);
    }
    
    @Override
    public void addMeasurementListener(AsynchronousBrokerMeasurementListener l) {
        listeners.add(l);
    }

    @Override
    public void processSubscription(Subscription s) {
        brokerStats.incrementSubscriptions();
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
        brokerStats.incrementPublications();
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
            
            brokerStats.incrementMatches();
            
            logger.log(Level.FINEST, "Adding matching subscription to the queue: {0}", matched.getServiceName());
            publicationProcessor.addMatchResult(matched);
            
            // should find the subscriber(s) from the matching subscription
            // and add them to the publication for later forwarding
            p.setRecipients(matched.getSubscribers());
            
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
            
            brokerStats.incrementCacheHits();
            
            logger.log(Level.FINEST, "Adding matching publication from cache to the subscription processor''s results queue: {0}", s.getServiceName());
            // adding the subscriber(s) to the publication's list of recipients
            matched.setRecipients(s.getSubscribers());
            subscriptionProcessor.addMatchResult(matched);
        }
        
        return matched; // for debug
    }
}
