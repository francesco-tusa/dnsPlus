package broker;

import publishing.Publication;
import subscribing.AsynchronousSubscriber;
import subscribing.Subscription;

public interface AsynchronousBroker extends Broker {
    
    void startProcessing();
    
    void registerSubscriber(AsynchronousSubscriber subscriber);
    
    //returns the first available subscription processing result
    Publication getSubscriptionResult(); 
    
    //returns the first available publication processing result (for debug)
    Subscription getPublicationResult();
    
    void stopProcessing();
}