package broker;

import publishing.Publication;
import subscribing.Subscription;

public interface AsynchronousCachingBroker {
    
    void processSubscription(Subscription s);
    //returns the first available subscription processing result
    Publication getSubscriptionResult(); 
    
    void processPublication(Publication p);
    //returns the first available publication processing result (for debug)
    Subscription getPublicationResult(); 
}