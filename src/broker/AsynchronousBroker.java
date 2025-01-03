package broker;

import publishing.Publication;
import subscribing.Subscription;

public interface AsynchronousBroker extends SynchronousBroker {
    
    void startProcessing();
    
    //returns the first available subscription processing result
    Publication getSubscriptionResult(); 
    
    //returns the first available publication processing result (for debug)
    Subscription getPublicationResult();
}