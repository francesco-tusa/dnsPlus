package broker.tree.binarybalanced;

import encryption.HEPS;
import publishing.Publication;
import subscribing.Subscription;
import broker.AsynchronousCachingBroker;

public final class ConcurrentBrokerWithBinaryBalancedTreeAndCache extends BrokerWithBinaryBalancedTreeAndCache implements AsynchronousCachingBroker {
    
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
            System.out.println("Subscription processor thread was interrupted: " + e.getMessage());
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
            System.out.println("Publication processor thread was interrupted: " + e.getMessage());
            publicationProcessor.stopReceiving();
            return null;
        }   
    }


    @Override
    public Subscription matchPublication(Publication p) 
    {
        Subscription matched  = super.matchPublication(p);
        
        if (matched != null) {
            System.out.println("Adding matching subscription to the queue: " + matched.getServiceName());
            publicationProcessor.addMatchResult(matched);
            System.out.println("Adding corresponding publication to the subscription processor's results queue: " + p.getServiceName());
            subscriptionProcessor.addMatchResult(p);
        }
        
        return matched; // for debug
    }
    

    @Override
    public Publication matchSubscription(Subscription s) 
    {   
        Publication matched = super.matchSubscription(s);
        
        if (matched != null) {
            System.out.println("Adding matching publication cache entry to the subscription processor's results queue: " + s.getServiceName());
            subscriptionProcessor.addMatchResult(matched);
        }
        
        return matched; // for debug

    }
    
    
    
//    public Publication getMatchingPublication() {
//        
//        // Retrieve the result
//        Publication p = null;
//        try {
//            p = subscriptionProcessor.getMatchResult();
//        } catch (InterruptedException e) {
//            System.out.println("Subscription processor thread was interrupted: " + e.getMessage());
//            subscriptionProcessor.stopProcessing();
//        }
//        
//        if (cacheFound == null) {
//            System.out.println("No entry for " + s.getServiceName() + " found in the cache");
//        } else {
//            System.out.println("Entry for " + s.getServiceName() + " found in the cache: " + cacheFound.getServiceName());
//        }
//
//        return p; 
//    }
    
//    public Subscription getMatchingSubscription() {
//        try {
//            return publicationProcessor.getMatchResult();
//        } catch (InterruptedException e) {
//            System.out.println("Publication processor thread was interrupted: " + e.getMessage());
//            publicationProcessor.stopReceiving();
//            return null;
//        }   
//    }
}
