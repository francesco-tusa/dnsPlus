package broker;

import publishing.Publication;
import subscribing.AsynchronousSubscriber;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public interface SynchronousBroker {

    // TODO should use an interface for subscriber
    void register(AsynchronousSubscriber subscriber);
    
    // call to process a publication
    void processPublication(Publication p);

    // call to process a subscription
    void processSubscription(Subscription s);
    
}
