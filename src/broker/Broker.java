package broker;

import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
 public interface Broker extends GenericBroker<Subscription, Publication> {
    
    // add a subscription to the table
    void addSubscription(Subscription s);

    // match a publication with the entries in the
    // subscription table
    Subscription matchPublication(Publication p);
    
    // call to process a publication
    void processPublication(Publication p);

    // call to process a subscription
    void processSubscription(Subscription s);
 }
