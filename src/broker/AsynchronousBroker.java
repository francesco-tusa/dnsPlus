package broker;

import publishing.Publication;
import subscribing.Subscription;

public interface AsynchronousBroker {
    
    // async call to process a subscription
    void processSubscription(Subscription s);
    
     // async call to process a publication
    void processPublication(Publication p);
    
    //returns the first available subscription processing result
    Publication getSubscriptionResult(); 
    
    //returns the first available publication processing result (for debug)
    Subscription getPublicationResult();
}