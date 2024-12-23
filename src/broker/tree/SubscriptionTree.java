package broker.tree;

import publishing.Publication;
import subscribing.Subscription;

/**
 *
 * @author uceeftu
 */
public interface SubscriptionTree {

    Subscription addNode(Subscription s);

    Subscription search(Publication p);
    
}
