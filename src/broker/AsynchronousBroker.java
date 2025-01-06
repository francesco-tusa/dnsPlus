package broker;

import publishing.Publication;
import subscribing.AsynchronousSubscriber;
import subscribing.Subscription;

public interface AsynchronousBroker extends Broker {
    
    void startProcessing();
    
        // TODO should use an interface for subscriber
    void register(AsynchronousSubscriber subscriber);
    
    //returns the first available subscription processing result
    Publication getSubscriptionResult(); 
    
    //returns the first available publication processing result (for debug)
    Subscription getPublicationResult();
}