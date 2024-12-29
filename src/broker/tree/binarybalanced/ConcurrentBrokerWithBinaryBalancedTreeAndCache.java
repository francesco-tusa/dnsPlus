package broker.tree.binarybalanced;

import encryption.HEPS;
import publishing.Publication;
import subscribing.Subscription;
import broker.AsynchronousCachingBroker;
import java.util.logging.Logger;
import java.util.logging.Level;
import utils.CustomLogger;

public final class ConcurrentBrokerWithBinaryBalancedTreeAndCache extends BrokerWithBinaryBalancedTreeAndCache implements AsynchronousCachingBroker {
    
    private static final Logger logger = CustomLogger.getLogger(ConcurrentBrokerWithBinaryBalancedTreeAndCache.class.getName());
    private SubscriptionProcessor subscriptionProcessor;
    private PublicationProcessor publicationProcessor;
        
    public ConcurrentBrokerWithBinaryBalancedTreeAndCache(String name, HEPS heps) {
        super(name, heps);
        subscriptionProcessor = new SubscriptionProcessor(this);
        publicationProcessor = new PublicationProcessor(this);
        subscriptionProcessor.startProcessing();
        publicationProcessor.startReceiving();
    }

    public SubscriptionProcessor getSubscriptionProcessor() {
        return subscriptionProcessor;
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
            subscriptionProcessor.stopProcessing();
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
            logger.log(Level.WARNING, "Publication processor thread was interrupted: {0}", e.getMessage());
            publicationProcessor.stopReceiving();
            return null;
        }   
    }


    @Override
    public Subscription matchPublication(Publication p) 
    {
        Subscription matched  = super.matchPublication(p);
        
        if (matched != null) {
            logger.log(Level.FINER, "Adding matching subscription to the queue: {0}", matched.getServiceName());
            publicationProcessor.addMatchResult(matched);
            logger.log(Level.FINER, "Adding corresponding publication to the subscription processor''s results queue: {0}", p.getServiceName());
            subscriptionProcessor.addMatchResult(p);
        }
        
        return matched; // for debug
    }
    

    @Override
    public Publication matchSubscription(Subscription s) 
    {   
        Publication matched = super.matchSubscription(s);
        
        if (matched != null) {
            logger.log(Level.FINER, "Adding matching publication from cache to the subscription processor''s results queue: {0}", s.getServiceName());
            subscriptionProcessor.addMatchResult(matched);
        }
        
        return matched; // for debug
    }
}
